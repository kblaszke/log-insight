# GitHub Copilot Custom Instructions for LogInsight

This file provides context and strict guidelines for GitHub Copilot when generating code, configurations, tests, and refactoring scripts for the **LogInsight** project.

## Project Context & Architecture

LogInsight is a reactive, multi-module Kotlin application built with Gradle to parse, analyze, and stream application log files.

- **Module Boundaries**:
  - `:log-insight-core`: Pure domain logic, log parser, and streaming pipeline (`LogPipeline`). Must remain 100% free of Spring Framework annotations or other external framework dependencies.
  - `:log-insight-app`: Spring Boot 3 (WebFlux) application, infrastructure, REST endpoints (SSE), and Model Context Protocol (MCP) integrations.
- **Runtimes & Compilation**:
  - Target JVM: **Java 21** (due to MCP Kotlin SDK requirements).
  - Kotlin version: **1.9.22**.
  - All compilation tasks and toolchains must target Java 21.

---

## Language & Coding Standards

- **Language**: Always write identifiers, class/method names, logs, exceptions, configurations, and code comments in **100% English**.
- **Immutability**: Always prefer `val` over `var`. Prefer read-only collections (`List`, `Set`, `Map`) over mutable variants.
- **Value Classes**: Use `@JvmInline value class` for domain-level primitive boundaries (e.g., `LogMessage`, `LogTimestamp`) to enforce type-safety with zero runtime overhead.
- **Exceptions**: Avoid throwing raw runtime exceptions. Prefer safe Kotlin idioms such as `runCatching`, `getOrElse`, `getOrNull`, or return typed domain result wrappers.

---

## Spring Boot & Model Context Protocol (MCP) Guidelines

When configuring or extending the MCP server inside the `:log-insight-app` module, strictly adhere to these lifecycle and transport constraints:

### 1. Stdio Transport & Standard Output Protection
- The MCP server uses **Stdio transport** (stdin/stdout) for JSON-RPC 2.0 communication with AI clients (e.g., Claude Desktop, IDE agents).
- **CRITICAL**: Never print application logs, banners, or system messages to `System.out`. Doing so will pollute the Stdio channel and immediately crash/disconnect the MCP client.
- **Action**: Always disable the Spring Boot banner and startup info logging in configurations.
- **Action**: Configure `logback-spring.xml` console appender to target `System.err` instead of `System.out`:
  ```xml
  <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
      <target>System.err</target>
  </appender>
  ```

### 2. Spring Beans & Kotlin final Modifier
- Kotlin classes and methods are `final` by default. Spring's standard `@Configuration` attempts to create CGLIB proxy classes, which fails on `final` structures.
- **Action**: Always configure Spring `@Configuration` classes with `proxyBeanMethods = false` to bypass proxying, allowing Kotlin classes and `@Bean` functions to remain safely `final`:
  ```kotlin
  @Configuration(proxyBeanMethods = false)
  class McpServerConfig {  }
  ```

### 3. Non-blocking Lifecycle Management
- **Never** trigger blocking operations (such as MCP's `server.connect(transport)`) inside blocking Spring lifecycle hooks (like `@PostConstruct` or during synchronous bean creation).
- **Action**: Always bootstrap the Stdio connection asynchronously within a dedicated, non-blocking coroutine scope to ensure the Spring context and testing environments can initialize smoothly.

### 4. Dependency Injection
- Never use field-based `@Autowired`. Always prefer constructor-based dependency injection.
- To keep the core domain completely clean, register core domain classes (like `LogPipeline` object) as explicit `@Bean` configurations in the infrastructure module (`AppConfig.kt`) rather than using implicit component scanning (`@Component`) in `:log-insight-core`.

---

## Testing Standards & Kotest Conventions

We use **Kotest** as our primary testing framework, utilizing the `FunSpec` style and JUnit Platform runner.

### 1. Substring Matching in Collections
- In Kotest, calling `collection shouldContain "substring"` on a collection of type `List<String>` performs an **exact match check** (`Collection.contains(element)`). It will fail if the element contains dynamic prefixes/timestamps (e.g., `[2026-08-08T12:00:00Z] INFO: Server started` vs `"INFO: Server started"`).
- **Action**: To perform partial matches (substring matches) on items within a collection, always assert on an indexed element of the list, or use collection inspectors:
  ```kotlin
  // Index-based (forces String matcher substring match)
  result[0] shouldContain "INFO: Server started"

  // Inspector-based
  result.forAtLeastOne { it shouldContain "INFO: Server started" }
  ```

### 2. Multi-module Test Execution
- Ensure all tests use the JUnit Platform runner.
- Always write fast, lightweight unit tests in Kotest by mocking or decoupling the domain dependencies, reserving `@SpringBootTest` only for high-level controller and integration verifications.
