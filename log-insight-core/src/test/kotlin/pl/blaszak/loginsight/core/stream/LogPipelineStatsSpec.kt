package pl.blaszak.loginsight.core.stream

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.inspectors.forExactly
import pl.blaszak.loginsight.core.model.LogLevel

class LogPipelineStatsSpec : FunSpec({
    val logPipeline = LogPipeline()

    test("should calculate correct log statistics and normalize error messages") {
        // Given
        val rawLogs = listOf(
            "[2026-08-15T10:00:00Z] [INFO] Service started successfully",
            "[2026-08-15T10:01:00Z] [WARN] Connection timeout, retrying...",
            "[2026-08-15T10:02:00Z] [ERROR] Database connection failed for user 42",
            "[2026-08-15T10:03:00Z] [ERROR] Database connection failed for user 109",
            "[2026-08-15T10:04:00Z] [ERROR] Out of memory error with UUID f81d4fae-7dec-11d0-a765-00a0c91e6bf6",
            "Invalid garbage line"
        )

        // When
        val stats = logPipeline.calculateStats(rawLogs.asSequence())

        // Then
        stats.totalEntries shouldBe 5L
        stats.levelCounts[LogLevel.INFO] shouldBe 1L
        stats.levelCounts[LogLevel.WARN] shouldBe 1L
        stats.levelCounts[LogLevel.ERROR] shouldBe 3L

        stats.topErrors shouldHaveSize 2

        // Verify regex masking worked as expected
        stats.topErrors.forExactly(1) { error ->
            error.message shouldBe "Database connection failed for user <NUM>"
            error.count shouldBe 2L
        }

        stats.topErrors.forExactly(1) { error ->
            error.message shouldBe "Out of memory error with UUID <UUID>"
            error.count shouldBe 1L
        }
    }
})