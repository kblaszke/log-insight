package pl.blaszak.loginsight.core.stream

import pl.blaszak.loginsight.core.model.LogEntry
import pl.blaszak.loginsight.core.model.LogLevel
import pl.blaszak.loginsight.core.model.LogStats
import pl.blaszak.loginsight.core.model.ErrorSummary
import pl.blaszak.loginsight.core.parser.LogParser
import kotlinx.coroutines.flow.*

class LogPipeline {

    /**
     * Transforms an asynchronous flow of raw text lines into a flow of domain [LogEntry] objects.
     */
    fun streamFromLines(lines: Flow<String>): Flow<LogEntry> {
        return lines.mapNotNull { line -> LogParser.parseLine(line) }
    }

    /**
     * Collects statistics from a flow of log entries, counting entries by log level.
     */
    suspend fun collectStats(entries: Flow<LogEntry>): Map<LogLevel, Long> {
        return entries.fold(emptyMap()) { acc, entry ->
            acc + (entry.level to (acc[entry.level] ?: 0L) + 1L)
        }
    }

    /**
     * Calculates high-level statistics from a lazy sequence of raw log lines.
     * This utilizes a single-pass sequence pipeline to ensure zero-allocation memory footprint
     * even when processing extremely large log files.
     */
    fun calculateStats(lines: Sequence<String>): LogStats {
        var totalEntries = 0L
        val levelCounts = mutableMapOf<LogLevel, Long>()
        val errorCounts = mutableMapOf<String, Long>()

        lines.forEach { line ->
            val entry = LogParser.parseLine(line)
            if (entry != null) {
                totalEntries++
                levelCounts[entry.level] = (levelCounts[entry.level] ?: 0L) + 1L

                if (entry.level == LogLevel.ERROR) {
                    val normalizedMsg = normalizeErrorMessage(entry.message.value)
                    errorCounts[normalizedMsg] = (errorCounts[normalizedMsg] ?: 0L) + 1L
                }
            }
        }

        val topErrors = errorCounts.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { ErrorSummary(it.key, it.value) }

        return LogStats(
            totalEntries = totalEntries,
            levelCounts = levelCounts.toMap(),
            topErrors = topErrors
        )
    }

    /**
     * Masks specific dynamic segments (UUIDs, digits) to cluster similar errors together
     * and trims length to prevent memory leakage with unique long messages.
     */
    private fun normalizeErrorMessage(message: String): String {
        return message
            .replace(Regex("\\b[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}\\b"), "<UUID>")
            .replace(Regex("\\d+"), "<NUM>")
            .trim()
            .take(120)
    }
}

/**
 * Filters the log stream to keep only entries with a level equal to or higher than [minLevel].
 * Declared as a top-level extension function for proper Kotlin package-level routing.
 */
fun Flow<LogEntry>.filterByLevel(minLevel: LogLevel): Flow<LogEntry> {
    return filter { entry -> entry.level.ordinal >= minLevel.ordinal }
}