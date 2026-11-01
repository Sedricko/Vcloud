package com.vcloud.app.view.root

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import com.vcloud.motor.VcloudCore
import com.vcloud.motor.core.root.RootPolicy

/** Janela "Permitir root?" aberta pelo servidor de root quando um app sem decisão pede su. Sem resposta = nega. */
class RootPromptActivity : Activity() {
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

        AlertDialog.Builder(this)
            .setTitle("Permitir root?")
            .setMessage("$nome quer usar root dentro do Vcloud.\n\nComando: $comando\n\nVale só dentro do Vcloud, não no celular. " +
                "Você muda isso depois em Configurações > Gerenciador de root.")
            .setCancelable(false)
            .setPositiveButton("Permitir sempre") { _, _ -> responder("allow") }
            .setNeutralButton("Só desta vez") { _, _ -> responder("once") }
            .setNegativeButton("Negar") { _, _ -> responder("deny") }
            .show()
    }

    private fun responder(r: String) {
        respondeu = true
        try { RootPolicy.padrao().responder(pacote, r) } catch (e: Exception) { /* servidor já desistiu */ }
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
