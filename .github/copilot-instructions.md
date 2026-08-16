# GitHub Copilot Custom Instructions for LogInsight (Compiled)

This file provides comprehensive context and strict guidelines for GitHub Copilot when generating code, configurations, tests, and refactoring scripts for the **LogInsight** project. It combines core repository guidelines with Kotlin 2.x upgrades, BlockHound thread-blocking verification, and Sequence-based lazy analytics.

---

## 1. Project Context & Architecture

LogInsight is a reactive, multi-module Kotlin application built with Gradle to parse, analyze, and stream application log files in a non-blocking manner.

### Module Boundaries
*   **:log-insight-core**: Pure domain logic, domain models, log parser, and streaming pipeline (`LogPipeline`). **Must remain 100% free of Spring Framework annotations** or other external framework dependencies.
*   **:log-insight-app**: Spring Boot 3 (WebFlux) application, infrastructure, REST endpoints (SSE), and Model Context Protocol (MCP) server integration.

---

## 2. Technical Stack & Dependencies

*   **Target JVM**: Java 21 (required by MCP Kotlin SDK).
*   **Kotlin**: 2.3.21 (configured with modern compilerOptions DSL).
*   **kotlinx.coroutines**: 1.11.0 (with full reactive Flow support).
*   **Model Context Protocol SDK**: 0.14.0 (utilizing compliant ToolSchema and kotlinx.io-based transports).
*   **Spring Boot**: 3.2.2 (WebFlux reactive engine).
*   **Testing**: Kotest 5.8.0 with JUnit Platform Runner, Spring extension, and BlockHound extension.

---

## 3. Strict Coding Standards

### Language & Immubality
*   **Language**: Always write identifiers, class/method names, logs, exceptions, configurations, and code comments in **100% English**.
*   **Immutability**: Always prefer `val` over `var`. Prefer read-only collections (`List`, `Set`, `Map`) over mutable variants.

### Primitive Obsession Guardrails
*   Use `@JvmInline value class` for domain-level primitive boundaries (e.g., `LogMessage`) to enforce type-safety with zero runtime allocation overhead:
    ```kotlin
    @JvmInline
    value class LogMessage(val value: String)
    ```

### Netty Event Loop Protection & Scheduling
*   **Never block Netty event loop threads** (`reactor-http-nio-*`) with blocking or CPU-heavy synchronous calls.
*   **Non-blocking File I/O**: Offload all synchronous file reads (e.g., live log tailing in `FileReader`) to `Dispatchers.IO` using explicit context offloading via `.flowOn(Dispatchers.IO)` and non-blocking suspending coroutine `delay()`.
*   **CPU-heavy operations**: Offload CPU-bound calculations and Regex parsing in `LogPipeline` to `Dispatchers.Default` using `.flowOn(Dispatchers.Default)`.

### Sequence-Based Lazy Analytics
*   When calculating high-level log statistics or processing large static log files (e.g., via the `analyze_log_stats` tool):
  *   **Always use Kotlin `Sequence`** (lazy, single-pass processing) instead of eager collections (`List`, `Map`) to keep memory footprint close to zero and prevent JVM `OutOfMemoryError`.
  *   **Dynamic Masking**: Dynamic segments (e.g., UUIDs, numbers, variable thread IDs) in log messages must be masked using Regex patterns before grouping to prevent unbounded map memory growth and group similar errors correctly.

---

## 4. Spring Boot & Model Context Protocol (MCP) Guidelines

### Stdio Transport & Standard Output Protection
*   The MCP server uses **Stdio transport** (stdin/stdout) for JSON-RPC 2.0 communication.
*   **CRITICAL**: Never print application logs, banners, or system messages to `System.out`. Doing so will pollute the Stdio channel and immediately crash/disconnect the MCP client.
*   **Action**: Always disable the Spring Boot banner and startup info logging in configurations.
*   **Action**: Configure `logback-spring.xml` console appender to target `System.err` instead of `System.out`:
    ```xml
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <target>System.err</target>
    </appender>
    ```

### Spring Beans & Kotlin final Modifier
*   Kotlin classes and methods are `final` by default. Spring's standard `@Configuration` attempts to create CGLIB proxy classes, which fails on `final` structures.
*   **Action**: Always configure Spring `@Configuration` classes with `proxyBeanMethods = false` to bypass proxying:
    ```kotlin
    @Configuration(proxyBeanMethods = false)
    class McpServerConfig { }
    ```
*   **Constructor Injection**: To inject configuration properties into backing fields of constructor, explicitly use use-site targets:
    ```kotlin
    class McpServerConfig(
        @param:Value("\${log-insight.mcp.target-file-path}") private val targetFilePath: String
    )
    ```

### Non-blocking Lifecycle Management
*   **Never** trigger blocking operations (such as MCP's `server.connect(transport)`) inside blocking Spring lifecycle hooks (like `@PostConstruct` or during synchronous bean creation).
*   **Action**: Always bootstrap the Stdio connection asynchronously within a dedicated, non-blocking coroutine scope to ensure the Spring context and testing environments can initialize smoothly.

---

## 5. Testing Standards & Kotest Conventions

We use **Kotest** as our primary testing framework, utilizing the `FunSpec` style and JUnit Platform runner.

### BlockHound Integration (Thread Blocking Detection)
*   The module `:log-insight-app` is integrated with **BlockHound** globally via `ProjectConfig` to automatically detect and intercept any blocking I/O calls on reactive threads.
*   All new WebFlux-related or reactive testing suites must verify event loop safety. If a test is expected to trigger a blocking operation, it must either be scheduled on `Dispatchers.IO` or explicitly configured to allow the block.

### Substring Matching in Collections
*   Calling `collection shouldContain "substring"` on a collection of type `List` performs an **exact match check** (`Collection.contains(element)`). It will fail if the element contains dynamic prefixes or timestamps.
*   **Action**: To perform partial matches (substring matches) on items within a collection, always assert on an indexed element of the list, or use collection inspectors:
    ```kotlin
    // Index-based (forces String matcher substring match)
    result[0] shouldContain "INFO: Server started"

    // Inspector-based (Kotest inspectors)
    result.forAtLeastOne { it shouldContain "INFO: Server started" }
    ```

### Multi-module Test Execution
*   Keep domain logic tests fast and lightweight in Kotest by mocking or decoupling dependencies, reserving `@SpringBootTest` only for high-level controller and integration verifications.