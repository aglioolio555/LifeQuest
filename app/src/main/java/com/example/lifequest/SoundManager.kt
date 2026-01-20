package com.example.lifequest

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

class SoundManager(context: Context) {
    private val soundPool: SoundPool
    private val coinSoundId: Int
    private val levelUpSoundId: Int

    init {
        // SoundPoolの設定（ゲーム用設定）
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setAudioAttributes(audioAttributes)
            .setMaxStreams(2) // 同時に鳴らせる音の数
            .build()

        // 音声をロード（res/rawフォルダのファイル名を指定）
        // ※ファイル名が違う場合は、R.raw.の後ろをご自身のファイル名に変えてください
        coinSoundId = soundPool.load(context, R.raw.se_stump, 1)
        levelUpSoundId = soundPool.load(context, R.raw.se_levelup, 1)
    }

    fun playCoinSound() {
        // 左音量1.0, 右音量1.0, 優先度0, ループなし(0), 再生速度1.0
        soundPool.play(coinSoundId, 1.0f, 1.0f, 0, 0, 1.0f)
    }

    fun playLevelUpSound() {
        soundPool.play(levelUpSoundId, 1.0f, 1.0f, 0, 0, 1.0f)
    }

    // アプリ終了時にメモリ解放するためのメソッド
    fun release() {
        soundPool.release()
    }
}