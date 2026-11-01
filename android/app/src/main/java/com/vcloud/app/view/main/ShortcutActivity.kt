package com.vcloud.app.view.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.vcloud.motor.VcloudCore


class ShortcutActivity:AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pkg = intent.getStringExtra("pkg")
        val userID = intent.getIntExtra("userId",0)

        lifecycleScope.launch {
            VcloudCore.get().launchApk(pkg,userID)
            finish()
        }
    }
}