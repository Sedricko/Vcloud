package com.vcloud.motor.core.root;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Credentials;
import android.net.LocalServerSocket;
import android.net.LocalSocket;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import com.vcloud.motor.VcloudCore;
import com.vcloud.motor.core.system.BProcessManagerService;
import com.vcloud.motor.core.system.ProcessRecord;
import com.vcloud.motor.utils.Diagnostico;

/**
 * Servidor do root interno: escuta o socket "vcloud_su" (o "su" do Vcloud conecta aqui), descobre QUAL app pediu
 * pelo processo que está do outro lado (não pelo que o app diz), aplica a política do painel e responde ALLOW/DENY.
 */
public final class RootServer {
    public static final String SOCKET = "vcloud_su";
    private static final long ESPERA_USUARIO_MS = 30_000;
    private static boolean iniciado;

    private RootServer() {}

    public static synchronized void start() {
        if (iniciado) return;
        iniciado = true;
        Thread t = new Thread(RootServer::laco, "vcloud-root");
        t.setDaemon(true);
        t.start();
    }

    private static void laco() {
        while (true) {
            try (LocalServerSocket servidor = new LocalServerSocket(SOCKET)) {
                Diagnostico.evento("ROOT servidor pronto (socket " + SOCKET + ")");
                while (true) {
                    final LocalSocket c = servidor.accept();
                    Thread t = new Thread(() -> atender(c), "vcloud-root-pedido");
                    t.setDaemon(true);
                    t.start();
                }
            } catch (Throwable e) {
                Diagnostico.evento("ROOT servidor caiu: " + e + " (tenta de novo em 3 s)");
                try { Thread.sleep(3000); } catch (InterruptedException ie) { return; }
            }
        }
    }

    private static void atender(LocalSocket c) {
        try (LocalSocket socket = c) {
            BufferedReader r = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            RootDecisor.Pedido pedido = RootDecisor.Pedido.ler(r);
            Credentials cred = socket.getPeerCredentials();
            if (cred.getUid() != android.os.Process.myUid()) { // outro app do celular tentando usar o socket abstrato
                socket.getOutputStream().write("DENY origem nao permitida\n".getBytes(StandardCharsets.UTF_8));
                Diagnostico.evento("ROOT pedido recusado: uid " + cred.getUid() + " não é do Vcloud");
                return;
            }
            String pkg = pacoteDoPid(cred.getPid());
            if (pkg == null && RootPolicy.pacoteValido(pedido.dicaPacote) && VcloudCore.get().isInstalled(pedido.dicaPacote, 0)) {
                pkg = pedido.dicaPacote; // processo que perdeu o pai (daemon): usa a dica do su, só se o app existe no Vcloud
            }
            String resposta = RootDecisor.decidir(RootPolicy.padrao(), pkg, pedido.comando, RootServer::perguntar);
            OutputStream o = socket.getOutputStream();
            o.write((resposta + "\n").getBytes(StandardCharsets.UTF_8));
            o.flush();
            Diagnostico.evento("ROOT " + pkg + ": " + resposta + " (" + pedido.comando + ")");
            avisar(RootPolicy.padrao().aviso(), pkg, resposta);
        } catch (Throwable e) {
            Diagnostico.evento("ROOT pedido falhou: " + e);
        }
    }

    /** Sobe pela árvore de processos (o su é filho do app) até achar um processo que o Vcloud iniciou. */
    static String pacoteDoPid(int pid) {
        for (int i = 0; i < 12 && pid > 1; i++) {
            ProcessRecord rec = BProcessManagerService.get().findProcessByPid(pid);
            if (rec != null) return rec.getPackageName();
            try {
                pid = RootDecisor.ppidDe(new String(Files.readAllBytes(new File("/proc/" + pid + "/status").toPath()), StandardCharsets.UTF_8));
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private static void avisar(RootPolicy.Aviso modo, String pkg, String resposta) {
        if (modo == RootPolicy.Aviso.NENHUM) return;
        final String texto = (pkg == null ? "App desconhecido" : pkg) + (resposta.startsWith("ALLOW") ? " usou root" : " teve o root negado");
        if (modo == RootPolicy.Aviso.MENSAGEM) {
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                try {
                    android.widget.Toast.makeText(VcloudCore.getContext(), texto, android.widget.Toast.LENGTH_SHORT).show();
                } catch (Throwable ignored) {
                    // sem tela para mostrar: o registro já tem o pedido
                }
            });
            return;
        }
        try { // NOTIFICACAO (precisa da permissão de notificações; sem ela o Android só não mostra)
            Context c = VcloudCore.getContext();
            android.app.NotificationManager nm = (android.app.NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            android.app.Notification.Builder b = android.os.Build.VERSION.SDK_INT >= 26
                    ? new android.app.Notification.Builder(c, "vcloud_root") : new android.app.Notification.Builder(c);
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                nm.createNotificationChannel(new android.app.NotificationChannel("vcloud_root", "Pedidos de root",
                        android.app.NotificationManager.IMPORTANCE_LOW));
            }
            b.setSmallIcon(android.R.drawable.stat_notify_sync_noanim).setContentTitle("Root do Vcloud")
                    .setContentText(texto).setAutoCancel(true);
            nm.notify(0x52000000 | (pkg == null ? 0 : pkg.hashCode() & 0xFFFF), b.build());
        } catch (Throwable e) {
            Diagnostico.evento("ROOT não consegui mostrar a notificação: " + e);
        }
    }

    private static String perguntar(String pkg, String comando) {
        try {
            Context ctx = VcloudCore.getContext();
            Intent i = new Intent().setComponent(new ComponentName(ctx.getPackageName(), "com.vcloud.app.view.root.RootPromptActivity"))
                    .putExtra("pkg", pkg).putExtra("cmd", comando)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            ctx.startActivity(i);
        } catch (Throwable e) {
            Diagnostico.evento("ROOT não consegui abrir a janela de pergunta: " + e);
            return null;
        }
        return RootPolicy.padrao().aguardarResposta(pkg, ESPERA_USUARIO_MS);
    }
}
