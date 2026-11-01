package com.vcloud.app.view.painel

import com.vcloud.motor.core.env.BEnvironment
import java.io.File

/**
 * Opções do painel lateral. Ficam em arquivos (como a política do root) porque quem muda (Configurações, processo principal)
 * e quem lê (o painel, processo ":black") são processos diferentes.
 */
object PainelConfig {
    enum class Opacidade(val alfa: Float, val rotulo: String, val chave: String) {
        NORMAL(1f, "Normal", "normal"),
        DISCRETA(0.45f, "Discreta", "discreta"),
        QUASE_INVISIVEL(0.15f, "Quase invisível", "quase")
    }

    private fun dir() = File(BEnvironment.getVirtualRoot(), "painel").apply { mkdirs() }

    fun oculto(): Boolean = File(dir(), "_oculto").exists()

    fun definirOculto(oculto: Boolean) {
        val f = File(dir(), "_oculto")
        if (oculto) f.createNewFile() else f.delete()
    }

    fun opacidade(): Opacidade {
        val s = try { File(dir(), "_opacidade").readText().trim() } catch (e: Exception) { "" }
        return Opacidade.values().firstOrNull { it.chave == s } ?: Opacidade.DISCRETA
    }

    fun definirOpacidade(o: Opacidade) = File(dir(), "_opacidade").writeText(o.chave)
}
