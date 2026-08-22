package pl.blaszak.loginsight.core.logSource

import com.google.cloud.logging.Logging
import com.google.cloud.logging.Payload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

class GcpLogSourceProvider(
    private val logging: Logging,
    private val logFilter: String
) : LogSourceProvider {

    override fun getLogLinesSequence(): Sequence<String> {
        val logEntries = logging.listLogEntries(
            Logging.EntryListOption.filter(logFilter)
        )
        return logEntries.iterateAll().asSequence().mapNotNull { entry ->
            // Explicit type annotation ensures getPayload<T>() method is invoked,
            // completely avoiding Kotlin's synthetic property resolution conflict with the private field 'payload'.
            val payload: Payload<*>? = entry.getPayload()
            payload?.getData()?.toString()
        }
    }

    override fun streamLogLines(): Flow<String> = flow {
        var lastTimestamp = Instant.now().toString()
        while (true) {
            val dynamicFilter = "$logFilter AND timestamp > \"$lastTimestamp\""
            val logEntries = logging.listLogEntries(Logging.EntryListOption.filter(dynamicFilter))

            var latestInstant = Instant.parse(lastTimestamp)
            for (entry in logEntries.iterateAll()) {
                val payload: Payload<*>? = entry.getPayload()
                val payloadStr = payload?.getData()?.toString()
                if (payloadStr != null) {
                    emit(payloadStr)
                }

                // entry.instantTimestamp maps to Java's public getInstantTimestamp() method,
                // which returns java.time.Instant instead of the deprecated Long returned by getTimestamp().
                val entryInstant = entry.instantTimestamp
                if (entryInstant.isAfter(latestInstant)) {
                    latestInstant = entryInstant
                }
            }
            lastTimestamp = latestInstant.toString()

            // Resolves "Legacy Long overload can be converted to Duration" warning
            // by using modern kotlinx.coroutines delay overload accepting Kotlin Duration.
            delay(5.seconds)
        }
    }.flowOn(Dispatchers.IO)
}