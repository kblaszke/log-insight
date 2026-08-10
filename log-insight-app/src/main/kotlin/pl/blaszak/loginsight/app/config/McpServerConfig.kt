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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import pl.blaszak.loginsight.core.model.LogLevel
import pl.blaszak.loginsight.core.stream.LogPipeline
import java.io.File

@Configuration(proxyBeanMethods = false)
class McpServerConfig(
    // Constructor injection of the LogPipeline bean
    private val logPipeline: LogPipeline
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

        // Registering the query_logs tool with its input schema wrapped in Tool.Input
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
            // Accessing arguments directly from request.arguments
            val levelFilter = (request.arguments["level"] as? JsonPrimitive)?.content
            val patternFilter = (request.arguments["pattern"] as? JsonPrimitive)?.content
            val limit = (request.arguments["limit"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 50

            log.info("MCP Tool query_logs invoked with level={}, pattern={}, limit={}", levelFilter, patternFilter, limit)

            val logEntries = runBlocking(Dispatchers.IO) {
                // Creating a demo log file matching the structure parsed by LogPipeline
                val tempLogFile = File.createTempFile("live-server-logs", ".log").apply {
                    deleteOnExit()
                    writeText(
                        """
                        [2026-08-08T12:00:00Z] [INFO] Server started
                        [2026-08-08T12:01:00Z] [DEBUG] Scanning ports
                        [2026-08-08T12:02:00Z] [WARN] Heavy CPU load
                        [2026-08-08T12:03:00Z] [ERROR] Service unavailable
                        """.trimIndent()
                    )
                }

                // Streaming the raw file lines and parsing them to LogEntry Flow
                val rawLinesFlow = pl.blaszak.loginsight.app.FileReader.readFileLines(tempLogFile)
                var parsedFlow = logPipeline.streamFromLines(rawLinesFlow)

                // Dynamic level filtering
                if (levelFilter != null) {
                    val level = runCatching { LogLevel.valueOf(levelFilter.uppercase()) }.getOrNull()
                    if (level != null) {
                        parsedFlow = parsedFlow.filter { it.level == level }
                    }
                }

                // Dynamic regex or substring pattern filtering
                if (patternFilter != null) {
                    val regex = runCatching { Regex(patternFilter, RegexOption.IGNORE_CASE) }.getOrNull()
                    if (regex != null) {
                        parsedFlow = parsedFlow.filter { regex.containsMatchIn(it.message.value) }
                    } else {
                        parsedFlow = parsedFlow.filter { it.message.value.contains(patternFilter, ignoreCase = true) }
                    }
                }

                // Limit results and collect to list
                parsedFlow.take(limit).toList()
            }

            CallToolResult(
                content = listOf(
                    TextContent(
                        text = logEntries.joinToString("\n") { entry ->
                            "[${entry.timestamp}] ${entry.level}: ${entry.message.value}"
                        }
                    )
                )
            )
        }

        // Initialize stdio connection asynchronously
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
