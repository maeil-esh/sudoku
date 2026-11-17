package com.uk2solutions.sudoku.ui.game

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * 효과음을 직접 합성해 재생한다(음원 파일·권한 불필요).
 * 기기의 "터치 소리" 설정(대부분 꺼져 있음)과 무관하게 미디어 볼륨으로 들린다.
 */
class SoundFx {
    private val rate = 22_050
    private val tap = build(listOf(Tone(0.0, 0.07, 760.0), Tone(0.0, 0.07, 1520.0, 0.35)), 0.07)
    private val mistake = build(listOf(Tone(0.0, 0.18, 190.0), Tone(0.0, 0.18, 143.0, 0.8)), 0.18)
    private val cheer = build(listOf(Tone(0.00, 0.14, 784.0), Tone(0.09, 0.14, 988.0), Tone(0.18, 0.26, 1175.0)), 0.44)

    private class Tone(val start: Double, val len: Double, val hz: Double, val gain: Double = 1.0)

    private fun build(tones: List<Tone>, total: Double): AudioTrack {
        val n = (rate * total).toInt()
        val buf = ShortArray(n)
        for (t in tones) {
            val from = (t.start * rate).toInt()
            val cnt = (t.len * rate).toInt()
            for (i in 0 until cnt) {
                val idx = from + i
                if (idx >= n) break
                val env = exp(-6.0 * i / cnt) * (if (i < 80) i / 80.0 else 1.0)  // 부드러운 시작 + 감쇠
                val v = sin(2 * PI * t.hz * i / rate) * env * t.gain * 0.45
                buf[idx] = (buf[idx] + (v * Short.MAX_VALUE).toInt()).coerceIn(-32768, 32767).toShort()
            }
        }
        val track = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(buf.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(buf, 0, buf.size)
        return track
    }

    private fun play(t: AudioTrack) {
        runCatching {
            t.stop()
            t.reloadStaticData()
            t.play()
        }
    }

    fun tap() = play(tap)
    fun mistake() = play(mistake)
    fun cheer() = play(cheer)
    fun release() { runCatching { tap.release(); mistake.release(); cheer.release() } }
}
