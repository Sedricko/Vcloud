package com.vcloud.app.view.painel

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.vcloud.app.R
import com.vcloud.app.view.main.MainActivity
import com.vcloud.app.view.root.RootManagerActivity
import com.vcloud.app.view.setting.DiagnosticoActivity
import com.vcloud.app.view.setting.SettingActivity
import com.vcloud.motor.VcloudCore
import com.vcloud.motor.core.system.BProcessManagerService
import com.vcloud.motor.utils.Diagnostico
import java.io.File
import kotlin.math.hypot

/**
 * Painel lateral sobre a tela cheia, só enquanto um app do Vcloud está na frente (nunca na tela inicial do celular nem sobre
 * outros apps). Uma aba na borda; tocar abre o painel (Minimizar, Capturar tela, Trocar de app, Configurações, Gerenciador de
 * root, Diagnóstico, Fechar app). Tudo leva a telas do próprio Vcloud: não sai do Vcloud. Roda no processo ":black".
 */
object PainelLateral {
    private class Frente(val pacote: String, val userId: Int)

    private var iniciado = false
    private var pausadoAte = 0L
    private var avisouPermissao = false
    private var aba: View? = null
    private var painel: View? = null
    private var frente: Frente? = null

    /** Só o processo ":black" (o servidor do Vcloud, sempre vivo enquanto há app aberto) cuida do painel. */
    fun iniciarSeForServidor(ctx: Context) {
        val nome = try { File("/proc/self/cmdline").readText().trim('\u0000') } catch (e: Exception) { "" }
        if (nome.endsWith(":black")) iniciar(ctx.applicationContext)
    }

    private fun iniciar(app: Context) {
        if (iniciado) return
        iniciado = true
        val h = Handler(Looper.getMainLooper())
        h.post(object : Runnable {
            override fun run() {
                try { tique(app) } catch (e: Throwable) { Diagnostico.evento("PAINEL erro: $e") }
                h.postDelayed(this, 1000)
            }
        })
    }

    /** A captura de tela esconde a aba por um tempo, para ela não sair na foto. */
    fun pausar(ms: Long) { pausadoAte = SystemClock.elapsedRealtime() + ms }

    // ---------- quem está na frente ----------

    /** Um processo de app do Vcloud (com.vcloud.app:pN) com tela em primeiro plano. O próprio Vcloud (processo principal) não conta. */
    private fun appEmFrente(app: Context): Frente? {
        val am = app.getSystemService(ActivityManager::class.java) ?: return null
        for (p in am.runningAppProcesses.orEmpty()) {
            if (p.processName.startsWith(app.packageName + ":p") && p.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                val rec = BProcessManagerService.get().findProcessByPid(p.pid) ?: continue
                return Frente(rec.packageName, rec.userId)
            }
        }
        return null
    }

    private fun appsAbertos(app: Context): List<Pair<String, Int>> {
        val am = app.getSystemService(ActivityManager::class.java) ?: return emptyList()
        return am.runningAppProcesses.orEmpty()
            .filter { it.processName.startsWith(app.packageName + ":p") }
            .mapNotNull { BProcessManagerService.get().findProcessByPid(it.pid) }
            .map { it.packageName to it.userId }
            .filterNot { it.first.startsWith("com.google.android.") || it.first == "com.android.vending" }
            .distinct()
    }

    /** Qualquer tela do Vcloud em primeiro plano: a principal, a de um app de dentro (com.vcloud.app:pN) ou as do painel. */
    private fun vcloudNaFrente(app: Context): Boolean {
        val am = app.getSystemService(ActivityManager::class.java) ?: return true // sem informação: não esconde nada
        return am.runningAppProcesses.orEmpty().any {
            it.processName.startsWith(app.packageName) && it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
        }
    }

    private fun tique(app: Context) {
        // Os apps de dentro leem este sinal e escondem as janelas flutuantes deles enquanto o Vcloud não está à vista.
        com.vcloud.motor.utils.GuardaSobreposicao.sinalizar(!vcloudNaFrente(app))
        val agora = appEmFrente(app)
        if (agora == null || SystemClock.elapsedRealtime() < pausadoAte) { esconderTudo(app); frente = null; return }
        if (!Settings.canDrawOverlays(app)) {
            if (!avisouPermissao) { avisouPermissao = true; Diagnostico.evento("PAINEL sem a permissão 'Sobrepor a outros apps'") }
            return
        }
        if (agora.pacote != frente?.pacote) { frente = agora; fecharPainel(app) } // trocou de app: o painel aberto era de outro
        if (aba == null) mostrarAba(app)
    }

    // ---------- janelas ----------

    private fun wm(app: Context) = app.getSystemService(WindowManager::class.java)
    private fun dp(app: Context, v: Int) = (v * app.resources.displayMetrics.density).toInt()
    private fun cor(app: Context, id: Int) = ContextCompat.getColor(app, id)
    private fun prefs(app: Context) = app.getSharedPreferences("painel_lateral", Context.MODE_PRIVATE)
    private fun direita(app: Context) = prefs(app).getBoolean("direita", true)

