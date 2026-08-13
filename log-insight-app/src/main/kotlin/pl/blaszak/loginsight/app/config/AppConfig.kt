package pl.blaszak.loginsight.app.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import pl.blaszak.loginsight.core.stream.LogPipeline

@Configuration(proxyBeanMethods = false)
class AppConfig {

    @Bean
    fun logPipeline(): LogPipeline = LogPipeline()
}