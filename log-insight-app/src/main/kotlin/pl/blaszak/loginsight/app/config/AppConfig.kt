package pl.blaszak.loginsight.app.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import pl.blaszak.loginsight.core.stream.LogPipeline

@Configuration
open class AppConfig {

    /**
     * Explicitly declares the LogPipeline domain instance as a Spring Bean.
     * This keeps the core logic independent of Spring framework annotations.
     */
    @Bean
    open fun logPipeline(): LogPipeline {
        return LogPipeline
    }
}