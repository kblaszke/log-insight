package pl.blaszak.loginsight.core.logSource

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds

object FileReader {

    /**
     * Reads a file line-by-line asynchronously and emits each line into a [Flow].
     * Uses [flowOn] with [Dispatchers.IO] to safely offload blocking I/O operations.
     */
    fun readFileLines(file: File): Flow<String> = flow {
        if (!file.exists() || !file.isFile) {
            throw IOException("Target file does not exist or is not a valid file: ${file.absolutePath}")
        }
        file.bufferedReader().use { reader ->
            var line = reader.readLine()
            while (line != null) {
                emit(line)
                line = reader.readLine()
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Continuously tails a file, emitting new lines as they are appended.
     * Uses [delay] to suspend and avoid blocking the thread when no new lines are available.
     */
    fun tailFileLines(file: File): Flow<String> = flow {
        if (!file.exists() || !file.isFile) {
            throw IOException("Target file does not exist or is not a valid file: ${file.absolutePath}")
        }
        file.bufferedReader().use { reader ->
            while (true) {
                val line = reader.readLine()
                if (line != null) {
                    emit(line)
                } else {
                    // Suspend the coroutine to yield control and avoid CPU spinning
                    delay(100.milliseconds)
                }
            }
        }
    }.flowOn(Dispatchers.IO)
}