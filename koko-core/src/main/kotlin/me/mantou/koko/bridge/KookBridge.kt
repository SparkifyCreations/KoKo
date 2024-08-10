package me.mantou.koko.bridge

import kotlinx.coroutines.Job
import me.mantou.koko.KoKoBot

interface KookBridge {
    fun init(koKoBot: KoKoBot): Job
    fun destroy()
}