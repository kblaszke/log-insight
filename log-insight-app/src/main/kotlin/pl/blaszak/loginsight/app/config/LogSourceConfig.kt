package pl.blaszak.loginsight.app.config

import com.google.cloud.logging.LoggingOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import pl.blaszak.loginsight.core.logSource.LocalLogSourceProvider
import pl.blaszak.loginsight.core.logSource.GcpLogSourceProvider
import pl.blaszak.loginsight.core.logSource.LogSourceProvider
import java.io.File

@Configuration(proxyBeanMethods = false)
class LogSourceConfig {

    @Value($$"${log-insight.source.type:local}")
    private lateinit var logSourceType: String

    @Value($$"${log-insight.mcp.target-file-path:logs/app.log}")
    private lateinit var targetFilePath: String

    @Value($$"${log-insight.gcp.filter:resource.type=\"gce_instance\"}")
    private lateinit var gcpLogFilter: String

    @Bean
    fun logSourceProvider(): LogSourceProvider {
        return when (logSourceType.lowercase()) {
            "local" -> LocalLogSourceProvider(File(targetFilePath))
            "gcp" -> GcpLogSourceProvider(LoggingOptions.getDefaultInstance().service, gcpLogFilter)
            else -> throw IllegalArgumentException("Unsupported log source type: $logSourceType")
        }
    }
}