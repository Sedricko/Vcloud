package com.vcloud.app.view.vivos

import android.os.Bundle
import com.vcloud.app.R
import com.vcloud.app.databinding.ActivitySettingBinding
import com.vcloud.app.util.inflate
import com.vcloud.app.view.base.BaseActivity

/** Tela "Manter apps vivos": mesmo molde das Configurações (barra + lista nativa de preferências). */
class VivosActivity : BaseActivity() {
    private val viewBinding: ActivitySettingBinding by inflate()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        initToolbar(viewBinding.toolbarLayout.toolbar, R.string.vivos_titulo, true)
        supportFragmentManager.beginTransaction().replace(R.id.fragment, VivosFragment()).commit()
    }
}
