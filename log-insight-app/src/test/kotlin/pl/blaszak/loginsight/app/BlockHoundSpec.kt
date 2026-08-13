package pl.blaszak.loginsight.app

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import reactor.blockhound.BlockingOperationError

class BlockHoundSpec : FunSpec({
    if (isBlockHoundInstrumentationSupported()) {
        test("should detect and intercept blocking call in non-blocking coroutine dispatcher") {
            shouldThrow<BlockingOperationError> {
                withContext(Dispatchers.Default) {
                    @Suppress("BlockingMethodInNonBlockingContext")
                    Thread.sleep(10)
                }
            }
        }
    }
})