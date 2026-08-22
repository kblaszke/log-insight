package pl.blaszak.loginsight.core.logSource

import kotlinx.coroutines.flow.Flow
import java.io.File

class LocalLogSourceProvider(
    private val targetFile: File
) : LogSourceProvider {

    override fun getLogLinesSequence(): Sequence<String> {
        return targetFile.useLines { lines -> lines.constrainOnce().toList().asSequence() }
    }

    override fun streamLogLines(): Flow<String> {
        return FileReader.tailFileLines(targetFile)
    }
}