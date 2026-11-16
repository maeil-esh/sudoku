package com.uk2solutions.sudoku.domain.challenge

import com.uk2solutions.sudoku.domain.model.Board
import com.uk2solutions.sudoku.domain.model.Difficulty
import com.uk2solutions.sudoku.domain.solver.Solver
import java.util.Base64

/** 한 판의 결과(도전장에 실어 보내는 값). */
data class ChallengeResult(val timeSec: Int, val mistakes: Int, val hints: Int, val score: Int) {
    fun serialize() = "$timeSec,$mistakes,$hints,$score"

    companion object {
        fun parse(s: String?): ChallengeResult? {
            val p = s?.split(",")?.mapNotNull { it.toIntOrNull() } ?: return null
            return if (p.size == 4) ChallengeResult(p[0], p[1], p[2], p[3]) else null
        }
    }
}

/** 도전장: 퍼즐(givens)과, 보낸 사람의 결과(선택). 정답은 받는 쪽에서 풀이기로 구한다(유일해). */
data class Challenge(val difficulty: Difficulty, val givens: String, val result: ChallengeResult?)

enum class Outcome { WIN, LOSE, DRAW }

object ChallengeCompare {
    /** 점수가 높은 쪽이 이기고, 같으면 시간이 짧은 쪽이 이긴다. */
    fun outcome(mine: ChallengeResult, theirs: ChallengeResult): Outcome = when {
        mine.score != theirs.score -> if (mine.score > theirs.score) Outcome.WIN else Outcome.LOSE
        mine.timeSec != theirs.timeSec -> if (mine.timeSec < theirs.timeSec) Outcome.WIN else Outcome.LOSE
        else -> Outcome.DRAW
    }
}

/**
 * 서버 없는 도전장 코드. `SDK1-` + base64url(버전, 난이도, 플래그, 퍼즐 81칸(4비트씩 41바이트), [결과], CRC16).
 * 공유 문구 안에 섞여 있어도 정규식으로 찾아낸다. 손상·조작·유일해가 아닌 퍼즐은 거부한다.
 */
object ChallengeCode {
    const val PREFIX = "SDK1-"
    private const val VERSION = 1
    private val TOKEN = Regex("SDK1-[A-Za-z0-9_-]{40,}")

    fun containsCode(text: String?): Boolean = text != null && TOKEN.containsMatchIn(text)

    fun encode(c: Challenge): String {
        val g = Board.parse(c.givens)
        val out = ArrayList<Byte>()
        out += VERSION.toByte(); out += c.difficulty.ordinal.toByte(); out += (if (c.result != null) 1 else 0).toByte()
        var i = 0
        while (i < 81) { // 숫자 두 칸을 한 바이트(상·하위 4비트)에
            val hi = g[i]; val lo = if (i + 1 < 81) g[i + 1] else 0
            out += ((hi shl 4) or lo).toByte(); i += 2
        }
        c.result?.let {
            val t = it.timeSec.coerceIn(0, 0xFFFFFF); val s = it.score.coerceIn(0, 0xFFFFFF)
            out += (t shr 16).toByte(); out += (t shr 8).toByte(); out += t.toByte()
            out += it.mistakes.coerceIn(0, 255).toByte(); out += it.hints.coerceIn(0, 255).toByte()
            out += (s shr 16).toByte(); out += (s shr 8).toByte(); out += s.toByte()
        }
        val body = out.toByteArray()
        val crc = crc16(body)
        val all = body + byteArrayOf((crc shr 8).toByte(), crc.toByte())
        return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(all)
    }

    /** 공유 문구 전체 또는 코드만 넣어도 된다. 유효하지 않으면 null. */
    fun decode(text: String?): Challenge? {
        val token = text?.let { TOKEN.find(it)?.value } ?: return null
        val bytes = try { Base64.getUrlDecoder().decode(token.removePrefix(PREFIX)) } catch (_: IllegalArgumentException) { return null }
        if (bytes.size < 44 + 2) return null
        val body = bytes.copyOfRange(0, bytes.size - 2)
        val crc = ((bytes[bytes.size - 2].toInt() and 0xFF) shl 8) or (bytes[bytes.size - 1].toInt() and 0xFF)
        if (crc != crc16(body)) return null
        if (body[0].toInt() != VERSION) return null
        val diff = Difficulty.entries.getOrNull(body[1].toInt()) ?: return null
        val hasResult = body[2].toInt() == 1
        if (body.size != 3 + 41 + (if (hasResult) 8 else 0)) return null
        val g = IntArray(81)
        for (i in 0 until 81) {
            val b = body[3 + i / 2].toInt() and 0xFF
            g[i] = if (i % 2 == 0) b shr 4 else b and 0x0F
            if (g[i] > 9) return null
        }
        if (Board.clueCount(g) < 17 || Solver.countSolutions(g, 2) != 1) return null
        val result = if (hasResult) {
            fun u(i: Int) = body[i].toInt() and 0xFF
            ChallengeResult(
                timeSec = (u(44) shl 16) or (u(45) shl 8) or u(46), mistakes = u(47), hints = u(48),
                score = (u(49) shl 16) or (u(50) shl 8) or u(51),
            )
        } else null
        return Challenge(diff, Board.format(g), result)
    }

    private fun crc16(data: ByteArray): Int {
        var crc = 0xFFFF
        for (b in data) {
            crc = crc xor ((b.toInt() and 0xFF) shl 8)
            repeat(8) { crc = if (crc and 0x8000 != 0) (crc shl 1) xor 0x1021 else crc shl 1; crc = crc and 0xFFFF }
        }
        return crc
    }
}
