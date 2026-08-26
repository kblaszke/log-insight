package pl.blaszak.loginsight.app.config

import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
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
import org.springframework.context.annotation.Profile
import pl.blaszak.loginsight.app.service.McpLogQueryService
import java.io.File
import kotlinx.coroutines.launch
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import org.springframework.boot.ApplicationRunner

@Configuration(proxyBeanMethods = false)
@Profile("mcp")
class McpServerConfig(
    private val mcpLogQueryService: McpLogQueryService,
    @param:Value("\${log-insight.mcp.target-file-path:logs/app.log}") private val targetFilePath: String
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

        // Tool 1: Raw querying & filtering
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
                        TextContent(text = "Target log file not found at: ${logFile.absolutePath}.")
                    ),
                    isError = true
                )
            }

            val logEntries = mcpLogQueryService.queryLogs(logFile, levelFilter, patternFilter, limit)
            CallToolResult(content = listOf(TextContent(text = logEntries.joinToString("\n"))))
        }

        // Tool 2: High-level sequence diagnostics
        server.addTool(
            name = "analyze_log_stats",
            description = "Analyze the log file to calculate high-level statistics, severity distributions, and top error patterns",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    putJsonObject("filePath") {
                        put("type", "string")
                        put("description", "Optional custom path to the log file. If omitted, the default log file is analyzed.")
                    }
                }
            )
        ) { request ->
            val customPath = (request.arguments?.get("filePath") as? JsonPrimitive)?.content
            val logFilePath = customPath ?: targetFilePath
            val logFile = File(logFilePath)

            log.info("MCP Tool analyze_log_stats invoked for file={}", logFilePath)

            if (!logFile.exists()) {
                return@addTool CallToolResult(
                    content = listOf(
                        TextContent(text = "Target log file not found at: ${logFile.absolutePath}.")
                    ),
                    isError = true
                )
            }

            val stats = mcpLogQueryService.calculateStats(logFile)

            if (stats.totalEntries == 0L) {
                return@addTool CallToolResult(
                    content = listOf(
                        TextContent(text = "Log file is empty or no valid log entries could be parsed.")
                    )
                )
            }

            val responseText = """
                ### Log Analysis Summary for ${logFile.name}

                * **Total Valid Entries Processed**: ${stats.totalEntries}

                #### Severity Level Distribution:
                ${stats.levelCounts.entries.joinToString("\n") { (level, count) ->
                val percent = (count.toDouble() / stats.totalEntries) * 100
                "* **$level**: $count (${String.format("%.2f", percent)}%)"
            }}

                #### Top Error Patterns (LogLevel ERROR):
                ${if (stats.topErrors.isEmpty()) "No error patterns detected." else stats.topErrors.joinToString("\n") { error ->
                "* **Count**: ${error.count} - `${error.message}`"
            }}
            """.trimIndent()

            CallToolResult(
                content = listOf(TextContent(text = responseText))
            )
        }

        return server
    }

    @Bean
    fun mcpServerRunner(mcpServer: Server): ApplicationRunner {
        return ApplicationRunner {
            log.info("Initializing MCP Server connection via Stdio transport...")
            mcpScope.launch {
                try {
                    val transport = StdioServerTransport(
                        input = System.`in`.asSource().buffered(),
                        output = System.out.asSink().buffered()
                    )
                    mcpServer.createSession(transport)
                    log.info("MCP Server successfully connected and listening.")
                } catch (e: Exception) {
                    log.error("Critical error during MCP Server connection lifecycle", e)
                }
            }
        }
    }

    @PreDestroy
    fun shutdownMcpServer() {
        log.info("Shutting down MCP Server Coroutine Scope...")
        mcpScope.cancel()
    }
}