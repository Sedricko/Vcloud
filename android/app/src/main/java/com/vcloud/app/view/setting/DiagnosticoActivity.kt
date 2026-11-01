package com.vcloud.app.view.setting

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Typeface
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.vcloud.motor.VcloudCore
import com.vcloud.motor.utils.Diagnostico

/** Diário interno: o que abriu, o que não abriu e por que fechou (mais novo em cima). */
class DiagnosticoActivity : AppCompatActivity() {
    private lateinit var texto: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Diagnóstico"
        texto = TextView(this).apply { typeface = Typeface.MONOSPACE; textSize = 12f; setTextIsSelectable(true) }
        fun botao(nome: String, acao: () -> Unit) = Button(this).apply {
            text = nome; setOnClickListener { acao() }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val barra = LinearLayout(this).apply {
            addView(botao("Atualizar") { mostrar() })
            addView(botao("Copiar") {
                (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager)
                    .setPrimaryClip(ClipData.newPlainText("diagnostico", Diagnostico.ler()))
            })
            addView(botao("Relatório") {
                VcloudCore.get().sendLogs("Pedido na tela Diagnóstico", true)
                texto.text = "Gerando relatório em Download/Vcloud...\n\n" + texto.text
            })
            addView(botao("Limpar") { Diagnostico.limpar(); mostrar() })
        }
        val pad = (16 * resources.displayMetrics.density).toInt()
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
            addView(barra)
            addView(ScrollView(this@DiagnosticoActivity).apply { addView(texto) })
        })
    }

    override fun onResume() {
        super.onResume()
        mostrar()
    }

    private fun mostrar() {
        // Cada evento começa com data; junta as linhas recuadas ao evento e mostra do mais novo para o mais velho.
        val eventos = ArrayList<String>()
        for (l in Diagnostico.ler().lines()) {
            if (l.startsWith(" ") && eventos.isNotEmpty()) eventos[eventos.size - 1] += "\n$l"
            else if (l.isNotBlank()) eventos.add(l)
        }
        texto.text = if (eventos.isEmpty()) "Nada registrado ainda. Abra um app dentro do Vcloud e volte aqui."
        else eventos.reversed().joinToString("\n\n")
    }
}
