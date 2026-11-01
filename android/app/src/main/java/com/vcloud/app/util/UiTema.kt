package com.vcloud.app.util

import android.content.Context
import android.graphics.Typeface
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.vcloud.app.R

/** Telas feitas por código seguem o tema do sistema: cores de values/colors.xml (claro) e values-night/colors.xml (escuro). */
fun Context.textoTema(t: String, tamanho: Float, negrito: Boolean = false, secundario: Boolean = false) = TextView(this).apply {
    text = t
    textSize = tamanho
    setTextColor(ContextCompat.getColor(context, if (secundario) R.color.secondary_text else R.color.on_surface))
    if (negrito) setTypeface(typeface, Typeface.BOLD)
}

/** Botão do tema (Material, contorno). */
fun Context.botaoTema(nome: String, preenchido: Boolean = false, acao: () -> Unit) = MaterialButton(
    this, null,
    if (preenchido) com.google.android.material.R.attr.materialButtonStyle else com.google.android.material.R.attr.materialButtonOutlinedStyle
).apply {
    text = nome
    isAllCaps = false
    setOnClickListener { acao() }
}
