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



        initSendLogs()
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
    private fun initSendLogs() {
        val sendLogsPreference: Preference? = findPreference("send_logs")
        sendLogsPreference?.setOnPreferenceClickListener {
            it.isEnabled = false
            VcloudCore.get()
                    .sendLogs(
                            "Manual Log Upload from Settings",
                            true,
                            object : VcloudCore.LogSendListener {
                                override fun onSuccess() {
                                    activity?.runOnUiThread { sendLogsPreference.isEnabled = true }
                                }

                                override fun onFailure(error: String?) {
                                    activity?.runOnUiThread { sendLogsPreference.isEnabled = true }
                                }
                            }
                    )
            toast("Gerando relatório em Download/Vcloud...")
            true
        }
    }
}
