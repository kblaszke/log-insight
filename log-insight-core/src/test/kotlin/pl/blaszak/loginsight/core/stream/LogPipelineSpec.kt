package pl.blaszak.loginsight.parser

import pl.blaszak.loginsight.core.model.LogLevel
import pl.blaszak.loginsight.core.stream.LogPipeline
import pl.blaszak.loginsight.core.stream.LogPipeline.filterByLevel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList

class LogPipelineSpec : FunSpec({

    val rawLogs = listOf(
        "[2026-08-06T12:00:00Z] [INFO] Connection established",
        "[2026-08-06T12:01:00Z] [WARN] Slow database query",
        "[2026-08-06T12:02:00Z] [ERROR] NullPointerException at line 42",
        "[2026-08-06T12:03:00Z] [DEBUG] Cache hit"
    )

    test("should parse raw lines into Flow of LogEntries") {
        // Given
        val linesFlow = rawLogs.asFlow()

        // When
        val result = LogPipeline.streamFromLines(linesFlow).toList()

        // Then
        result.size shouldBe 4
        result[0].level shouldBe LogLevel.INFO   // Index 0 (first element) is INFO
        result[2].level shouldBe LogLevel.ERROR  // Index 2 (third element) is ERROR
    }

    test("should filter log entries by minimum level") {
        // Given
        val entriesFlow = LogPipeline.streamFromLines(rawLogs.asFlow())

        // When - filter only WARN and ERROR
        val filtered = entriesFlow.filterByLevel(LogLevel.WARN).toList()

        // Then
        filtered.size shouldBe 2
        filtered.map { it.level } shouldBe listOf(LogLevel.WARN, LogLevel.ERROR)
    }

    test("should correctly aggregate statistics from stream") {
        // Given
        val logsWithDuplicates = listOf(
            "[2026-08-06T12:00:00Z] [INFO] Info A",
            "[2026-08-06T12:01:00Z] [INFO] Info B",
            "[2026-08-06T12:02:00Z] [ERROR] Error A",
            "[2026-08-06T12:03:00Z] [WARN] Warn A"
        ).asFlow()

        val entriesFlow = LogPipeline.streamFromLines(logsWithDuplicates)

        // When
        val stats = LogPipeline.collectStats(entriesFlow)

        // Then
        stats[LogLevel.INFO] shouldBe 2L
        stats[LogLevel.ERROR] shouldBe 1L
        stats[LogLevel.WARN] shouldBe 1L
        stats.getOrDefault(LogLevel.DEBUG, 0L) shouldBe 0L
    }
})