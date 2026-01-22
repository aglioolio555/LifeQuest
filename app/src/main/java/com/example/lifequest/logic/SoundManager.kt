package com.example.lifequest.logic

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.example.lifequest.R

class SoundManager(context: Context) {
    private val soundPool: SoundPool
    private val soundLevelUp: Int
    private val soundCoin: Int
    private val soundTimerFinish: Int // ★追加

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()

        soundLevelUp = soundPool.load(context, R.raw.se_levelup, 1)
        soundCoin = soundPool.load(context, R.raw.se_stump, 1)
        soundTimerFinish = soundPool.load(context, R.raw.se_levelup, 1) // ★仮でレベルアップ音を再利用（専用音があれば差し替えてください）
    }

    fun playLevelUpSound() {
        soundPool.play(soundLevelUp, 1f, 1f, 0, 0, 1f)
    }

    fun playCoinSound() {
        soundPool.play(soundCoin, 1f, 1f, 0, 0, 1f)
    }

    // ★追加
    fun playTimerFinishSound() {
        soundPool.play(soundTimerFinish, 1f, 1f, 1, 0, 1.2f) // 少しピッチを変えて区別
    }

    fun release() {
        soundPool.release()
    }
}