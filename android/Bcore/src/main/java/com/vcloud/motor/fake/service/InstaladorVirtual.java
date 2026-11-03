package com.vcloud.motor.fake.service;

import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInstaller;
import android.os.ParcelFileDescriptor;
import android.system.Os;
import android.system.OsConstants;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import com.vcloud.motor.VcloudCore;
import com.vcloud.motor.app.BActivityThread;
import com.vcloud.motor.core.GmsCore;
import com.vcloud.motor.core.env.BEnvironment;
import com.vcloud.motor.entity.pm.InstallOption;
import com.vcloud.motor.entity.pm.InstallResult;
import com.vcloud.motor.utils.Diagnostico;
import com.vcloud.motor.utils.Reflector;
import com.vcloud.motor.utils.Slog;
import com.vcloud.motor.utils.ValorPadrao;

/**
 * Vcloud: instalador de pacotes de MENTIRA para os apps de dentro (Play Store).
 * A Play Store pede ao instalador do celular para criar a instalação, gravar os arquivos e confirmar. Aqui isso vira:
 * pasta em blackbox/instalador/ID (arquivos gravados) -> ao confirmar, o instalador do próprio Vcloud instala o APK dentro.
 * Desinstalar também age só dentro do Vcloud; nunca mexe nos apps do celular.
 * As sessões ficam em arquivos porque a Play Store tem vários processos (principal e :background).
 * shortcut: só o APK base é instalado (splits de config ainda são ignorados), próxima etapa: splits.
 */
public final class InstaladorVirtual {
    private static final String TAG = "InstaladorVirtual";
    public static final Object NAO = new Object();
    // Vcloud: PendingIntents de broadcast que a Play Store criou neste processo (alvo -> intent original). O resultado do commit vai direto
    // pelo broadcast normal (que já alcança os receptores registrados dentro do app), sem passar pela fachada do PendingIntent.
    private static final java.util.Map<android.os.IBinder, Intent> PENDENTES = new java.util.concurrent.ConcurrentHashMap<>();

    public static void lembrar(android.os.IBinder alvo, Intent original) {
        if (alvo != null && original != null) PENDENTES.put(alvo, new Intent(original));
    }
    private static final int PRIMEIRO_ID = 700000;
    private static final List<Object> CALLBACKS = new ArrayList<>();

    private InstaladorVirtual() {}

    private static File raiz() {
        File f = new File(BEnvironment.getVirtualRoot(), "instalador");
        f.mkdirs();
        return f;
    }

    private static File pasta(int id) {
        return new File(raiz(), String.valueOf(id));
    }

    private static boolean existe(int id) {
        return id >= PRIMEIRO_ID && new File(pasta(id), "meta.properties").exists();
    }

    /** Responde as chamadas do IPackageInstaller; devolve NAO se não for com ela. */
    public static Object tratar(Method m, Object[] a) throws Throwable {
        String nome = m.getName();
        Diagnostico.eventoLimitado("instalador|chamada|" + nome, 5_000, "INSTALADOR chamada " + nome);
        switch (nome) {
            case "createSession":
                return criar(a[0]);
            case "openSession": {
                int id = (Integer) a[0];
                if (!existe(id)) return NAO;
                return sessao(m.getReturnType(), id);
            }
            case "getSessionInfo": {
                int id = (Integer) a[0];
                if (!existe(id)) return NAO;
                return info(id);
            }
            case "getAllSessions":
            case "getMySessions":
            case "getStagedSessions":
                return lista(m.getReturnType());
            case "abandonSession": {
                int id = (Integer) a[0];
                if (!existe(id)) return NAO;
                apagar(pasta(id));
                return null;
            }
            case "registerCallback":
                if (a != null && a.length > 0 && a[0] != null) synchronized (CALLBACKS) { CALLBACKS.add(a[0]); }
                return null;
            case "unregisterCallback":
                if (a != null && a.length > 0) synchronized (CALLBACKS) { CALLBACKS.remove(a[0]); }
                return null;
            case "updateSessionAppIcon":
            case "updateSessionAppLabel":
            case "setPermissionsResult":
                return null;
            case "uninstall":
                desinstalar(a);
                return null;
            default:
                return NAO;
        }
    }

