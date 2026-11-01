package com.vcloud.app.view.root

import android.app.AlertDialog
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import com.vcloud.app.view.base.BaseActivity
import com.vcloud.motor.VcloudCore
import com.vcloud.motor.core.root.RootPolicy
import com.vcloud.motor.core.root.RootPolicy.Estado

/**
 * Gerenciador de root do Vcloud: chave geral, quem pode usar (Perguntar / Permitido / Negado) e o registro dos pedidos.
 * O root só existe dentro do Vcloud; aqui você decide quem usa.
 */
class RootManagerActivity : BaseActivity() {
    private val politica by lazy { RootPolicy.padrao() }
    private lateinit var lista: LinearLayout
    private lateinit var registro: TextView
    private lateinit var geral: Switch

    private fun px(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val titulo = TextView(this).apply { text = "Gerenciador de root"; textSize = 20f; setTypeface(typeface, Typeface.BOLD) }
        val aviso = TextView(this).apply {
            text = "O root vale só dentro do Vcloud, não no celular. Depois de mudar, feche e abra o app de novo."
            textSize = 13f; setPadding(0, px(4), 0, px(8))
        }
        geral = Switch(this).apply {
            text = "Root do Vcloud (chave geral)"
            isChecked = politica.geralLigado()
            setOnCheckedChangeListener { _, ligado -> politica.ligarGeral(ligado) }
        }
        lista = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        registro = TextView(this).apply { typeface = Typeface.MONOSPACE; textSize = 12f; setTextIsSelectable(true) }

        fun botao(nome: String, acao: () -> Unit) = Button(this).apply {
            text = nome; isAllCaps = false; setOnClickListener { acao() }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val barra = LinearLayout(this).apply {
            addView(botao("Atualizar") { carregar() })
            addView(botao("Limpar registro") { politica.limparRegistro(); carregar() })
        }
        fun secao(t: String) = TextView(this).apply { text = t; textSize = 16f; setTypeface(typeface, Typeface.BOLD); setPadding(0, px(16), 0, px(4)) }

        val pad = px(16)
        setContentView(ScrollView(this).apply {
            addView(LinearLayout(this@RootManagerActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(pad, pad, pad, pad)
                addView(titulo); addView(aviso); addView(geral)
                addView(secao("Quem pode usar root")); addView(lista)
                addView(secao("Pedidos recentes")); addView(barra); addView(registro)
            })
        })
    }

    override fun onResume() {
        super.onResume()
        carregar()
    }

    private fun carregar() {
        registro.text = politica.lerRegistro().lines().filter { it.isNotBlank() }.takeLast(40).reversed()
            .joinToString("\n").ifEmpty { "Nenhum pedido ainda." }
        Thread {
            val apps = try {
                VcloudCore.get().getInstalledApplications(0, 0).orEmpty()
                    .filterNot { it.packageName.startsWith("com.google.android.") || it.packageName == "com.android.vending" }
                    .map { ai ->
                    val nome = try { ai.loadLabel(VcloudCore.getPackageManager()).toString() } catch (e: Exception) { ai.packageName }
                    nome to ai.packageName
                }.sortedBy { it.first.lowercase() }
            } catch (e: Exception) {
                emptyList()
            }
            runOnUiThread { mostrar(apps) }
        }.start()
    }

    private fun mostrar(apps: List<Pair<String, String>>) {
        lista.removeAllViews()
        if (apps.isEmpty()) {
            lista.addView(TextView(this).apply { text = "Nenhum app dentro do Vcloud ainda." })
            return
        }
        for ((nome, pacote) in apps) {
            if (!RootPolicy.pacoteValido(pacote)) continue
            val estado = politica.estado(pacote)
            val linha = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(0, px(6), 0, px(6)) }
            val textos = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                addView(TextView(context).apply { text = nome; textSize = 15f })
                addView(TextView(context).apply { text = pacote; textSize = 11f; alpha = 0.7f })
            }
            val botao = Button(this).apply {
                text = rotulo(estado); isAllCaps = false
                setOnClickListener { escolher(nome, pacote) }
            }
            linha.addView(textos); linha.addView(botao)
            lista.addView(linha)
        }
    }

    private fun rotulo(e: Estado) = when (e) {
        Estado.PERMITIDO -> "Permitido"
        Estado.NEGADO -> "Negado"
        Estado.PERGUNTAR -> "Perguntar"
    }

    private fun escolher(nome: String, pacote: String) {
        val opcoes = arrayOf("Perguntar a cada vez", "Permitir", "Negar")
        val estados = arrayOf(Estado.PERGUNTAR, Estado.PERMITIDO, Estado.NEGADO)
        AlertDialog.Builder(this)
            .setTitle("Root para $nome")
            .setSingleChoiceItems(opcoes, estados.indexOf(politica.estado(pacote))) { d, i ->
                politica.definir(pacote, estados[i])
                d.dismiss()
                carregar()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
