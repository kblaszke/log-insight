package pl.blaszak.loginsight.app.config

import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldContain
import io.modelcontextprotocol.kotlin.sdk.server.Server
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("mcp")
class McpLogStatsSpec(
    private val mcpServer: Server
) : FunSpec() {
    init {
        test("should register both query and stats analysis tools in the MCP server") {
            val registeredTools = mcpServer.tools.keys

            registeredTools shouldContain "query_logs"
            registeredTools shouldContain "analyze_log_stats"
        }
    }

    override fun extensions() = listOf(SpringExtension)
}