    // ---------- sessões ----------

    private static synchronized int criar(Object params) throws Exception {
        String pacote = null, rotulo = null;
        try { pacote = Reflector.with(params).field("appPackageName").get(); } catch (Throwable ignored) { }
        try { Object r = Reflector.with(params).field("appLabel").get(); rotulo = r == null ? null : r.toString(); } catch (Throwable ignored) { }
        int id = PRIMEIRO_ID;
        File dir;
        while (!(dir = pasta(id)).mkdirs()) {
            id++;
            if (id > PRIMEIRO_ID + 100000) throw new java.io.IOException("sem espaço para sessões");
        }
        Properties p = new Properties();
        if (pacote != null) p.setProperty("pacote", pacote);
        if (rotulo != null) p.setProperty("rotulo", rotulo);
        p.setProperty("guest", String.valueOf(BActivityThread.getAppPackageName()));
        try (OutputStream o = new FileOutputStream(new File(dir, "meta.properties"))) { p.store(o, null); }
        Diagnostico.eventoLimitado("instalador|criar|" + id, 1000, "INSTALADOR sessão " + id + " criada para " + pacote);
        return id;
    }

    private static Properties meta(int id) {
        Properties p = new Properties();
        try (InputStream i = new FileInputStream(new File(pasta(id), "meta.properties"))) { p.load(i); } catch (Throwable ignored) { }
        return p;
    }

    private static Object info(int id) {
        try {
            Properties p = meta(id);
            PackageInstaller.SessionInfo si = Reflector.on(PackageInstaller.SessionInfo.class).constructor().newInstance();
            Reflector.with(si).field("sessionId").set(id);
            Reflector.with(si).field("installerPackageName").set(p.getProperty("guest"));
            Reflector.with(si).field("appPackageName").set(p.getProperty("pacote"));
            Reflector.with(si).field("active").set(true);
            return si;
        } catch (Throwable t) {
            Slog.w(TAG, "não consegui montar SessionInfo: " + t);
            return null;
        }
    }

    private static Object lista(Class<?> tipo) {
        List<Object> itens = new ArrayList<>();
        File[] pastas = raiz().listFiles();
        if (pastas != null) {
            for (File f : pastas) {
                try {
                    int id = Integer.parseInt(f.getName());
                    if (existe(id)) { Object i = info(id); if (i != null) itens.add(i); }
                } catch (NumberFormatException ignored) { }
            }
        }
        try {
            return tipo.getConstructor(List.class).newInstance(itens);
        } catch (Throwable t) {
            Slog.w(TAG, "não consegui montar a lista de sessões: " + t);
            return ValorPadrao.de(tipo);
        }
    }

