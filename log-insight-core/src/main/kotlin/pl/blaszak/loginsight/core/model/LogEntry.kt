package pl.blaszak.loginsight.core.model

import java.time.Instant

@JvmInline
value class LogMessage(val value: String)

data class LogEntry(
    val timestamp: Instant,
    val level: LogLevel,
    val message: LogMessage
)
