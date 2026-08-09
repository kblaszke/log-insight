package pl.blaszak.loginsight.app.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class OpenApiConfig {

    @Bean
    open fun openApi(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("LogInsight API")
                .description("Reactive log streaming and analysis API")
                .version("1.0.0")
        )
}