    private fun parametros(w: Int, h: Int, flags: Int) = WindowManager.LayoutParams(
        w, h, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, flags, PixelFormat.TRANSLUCENT
    )

    private fun esconderTudo(app: Context) {
        fecharPainel(app)
        aba?.let { try { wm(app).removeView(it) } catch (e: Exception) { /* já saiu */ } }
        aba = null
    }

    /** Cantos arredondados só do lado de dentro, como na amostra. */
    private fun cantos(app: Context, dir: Boolean, r: Int): FloatArray {
        val x = dp(app, r).toFloat()
        return if (dir) floatArrayOf(x, x, 0f, 0f, 0f, 0f, x, x) else floatArrayOf(0f, 0f, x, x, x, x, 0f, 0f)
    }

    private fun mostrarAba(app: Context) {
        val dir = direita(app)
        val pilula = View(app).apply {
            background = GradientDrawable().apply {
                setColor(0xA6DCE4F5.toInt()); setStroke(dp(app, 1), 0x66FFFFFF)
                cornerRadii = cantos(app, dir, 8)
            }
        }
        val v = FrameLayout(app).apply {
            addView(pilula, FrameLayout.LayoutParams(dp(app, 11), dp(app, 64), (if (dir) Gravity.END else Gravity.START) or Gravity.CENTER_VERTICAL))
        }
        val lp = parametros(dp(app, 28), dp(app, 84),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        ).apply {
            gravity = Gravity.TOP or (if (dir) Gravity.END else Gravity.START)
            y = prefs(app).getInt("y", dp(app, 300))
        }
        v.setOnTouchListener(arraste(app, lp))
        wm(app).addView(v, lp)
        aba = v
    }

