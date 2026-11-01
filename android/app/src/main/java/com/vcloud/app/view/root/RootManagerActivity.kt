package com.vcloud.app.view.root

import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.vcloud.app.view.base.BaseActivity
import com.vcloud.motor.VcloudCore
import com.vcloud.motor.core.root.RootPolicy
import com.vcloud.motor.core.root.RootPolicy.Estado

/**
 * Gerenciador de root do Vcloud: chave geral, resposta automática (responder todos da mesma maneira), aviso na tela,
 * quem pode usar (chave por app; tocar no app dá as 3 opções) e o registro dos pedidos.
 * O root só existe dentro do Vcloud; aqui você decide quem usa.
 */
class RootManagerActivity : BaseActivity() {
    private class AppRoot(val nome: String, val pacote: String, val icone: Drawable?)

    private val politica by lazy { RootPolicy.padrao() }
    private lateinit var lista: LinearLayout
    private lateinit var registro: TextView
    private lateinit var automatico: TextView

    private fun px(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun texto(t: String, tamanho: Float, negrito: Boolean = false) = TextView(this).apply {
        text = t; textSize = tamanho; if (negrito) setTypeface(typeface, Typeface.BOLD)
    }

    private fun secao(t: String) = texto(t, 16f, true).apply { setPadding(0, px(18), 0, px(4)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val aviso = texto("O root vale só dentro do Vcloud, não no celular. Depois de mudar, feche e abra o app de novo.", 13f)
            .apply { setPadding(0, px(4), 0, px(8)) }

        val geral = Switch(this).apply {
            text = "Root do Vcloud (chave geral)"
            isChecked = politica.geralLigado()
            setOnCheckedChangeListener { _, ligado -> politica.ligarGeral(ligado) }
        }
        val avisoTela = Switch(this).apply {
            text = "Avisar na tela quando um app usar root"
            isChecked = politica.avisoLigado()
            setPadding(0, px(10), 0, 0)
            setOnCheckedChangeListener { _, ligado -> politica.ligarAviso(ligado) }
        }

        automatico = texto("", 14f).apply { setPadding(0, px(4), 0, px(4)) }
        val trocarAuto = Button(this).apply {
            text = "Resposta automática"; isAllCaps = false
            setOnClickListener { escolherAutomatico() }
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

        val pad = px(16)
        setContentView(ScrollView(this).apply {
            addView(LinearLayout(this@RootManagerActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(pad, pad, pad, pad)
                addView(texto("Gerenciador de root", 20f, true)); addView(aviso); addView(geral); addView(avisoTela)
                addView(secao("Responder todos da mesma maneira")); addView(automatico); addView(trocarAuto)
                addView(secao("Apps")); addView(lista)
                addView(secao("Pedidos recentes")); addView(barra); addView(registro)
            })
        })
    }

    override fun onResume() {
        super.onResume()
        carregar()
    }

    private fun rotuloAuto(e: Estado) = when (e) {
        Estado.PERMITIDO -> "Permitir todos os apps (que não tenham decisão própria)"
        Estado.NEGADO -> "Negar todos os apps (que não tenham decisão própria)"
        Estado.PERGUNTAR -> "Perguntar a cada app (padrão)"
    }

    private fun escolherAutomatico() {
        val opcoes = arrayOf("Perguntar a cada app (padrão)", "Permitir todos", "Negar todos")
        val estados = arrayOf(Estado.PERGUNTAR, Estado.PERMITIDO, Estado.NEGADO)
        MaterialAlertDialogBuilder(this)
            .setTitle("Resposta automática")
            .setMessage("Vale para os apps sem decisão própria na lista. A escolha de um app sempre vence.")
            .setSingleChoiceItems(opcoes, estados.indexOf(politica.modoAutomatico())) { d, i ->
                politica.definirModoAutomatico(estados[i]); d.dismiss(); carregar()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun carregar() {
        automatico.text = rotuloAuto(politica.modoAutomatico())
        registro.text = politica.lerRegistro().lines().filter { it.isNotBlank() }.takeLast(40).reversed()
            .joinToString("\n").ifEmpty { "Nenhum pedido ainda." }
        Thread {
            val pm = VcloudCore.getPackageManager()
            val apps = try {
                VcloudCore.get().getInstalledApplications(0, 0).orEmpty()
                    .filterNot { it.packageName.startsWith("com.google.android.") || it.packageName == "com.android.vending" }
                    .filter { RootPolicy.pacoteValido(it.packageName) }
                    .map { ai ->
                        val nome = try { ai.loadLabel(pm).toString() } catch (e: Exception) { ai.packageName }
                        val icone = try { ai.loadIcon(pm) } catch (e: Exception) { null }
                        AppRoot(nome, ai.packageName, icone)
                    }.sortedBy { it.nome.lowercase() }
            } catch (e: Exception) {
                emptyList()
            }
            runOnUiThread { mostrar(apps) }
        }.start()
    }

    private fun mostrar(apps: List<AppRoot>) {
        lista.removeAllViews()
        if (apps.isEmpty()) {
            lista.addView(texto("Nenhum app dentro do Vcloud ainda.", 14f))
            return
        }
        for (app in apps) {
            val estado = politica.estado(app.pacote)
            val linha = LinearLayout(this).apply {
                gravity = Gravity.CENTER_VERTICAL; setPadding(0, px(8), 0, px(8))
                setOnClickListener { escolher(app) }
            }
            val icone = ImageView(this).apply {
                setImageDrawable(app.icone)
                layoutParams = LinearLayout.LayoutParams(px(44), px(44)).apply { rightMargin = px(14) }
            }
            val textos = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                addView(texto(app.nome, 15f))
                addView(texto(app.pacote, 11f).apply { alpha = 0.65f })
                addView(texto(when (estado) {
                    Estado.PERMITIDO -> "Permitido"
                    Estado.NEGADO -> "Negado"
                    Estado.PERGUNTAR -> "Segue a resposta automática"
                }, 12f, true))
            }
            // chave rápida: ligada = permitido; desligada = negado. Tocar no app abre as 3 opções (inclui "perguntar").
            val chave = Switch(this).apply {
                isChecked = estado == Estado.PERMITIDO
                setOnCheckedChangeListener { _, ligado ->
                    politica.definir(app.pacote, if (ligado) Estado.PERMITIDO else Estado.NEGADO); carregar()
                }
            }
            linha.addView(icone); linha.addView(textos); linha.addView(chave)
            lista.addView(linha)
        }
    }

    private fun escolher(app: AppRoot) {
        val opcoes = arrayOf("Seguir a resposta automática / perguntar", "Permitir", "Negar")
        val estados = arrayOf(Estado.PERGUNTAR, Estado.PERMITIDO, Estado.NEGADO)
        MaterialAlertDialogBuilder(this)
            .setTitle("Root para ${app.nome}")
            .setSingleChoiceItems(opcoes, estados.indexOf(politica.estado(app.pacote))) { d, i ->
                politica.definir(app.pacote, estados[i]); d.dismiss(); carregar()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
