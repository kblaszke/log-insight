package pl.blaszak.loginsight.app

import java.lang.management.ManagementFactory

private const val BLOCK_HOUND_REDEFINITION_FLAG = "-XX:+AllowRedefinitionToAddDeleteMethods"

internal fun isBlockHoundInstrumentationSupported(): Boolean {
    return BLOCK_HOUND_REDEFINITION_FLAG in ManagementFactory.getRuntimeMXBean().inputArguments
}
