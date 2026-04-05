---
name: new-action
description: Scaffold a new IntelliJ Action for the KMP Module Generator plugin. Use when the user wants to add a new action, menu item, or command to the plugin.
---

## Gather required inputs

Ask the user for the following if not already provided:

1. **Class name** — e.g. `ExportTemplateAction` (must end with `Action`)
2. **Menu label (text)** — e.g. `"Export Template..."` (shown in menus/toolbars)
3. **Description** — one sentence shown as a tooltip
4. **Needs VirtualFile access?** — yes/no (determines whether to get `virtualFile` from the event)
5. **Icon** — optional `AllIcons.*` reference, e.g. `AllIcons.Actions.Download`; omit if none

## Create the Kotlin class

Create the file at:
`src/main/kotlin/com/github/nonoxys/kmpmodulegenerator/actions/<ClassName>.kt`

Use this template:

```kotlin
package com.github.nonoxys.kmpmodulegenerator.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
// Add if VirtualFile access is needed:
// import com.intellij.openapi.actionSystem.CommonDataKeys

class <ClassName>(
) : AnAction(
    "<text>",
    "<description>",
    null // replace null with icon reference if provided
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        // val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return  // uncomment if VirtualFile needed
        // TODO: implement action logic
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        // Tighten the condition below if VirtualFile is required:
        // val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = project != null
    }
}
```

Fill in `<ClassName>`, `<text>`, `<description>` from the user's answers.
If VirtualFile is needed, uncomment the relevant lines and add `&& virtualFile != null` to the `isEnabledAndVisible` check.
If an icon is provided, replace `null` with the icon expression (e.g. `AllIcons.Actions.Download`).

## Register in plugin.xml

Open `src/main/resources/META-INF/plugin.xml` and locate the `KmpModuleGenerator.NewModuleGroup` action group.

Add the new `<action>` entry inside that group:

```xml
<action id="KmpModuleGenerator.<ClassName>"
        class="com.github.nonoxys.kmpmodulegenerator.actions.<ClassName>"
        text="<text>"
        description="<description>"
        icon="<AllIcons.X.Y>"/>
```

Omit the `icon` attribute if none was provided.

If the new action should be visually separated from adjacent actions, add a `<separator/>` before or after it as appropriate.

## Verify

1. Run `./gradlew buildPlugin` — should compile without errors.
2. Run `./gradlew verifyPlugin` — should pass structural checks.
3. Optionally run `./gradlew runIde` to confirm the action appears in the correct menu position.
