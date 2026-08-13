package pl.blaszak.loginsight.app.config

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain
import pl.blaszak.loginsight.app.service.McpLogQueryService
import pl.blaszak.loginsight.core.stream.LogPipeline
import java.io.File

class McpLogQueryServiceSpec : FunSpec({

    val logPipeline = LogPipeline()
    val service = McpLogQueryService(logPipeline)

    test("should correctly filter logs by severity level") {
        val tempFile = File.createTempFile("test-logs", ".log").apply {
            deleteOnExit()
            writeText(
                """
                [2026-08-08T12:00:00Z] [INFO] Test info message
                [2026-08-08T12:01:00Z] [ERROR] Test error message
                """.trimIndent()
            )
        }

        val result = service.queryLogs(tempFile, "ERROR", null, 10)

        result shouldHaveSize 1
        result[0] shouldContain "ERROR: Test error message"
    }

    test("should correctly filter logs by regex pattern") {
        val tempFile = File.createTempFile("test-logs-pattern", ".log").apply {
            deleteOnExit()
            writeText(
                """
                [2026-08-08T12:00:00Z] [INFO] Connection established
                [2026-08-08T12:01:00Z] [INFO] Connection timeout occurred
                """.trimIndent()
            )
        }

        val result = service.queryLogs(tempFile, null, "timeout", 10)

        result shouldHaveSize 1
        result[0] shouldContain "Connection timeout occurred"
    }

    test("should respect limit when returning results") {
        val tempFile = File.createTempFile("test-logs-limit", ".log").apply {
            deleteOnExit()
            writeText(
                """
                [2026-08-08T12:00:00Z] [INFO] Log 1
                [2026-08-08T12:01:00Z] [INFO] Log 2
                [2026-08-08T12:02:00Z] [INFO] Log 3
                """.trimIndent()
            )
        }

        val result = service.queryLogs(tempFile, null, null, 2)

        result shouldHaveSize 2
        result[0] shouldContain "Log 1"
        result[1] shouldContain "Log 2"
    }
})
