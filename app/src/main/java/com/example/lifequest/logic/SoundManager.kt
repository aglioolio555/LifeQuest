package com.example.lifequest.logic

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.lifequest.R

// サウンドの種類を定義
enum class SoundType {
    LEVEL_UP,
    QUEST_COMPLETE,
    TIMER_START,
    TIMER_FINISH,
    TIMER_PAUSE,
    UI_CLICK,
    ERROR
}

// CompositionLocalの定義（UIツリーのどこからでもアクセス可能にするため）
val LocalSoundManager = staticCompositionLocalOf<SoundManager> {
    error("No SoundManager provided")
}

class SoundManager(context: Context) {
    private val soundPool: SoundPool
    private val soundMap = mutableMapOf<SoundType, Int>()

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(audioAttributes)
            .build()

        // ★リソースIDは実際のファイル名に合わせて変更してください
        // まだファイルがない場合は、既存の se_levelup などを仮で割り当ててもOKです
        soundMap[SoundType.LEVEL_UP] = soundPool.load(context, R.raw.se_levelup, 1) // 既存
//        soundMap[SoundType.QUEST_COMPLETE] = soundPool.load(context, R.raw.se_stump, 1) // 既存(旧Coin)

        // 新規追加分 (res/raw/ にファイルを追加してください)
         soundMap[SoundType.TIMER_START] = soundPool.load(context, R.raw.se_timerstart, 1)
//         soundMap[SoundType.TIMER_FINISH] = soundPool.load(context, R.raw.se_timer_finish, 1)
         soundMap[SoundType.UI_CLICK] = soundPool.load(context, R.raw.se_ui_click, 1)
         soundMap[SoundType.ERROR] = soundPool.load(context, R.raw.se_error, 1)
        soundMap[SoundType.TIMER_PAUSE]=soundPool.load(context,R.raw.se_timer_stop,1)
    }

    fun play(type: SoundType, volume: Float = 1f, rate: Float = 1f) {
        val soundId = soundMap[type] ?: return
        soundPool.play(soundId, volume, volume, 1, 0, rate)
    }

    // ショートカットメソッド
    fun playClick() {
        // ピッチをわずかにランダムにすることで機械的なリアリティを出す
        val randomRate = 0.95f + (Math.random().toFloat() * 0.1f)
        play(SoundType.UI_CLICK, volume = 0.5f, rate = randomRate)
    }

    fun playTimerStart() = play(SoundType.TIMER_START)
    fun playTimerFinish() = play(SoundType.TIMER_FINISH)
    fun playQuestComplete() = play(SoundType.QUEST_COMPLETE)
    fun playLevelUp() = play(SoundType.LEVEL_UP)
    fun playError() = play(SoundType.ERROR)
    fun playTimerPause() = play(SoundType.TIMER_PAUSE)

    // 既存互換用
    fun playLevelUpSound() = playLevelUp()
    fun playCoinSound() = playQuestComplete()
    fun playTimerFinishSound() = playTimerFinish()


    fun release() {
        soundPool.release()
    }
}