package com.example.lifequest.logic

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.lifequest.R

// サウンドの種類を定義
enum class SoundType {
    LEVEL_UP,
    QUEST_COMPLETE,
    BONUS,
    REQUEST,
    DELETE,
    TIMER_START,
    TIMER_FINISH,
    TIMER_PAUSE,
    UI_CLICK,
    ERROR,
    BGM_START,
    BGM_PAUSE,
    BGM_RESUME,
    BGM_STOP
}

// CompositionLocalの定義（UIツリーのどこからでもアクセス可能にするため）
val LocalSoundManager = staticCompositionLocalOf<SoundManager> {
    error("No SoundManager provided")
}

class SoundManager(private val context: Context) {
    private val soundPool: SoundPool
    private val soundMap = mutableMapOf<SoundType, Int>()

    //BGM制御用のMediaPlayer
    private var bgmPlayer: MediaPlayer? = null

    //BGMリスト (res/raw/にファイルを配置してください)
    // ファイルがない場合は空リスト、またはダミーでエラー回避してください
    private val bgmList = listOf(
        R.raw.bgm_brownnoise,
        R.raw.bgm_pinknoise,
        R.raw.bgm_whitenoise,
        R.raw.bgm_forestrain,
        R.raw.bgm_rainlong,
        R.raw.bgm_seawaves,
        R.raw.bgm_firecrackling,
        R.raw.bgm_waterflowing,
    )

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(audioAttributes)
            .build()

        soundMap[SoundType.LEVEL_UP] = soundPool.load(context, R.raw.se_levelup, 1) // 既存
        soundMap[SoundType.QUEST_COMPLETE] = soundPool.load(context, R.raw.se_questcompleted, 1) // 既存(旧Coin)
        soundMap[SoundType.REQUEST]=soundPool.load(context,R.raw.se_request,1)
        soundMap[SoundType.DELETE]=soundPool.load(context,R.raw.se_delete,1)
        soundMap[SoundType.BONUS]=soundPool.load(context,R.raw.se_bonus,1)
        soundMap[SoundType.TIMER_START] = soundPool.load(context, R.raw.se_timerstart, 1)
        soundMap[SoundType.TIMER_FINISH] = soundPool.load(context, R.raw.se_timerfinish, 1)
        soundMap[SoundType.UI_CLICK] = soundPool.load(context, R.raw.se_ui_click, 1)
        soundMap[SoundType.ERROR] = soundPool.load(context, R.raw.se_error, 1)
        soundMap[SoundType.TIMER_PAUSE]=soundPool.load(context,R.raw.se_timerstop,1)
    }

    fun play(type: SoundType, volume: Float = 1f, rate: Float = 1f) {
        when (type) {
            //BGMタイプなら専用メソッドを呼ぶ
            SoundType.BGM_START -> startFocusBgm()
            SoundType.BGM_PAUSE -> pauseBgm()
            SoundType.BGM_RESUME -> resumeBgm()
            SoundType.BGM_STOP -> stopBgm()

            // それ以外（SE）は従来のSoundPoolで再生
            else -> {
                val soundId = soundMap[type] ?: return
                soundPool.play(soundId, volume, volume, 1, 0, rate)
            }
        }
    }

    // ショートカットメソッド
    fun playClick() {
        // ピッチをわずかにランダムにすることで機械的なリアリティを出す
        val randomRate = 0.975f + (Math.random().toFloat() * 0.05f)
        play(SoundType.UI_CLICK, volume = 0.5f, rate = randomRate)
    }

    fun playTimerStart() = play(SoundType.TIMER_START)
    fun playTimerFinish() = play(SoundType.TIMER_FINISH)
    fun playQuestComplete() = play(SoundType.QUEST_COMPLETE)
    fun playBonus()=play(SoundType.BONUS)
    fun playRequest()=play(SoundType.REQUEST)
    fun playDelete()=play(SoundType.DELETE)
    fun playLevelUp() = play(SoundType.LEVEL_UP)
    fun playError() = play(SoundType.ERROR)
    fun playTimerPause() = play(SoundType.TIMER_PAUSE)

    //BGM制御メソッド

    /**
     * 集中BGMの再生を開始する（セッション開始時）
     * ランダムに選曲し、ループ再生を行う
     */
    fun startFocusBgm() {
        // すでに再生中なら何もしない（セッション固定のため）
        if (bgmPlayer != null) return
        if (bgmList.isEmpty()) return

        // ランダム選曲
        val bgmResId = bgmList.random()

        try {
            bgmPlayer = MediaPlayer.create(context, bgmResId).apply {
                isLooping = true // ループ再生
                setVolume(0.2f, 0.2f) // 音量はSEより小さく (0.0f ~ 1.0f)
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            bgmPlayer = null
        }
    }

    /**
     * BGMを一時停止（タイマー中断時）
     */
    fun pauseBgm() {
        if (bgmPlayer?.isPlaying == true) {
            bgmPlayer?.pause()
        }
    }

    /**
     * BGMを再開（タイマー再開時）
     */
    fun resumeBgm() {
        // プレイヤーが存在し、かつ再生していない場合のみ再開
        if (bgmPlayer != null && bgmPlayer?.isPlaying == false) {
            bgmPlayer?.start()
        }
    }

    /**
     * BGMを完全停止してリソースを解放（タイマー終了/完了/中断時）
     */
    fun stopBgm() {
        bgmPlayer?.let { player ->
            if (player.isPlaying) {
                player.stop()
            }
            player.release()
        }
        bgmPlayer = null
    }
    fun release() {
        soundPool.release()
        // BGMリソースも解放
        stopBgm()
    }
}