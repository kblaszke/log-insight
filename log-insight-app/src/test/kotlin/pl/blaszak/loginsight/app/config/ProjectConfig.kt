package pl.blaszak.loginsight.app.config

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.extensions.blockhound.BlockHound
import pl.blaszak.loginsight.app.isBlockHoundInstrumentationSupported

class ProjectConfig : AbstractProjectConfig() {
    override fun extensions() = if (isBlockHoundInstrumentationSupported()) {
        listOf(BlockHound())
    } else {
        emptyList()
    }
}