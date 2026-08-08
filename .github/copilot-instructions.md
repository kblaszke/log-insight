# LogInsight Project - AI Developer Instructions

This file provides context and strict guidelines for GitHub Copilot when generating code, refactoring, or writing tests in this repository. Trust these instructions implicitly.

## Project Architecture & Stack
- **Project Structure**: Multi-module Gradle build (`log-insight`) consisting of:
    - `:log-insight-core`: Pure domain logic, parser, and streaming pipeline. No framework dependencies.
    - `:log-insight-app`: Application entry point, CLI, Spring WebFlux, and future Model Context Protocol (MCP) integrations.
- **Runtimes & Compilation**:
    - Target JVM: Java 17.
    - Kotlin version: 1.9.22.
    - All subprojects are configured globally using Java 17 toolchains and Kotlin JVM target 17.

## Language & Coding Standards
- **Language**: Always write identifiers, class/method names, logs, exceptions, and comments in **100% English**.
- **Immutability**: Prefer `val` over `var` whenever possible to ensure read-only, robust references.
- **Type Safety**: Use `@JvmInline value class` wrapper types (e.g., `LogMessage`) for primitives to prevent type mixing without JVM memory allocation overhead.
- **Asynchronous & Reactive Streams**:
    - Use Kotlin Coroutines and cold `Flow<T>` for asynchronous stream processing.
    - Always offload blocking I/O operations (like file reading or network requests) to `Dispatchers.IO` using the `flowOn(Dispatchers.IO)` operator.
    - Suspending terminal operations (like `collect` or custom aggregators) must be marked with the `suspend` keyword.
- **Syntactic Sugar**: Prefer expression bodies for single-line functions and use scope functions (`let`, `apply`, `run`, `also`, `with`) in an idiomatic, non-nested way.

## Testing Standards
- **Framework**: Use **Kotest** (version 5.8.x+) with JUnit Platform runner.
- **Testing Style**: Always use the **`FunSpec`** style for test definitions.
- **Data-Driven Testing**:
    - Use the native `withTests(...)` function (or appropriate spec-specific `withXXX` leaf creators) for example-based and parameterized tests.
    - Avoid legacy `withData` containers when defining simple parameterized leaf assertions.
- **Assertion Rules**:
    - **CRITICAL COLLECT RULE**: When testing a Kotlin `Flow` and consuming it using `.toList()` terminal operations, remember that `.toList()` returns a standard JVM `List<T>`.
    - **NEVER** attempt to call element properties directly on the list receiver (e.g., `result.level` is incorrect).
    - **ALWAYS** access individual elements in collection assertions by their index explicitly (e.g., use `result.level` and `result[14].level`).
    - Use proper Kotest matchers (e.g., `shouldBe`, `shouldNotBe`, `shouldContain`).

## Spring Boot & WebFlux Standards (Module `:log-insight-app`)
- **Configuration over scanning**:
    - Prefer explicit bean declaration inside `@Configuration` classes instead of polluting classes with `@Component`, `@Service`, or `@Repository` annotations.
    - Only use stereotype annotations (like `@RestController` or `@Controller`) where it is strictly required by the framework's routing and scanning mechanisms.
- **Bean Instantiation**:
    - Always instantiate beans inside configuration classes programmatically using standard Kotlin constructors, factory functions, or builders.
    - Prefer implicit constructor-based dependency injection for configuration wiring. Never use field injection or the `@Autowired` annotation on properties.
- **Typed Configurations**:
    - Never read properties directly using `@Value("\${...}")`.
    - Always use typed, immutable `@ConfigurationProperties` bound to Kotlin `data class` structures.
    - Enable constructor binding using `@ConstructorBinding` (or standard Kotlin constructor-binding rules for Spring Boot 3).

## Language Idioms & Code Style
- **Naming Conventions**:
    - Use strict **camelCase** for variables, properties, parameters, and function names.
    - Use **PascalCase** for class, interface, object, and enum names.
    - Use **SCREAMING_SNAKE_CASE** for companion object constants.
- **Value Class Enforcement**:
    - Always enforce domain boundaries by wrapping raw primitives (like `String` or `Long`) into `@JvmInline value class` types (e.g., `LogMessage`) at public API/domain interfaces [6].
    - Ensure that Copilot does not default to raw types in controller parameters or domain signatures.
- **Error Handling**:
    - Avoid throwing raw or generic exceptions (like `RuntimeException`).
    - Prefer returning typed results, nullable values, or utilizing standard Kotlin idioms like `runCatching` for safe execution.