package pl.blaszak.loginsight.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.IOException

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
}