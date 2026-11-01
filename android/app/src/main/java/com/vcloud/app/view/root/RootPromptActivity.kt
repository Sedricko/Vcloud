package com.vcloud.app.view.root

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.vcloud.motor.VcloudCore
import com.vcloud.motor.core.root.RootPolicy

/** Janela "Permitir root?" aberta pelo servidor de root quando um app sem decisão pede su. Sem resposta = nega. */
class RootPromptActivity : AppCompatActivity() {
    private var respondeu = false
    private var pacote = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pacote = intent.getStringExtra("pkg").orEmpty()
        val comando = intent.getStringExtra("cmd").orEmpty()
        if (!RootPolicy.pacoteValido(pacote)) { finish(); return }
        val nome = try {
            VcloudCore.get().getInstalledApplications(0, 0).orEmpty().firstOrNull { it.packageName == pacote }
                ?.loadLabel(VcloudCore.getPackageManager())?.toString() ?: pacote
        } catch (e: Exception) { pacote }

        val todos = android.widget.CheckBox(this).apply {
            text = "Usar a mesma resposta para todos os apps"
            setPadding(0, 0, 0, 0)
        }
        val caixa = android.widget.FrameLayout(this).apply {
            val m = (24 * resources.displayMetrics.density).toInt()
            setPadding(m, 0, m, 0)
            addView(todos)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Permitir root?")
            .setView(caixa)
            .setMessage("$nome quer usar root dentro do Vcloud.\n\nComando: $comando\n\nVale só dentro do Vcloud, não no celular. " +
                "Você muda isso depois em Configurações > Gerenciador de root.")
            .setCancelable(false)
            .setPositiveButton("Permitir sempre") { _, _ -> responder("allow", todos.isChecked) }
            .setNeutralButton("Só desta vez") { _, _ -> responder("once", false) }
            .setNegativeButton("Negar") { _, _ -> responder("deny", todos.isChecked) }
            .show()
    }

    private fun responder(r: String, paraTodos: Boolean) {
        respondeu = true
        try {
            val p = RootPolicy.padrao()
            p.responder(pacote, r)
            // "mesma resposta para todos": vira a resposta automática dos apps sem decisão própria
            if (paraTodos) p.definirModoAutomatico(if (r == "allow") RootPolicy.Estado.PERMITIDO else RootPolicy.Estado.NEGADO)
        } catch (e: Exception) { /* servidor já desistiu */ }
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        // fechou sem escolher (tela girou, sistema matou): nega, em vez de deixar o app esperando
        if (!respondeu && RootPolicy.pacoteValido(pacote)) {
            try { RootPolicy.padrao().responder(pacote, "deny") } catch (e: Exception) { /* idem */ }
        }
    }
}
