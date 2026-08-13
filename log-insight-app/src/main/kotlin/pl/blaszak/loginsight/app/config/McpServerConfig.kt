package pl.blaszak.loginsight.app.config

import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.TextContent

import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
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
    // Use @param:Value to target the constructor parameter explicitly
    @param:Value($$"${log-insight.mcp.target-file-path:logs/app.log}")
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

        // Using compliant ToolSchema instead of deprecated Tool.Input to prevent silent client drops
        server.addTool(
            name = "query_logs",
            description = "Query and filter log entries by severity level and regex pattern",
            inputSchema = ToolSchema(
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
            val levelFilter = (request.arguments?.get("level") as? JsonPrimitive)?.content
            val patternFilter = (request.arguments?.get("pattern") as? JsonPrimitive)?.content
            val limit = (request.arguments?.get("limit") as? JsonPrimitive)?.content?.toIntOrNull() ?: 50

            log.info("MCP Tool query_logs invoked with level={}, pattern={}, limit={}", levelFilter, patternFilter, limit)

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

            val logEntries = mcpLogQueryService.queryLogs(logFile, levelFilter, patternFilter, limit)

            CallToolResult(
                content = listOf(
                    TextContent(
                        text = logEntries.joinToString("\n")
                    )
                )
            )
        }

        return server
    }

    @PreDestroy
    fun shutdownMcpServer() {
        log.info("Shutting down MCP Server Coroutine Scope...")
        mcpScope.cancel()
    }
}