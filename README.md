## KMP Module Generator

<div align="center">

[![Marketplace version][badge:marketplace-version]][url:plugin-homepage]
[![GitHub releases][badge:gh-release]][url:gh-releases]
[![License][badge:license]][url:gh-license]

</div>

<!-- Plugin description -->
KMP Module Generator is an IntelliJ-based plugin that automates Kotlin Multiplatform and Android module creation using
reusable FreeMarker templates.

It helps:

- Generate consistent project modules from templates instead of copy-pasting
- Preview generated files and Gradle changes before applying them
- Keep templates in VCS or a shared folder for the whole team
- Automatically update `settings.gradle(.kts)` for all detected modules
<!-- Plugin description end -->

---

### Table of Contents

- [Overview](#overview)
- [Feature Highlights](#feature-highlights)
- [Requirements](#requirements)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [Template Storage and Configuration](#template-storage-and-configuration)
- [Template Structure](#template-structure)
- [FreeMarker Basics in Templates](#freemarker-basics-in-templates)
- [Troubleshooting](#troubleshooting)
- [License](#license)

---

### Overview

KMP Module Generator lets you define project/module templates as plain FreeMarker files and generate ready-to-use Kotlin
Multiplatform or Android modules from them directly in the IDE.

The plugin is designed to:

- **Reduce boilerplate** when creating new features or modules
- **Enforce architecture conventions** across a team
- **Stay transparent**: everything is regular files under version control

### Feature Highlights

- **Template-based generation**
    - Templates are just files in a folder (`template.xml` + `root/` with `.ftl` files)
    - Full FreeMarker support (variables, conditions, loops, functions)
- **IDE-first UX**
    - Create, edit, and apply templates via familiar IDE actions
    - Preview folder structure, files, and `settings.gradle(.kts)` changes before generation
- **Automatic Gradle configuration**
    - Detects modules via `build.gradle` / `build.gradle.kts` in the template structure
    - Adds all nested modules to `settings.gradle(.kts)` with correct paths (for example, `:src:cool-feature:api`)
- **Flexible storage**
    - Default per-project folder `.idea/kmp-templates/`
    - Optional custom folder for sharing templates across projects and teams

### Requirements

- IntelliJ-based IDE with platform version **243+** (for example, IntelliJ IDEA 2024.3 or newer)
- Kotlin plugin (bundled in IntelliJ IDEA)

---

### Installation

- **From JetBrains Marketplace**
    1. Open `Settings | Plugins | Marketplace`
    2. Search for `KMP Module Generator`
    3. Click `Install` and restart the IDE

- **From GitHub Releases**
    1. Open the plugin's Releases page on GitHub
    2. Download the latest `kmp-module-generator-*.zip` artifact
    3. Install it via `Settings | Plugins | ⚙ | Install Plugin from Disk...`

---

### Quick Start

1. **Create your first template**
    - Right-click a folder in the Project tool window
    - `New | Generate Module | New Template...`
    - Fill in template ID, name, description, and parameters

2. **Generate a module from a template**
    - Right-click the target folder (for example, `src`, `feature`, `modules`)
    - `New | Generate Module | From Template...`
    - Choose a template, fill in parameters, preview changes, and confirm

3. **Edit an existing template**
    - Right-click any folder
    - `New | Generate Module | Edit Template...`
    - Select a template by ID, edit parameters and metadata, save

---

### Template Storage and Configuration

By default, templates are stored under:

- `.idea/kmp-templates/` in the current project

To use a shared or external folder:

1. Open `Settings | Tools | KMP Module Templates`
2. Enable **Use custom template folder**
3. Select a folder (for example, a shared directory or a separate repo)
4. Use **Open Templates Folder** for quick navigation

---

### Template Structure

A typical template looks like this:

```text
<templates-root>/my-template/
├── template.xml           # Template configuration and parameters
├── root/                  # Files and directories to generate
│   ├── build.gradle.kts.ftl
│   └── src/
│       └── main/
│           └── kotlin/
│               └── ${packageName?replace(".", "/")}/
│                   └── MyClass.kt.ftl
```

#### `template.xml`

Defines template metadata and input parameters:

```xml
<?xml version="1.0"?>
<template>
    <id>my-template</id>
    <name>My Custom Template</name>
    <description>Short description for the template</description>

    <parameters>
        <parameter name="moduleName">
            <displayName>Module Name</displayName>
            <description>Name of the module to generate</description>
            <type>TEXT</type>
            <required>true</required>
        </parameter>

        <parameter name="packageName">
            <displayName>Package Name</displayName>
            <description>Base package for generated sources</description>
            <type>TEXT</type>
            <required>true</required>
        </parameter>

        <parameter name="useCompose">
            <displayName>Use Jetpack Compose</displayName>
            <type>BOOLEAN</type>
            <default>true</default>
        </parameter>
    </parameters>
</template>
```

Supported parameter types (for the UI):

- **TEXT** – single-line free-form text
- **BOOLEAN** – checkbox (true/false)
- **DROPDOWN** – value selected from a predefined list of options
- **MULTILINE_TEXT** – multi-line text (for descriptions, JSON, etc.)

#### `root/` and `.ftl` files

Everything under `root/` is processed and copied into the target folder.  
File and directory names, as well as file contents, are treated as FreeMarker templates.

Example `build.gradle.kts.ftl`:

```kotlin
plugins {
    kotlin("android")
    <#if useCompose == "true">
    id("org.jetbrains.compose")
    </#if>
}

android {
    namespace = "${packageName}"

    <#if useCompose == "true">
    buildFeatures {
        compose = true
    }
    </#if>
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    <#if useCompose == "true">
    implementation("androidx.compose.ui:ui:1.5.4")
    implementation("androidx.compose.material3:material3:1.1.2")
    </#if>
}
```

Example `MyClass.kt.ftl`:

```kotlin
package ${packageName}

<#if useCompose == "true">
        import androidx . compose . runtime . Composable

        @Composable
        fun MyScreen() {
            // Compose UI here
        }
<#else>
class MyClass {
    fun doSomething() {
        println("Hello from ${moduleName}!")
    }
}
</#if>
```

---

### FreeMarker Basics in Templates

- **Variables**

```ftl
${variableName}                     <!-- simple variable -->
${packageName?replace(".", "/")}    <!-- package as path, e.g. com/example/app -->
```

- **Conditions**

```ftl
<#if condition == "true">
    // when condition is true
<#elseif otherCondition>
    // alternative branch
<#else>
    // fallback
</#if>
```

- **Loops**

```ftl
<#list items as item>
    implementation("${item}")
</#list>
```

- **Useful functions**

```ftl
${moduleName?cap_first}           <!-- capitalize first letter -->
${packageName?replace(".", "/")}  <!-- convert package to path -->
```

Complete FreeMarker reference:  
`https://freemarker.apache.org/docs/`

---

### Troubleshooting

- **Template changes are not visible**
    - Ensure you edit `template.xml` and `.ftl` files in the configured templates folder
    - Re-open the generation or edit dialog (templates are reloaded before dialogs)

- **Modules are not added to `settings.gradle(.kts)`**
    - Check that your template contains `build.gradle` or `build.gradle.kts` under `root/`
    - Each directory that contains a Gradle file is treated as a module
    - Nested modules are supported: paths like `src/cool-feature/api/build.gradle.kts` become `:src:cool-feature:api`

### License

```
Copyright (c) 2025 Andrey Dobrov

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

[badge:marketplace-version]: https://img.shields.io/jetbrains/plugin/v/28838?label=JB%20Marketplace&style=for-the-badge&color=purple

[badge:gh-release]: https://img.shields.io/github/v/release/NonoxyS/kmp-module-generator?label=GitHub%20Releases&style=for-the-badge

[badge:license]: https://img.shields.io/github/license/NonoxyS/kmp-module-generator?style=for-the-badge&color=white

[url:plugin-homepage]: https://plugins.jetbrains.com/plugin/28838-kmp-module-generator/

[url:gh-releases]: https://github.com/NonoxyS/kmp-module-generator/releases

[url:gh-license]: https://github.com/NonoxyS/kmp-module-generator/blob/main/LICENSE