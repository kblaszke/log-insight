package pl.blaszak.loginsight.app.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import pl.blaszak.loginsight.app.FileReader
import pl.blaszak.loginsight.core.model.LogLevel
import pl.blaszak.loginsight.core.stream.LogPipeline
import java.io.File

@Service
class McpLogQueryService(
    private val logPipeline: LogPipeline
) {
    /**
     * Queries and filters logs from a target file based on severity level and regex pattern.
     */
    suspend fun queryLogs(
        logFile: File,
        levelFilter: String?,
        patternFilter: String?,
        limit: Int
    ): List<String> = withContext(Dispatchers.IO) {
        val rawLinesFlow = FileReader.readFileLines(logFile)
        var parsedFlow = logPipeline.streamFromLines(rawLinesFlow)

        // Filter by log level if specified
        if (levelFilter != null) {
            val level = runCatching { LogLevel.valueOf(levelFilter.uppercase()) }.getOrNull()
            if (level != null) {
                parsedFlow = parsedFlow.filter { it.level == level }
            }
        }

        // Filter by regex or substring pattern if specified
        if (patternFilter != null) {
            val regex = runCatching { Regex(patternFilter, RegexOption.IGNORE_CASE) }.getOrNull()
            parsedFlow = if (regex != null) {
                parsedFlow.filter { regex.containsMatchIn(it.message.value) }
            } else {
                parsedFlow.filter { it.message.value.contains(patternFilter, ignoreCase = true) }
            }
        }

        // Take up to the requested limit and map to formatted strings
        parsedFlow.take(limit)
            .toList()
            .map { entry -> "[${entry.timestamp}] ${entry.level}: ${entry.message.value}" }
    }
}