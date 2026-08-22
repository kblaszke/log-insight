package pl.blaszak.loginsight.core.logSource

import kotlinx.coroutines.flow.Flow

interface LogSourceProvider {
    /**
     * Returns a lazy Sequence for high-performance one-pass analytics.
     */
    fun getLogLinesSequence(): Sequence<String>

    /**
     * Streams log lines in a reactive, non-blocking manner.
     */
    fun streamLogLines(): Flow<String>
}