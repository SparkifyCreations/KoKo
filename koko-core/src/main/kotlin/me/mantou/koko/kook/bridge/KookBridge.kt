package me.mantou.koko.kook.bridge

import kotlinx.coroutines.Job
import me.mantou.koko.KoKoBot

interface KookBridge {
    fun init(koKoBot: KoKoBot): Job
    fun destroy()
}