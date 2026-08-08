package pl.blaszak.loginsight.core.parser

import io.kotest.core.spec.style.FunSpec
// don't use kotest datatest extension to avoid extra dependency; use plain collections
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
// removed bad import
import pl.blaszak.loginsight.core.model.LogLevel
import java.time.Instant

class LogParserSpec : FunSpec({

    context("parsing valid log lines") {
        val cases = listOf(
            Row("[2026-08-06T12:00:00Z] [INFO] Application started", Instant.parse("2026-08-06T12:00:00Z"), LogLevel.INFO, "Application started"),
            Row("[2026-08-06T12:05:30.123Z] [WARN] Slow DB query detected", Instant.parse("2026-08-06T12:05:30.123Z"), LogLevel.WARN, "Slow DB query detected"),
            Row("[2026-08-06T12:10:00Z] [ERROR] Connection timeout", Instant.parse("2026-08-06T12:10:00Z"), LogLevel.ERROR, "Connection timeout")
        )
        cases.forEach { row ->
            val result = LogParser.parseLine(row.rawLine)
            result shouldNotBe null
            result!!.timestamp shouldBe row.expectedTimestamp
            result.level shouldBe row.expectedLevel
            result.message.value shouldBe row.expectedMessage
        }
    }

    context("handling invalid log lines gracefully") {
        val invalids = listOf(
            "invalid line format",
            "[invalid-date] [INFO] Message",
            "[2026-08-06T12:00:00Z] [UNKNOWN_LEVEL] Message",
            "[2026-08-06T12:00:00Z] [] Empty level"
        )
        invalids.forEach { invalidLine ->
            LogParser.parseLine(invalidLine) shouldBe null
        }
    }
})

data class Row(
    val rawLine: String,
    val expectedTimestamp: Instant,
    val expectedLevel: LogLevel,
    val expectedMessage: String
)

