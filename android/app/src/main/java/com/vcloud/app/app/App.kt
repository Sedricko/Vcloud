package com.vcloud.app.app

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.util.Log
import com.vcloud.motor.VcloudCore


class App : Application() {

    companion object {

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private lateinit var mContext: Context

        @JvmStatic
        fun getContext(): Context {
            return mContext
        }
    }

    override fun attachBaseContext(base: Context?) {
        try {
            super.attachBaseContext(base)

            try {
                VcloudCore.get().closeCodeInit()
            } catch (e: Exception) {
                Log.e("App", "Error in closeCodeInit: ${e.message}")
            }

            try {
                VcloudCore.get().onBeforeMainApplicationAttach(this, base)
            } catch (e: Exception) {
                Log.e("App", "Error in onBeforeMainApplicationAttach: ${e.message}")
            }

            mContext = base!!

            try {
                AppManager.doAttachBaseContext(base)
            } catch (e: Exception) {
                Log.e("App", "Error in doAttachBaseContext: ${e.message}")
            }

            try {

                VcloudCore.get().onAfterMainApplicationAttach(this, base)

            } catch (e: Exception) {

                Log.e("App", "Error in onAfterMainApplicationAttach: ${e.message}")

            }
        } catch (e: Exception) {
            Log.e("App", "Critical error in attachBaseContext: ${e.message}")
            if (base != null) {
                mContext = base
            }
        }
    }

    override fun onCreate() {
        try {
            super.onCreate()
            AppManager.doOnCreate(mContext)
            com.vcloud.app.view.painel.PainelLateral.iniciarSeForServidor(mContext)
            // Google de dentro: reinício único dos dados (estado velho do checkin); espera o servidor do motor subir
            if (VcloudCore.get().isMainProcess) {
                Thread {
                    try { Thread.sleep(8000) } catch (_: InterruptedException) { }
                    com.vcloud.motor.utils.GoogleModo.reiniciarDadosUmaVez("google_reset_4_0_47")
                }.start()
            }
        } catch (e: Exception) {
            Log.e("App", "Error in onCreate: ${e.message}")
        }
    }
}