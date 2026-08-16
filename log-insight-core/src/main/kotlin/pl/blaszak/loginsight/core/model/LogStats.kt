package pl.blaszak.loginsight.core.model

data class LogStats(
    val totalEntries: Long,
    val levelCounts: Map<LogLevel, Long>,
    val topErrors: List<ErrorSummary>
)

data class ErrorSummary(
    val message: String,
    val count: Long
)