package com.vcloud.app.view.setting

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.vcloud.motor.VcloudCore
import com.vcloud.app.R
import com.vcloud.app.app.AppManager
import com.vcloud.app.util.toast
import com.vcloud.app.view.gms.GmsManagerActivity

class SettingFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.setting, rootKey)

        initGms()


        invalidHideState {
            val daemonPreference: Preference = (findPreference("daemon_enable")!!)
            val mDaemonEnable = AppManager.mVcloudLoader.daemonEnable()
            daemonPreference.setDefaultValue(mDaemonEnable)
            daemonPreference
        }



        initSobrepor()
        findPreference<Preference>("gerenciador_root")?.setOnPreferenceClickListener {
            startActivity(android.content.Intent(requireContext(), com.vcloud.app.view.root.RootManagerActivity::class.java))
            true
        }
        findPreference<Preference>("diagnostico")?.setOnPreferenceClickListener {
            startActivity(android.content.Intent(requireContext(), DiagnosticoActivity::class.java))
            true
        }
    }

    override fun onResume() {
        super.onResume()
        findPreference<Preference>("sobrepor")?.summary =
                if (android.provider.Settings.canDrawOverlays(requireContext())) "Ligado: os apps de dentro podem mostrar balões flutuantes"
                else "Desligado: toque para ligar (necessário para o balão do Unit)"
    }

    // Vcloud: a permissão vale para o Vcloud inteiro; os apps de dentro desenham por cima usando a dele.
    private fun initSobrepor() {
        findPreference<Preference>("sobrepor")?.setOnPreferenceClickListener {
            startActivity(android.content.Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:" + requireContext().packageName)))
            true
        }
    }

    private fun initGms() {
        val gmsManagerPreference: Preference = (findPreference("gms_manager")!!)

        if (VcloudCore.get().isSupportGms) {

            gmsManagerPreference.setOnPreferenceClickListener {
                GmsManagerActivity.start(requireContext())
                true
            }
        } else {
            gmsManagerPreference.summary = getString(R.string.no_gms)
            gmsManagerPreference.isEnabled = false
        }
    }

    private fun invalidHideState(block: () -> Preference) {
        val pref = block()
        pref.setOnPreferenceChangeListener { preference, newValue ->
            val tmpHide = (newValue == true)
            when (preference.key) {
                "root_hide" -> {

                    AppManager.mVcloudLoader.invalidHideRoot(tmpHide)
                }
                "daemon_enable" -> {
                    AppManager.mVcloudLoader.invalidDaemonEnable(tmpHide)
                }
                "use_vpn_network" -> {
                    AppManager.mVcloudLoader.invalidUseVpnNetwork(tmpHide)
                }
                "disable_flag_secure" -> {
                    AppManager.mVcloudLoader.invalidDisableFlagSecure(tmpHide)
                }
            }

            toast(R.string.restart_module)
            return@setOnPreferenceChangeListener true
        }
    }
}
