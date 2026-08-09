package pl.blaszak.loginsight.app.controller

import pl.blaszak.loginsight.app.FileReader
import pl.blaszak.loginsight.app.dto.LogEntryDto
import pl.blaszak.loginsight.core.model.LogEntry
import pl.blaszak.loginsight.core.model.LogLevel
import pl.blaszak.loginsight.core.stream.LogPipeline
import pl.blaszak.loginsight.core.stream.LogPipeline.filterByLevel
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

@RestController
@RequestMapping("/api/logs")
class LogStreamController(
    private val logPipeline: LogPipeline
) {

    /**
     * Streams log entries asynchronously as Server-Sent Events (SSE).
     * Automatically converts Kotlin [Flow] to reactive stream chunks.
     * Returns LogEntryDto for JSON serialization compatibility.
     */
    @GetMapping("/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamLogs(
        @RequestParam(defaultValue = "INFO") minLevel: String
    ): Flow<LogEntryDto> {
        val level = runCatching { LogLevel.valueOf(minLevel.uppercase()) }
            .getOrDefault(LogLevel.INFO)

        // For demo purposes, we point to a temporary mock log file on disk
        val tempLogFile = createDemoLogFile()

        val rawLinesFlow = FileReader.readFileLines(tempLogFile)
        val parsedEntriesFlow = logPipeline.streamFromLines(rawLinesFlow)

        // Filter by level and map to DTO for JSON serialization
        return parsedEntriesFlow.filterByLevel(level)
            .map { entry -> entry.toDto() }
    }

    /**
     * Extension function to convert LogEntry domain model to LogEntryDto for JSON serialization.
     */
    private fun LogEntry.toDto(): LogEntryDto =
        LogEntryDto(
            timestamp = timestamp,
            level = level.name,
            message = message.value
        )

    private fun createDemoLogFile(): File {
        return File.createTempFile("live-server-logs", ".log").apply {
            deleteOnExit()
            writeText(
                """
                    [2026-08-08T12:00:00Z] [INFO] Server started
                    [2026-08-08T12:01:00Z] [DEBUG] Scanning ports
                    [2026-08-08T12:02:00Z] [WARN] Heavy CPU load
                    [2026-08-08T12:03:00Z] [ERROR] Service unavailable
                """.trimIndent()
            )
        }
    }
}