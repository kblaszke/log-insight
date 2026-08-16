# Skill: Reactive Log Analysis and Testing in LogInsight

This skill defines the authoritative procedure for coding agents to analyze system logs, implement reactive log-processing features, and validate them with non-blocking tests in the **LogInsight** codebase.

---

## 1. Context & Architecture

LogInsight is a reactive, multi-module Kotlin application. Coding agents must respect the strict module boundaries:
*   **`:log-insight-core`**: Pure domain logic, stateless log parser, and lazy streaming pipelines (`Sequence` and `Flow`). **Strictly zero framework dependencies (no Spring, no WebFlux).**
*   **`:log-insight-app`**: Spring Boot 3 WebFlux, non-blocking file I/O, REST endpoints (SSE), and Model Context Protocol (MCP) server.

---

## 2. Tooling: Model Context Protocol (MCP)

When tasked with investigating logs, troubleshooting errors, or gathering runtime statistics, **never use raw shell commands (like `grep`, `cat`, or `find`)**. You must invoke the dedicated MCP tools exposed by the application server:

1.  **`analyze_log_stats(filePath: String)`**:
    *   *Usage*: Run this first when asked to summarize log health, investigate error distribution, or analyze high-level trends.
    *   *Behavior*: Performs a memory-efficient, single-pass scan of the file as a lazy `Sequence` with dynamic key masking (masking UUIDs and numbers to prevent memory leaks during aggregation).
2.  **`query_logs(level: String?, pattern: String?, limit: Int?)`**:
    *   *Usage*: Run this to fetch specific log lines (e.g., extracting stack traces for a specific error message found by `analyze_log_stats`).

---

## 3. Step-by-Step Workflow

### Step 3.1: Log Investigation & Diagnosis
1.  Call `analyze_log_stats` to locate where the anomalies (errors, warnings) are concentrated.
2.  Identify the top masked error patterns.
3.  Query specific occurrences of those patterns using `query_logs` with a regex `pattern` to inspect the full context and stack trace.

### Step 3.2: Non-Blocking Implementation Rules
When implementing or modifying log processing pipelines:
1.  **Protect Netty Event Loop**: Never execute blocking file I/O or CPU-bound tasks on Netty’s reactive threads (e.g., `reactor-http-nio-*`).
2.  **Offload Blocking I/O**: Wrap blocking operations (like standard file reads) using:
    ```kotlin
    withContext(Dispatchers.IO) { ... }
    // Or for Flow:
    flow.flowOn(Dispatchers.IO)
    ```
3.  **Offload CPU-bound Work**: Offload heavy regex matching or data parsing to:
    ```kotlin
    withContext(Dispatchers.Default) { ... }
    // Or for Flow:
    flow.flowOn(Dispatchers.Default)
    ```
4.  **Enforce Immutability**: Always use `val` and read-only Kotlin collections. Protect domain-level primitives using `@JvmInline value class`.

### Step 3.3: Writing Compliant Kotest Tests
Every new feature or bugfix must be accompanied by Kotest tests matching these criteria:
1.  **Use `FunSpec`**: Structure tests using the `FunSpec` style. Use `withTests` (or `withContexts`) for example-based/table-driven tests:
    ```kotlin
    class LogParserSpec : FunSpec({
        context("parsing log lines") {
            withTests(
                "[2026-08-12T17:10:27Z] INFO: System started" to LogLevel.INFO,
                "[2026-08-12T17:15:00Z] ERROR: DB connection failed" to LogLevel.ERROR
            ) { (line, expectedLevel) ->
                val entry = LogParser.parseLine(line)
                entry?.level shouldBe expectedLevel
            }
        }
    })
    ```
2.  **Assert Partial Substring Matches**: Never use `list shouldContain "substring"` on collection lists, as it performs an exact equality check on the elements. Instead, use index-based assertions or Kotest **Inspectors**:
    ```kotlin
    // CORRECT:
    result[0] shouldContain "INFO: System started"
    // Or using Inspectors:
    result.forAtLeastOne { it shouldContain "INFO: System started" }
    ```

### Step 3.4: Event-Loop Blocking Protection (BlockHound)
To ensure no blocking calls slip into non-blocking threads:
1.  **Verify via BlockHound**: Ensure the `BlockHound` extension is registered in integration test suites inside the `:log-insight-app` module:
    ```kotlin
    import io.kotest.extensions.blockhound.BlockHound

    class ReactiveLogPipelineSpec : FunSpec({
        extension(BlockHound()) // Automatically blocks Thread.sleep, blocking I/O on default/non-blocking dispatchers
        
        test("should execute reactive pipeline without blocking") {
            // Your reactive flow execution here
        }
    })
    ```

---

## 4. MCP Server Safety: Protecting the Stdio Transport

Because the MCP server communicates with AI clients over standard input/output (`StdioServerTransport`):
1.  **Absolute Output Protection**: **Never write to `System.out`**. This includes `println()`, logger console appenders mapped to `System.out`, and Spring Boot's startup banners or info messages.
2.  **Redirect Streams**: Always redirect all framework logging, console appenders, and diagnostic messages to **`System.err`** to keep the JSON-RPC channel on `System.out` clean.
