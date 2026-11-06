package com.vcloud.motor.utils;

import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;

import com.vcloud.motor.VcloudCore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Vcloud: o analisador de pacote do motor devolve as telas SEM os intent-filters no Android 17 (a busca por "tela principal" voltava vazia e a
 * loja não achava o app). Lê os filtros direto do AndroidManifest.xml do APK. Cache por APK.
 */
public class ManifestFiltros {
    private static final Map<String, Map<String, List<IntentFilter>>> CACHE = new HashMap<>();
    private static final Map<String, Map<String, String>> ALIAS = new HashMap<>();

    /** Tela de verdade por trás de um activity-alias (o ícone do Chrome é o alias "...apps.chrome.Main"); null se não for alias. */
    public static synchronized String alvoDoAlias(ApplicationInfo ai, String nome) {
        if (ai == null || ai.sourceDir == null) return null;
        telas(ai);
        Map<String, String> m = ALIAS.get(ai.sourceDir + "|" + new java.io.File(ai.sourceDir).lastModified());
        return m == null ? null : m.get(nome);
    }

    /** nome da classe da tela -> filtros. Nomes relativos (".Main") já vêm completos com o pacote. */
    public static synchronized Map<String, List<IntentFilter>> telas(ApplicationInfo ai) {
        if (ai == null || ai.sourceDir == null) return new HashMap<>();
        String chave = ai.sourceDir + "|" + new java.io.File(ai.sourceDir).lastModified(); // APK trocado (atualização) = cache novo
        Map<String, List<IntentFilter>> c = CACHE.get(chave);
        if (c != null) return c;
        c = new HashMap<>();
        Map<String, String> alias = new HashMap<>();
        ALIAS.put(chave, alias);
        try {
            Resources r = VcloudCore.getPackageManager().getResourcesForApplication(ai);
            XmlResourceParser p = r.getAssets().openXmlResourceParser("AndroidManifest.xml");
            String tela = null;
            IntentFilter f = null;
            int ev;
            while ((ev = p.next()) != XmlResourceParser.END_DOCUMENT) {
                if (ev == XmlResourceParser.START_TAG) {
                    String t = p.getName();
                    if (t.equals("activity") || t.equals("activity-alias")) {
                        String n = p.getAttributeValue("http://schemas.android.com/apk/res/android", "name");
                        if (n != null && n.startsWith(".")) n = ai.packageName + n;
                        else if (n != null && !n.contains(".")) n = ai.packageName + "." + n;
                        tela = n;
                        if (t.equals("activity-alias") && n != null) {
                            String alvo = p.getAttributeValue("http://schemas.android.com/apk/res/android", "targetActivity");
                            if (alvo != null && alvo.startsWith(".")) alvo = ai.packageName + alvo;
                            else if (alvo != null && !alvo.contains(".")) alvo = ai.packageName + "." + alvo;
                            if (alvo != null) alias.put(n, alvo);
                        }
                    } else if (t.equals("intent-filter") && tela != null) {
                        f = new IntentFilter();
                    } else if (f != null && t.equals("action")) {
                        String a = p.getAttributeValue("http://schemas.android.com/apk/res/android", "name");
                        if (a != null) f.addAction(a);
                    } else if (f != null && t.equals("category")) {
                        String a = p.getAttributeValue("http://schemas.android.com/apk/res/android", "name");
                        if (a != null) f.addCategory(a);
                    }
                } else if (ev == XmlResourceParser.END_TAG) {
                    String t = p.getName();
                    if (t.equals("intent-filter") && tela != null && f != null) {
                        List<IntentFilter> l = c.get(tela);
                        if (l == null) { l = new ArrayList<>(); c.put(tela, l); }
                        l.add(f);
                        f = null;
                    } else if (t.equals("activity") || t.equals("activity-alias")) {
                        tela = null;
                    }
                }
            }
            p.close();
        } catch (Throwable t) {
            Diagnostico.eventoLimitado("manifestfiltros|" + ai.packageName, 30_000, "RESOLVEDOR não consegui ler os filtros de " + ai.packageName + ": " + t);
        }
        CACHE.put(chave, c);
        return c;
    }
}
