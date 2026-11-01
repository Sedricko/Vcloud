package com.vcloud.app.view.setting

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Typeface
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.vcloud.app.util.botaoTema
import com.vcloud.app.util.textoTema
import com.vcloud.app.view.base.BaseActivity
import com.vcloud.motor.VcloudCore
import com.vcloud.motor.utils.Diagnostico

/**
 * Central de diagnóstico: o único lugar de erros e relatório do Vcloud.
 * Mostra o histórico dos apps (abriu / não abriu / fechou e por quê, mais novo em cima) e gera o relatório.
 * Herda de BaseActivity para respeitar as barras do sistema (Android 15+ desenha por baixo delas).
 */
class DiagnosticoActivity : BaseActivity() {
    private lateinit var resumo: TextView
    private lateinit var texto: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dp = resources.displayMetrics.density
        fun px(v: Int) = (v * dp).toInt()

        val titulo = textoTema("Central de diagnóstico", 20f, negrito = true)
        resumo = textoTema("", 13f, secundario = true).apply { setPadding(0, px(4), 0, px(8)) }
        texto = textoTema("", 12f).apply { typeface = Typeface.MONOSPACE; setTextIsSelectable(true) }

        fun botao(nome: String, acao: () -> Unit) = botaoTema(nome, acao = acao).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = px(6) }
        }
        val linha1 = LinearLayout(this).apply {
            addView(botao("Atualizar") { mostrar() })
            addView(botao("Copiar") {
                (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager)
                    .setPrimaryClip(ClipData.newPlainText("diagnostico", Diagnostico.ler()))
                resumo.text = "Histórico copiado."
            })
        }
        val linha2 = LinearLayout(this).apply {
            addView(botao("Testar Google") { testarGoogle() })
            addView(botao("Gerar relatório") { gerarRelatorio() })
            addView(botao("Limpar histórico") { Diagnostico.limpar(); mostrar() })
        }
        val pad = px(16)
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
            addView(titulo); addView(resumo); addView(linha1); addView(linha2)
            addView(ScrollView(this@DiagnosticoActivity).apply { addView(texto) },
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        })
    }

    override fun onResume() {
        super.onResume()
        mostrar()
    }

    private fun testarGoogle() {
        resumo.text = "Testando o Google..."
        Thread {
            val txt = try { com.vcloud.motor.utils.ProbeGoogle.relatorio(applicationContext) } catch (e: Throwable) { "Falhou: $e" }
            com.vcloud.motor.utils.Diagnostico.evento("GOOGLE teste do Vcloud:\n" + txt.trimEnd().lines().joinToString("\n") { "    $it" })
            runOnUiThread { mostrar(); resumo.text = "Teste do Google salvo no histórico." }
        }.start()
    }

    private fun gerarRelatorio() {
        resumo.text = "Gerando relatório..."
        Thread {
            val msg = try {
                "Relatório salvo em " + VcloudCore.get().writeReport("gerado na Central de diagnóstico").absolutePath
            } catch (e: Exception) {
                "Não consegui salvar o relatório: ${e.message}"
            }
            runOnUiThread { resumo.text = msg }
        }.start()
    }

    private fun mostrar() {
        // Cada evento começa com data; junta as linhas recuadas ao evento e mostra do mais novo para o mais velho.
        val eventos = ArrayList<String>()
        for (l in Diagnostico.ler().lines()) {
            if (l.startsWith(" ") && eventos.isNotEmpty()) eventos[eventos.size - 1] += "\n$l"
            else if (l.isNotBlank()) eventos.add(l)
        }
        val quedas = eventos.count { it.contains("  FECHOU  ") || it.contains("  NÃO ABRIU  ") }
        resumo.text = if (eventos.isEmpty()) "Nenhum registro ainda."
        else "${eventos.size} registros, $quedas fechamentos ou falhas. Relatórios ficam em Download/Vcloud."
        texto.text = if (eventos.isEmpty()) "Abra um app dentro do Vcloud e volte aqui."
        else eventos.reversed().joinToString("\n\n")
    }
}