    /** Arrastar muda a altura (e o lado, se soltar na outra metade da tela); só tocar abre o painel. */
    private fun arraste(app: Context, lp: WindowManager.LayoutParams): View.OnTouchListener {
        var x0 = 0f; var y0 = 0f; var yIni = 0; var moveu = false
        val folga = ViewConfiguration.get(app).scaledTouchSlop
        return View.OnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { x0 = e.rawX; y0 = e.rawY; yIni = lp.y; moveu = false; true }
                MotionEvent.ACTION_MOVE -> {
                    if (!moveu && hypot(e.rawX - x0, e.rawY - y0) > folga) moveu = true
                    if (moveu) {
                        val max = app.resources.displayMetrics.heightPixels - lp.height
                        lp.y = (yIni + (e.rawY - y0)).toInt().coerceIn(0, max.coerceAtLeast(0))
                        try { wm(app).updateViewLayout(v, lp) } catch (ex: Exception) { /* janela já saiu */ }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moveu) abrirPainel(app)
                    else {
                        prefs(app).edit().putInt("y", lp.y).putBoolean("direita", e.rawX > app.resources.displayMetrics.widthPixels / 2f).apply()
                        esconderTudo(app) // volta no próximo tique, já do lado certo
                    }
                    true
                }
                else -> false
            }
        }
    }

    // ---------- painel aberto ----------

    private fun fecharPainel(app: Context) {
        painel?.let { try { wm(app).removeView(it) } catch (e: Exception) { /* já saiu */ } }
        painel = null
        aba?.visibility = View.VISIBLE
    }

    private fun abrirPainel(app: Context) {
        val f = frente ?: return
        if (painel != null) return
        val dir = direita(app)
        val raiz = FrameLayout(app).apply {
            setBackgroundColor(0x59080C1A) // escurece o jogo atrás, como na amostra
            setOnTouchListener { _, e -> if (e.action == MotionEvent.ACTION_DOWN) fecharPainel(app); true }
        }
        val caixa = LinearLayout(app).apply {
            orientation = LinearLayout.VERTICAL
            isClickable = true // engole o toque: só fora da caixa fecha
            setPadding(dp(app, 12), dp(app, 12), dp(app, 12), dp(app, 12))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(cor(app, R.color.glass_start), cor(app, R.color.glass_end))).apply {
                setStroke(dp(app, 1), cor(app, R.color.glass_stroke)); cornerRadii = cantos(app, dir, 22)
            }
        }
        raiz.addView(caixa, FrameLayout.LayoutParams(dp(app, 224), FrameLayout.LayoutParams.WRAP_CONTENT,
            (if (dir) Gravity.END else Gravity.START) or Gravity.TOP).apply { topMargin = dp(app, 130) })
        wm(app).addView(raiz, parametros(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS))
        painel = raiz
        aba?.visibility = View.GONE
        menu(app, caixa, f)
    }

    private fun texto(app: Context, t: String, sp: Float, negrito: Boolean = false, dim: Boolean = false) = TextView(app).apply {
        text = t; textSize = sp
        setTextColor(cor(app, if (dim) R.color.glass_text_dim else R.color.glass_text))
        if (negrito) setTypeface(typeface, Typeface.BOLD)
    }

    private fun linha(app: Context, glifo: String, rotulo: String, acao: () -> Unit) = LinearLayout(app).apply {
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(app, 6), dp(app, 7), dp(app, 6), dp(app, 7))
        addView(TextView(app).apply {
            text = glifo; textSize = 15f; gravity = Gravity.CENTER; setTypeface(typeface, Typeface.BOLD)
            setTextColor(cor(app, R.color.glass_text))
            background = GradientDrawable().apply {
                setColor(cor(app, R.color.tile_bg)); setStroke(dp(app, 1), cor(app, R.color.tile_stroke)); cornerRadius = dp(app, 10).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(dp(app, 34), dp(app, 34)).apply { rightMargin = dp(app, 12) }
        })
        addView(texto(app, rotulo, 14f))
        setOnClickListener { acao() }
    }

    private fun nomeDoApp(pacote: String, userId: Int): String = try {
        VcloudCore.get().getInstalledApplications(0, userId).orEmpty().firstOrNull { it.packageName == pacote }
            ?.loadLabel(VcloudCore.getPackageManager())?.toString() ?: pacote
    } catch (e: Exception) { pacote }

    private fun menu(app: Context, caixa: LinearLayout, f: Frente) {
        caixa.removeAllViews()
        val cab = LinearLayout(app).apply {
            setPadding(dp(app, 6), 0, dp(app, 6), dp(app, 8))
            addView(texto(app, "Vcloud", 15f, negrito = true).apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) })
            addView(texto(app, nomeDoApp(f.pacote, f.userId), 12f, dim = true).apply { maxLines = 1 })
        }
        caixa.addView(cab)
        caixa.addView(linha(app, "—", "Minimizar") { voltarAoVcloud(app) })
        caixa.addView(linha(app, "▢", "Capturar tela") { capturar(app) })
        caixa.addView(linha(app, "⇄", "Trocar de app") { trocar(app, caixa, f) })
        caixa.addView(linha(app, "⚙", "Configurações") { abrirTela(app, SettingActivity::class.java) })
        caixa.addView(linha(app, "#", "Gerenciador de root") { abrirTela(app, RootManagerActivity::class.java) })
        caixa.addView(linha(app, "!", "Diagnóstico") { abrirTela(app, DiagnosticoActivity::class.java) })
        caixa.addView(View(app).apply {
            setBackgroundColor(cor(app, R.color.glass_stroke))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(app, 1)).apply { topMargin = dp(app, 6); bottomMargin = dp(app, 6) }
        })
        caixa.addView(linha(app, "✕", "Fechar app") { confirmarFechar(app, caixa, f) })
    }

    private fun trocar(app: Context, caixa: LinearLayout, f: Frente) {
        caixa.removeAllViews()
        caixa.addView(linha(app, "‹", "Voltar") { menu(app, caixa, f) })
        val abertos = appsAbertos(app)
        if (abertos.isEmpty()) caixa.addView(texto(app, "Nenhum outro app aberto.", 13f, dim = true).apply { setPadding(dp(app, 6), dp(app, 8), 0, 0) })
        for ((pacote, userId) in abertos) {
            val atual = pacote == f.pacote
            caixa.addView(linha(app, if (atual) "●" else "○", nomeDoApp(pacote, userId)) {
                fecharPainel(app)
                if (!atual) Thread { try { VcloudCore.get().launchApk(pacote, userId) } catch (e: Throwable) { Diagnostico.evento("PAINEL trocar falhou: $e") } }.start()
            })
        }
    }

    private fun confirmarFechar(app: Context, caixa: LinearLayout, f: Frente) {
        caixa.removeAllViews()
        caixa.addView(texto(app, "Fechar ${nomeDoApp(f.pacote, f.userId)}?", 15f, negrito = true).apply { setPadding(dp(app, 6), 0, dp(app, 6), dp(app, 4)) })
        caixa.addView(texto(app, "O que não foi salvo no app pode ser perdido.", 12f, dim = true).apply { setPadding(dp(app, 6), 0, dp(app, 6), dp(app, 8)) })
        caixa.addView(linha(app, "✕", "Fechar") {
            voltarAoVcloud(app) // antes de matar: assim não cai na tela inicial do celular
            Handler(Looper.getMainLooper()).postDelayed({
                try { VcloudCore.get().stopPackage(f.pacote, f.userId) } catch (e: Throwable) { Diagnostico.evento("PAINEL fechar falhou: $e") }
            }, 400)
        })
        caixa.addView(linha(app, "‹", "Cancelar") { menu(app, caixa, f) })
    }

    // ---------- ações: todas ficam dentro do Vcloud ----------

    // O Android só deixa um serviço em segundo plano abrir tela enquanto a janela sobre a tela está visível:
    // por isso a tela abre ANTES de o painel fechar.
    private fun abrirTela(app: Context, tela: Class<*>) {
        app.startActivity(Intent(app, tela).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        fecharPainel(app)
    }

    private fun voltarAoVcloud(app: Context) {
        app.startActivity(Intent(app, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        fecharPainel(app)
    }

    private fun capturar(app: Context) {
        app.startActivity(Intent(app, CapturaActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        pausar(15_000) // a aba some até a captura sair (o Android pede a permissão de captura antes)
        fecharPainel(app)
    }
}
