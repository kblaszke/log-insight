package pl.blaszak.loginsight.core.parser

import pl.blaszak.loginsight.core.model.LogEntry
import pl.blaszak.loginsight.core.model.LogLevel
import pl.blaszak.loginsight.core.model.LogMessage
import java.time.Instant
import java.time.format.DateTimeParseException

object LogParser {
    // Regex for: [2026-08-06T12:00:00Z] [LEVEL] Message
    private val logPattern = Regex("""^\[([^\]]+)\]\s+\[([^\]]+)\]\s+(.+)$""")

    fun parseLine(line: String): LogEntry? {
        val matchResult = logPattern.matchEntire(line.trim()) ?: return null
        val (timestampStr, levelStr, messageStr) = matchResult.destructured

        val timestamp = try {
            Instant.parse(timestampStr)
        } catch (e: DateTimeParseException) {
            return null
        }

        val level = try {
            LogLevel.valueOf(levelStr.uppercase())
        } catch (e: IllegalArgumentException) {
            return null
        }

        return LogEntry(
            timestamp = timestamp,
            level = level,
            message = LogMessage(messageStr)
        )
    }
}

