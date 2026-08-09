package pl.blaszak.loginsight.app.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

/**
 * Data Transfer Object for LogEntry used in REST API responses.
 * This DTO is JSON-serializable and decouples the API contract from the domain model.
 */
data class LogEntryDto(
    @JsonProperty("timestamp")
    val timestamp: Instant,

    @JsonProperty("level")
    val level: String,

    @JsonProperty("message")
    val message: String
)

