---
name: new-service
description: >
  Scaffold a new IntelliJ Platform service for the KMP Module Generator plugin.
  Use this skill whenever the user wants to add a new service, create a service class,
  register a service in plugin.xml, or asks how to wire up dependency injection between
  services. Triggers on phrases like "add a service", "create a service", "new service",
  "need a service for X", or "where do I put shared logic".
---

# New IntelliJ Service — KMP Module Generator

## Step 1: Gather inputs

Ask the user for three things (or infer from context if already stated):

1. **Class name** — e.g., `CacheService`, `ValidationService`
2. **Level** — `PROJECT` (needs access to the open project) or `APP` (singleton across all projects)
3. **One-sentence responsibility** — what this service does; used as a KDoc comment

If the user is unsure about level: choose `PROJECT` when the service needs to read project files, settings, or modules. Choose `APP` for stateless utilities or global caches.

---

## Step 2: Create the Kotlin class

File location:
```
src/main/kotlin/com/github/nonoxys/kmpmodulegenerator/services/<ClassName>.kt
```

### PROJECT-level template

```kotlin
package com.github.nonoxys.kmpmodulegenerator.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project

/**
 * <One-sentence responsibility from user input.>
 */
@Service(Service.Level.PROJECT)
class <ClassName>(private val project: Project) {

    private val log = Logger.getInstance(<ClassName>::class.java)

    // TODO: implement

    companion object {
        fun getInstance(project: Project): <ClassName> =
            project.getService(<ClassName>::class.java)
    }
}
```

### APP-level template

```kotlin
package com.github.nonoxys.kmpmodulegenerator.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger

/**
 * <One-sentence responsibility from user input.>
 */
@Service(Service.Level.APP)
class <ClassName> {

    private val log = Logger.getInstance(<ClassName>::class.java)

    // TODO: implement

    companion object {
        fun getInstance(): <ClassName> =
            ApplicationManager.getApplication().getService(<ClassName>::class.java)
    }
}
```

No interface is needed — IntelliJ resolves services by class type, not interface.

---

## Step 3: Register in plugin.xml

File: `src/main/resources/META-INF/plugin.xml`

Locate the `<extensions defaultExtensionNs="com.intellij">` block and add the appropriate tag **inside** it:

**PROJECT-level:**
```xml
<projectService serviceImplementation="com.github.nonoxys.kmpmodulegenerator.services.<ClassName>"/>
```

**APP-level:**
```xml
<applicationService serviceImplementation="com.github.nonoxys.kmpmodulegenerator.services.<ClassName>"/>
```

Both tags go inside `<extensions defaultExtensionNs="com.intellij">`. Do not create a new extensions block.

---

## Step 4: Inject the service into callers

Use lazy delegation so the service is resolved once on first access:

```kotlin
// Inside an Action, another Service, or any class that holds a Project reference:
private val myService: <ClassName> by lazy { <ClassName>.getInstance(project) }

// For app-level services (no project needed):
private val myService: <ClassName> by lazy { <ClassName>.getInstance() }
```

This matches the existing pattern used in the codebase (e.g., `FtlTemplateService` in `ModuleGeneratorService`).

---

## Step 5: Verify

1. Confirm the class compiles: `./gradlew compileKotlin`
2. Confirm the plugin structure is valid: `./gradlew verifyPlugin`

If `verifyPlugin` reports an unresolved service, double-check that the fully-qualified class name in `plugin.xml` exactly matches the package + class name in the `.kt` file.
