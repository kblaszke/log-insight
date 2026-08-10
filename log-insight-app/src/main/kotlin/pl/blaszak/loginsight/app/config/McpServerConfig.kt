package pl.blaszak.loginsight.app.config

import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import pl.blaszak.loginsight.app.service.McpLogQueryService
import java.io.File

@Configuration(proxyBeanMethods = false)
class McpServerConfig(
    private val mcpLogQueryService: McpLogQueryService,
    // Injecting the path to the real log file from application properties with a fallback
    @param: Value("\${log-insight.mcp.target-file-path:logs/app.log}")
    private val targetFilePath: String
) {

    private val log = LoggerFactory.getLogger(McpServerConfig::class.java)
    private val mcpScope = CoroutineScope(Dispatchers.Default)

    @Bean
    fun mcpServer(): Server {
        val server = Server(
            serverInfo = Implementation(
                name = "log-insight-mcp-server",
                version = "1.0.0"
            ),
            options = ServerOptions(
                capabilities = ServerCapabilities(
                    tools = ServerCapabilities.Tools(listChanged = true)
                )
            )
        )

        server.addTool(
            name = "query_logs",
            description = "Query and filter log entries by severity level and regex pattern",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    putJsonObject("level") {
                        put("type", "string")
                        put("description", "Optional severity level to filter by (e.g., ERROR, WARN, INFO, DEBUG)")
                    }
                    putJsonObject("pattern") {
                        put("type", "string")
                        put("description", "Optional regex or substring pattern to match against log messages")
                    }
                    putJsonObject("limit") {
                        put("type", "integer")
                        put("description", "Maximum number of log entries to return (default: 50)")
                    }
                }
            )
        ) { request ->
            val levelFilter = (request.arguments["level"] as? JsonPrimitive)?.content
            val patternFilter = (request.arguments["pattern"] as? JsonPrimitive)?.content
            val limit = (request.arguments["limit"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 50

            log.info("MCP Tool query_logs invoked with level={}, pattern={}, limit={}", levelFilter, patternFilter, limit)

            // Reference the real, configured log file
            val logFile = File(targetFilePath)
            if (!logFile.exists()) {
                return@addTool CallToolResult(
                    content = listOf(
                        TextContent(
                            text = "Target log file not found at: ${logFile.absolutePath}. Please check server configuration."
                        )
                    ),
                    isError = true
                )
            }

            // Await the suspending call directly without runBlocking wrapper
            val logEntries = mcpLogQueryService.queryLogs(logFile, levelFilter, patternFilter, limit)

            CallToolResult(
                content = listOf(
                    TextContent(
                        text = logEntries.joinToString("\n")
                    )
                )
            )
        }

        log.info("Initializing MCP Server connection via Stdio transport...")
        mcpScope.launch {
            try {
                val transport = StdioServerTransport()
                server.connect(transport)
                log.info("MCP Server successfully connected and listening.")
            } catch (e: Exception) {
                log.error("Error during MCP Server connection lifecycle", e)
            }
        }

        return server
    }

    @PreDestroy
    fun shutdownMcpServer() {
        log.info("Shutting down MCP Server Coroutine Scope...")
        mcpScope.cancel()
    }
}