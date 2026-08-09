package pl.blaszak.loginsight.app.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
open class JacksonConfig {

    /**
     * Creates a primary ObjectMapper bean with Kotlin module support
     * for proper serialization/deserialization of Kotlin value classes and data classes.
     */
    @Bean
    @Primary
    open fun objectMapper(): ObjectMapper =
        ObjectMapper().registerModule(KotlinModule.Builder().build())

    /**
     * Customizes Spring Boot's default ObjectMapper builder to ensure the Kotlin module
     * is registered in all ObjectMapper instances created by Spring Boot.
     */
    @Bean
    open fun kotlinModuleCustomizer(): Jackson2ObjectMapperBuilderCustomizer {
        return Jackson2ObjectMapperBuilderCustomizer { builder ->
            builder.modules(KotlinModule.Builder().build())
        }
    }
}











