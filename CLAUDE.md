# CLAUDE.md

This file provides guidance to AI agents when working with code in this repository.

## Build & Development Commands

```bash
./gradlew buildPlugin          # Build the plugin artifact
./gradlew verifyPlugin         # Verify plugin structure and compatibility
./gradlew runIde               # Run IDE with plugin installed (for manual testing)
./gradlew test                 # Run unit tests
./gradlew check                # Run all checks (tests + Kover coverage)
./gradlew qodana               # Run Qodana static analysis
./gradlew publishPlugin        # Publish to JetBrains Marketplace (requires env vars)
```

## Architecture Overview

This is an **IntelliJ IDEA plugin** that generates Kotlin Multiplatform (KMP) and Android modules from reusable FreeMarker (`.ftl`) templates.

### Core Flow

1. User right-clicks a directory in the IDE → selects one of the three plugin actions
2. A dialog collects input (template selection, variable values)
3. `ModuleGeneratorService` processes `.ftl` templates via `FtlTemplateService` and writes files
4. `settings.gradle(.kts)` is updated automatically with the new module include

### Three Entry Points (Actions)

- **GenerateModuleAction** — generate a module from an existing template
- **CreateTemplateAction** — wizard to create a new template
- **EditTemplateAction** — edit template metadata and variables

### Key Services

| Service | Responsibility |
|---|---|
| `TemplateService` | Discovers and loads templates from `.idea/kmp-templates/` (default) or custom path |
| `ModuleGeneratorService` | Orchestrates file creation, directory structure, and `settings.gradle` updates |
| `FtlTemplateService` | Processes FreeMarker templates with user-provided variable values |

### Template Structure

Templates live in `.idea/kmp-templates/<template-name>/`:
- `template.xml` — metadata: name, description, variables (TEXT/BOOLEAN/DROPDOWN/MULTILINE_TEXT types)
- `root/` — directory tree of `.ftl` files; filenames and directory names also support FreeMarker expressions

### Key Data Models

- `ModuleTemplate` — parsed template (metadata + file list)
- `TemplateVariable` — a single configurable parameter with type and default value
- `ModuleConfiguration` — user inputs collected at generation time
- `GenerationPreview` / `GenerationResult` — preview and result of the generation step

### Module Path Detection

`ModuleGeneratorService` inspects the directory tree for `build.gradle(.kts)` files to determine the Gradle module path (e.g., `:src:cool-feature:api`). This supports nested multi-module projects.

### Settings

`TemplateSettings` (persistent component) stores the custom templates folder path. Configurable in IDE preferences via `TemplateSettingsConfigurable`.

## Technology Notes

- **Kotlin 2.1.20**, JVM target Java 21
- **IntelliJ Platform**: min build 243 (IDEA 2024.3+); uses IntelliJ Platform Gradle Plugin v2
- **FreeMarker 2.3.34** for template rendering
- **JUnit 4** for unit tests; Kover for coverage
- Dependency versions managed in `gradle/libs.versions.toml`
- Both `.gradle` (Groovy) and `.gradle.kts` (Kotlin DSL) Gradle files are supported at runtime
