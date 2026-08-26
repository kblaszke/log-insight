package pl.blaszak.loginsight.app

import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["spring.main.web-application-type=reactive"]
)
@AutoConfigureWebTestClient
class LogStreamControllerSpec(
    private val webTestClient: WebTestClient
) : FunSpec() {

    init {
        test("should stream parsed logs via Server-Sent Events filtered by minLevel") {
            webTestClient.get()
                .uri("/api/logs/stream?minLevel=WARN")
                .exchange()
                .expectStatus().isOk
                .expectHeader().contentType("text/event-stream;charset=UTF-8")
        }
    }

    override fun extensions() = listOf(SpringExtension)
}
