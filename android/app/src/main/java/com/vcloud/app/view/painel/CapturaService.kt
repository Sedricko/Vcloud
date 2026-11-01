package com.vcloud.app.view.painel

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.widget.Toast
import com.vcloud.motor.utils.Diagnostico
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Tira UMA foto da tela e salva em Pictures/Vcloud. O Android exige um serviço em primeiro plano durante a captura. */
class CapturaService : Service() {
    private val main = Handler(Looper.getMainLooper())
    private var projecao: MediaProjection? = null
    private var tela: VirtualDisplay? = null
    private var leitor: ImageReader? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("vcloud_captura", "Captura de tela", NotificationManager.IMPORTANCE_LOW))
        val n = Notification.Builder(this, "vcloud_captura").setSmallIcon(android.R.drawable.stat_notify_sync_noanim)
            .setContentTitle("Vcloud").setContentText("Capturando a tela").build()
        // Android 14+: o serviço precisa estar em primeiro plano (tipo mediaProjection) ANTES de pegar a projeção.
        startForeground(1042, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)

        val code = intent?.getIntExtra("code", 0) ?: 0
        val data = intent?.getParcelableExtra<Intent>("data")
        if (data == null) { terminar("Captura cancelada"); return START_NOT_STICKY }
        try {
            val mpm = getSystemService(MediaProjectionManager::class.java)
            val p = mpm.getMediaProjection(code, data)
            projecao = p
            p.registerCallback(object : MediaProjection.Callback() { override fun onStop() { liberar() } }, main)
            val m = resources.displayMetrics
            val r = ImageReader.newInstance(m.widthPixels, m.heightPixels, PixelFormat.RGBA_8888, 2)
            leitor = r
            tela = p.createVirtualDisplay("vcloud-captura", m.widthPixels, m.heightPixels, m.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, r.surface, null, main)
            tentar(0)
        } catch (e: Throwable) {
            Diagnostico.evento("CAPTURA falhou ao iniciar: $e")
            terminar("Não consegui capturar a tela")
        }
        return START_NOT_STICKY
    }

    // O primeiro quadro demora um pouco: tenta algumas vezes antes de desistir.
    private fun tentar(n: Int) {
        main.postDelayed({
            val img = try { leitor?.acquireLatestImage() } catch (e: Throwable) { null }
            if (img == null) {
                if (n < 8) tentar(n + 1) else terminar("A captura não gerou imagem")
                return@postDelayed
            }
            try {
                val w = resources.displayMetrics.widthPixels
                val h = resources.displayMetrics.heightPixels
                val plano = img.planes[0]
                val sobra = plano.rowStride - plano.pixelStride * w
                val bruto = Bitmap.createBitmap(w + sobra / plano.pixelStride, h, Bitmap.Config.ARGB_8888)
                bruto.copyPixelsFromBuffer(plano.buffer)
                val foto = Bitmap.createBitmap(bruto, 0, 0, w, h)
                salvar(foto)
                terminar("Captura salva em Pictures/Vcloud")
            } catch (e: Throwable) {
                Diagnostico.evento("CAPTURA falhou ao salvar: $e")
                terminar("Não consegui salvar a captura")
            } finally {
                img.close()
            }
        }, if (n == 0) 900L else 300L)
    }

    private fun salvar(foto: Bitmap) {
        val nome = "Vcloud_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".png"
        val v = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, nome)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Vcloud")
        }
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v) ?: throw IllegalStateException("sem uri")
        contentResolver.openOutputStream(uri)?.use { foto.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun terminar(msg: String) {
        Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
        liberar()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun liberar() {
        try { tela?.release() } catch (e: Throwable) { /* já liberada */ }
        try { leitor?.close() } catch (e: Throwable) { /* idem */ }
        try { projecao?.stop() } catch (e: Throwable) { /* idem */ }
        tela = null; leitor = null; projecao = null
    }

    override fun onDestroy() { liberar(); super.onDestroy() }
}
