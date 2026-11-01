package com.vcloud.app.view.root

import android.Manifest
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.vcloud.app.R
import com.vcloud.app.util.botaoTema
import com.vcloud.app.util.textoTema
import com.vcloud.app.view.base.BaseActivity
import com.vcloud.motor.VcloudCore
import com.vcloud.motor.core.root.RootPolicy
import com.vcloud.motor.core.root.RootPolicy.Aviso
import com.vcloud.motor.core.root.RootPolicy.Estado

/**
 * Gerenciador de root do Vcloud (segue o tema do sistema): chave geral, notificações, resposta para todos os apps,
 * chave por app (tocar no app dá as 3 opções) e o registro dos pedidos.
 * O root só existe dentro do Vcloud; aqui você decide quem usa.
 */
class RootManagerActivity : BaseActivity() {
    private class AppRoot(val nome: String, val pacote: String, val icone: Drawable?)

    private val politica by lazy { RootPolicy.padrao() }
    private var apps: List<AppRoot> = emptyList()
    private lateinit var lista: LinearLayout
    private lateinit var registro: TextView
    private lateinit var grupoAviso: RadioGroup
    private lateinit var grupoAuto: RadioGroup

    private val pedirNotificacao =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedida ->
            if (!concedida) {
                politica.definirAviso(Aviso.NENHUM); marcarGrupos()
                android.widget.Toast.makeText(this, "Sem a permissão de notificações o aviso fica em Nenhuma.", android.widget.Toast.LENGTH_LONG).show()
            }
        }

    private fun px(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun secao(t: String) = textoTema(t, 16f, negrito = true).apply { setPadding(0, px(18), 0, px(4)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val geral = SwitchMaterial(this).apply {
            text = "Root do Vcloud (chave geral)"
            setTextColor(ContextCompat.getColor(context, R.color.on_surface))
            isChecked = politica.geralLigado()
            setOnCheckedChangeListener { _, ligado -> politica.ligarGeral(ligado) }
        }

        grupoAviso = grupo(listOf("Nenhuma", "Mensagem na tela", "Notificação")) { i ->
            val novo = Aviso.values()[i]
            politica.definirAviso(novo)
            if (novo == Aviso.NOTIFICACAO && Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                pedirNotificacao.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        grupoAuto = grupo(listOf("Perguntar a cada app", "Permitir todos", "Negar todos")) { i ->
            politica.definirModoAutomatico(arrayOf(Estado.PERGUNTAR, Estado.PERMITIDO, Estado.NEGADO)[i]); carregar()
        }

        val emMassa = LinearLayout(this).apply {
            fun b(nome: String, e: Estado) = botaoTema(nome) { confirmarEmMassa(e) }.apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = px(6) }
            }
            addView(b("Permitir todos", Estado.PERMITIDO)); addView(b("Negar todos", Estado.NEGADO)); addView(b("Perguntar", Estado.PERGUNTAR))
        }

        lista = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        registro = textoTema("", 12f).apply { typeface = android.graphics.Typeface.MONOSPACE; setTextIsSelectable(true) }
        val barra = LinearLayout(this).apply {
            addView(botaoTema("Atualizar") { carregar() }.apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = px(8) } })
            addView(botaoTema("Limpar registro") { politica.limparRegistro(); carregar() }.apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) })
        }

        val pad = px(16)
        setContentView(ScrollView(this).apply {
            addView(LinearLayout(this@RootManagerActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(pad, pad, pad, pad)
                addView(textoTema("Gerenciador de root", 20f, negrito = true))
                addView(textoTema("O root vale só dentro do Vcloud, não no celular. Depois de mudar, feche e abra o app de novo.", 13f, secundario = true)
                    .apply { setPadding(0, px(4), 0, px(8)) })
                addView(geral)
                addView(secao("Notificações")); addView(grupoAviso)
                addView(secao("Responder todos da mesma maneira")); addView(grupoAuto)
                addView(textoTema("Aplicar agora a todos os apps da lista:", 12f, secundario = true).apply { setPadding(0, px(8), 0, px(4)) })
                addView(emMassa)
                addView(secao("Apps")); addView(lista)
                addView(secao("Pedidos recentes")); addView(barra); addView(registro)
            })
        })
        marcarGrupos()
    }

    override fun onResume() {
        super.onResume()
        carregar()
    }

    /** Grupo de opções (uma só marcada). O listener entra depois de marcar, para não disparar na montagem. */
    private fun grupo(opcoes: List<String>, aoEscolher: (Int) -> Unit) = RadioGroup(this).apply {
        opcoes.forEach { t ->
            addView(RadioButton(context).apply {
                id = View.generateViewId(); text = t; textSize = 14f
                setTextColor(ContextCompat.getColor(context, R.color.on_surface))
            })
        }
        tag = aoEscolher
    }

    private fun marcarGrupos() {
        fun ligar(g: RadioGroup, i: Int) {
            g.setOnCheckedChangeListener(null)
            (g.getChildAt(i) as RadioButton).isChecked = true
            @Suppress("UNCHECKED_CAST") val acao = g.tag as (Int) -> Unit
            g.setOnCheckedChangeListener { grp, id -> acao(grp.indexOfChild(grp.findViewById(id))) }
        }
        ligar(grupoAviso, politica.aviso().ordinal)
        ligar(grupoAuto, listOf(Estado.PERGUNTAR, Estado.PERMITIDO, Estado.NEGADO).indexOf(politica.modoAutomatico()))
    }

    private fun confirmarEmMassa(estado: Estado) {
        if (apps.isEmpty()) return
        val que = when (estado) { Estado.PERMITIDO -> "Permitir root"; Estado.NEGADO -> "Negar root"; Estado.PERGUNTAR -> "Voltar a perguntar" }
        MaterialAlertDialogBuilder(this)
            .setTitle("$que para todos?")
            .setMessage("Isso muda a decisão de ${apps.size} apps, inclusive dos que você ajustou um a um.")
            .setPositiveButton("Aplicar") { _, _ -> apps.forEach { politica.definir(it.pacote, estado) }; carregar() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun carregar() {
        marcarGrupos()
        registro.text = politica.lerRegistro().lines().filter { it.isNotBlank() }.takeLast(40).reversed()
            .joinToString("\n").ifEmpty { "Nenhum pedido ainda." }
        Thread {
            val pm = VcloudCore.getPackageManager()
            val achados = try {
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
            runOnUiThread { apps = achados; mostrar() }
        }.start()
    }

    private fun mostrar() {
        lista.removeAllViews()
        if (apps.isEmpty()) {
            lista.addView(textoTema("Nenhum app dentro do Vcloud ainda.", 14f))
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
                addView(textoTema(app.nome, 15f))
                addView(textoTema(app.pacote, 11f, secundario = true))
                addView(textoTema(when (estado) {
                    Estado.PERMITIDO -> "Permitido"
                    Estado.NEGADO -> "Negado"
                    Estado.PERGUNTAR -> "Automático"
                }, 12f, negrito = true))
            }
            // chave rápida: ligada = permitido; desligada = negado. Tocar no app abre as 3 opções (inclui "automático").
            val chave = SwitchMaterial(this).apply {
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
        val opcoes = arrayOf("Automático (segue a resposta para todos)", "Permitir", "Negar")
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
