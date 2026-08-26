package pl.blaszak.loginsight.logsCreator

import java.io.File
import java.time.Instant
import java.util.UUID

fun main() {
    val logFile = File("logs/app.log")
    logFile.parentFile.mkdirs()

    logFile.bufferedWriter().use { writer ->
        var current = Instant.now().minusSeconds(3600)

        // 1. Wygeneruj standardowy ruch INFO
        for (i in 1..500) {
            current = current.plusMillis(100)
            writer.write("[$current] [INFO] [transaction-${UUID.randomUUID()}] Request processed successfully in ${10 + (i % 15)}ms\n")
        }

        // 2. Wstrzyknij katastrofę Netty (Anomalia blokowania wątku)
        current = current.plusSeconds(5)
        writer.write("[$current] [WARN] [reactor-http-nio-2] [NettyEventLoop] Thread 'reactor-http-nio-2' has been blocked for 35000ms! (Possible blocking I/O operation on non-blocking thread)\n")

        current = current.plusMillis(200)
        writer.write("[$current] [ERROR] [reactor-http-nio-2] [LogPipeline] Critical Exception in Reactive Stream processing: reactor.blockhound.BlockingOperationError: Blocking call! java.io.FileInputStream.readBytes\n")
        writer.write("    at reactor.blockhound.BlockHound\$Builder.lambda\$install\$8(BlockHound.java:427)\n")
        writer.write("    at java.base/java.io.FileInputStream.readBytes(Native Method)\n")
        writer.write("    at java.base/java.io.FileInputStream.read(FileInputStream.java:287)\n")
        writer.write("    at pl.blaszak.loginsight.app.FileReader\$readFileLines\$1.invokeSuspend(FileReader.kt:18)\n")

        // 3. Wygeneruj błędy o wysokiej kardynalności (test dla grupowania i maskowania UUID w analyze_log_stats)
        for (i in 1..200) {
            current = current.plusMillis(50)
            val badUuid = UUID.randomUUID().toString()
            writer.write("[$current] [ERROR] [payment-worker] [PaymentGateway] Failed to settle transaction with ID $badUuid: Connection timed out to gateway api.gateway.com/v1/settle\n")
        }
    }
    println("Successfully generated logs with targeted anomalies at ${logFile.absolutePath}")
}