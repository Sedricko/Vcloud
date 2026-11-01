package com.vcloud.app.view.vivos

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.vcloud.app.R
import com.vcloud.app.view.setting.DiagnosticoActivity
import com.vcloud.app.view.setting.SettingActivity
import com.vcloud.motor.utils.CausaDeErro
import com.vcloud.motor.utils.Diagnostico

/**
 * Mostra o que pode estar fechando os apps em segundo plano e como evitar. Não muda nada no celular sozinho:
 * tudo passa por telas do próprio Android (bateria, Opções do desenvolvedor).
 */
class VivosFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.vivos, rootKey)
        findPreference<Preference>("viv_bateria")?.setOnPreferenceClickListener { bateria(); true }
        findPreference<Preference>("viv_fantasma")?.setOnPreferenceClickListener { fantasma(); true }
        findPreference<Preference>("viv_encerrados")?.setOnPreferenceClickListener {
            startActivity(Intent(requireContext(), DiagnosticoActivity::class.java)); true
        }
        findPreference<Preference>("viv_segundo_plano")?.setOnPreferenceClickListener {
            startActivity(Intent(requireContext(), SettingActivity::class.java)); true
        }
    }

    override fun onResume() {
        super.onResume() // volta do pedido do Android: atualiza o estado
        val ctx = requireContext()
        findPreference<Preference>("viv_bateria")?.summary =
            if (semRestricaoDeBateria(ctx)) "Sem restrição: o Android não pausa o Vcloud para poupar bateria."
            else "O Android pode pausar o Vcloud para poupar bateria. Toque para liberar."
        findPreference<Preference>("viv_fantasma")?.summary = when (estadoFantasma(ctx)) {
            false -> "Desativado: o Android não limita os processos dos apps. Toque para rever."
            true -> "Ativo: o Android pode encerrar processos dos jogos. Toque para ver como desativar."
            null -> "Não consegui verificar neste Android. Toque para ver como desativar."
        }
        val n = CausaDeErro.contarEncerradosPeloAndroid(Diagnostico.ler())
        findPreference<Preference>("viv_encerrados")?.summary =
            if (n == 0) "Nenhum no histórico. Toque para abrir a Central."
            else "$n no histórico (pouca memória ou limite de uso). Toque para ver na Central."
    }

    private fun semRestricaoDeBateria(ctx: Context) =
        ctx.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(ctx.packageName)

    /** true = limite ativo, false = desativado, null = o Android não deixa o app ler (nesse caso não afirmamos nada). */
    private fun estadoFantasma(ctx: Context): Boolean? = try {
        when (Settings.Global.getString(ctx.contentResolver, "settings_enable_monitor_phantom_procs")) {
            "false" -> false
            "true" -> true
            else -> null
        }
    } catch (e: Exception) { null }

    private fun bateria() {
        val ctx = requireContext()
        if (semRestricaoDeBateria(ctx)) { abrir(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)); return }
        MaterialAlertDialogBuilder(ctx)
            .setTitle("Liberar a bateria?")
            .setMessage("O Android vai perguntar se o Vcloud pode ficar sem restrição de bateria. Isso ajuda os jogos a continuarem abertos.")
            .setPositiveButton("Continuar") { _, _ ->
                abrir(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + ctx.packageName)))
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun desenvolvedorLigado(ctx: Context) = try {
        Settings.Global.getInt(ctx.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1
    } catch (e: Exception) { false }

    /** Leva direto à tela certa: Opções do desenvolvedor (se já ligadas) ou Sobre o telefone (para ligá-las). */
    private fun fantasma() {
        val b = MaterialAlertDialogBuilder(requireContext()).setTitle("Limite de processos fantasma")
        if (desenvolvedorLigado(requireContext())) {
            b.setMessage("Nas Opções do desenvolvedor, procure \"Desativar restrições de processos secundários\" (o nome varia) e ligue. Se o seu celular não tiver essa chave, este ajuste não está disponível nele.")
                .setPositiveButton("Abrir Opções do desenvolvedor") { _, _ -> abrir(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)) }
        } else {
            b.setMessage("As Opções do desenvolvedor estão desligadas. Em Sobre o telefone > Informações do software, toque 7 vezes em Número da versão. Depois volte aqui.")
                .setPositiveButton("Abrir Sobre o telefone") { _, _ -> abrir(Intent(Settings.ACTION_DEVICE_INFO_SETTINGS)) }
        }
        b.setNegativeButton("Fechar", null).show()
    }

    private fun abrir(i: Intent) {
        try { startActivity(i) } catch (e: Exception) {
            Toast.makeText(requireContext(), "Este celular não abriu essa tela do Android.", Toast.LENGTH_LONG).show()
        }
    }
}
