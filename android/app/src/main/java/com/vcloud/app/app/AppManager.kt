package com.vcloud.app.app

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.vcloud.app.view.main.VcloudLoader


object AppManager {
    private const val TAG = "AppManager"

    @JvmStatic
    val mVcloudLoader by lazy {
        try {
            VcloudLoader()
        } catch (e: Exception) {
            Log.e(TAG, "Error creating VcloudLoader: ${e.message}")

            VcloudLoader() 
        }
    }

    @JvmStatic
    val mVcloudCore by lazy {
        try {
            mVcloudLoader.getVcloudCore()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting VcloudCore: ${e.message}")
            throw e 
        }
    }

    @JvmStatic
    val mRemarkSharedPreferences: SharedPreferences by lazy {
        try {
            App.getContext().getSharedPreferences("UserRemark", Context.MODE_PRIVATE)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating SharedPreferences: ${e.message}")
            throw e 
        }
    }

    fun doAttachBaseContext(context: Context) {
        try {
            mVcloudLoader.attachBaseContext(context)
            mVcloudLoader.addLifecycleCallback()
        } catch (e: Exception) {
            Log.e(TAG, "Error in doAttachBaseContext: ${e.message}")
            
        }
    }

    fun doOnCreate(context: Context) {
        try {
            mVcloudLoader.doOnCreate(context)
            initThirdService(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error in doOnCreate: ${e.message}")
            
        }
    }

    private fun initThirdService(context: Context) {
        try {
            
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in initThirdService: ${e.message}")
        }
    }
}
