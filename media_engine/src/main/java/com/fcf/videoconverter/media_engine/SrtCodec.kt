package com.fcf.videoconverter.media_engine

import com.fcf.videoconverter.domain.model.SubtitleCue

object SrtCodec {
    private val timeRegex = Regex("""(\d{2}):(\d{2}):(\d{2})[,.](\d{3})\s*-->\s*(\d{2}):(\d{2}):(\d{2})[,.](\d{3})""")

    fun parse(srtContent: String): List<SubtitleCue> {
        val blocks = srtContent.trim().split(Regex("""\r?\n\s*\r?\n"""))
        val cues = mutableListOf<SubtitleCue>()
        blocks.forEachIndexed { idx, block ->
            val lines = block.lines().map { it.trimEnd() }.filter { it.isNotEmpty() }
            if (lines.size >= 2) {
                val timeLineIndex = lines.indexOfFirst { timeRegex.containsMatchIn(it) }
                if (timeLineIndex >= 0) {
                    val match = timeRegex.find(lines[timeLineIndex])!!
                    val startMs = toMillis(match.groupValues[1], match.groupValues[2], match.groupValues[3], match.groupValues[4])
                    val endMs = toMillis(match.groupValues[5], match.groupValues[6], match.groupValues[7], match.groupValues[8])
                    val text = lines.drop(timeLineIndex + 1).joinToString("\n")
                    cues.add(SubtitleCue(index = cues.size + 1, startMs = startMs, endMs = endMs, text = text))
                }
            }
        }
        return cues
    }

    fun shiftTiming(cues: List<SubtitleCue>, offsetMs: Long): List<SubtitleCue> =
        cues.map { cue ->
            cue.copy(
                startMs = (cue.startMs + offsetMs).coerceAtLeast(0L),
                endMs = (cue.endMs + offsetMs).coerceAtLeast(100L)
            )
        }

    fun serialize(cues: List<SubtitleCue>): String = buildString {
        cues.forEachIndexed { idx, cue ->
            append(idx + 1).append('\n')
            append(formatMillis(cue.startMs)).append(" --> ").append(formatMillis(cue.endMs)).append('\n')
            append(cue.text.trim()).append("\n\n")
        }
    }

    private fun toMillis(h: String, m: String, s: String, ms: String): Long =
        h.toLong() * 3_600_000L + m.toLong() * 60_000L + s.toLong() * 1_000L + ms.toLong()

    private fun formatMillis(totalMs: Long): String {
        val clamped = totalMs.coerceAtLeast(0L)
        val hours = clamped / 3_600_000L
        val minutes = (clamped % 3_600_000L) / 60_000L
        val seconds = (clamped % 60_000L) / 1_000L
        val millis = clamped % 1_000L
        return "%02d:%02d:%02d,%03d".format(hours, minutes, seconds, millis)
    }
}
