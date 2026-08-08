package pl.blaszak.loginsight.core.stream

import pl.blaszak.loginsight.core.model.LogEntry
import pl.blaszak.loginsight.core.model.LogLevel
import pl.blaszak.loginsight.core.parser.LogParser
import kotlinx.coroutines.flow.*

object LogPipeline {

    /**
     * Transforms an asynchronous flow of raw text lines into a flow of domain [LogEntry] objects.
     */
    fun streamFromLines(lines: Flow<String>): Flow<LogEntry> {
        return lines
            .mapNotNull { line -> LogParser.parseLine(line) }
    }

    /**
     * Filters the log stream to keep only entries with a level equal to or higher than [minLevel].
     */
    fun Flow<LogEntry>.filterByLevel(minLevel: LogLevel): Flow<LogEntry> {
        return filter { entry -> entry.level.ordinal >= minLevel.ordinal }
    }

    /**
     * Aggregates log statistics by counting occurrences of each log level.
     * This is a suspending terminal operation that consumes the flow.
     */
    suspend fun collectStats(entries: Flow<LogEntry>): Map<LogLevel, Long> {
        val stats = mutableMapOf<LogLevel, Long>()
        entries.collect { entry ->
            stats[entry.level] = stats.getOrDefault(entry.level, 0L) + 1
        }
        return stats
    }
}