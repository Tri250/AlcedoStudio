package com.ahuamiao.adcleaner

import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * 执行进度的语音播报（吸收自 william-oy/Android-Phone-Ad-Cleaner 的适老化反馈）。
 * 说哪句话由 view.py 的 announcements() 决定（界面判断在 Python，CI 测得到），这里只管说出来。
 * 没有语音开关时不初始化；设备没有 TTS 引擎（个别精简 ROM）或初始化失败时静默降级：不播报、不报错、不重试。
 */
object Voice {
    private val main = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var ready = false
    private var failed = false
    /** 引擎初始化完成前要说的那一句；就绪后补上，初始化失败就丢掉。 */
    private var pending: Pair<String, Boolean>? = null

    /** 用户打开开关时先连一次引擎：等真正开跑，第一句就能说上。 */
    fun warmup() = main.post { ensure() }

    /** 进度类播报打断上一句（flush），免得慢一句追一句；结束句排到队尾（add），不被后续打断。 */
    @JvmStatic
    fun speak(text: String, flush: Boolean = true) = main.post {
        if (failed || text.isBlank()) return@post
        val engine = ensure()
        if (engine == null || !ready) {
            pending = text to flush
            return@post
        }
        utter(engine, text, flush)
    }

    private fun utter(engine: TextToSpeech, text: String, flush: Boolean) {
        runCatching {
            engine.speak(text, if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, "aac_voice")
        }
    }

    /** 惰性初始化：第一次要用才连 TTS 引擎，全程主线程（创建与回调都在主线程，不用加锁）。 */
    private fun ensure(): TextToSpeech? {
        tts?.let { return it }
        if (failed) return null
        var engine: TextToSpeech? = null
        engine = TextToSpeech(Native.app) { status ->
            ready = status == TextToSpeech.SUCCESS
            failed = !ready
            if (ready) {
                runCatching { engine?.language = Locale.SIMPLIFIED_CHINESE }
                pending?.let { (text, flush) -> engine?.let { utter(it, text, flush) } }
            }
            pending = null
        }
        tts = engine
        return engine
    }
}