    private static Object sessao(Class<?> tipo, final int id) {
        return Proxy.newProxyInstance(tipo.getClassLoader(), new Class<?>[]{tipo}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method m, Object[] a) throws Throwable {
                try {
                    return sessaoChamada(m, a, id);
                } catch (Throwable t) {
                    Throwable c = t instanceof java.lang.reflect.InvocationTargetException && t.getCause() != null ? t.getCause() : t;
                    Diagnostico.eventoLimitado("instalador|sessao|" + m.getName(), 5000, "INSTALADOR sessão " + id + " " + m.getName() + " falhou: " + c);
                    throw c;
                }
            }
        });
    }

    private static Object sessaoChamada(Method m, Object[] a, int id) throws Throwable {
        File dir = pasta(id);
        Diagnostico.eventoLimitado("instalador|sessmeth|" + m.getName(), 5_000, "INSTALADOR sessão método " + m.getName());
        switch (m.getName()) {
            case "openWrite":
                return abrirEscrita(new File(dir, nomeSeguro((String) a[0])), ((Number) a[1]).longValue());
            case "write": {
                File alvo = new File(dir, nomeSeguro((String) a[0]));
                long deslocamento = ((Number) a[1]).longValue();
                try (ParcelFileDescriptor.AutoCloseInputStream in = new ParcelFileDescriptor.AutoCloseInputStream((ParcelFileDescriptor) a[3]);
                     RandomAccessFile raf = new RandomAccessFile(alvo, "rw")) {
                    raf.seek(deslocamento);
                    byte[] b = new byte[64 * 1024];
                    int n;
                    while ((n = in.read(b)) > 0) raf.write(b, 0, n);
                }
                return null;
            }
            case "openWriteAppMetadata": // Session.setAppMetadata (a Play Store grava os metadados do app antes de confirmar)
                return abrirEscrita(new File(dir, "_app_metadata"), 0);
            case "openRead":
                return ParcelFileDescriptor.open(new File(dir, nomeSeguro((String) a[0])), ParcelFileDescriptor.MODE_READ_ONLY);
            case "getNames": {
                List<String> nomes = new ArrayList<>();
                File[] fs = dir.listFiles();
                if (fs != null) for (File f : fs) if (!f.getName().equals("meta.properties") && !f.getName().startsWith("_")) nomes.add(f.getName());
                return nomes.toArray(new String[0]);
            }
            case "removeSplit":
                new File(dir, nomeSeguro((String) a[0])).delete();
                return null;
            case "commit": {
                IntentSender destino = null;
                for (Object o : a) if (o instanceof IntentSender) destino = (IntentSender) o;
                confirmar(id, destino);
                return null;
            }
            case "abandon":
                apagar(dir);
                return null;
            case "getParentSessionId":
                return -1;
            default:
                return ValorPadrao.de(m.getReturnType());
        }
    }

    private static String nomeSeguro(String nome) {
        // Vcloud: a Play Store confere os nomes que gravou ("config.arm64_v8a", "com.pacote.apk"...) com o getNames(); não mexer neles.
        String n = nome == null ? "base.apk" : new File(nome).getName();
        return n.equals("meta.properties") ? "_meta" : n;
    }

    /** Servidor do "FileBridge" do Android: o cliente escreve comandos (1 grava, 2 fsync, 3 fecha) num socket. */
    private static ParcelFileDescriptor abrirEscrita(File arquivo, long deslocamento) throws Exception {
        FileDescriptor[] par = {new FileDescriptor(), new FileDescriptor()};
        Os.socketpair(OsConstants.AF_UNIX, OsConstants.SOCK_STREAM, 0, par[0], par[1]);
        final FileDescriptor servidor = par[0];
        final RandomAccessFile raf = new RandomAccessFile(arquivo, "rw");
        raf.seek(deslocamento);
        ParcelFileDescriptor cliente = ParcelFileDescriptor.dup(par[1]);
        Os.close(par[1]);
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try (FileInputStream in = new FileInputStream(servidor); FileOutputStream out = new FileOutputStream(servidor)) {
                    byte[] cab = new byte[8];
                    byte[] buf = new byte[64 * 1024];
                    while (lerTudo(in, cab, 8)) {
                        int cmd = ((cab[0] & 255) << 24) | ((cab[1] & 255) << 16) | ((cab[2] & 255) << 8) | (cab[3] & 255);
                        int len = ((cab[4] & 255) << 24) | ((cab[5] & 255) << 16) | ((cab[6] & 255) << 8) | (cab[7] & 255);
                        if (cmd == 1) {
                            while (len > 0) {
                                int n = in.read(buf, 0, Math.min(buf.length, len));
                                if (n < 0) return;
                                raf.write(buf, 0, n);
                                len -= n;
                            }
                        } else if (cmd == 2 || cmd == 3) {
                            raf.getFD().sync();
                            out.write(cab, 0, 8); // devolve o mesmo cabeçalho: o cliente espera
                            if (cmd == 3) return;
                        }
                    }
                } catch (Throwable ignored) {
                } finally {
                    try { raf.close(); } catch (Throwable ignored) { }
                }
            }
        }, "instalador-escrita");
        t.setDaemon(true);
        t.start();
        return cliente;
    }

    private static boolean lerTudo(InputStream in, byte[] b, int n) throws java.io.IOException {
        int lido = 0;
        while (lido < n) {
            int r = in.read(b, lido, n - lido);
            if (r < 0) return false;
            lido += r;
        }
        return true;
    }

    private static File escolherBase(File dir, String pacote) {
        File base = pacote == null ? null : new File(dir, pacote + ".apk");
        if (base != null && base.exists()) return base;
        base = new File(dir, "base.apk");
        if (base.exists()) return base;
        File melhor = null;
        File[] fs = dir.listFiles();
        if (fs != null) for (File f : fs) if (f.getName().endsWith(".apk") && (melhor == null || f.length() > melhor.length())) melhor = f;
        return melhor;
    }

    /** As partes (splits) com bibliotecas nativas (ex.: config.arm64_v8a): copia as .so para a pasta de bibliotecas do app. */
    private static void copiarBibliotecasDasPartes(File dir, File base, String pacote) {
        File[] fs = dir.listFiles();
        if (fs == null) return;
        for (File f : fs) {
            if (f.equals(base) || f.getName().equals("meta.properties") || f.getName().startsWith("_") || f.getName().endsWith(".dm")) continue;
            try {
                com.vcloud.motor.utils.NativeUtils.copyNativeLib(f, BEnvironment.getAppLibDir(pacote));
                Diagnostico.evento("INSTALADOR bibliotecas da parte " + f.getName() + " copiadas para " + pacote);
            } catch (Throwable t) {
                Diagnostico.eventoLimitado("instalador|lib|" + f.getName(), 5000, "INSTALADOR parte " + f.getName() + " sem bibliotecas (" + t + ")");
            }
        }
    }

    private static void confirmar(final int id, final IntentSender destino) {
        final int userId = BActivityThread.getUserId();
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean ok = false;
                String msg = null;
                String pacote = meta(id).getProperty("pacote");
                File dir = pasta(id);
                try {
                    File base = escolherBase(dir, pacote);
                    File[] fs = dir.listFiles();
                    Diagnostico.evento("INSTALADOR sessão " + id + " confirmada: pacote=" + pacote + " arquivos=" + (fs == null ? 0 : fs.length)
                            + (fs == null ? "" : " " + nomes(fs)));
                    if (base == null) {
                        msg = "nenhum APK foi gravado";
                    } else if (pacote != null && GmsCore.isGoogleAppOrService(pacote)) {
                        msg = "o Vcloud não atualiza o Google de dentro por aqui";
                    } else {
                        InstallResult r = VcloudCore.getBPackageManager().installPackageAsUser(base.getAbsolutePath(), InstallOption.installByStorage(), userId);
                        ok = r != null && r.success;
                        msg = r == null ? "o Vcloud não respondeu" : r.msg;
                        if (r != null && r.packageName != null) pacote = r.packageName;
                        if (ok) copiarBibliotecasDasPartes(dir, base, pacote);
                    }
                } catch (Throwable t) {
                    msg = t.toString();
                } finally {
                    Diagnostico.evento("INSTALADOR sessão " + id + (ok ? " instalou " : " NÃO instalou ") + pacote + (msg != null ? " | " + msg : ""));
                    avisar(destino, id, pacote, ok ? PackageInstaller.STATUS_SUCCESS : PackageInstaller.STATUS_FAILURE, msg);
                    notificarSessao(id, ok);
                    apagar(dir);
                }
            }
        }, "instalador-confirmar").start();
    }

    /**
     * No Android de verdade, ao terminar a sessão o sistema (1) chama os callbacks de sessão e (2) manda o broadcast
     * android.content.pm.action.SESSION_UPDATED ao instalador (a Play Store tem receptores para ele). Sem isso ela ficava em "Instalando...".
     */
    private static void notificarSessao(int id, boolean ok) {
        synchronized (CALLBACKS) {
            for (Object cb : new ArrayList<>(CALLBACKS)) {
                try { cb.getClass().getMethod("onSessionFinished", int.class, boolean.class).invoke(cb, id, ok); } catch (Throwable ignored) { }
            }
        }
    }

    private static String nomes(File[] fs) {
        String[] n = new String[fs.length];
        for (int i = 0; i < fs.length; i++) n[i] = fs[i].getName();
        return Arrays.toString(n);
    }

    private static void avisar(IntentSender destino, int id, String pacote, int status, String msg) {
        if (destino == null) return;
        try {
            Intent i = new Intent();
            i.putExtra(PackageInstaller.EXTRA_STATUS, status);
            i.putExtra(PackageInstaller.EXTRA_STATUS_MESSAGE, msg);
            i.putExtra(PackageInstaller.EXTRA_PACKAGE_NAME, pacote);
            i.putExtra(PackageInstaller.EXTRA_SESSION_ID, id);
            Intent original = null;
            try {
                Object alvo = IntentSender.class.getMethod("getTarget").invoke(destino);
                if (alvo instanceof android.os.IInterface) original = PENDENTES.get(((android.os.IInterface) alvo).asBinder());
            } catch (Throwable ignored) { }
            if (original != null) {
                Intent o = new Intent(original);
                o.putExtras(i);
                VcloudCore.getContext().sendBroadcast(o);
                Diagnostico.eventoLimitado("instalador|direto|" + id, 1000, "INSTALADOR resultado enviado por broadcast direto: " + o.getAction());
                return;
            }
            Diagnostico.eventoLimitado("instalador|semmapa|" + id, 1000, "INSTALADOR sem o intent original do PendingIntent; usando sendIntent");
            destino.sendIntent(VcloudCore.getContext(), 0, i, null, null);
            Diagnostico.eventoLimitado("instalador|enviado|" + id, 1000, "INSTALADOR aviso entregue ao IntentSender de " + destino.getCreatorPackage()
                    + " (status " + status + ", sessão " + id + ")");
        } catch (Throwable t) {
            Diagnostico.eventoLimitado("instalador|aviso", 5000, "INSTALADOR não consegui avisar a Play Store: " + t);
        }
    }

    // ---------- desinstalar ----------

    private static void desinstalar(final Object[] a) {
        String pacote = null;
        IntentSender destino = null;
        for (Object o : a) {
            if (o instanceof IntentSender) destino = (IntentSender) o;
            else if (o != null && o.getClass().getName().endsWith("VersionedPackage")) {
                try { pacote = (String) o.getClass().getMethod("getPackageName").invoke(o); } catch (Throwable ignored) { }
            }
        }
        final String alvo = pacote;
        final IntentSender sender = destino;
        final int userId = BActivityThread.getUserId();
        new Thread(new Runnable() {
            @Override
            public void run() {
                int status = PackageInstaller.STATUS_FAILURE_INVALID;
                String msg = "não está instalado dentro do Vcloud";
                try {
                    if (alvo != null && VcloudCore.getBPackageManager().isInstalled(alvo, userId)) {
                        if (GmsCore.isGoogleAppOrService(alvo)) {
                            status = PackageInstaller.STATUS_FAILURE;
                            msg = "o Vcloud não remove o Google de dentro por aqui";
                        } else {
                            VcloudCore.getBPackageManager().uninstallPackageAsUser(alvo, userId);
                            status = PackageInstaller.STATUS_SUCCESS;
                            msg = null;
                        }
                    }
                } catch (Throwable t) {
                    status = PackageInstaller.STATUS_FAILURE;
                    msg = t.toString();
                }
                Diagnostico.evento("INSTALADOR desinstalar " + alvo + " -> status " + status + (msg != null ? " | " + msg : ""));
                avisar(sender, -1, alvo, status, msg);
            }
        }, "instalador-desinstalar").start();
    }

    private static void apagar(File dir) {
        File[] fs = dir.listFiles();
        if (fs != null) for (File f : fs) f.delete();
        dir.delete();
    }
}
