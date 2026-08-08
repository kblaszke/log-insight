package pl.blaszak.loginsight.app

import pl.blaszak.loginsight.core.model.LogLevel
import pl.blaszak.loginsight.core.stream.LogPipeline
import pl.blaszak.loginsight.core.stream.LogPipeline.filterByLevel
import kotlinx.coroutines.runBlocking
import java.io.File

fun main() {
    println("=== LogInsight Application Starting ===")

    // Create a temporary mock log file for offline local verification
    val tempLogFile = File.createTempFile("mock-app-logs", ".log").apply {
        deleteOnExit()
        writeText(
            """
                [2026-08-08T10:00:00Z] [INFO] Service started successfully
                [2026-08-08T10:01:15Z] [DEBUG] Cache checking
                [2026-08-08T10:02:30Z] [WARN] High memory usage detected
                [2026-08-08T10:05:00Z] [ERROR] Database connection failed
                [2026-08-08T10:06:12Z] [INFO] Retrying connection...
                [2026-08-08T10:07:05Z] [ERROR] Database connection failed again
            """.trimIndent()
        )
    }

    println("Reading mock logs from: ${tempLogFile.absolutePath}")

    // Bridging synchronous main method with coroutine context using runBlocking
    runBlocking {
        // 1. Get raw lines flow from file reader
        val rawLinesFlow = FileReader.readFileLines(tempLogFile)

        // 2. Map raw text flow to parsed domain LogEntry flow
        val parsedEntriesFlow = LogPipeline.streamFromLines(rawLinesFlow)

        // 3. Filter severe logs (WARN and above) and print them as they arrive
        println("\n--- Severe Logs (WARN & ERROR) ---")
        parsedEntriesFlow.filterByLevel(LogLevel.WARN).collect { entry ->
            println("[${entry.timestamp}] [${entry.level}] ${entry.message.value}")
        }

        // 4. Recome another stream to aggregate and print final statistics
        val freshLinesFlow = FileReader.readFileLines(tempLogFile)
        val freshEntriesFlow = LogPipeline.streamFromLines(freshLinesFlow)
        val stats = LogPipeline.collectStats(freshEntriesFlow)

        println("\n--- Log Statistics ---")
        stats.forEach { (level, count) ->
            println("${level.name}: $count")
        }
    }

    println("\n=== LogInsight Application Finished ===")
}