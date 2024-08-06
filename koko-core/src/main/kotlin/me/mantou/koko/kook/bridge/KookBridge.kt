package me.mantou.koko.kook.bridge

import me.mantou.koko.KoKoBot

interface KookBridge {
    suspend fun init(koKoBot: KoKoBot)
}