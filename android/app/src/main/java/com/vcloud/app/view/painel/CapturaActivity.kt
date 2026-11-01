package com.vcloud.app.view.painel

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Toast

/** Pede ao Android a permissão de captura de tela (ele pergunta a cada captura) e passa o resultado ao CapturaService. */
class CapturaActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mpm = getSystemService(MediaProjectionManager::class.java)
        startActivityForResult(mpm.createScreenCaptureIntent(), 1)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null) {
            startForegroundService(Intent(this, CapturaService::class.java).putExtra("code", resultCode).putExtra("data", data))
        } else {
            Toast.makeText(this, "Captura cancelada", Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}
