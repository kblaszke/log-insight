package pl.blaszak.loginsight.core.stream

import pl.blaszak.loginsight.core.model.LogEntry
import pl.blaszak.loginsight.core.model.LogLevel
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
        return entries.fold(emptyMap<LogLevel, Long>()) { acc, entry ->
            acc + (entry.level to (acc[entry.level] ?: 0L) + 1L)
        }
    }
}

/**
 * Filters the log stream to keep only entries with a level equal to or higher than [minLevel].
 * Declared as a top-level extension function for proper Kotlin package-level routing.
 */
fun Flow<LogEntry>.filterByLevel(minLevel: LogLevel): Flow<LogEntry> {
    return filter { entry -> entry.level.ordinal >= minLevel.ordinal }
}