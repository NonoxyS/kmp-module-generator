package com.github.nonoxys.kmpmodulegenerator.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.*
import javax.swing.JComponent

/**
 * Dialog for editing a single parameter using Kotlin UI DSL
 */
class ParameterEditorDialog(
    private val project: Project,
    private val existingParameter: ParameterData?
) : DialogWrapper(project) {

    private var name: String = existingParameter?.name ?: ""
    private var displayName: String = existingParameter?.displayName ?: ""
    private var description: String = existingParameter?.description ?: ""
    private var type: String = existingParameter?.type ?: "TEXT"
    private var required: Boolean = existingParameter?.required ?: true
    private var defaultValue: String = existingParameter?.defaultValue ?: ""

    init {
        title = if (existingParameter != null) "Edit Parameter" else "Add Parameter"
        init()
    }

    override fun createCenterPanel(): JComponent = panel {
        row("Name:") {
            textField()
                .bindText(::name)
                .columns(COLUMNS_MEDIUM)
                .validationOnInput {
                    when {
                        it.text.isBlank() -> error("Parameter name is required")
                        !it.text.matches(Regex("^[a-zA-Z][a-zA-Z0-9_]*$")) ->
                            error("Must start with letter and contain only letters, numbers, and underscores")

                        else -> null
                    }
                }
                .comment("Internal name used in templates (e.g., 'moduleName')")
        }

        row("Display Name:") {
            textField()
                .bindText(::displayName)
                .columns(COLUMNS_MEDIUM)
                .validationOnInput {
                    if (it.text.isBlank()) error("Display name is required") else null
                }
                .comment("Name shown in UI (e.g., 'Module Name')")
        }

        row("Description:") {
            textArea()
                .bindText(::description)
                .rows(3)
                .comment("Help text for users")
        }

        row("Type:") {
            comboBox(listOf("TEXT", "PACKAGE", "BOOLEAN", "NUMBER", "DROPDOWN", "MULTILINE_TEXT"))
                .bindItem(::type.toNullableProperty())
        }

        row {
            checkBox("Required")
                .bindSelected(::required)
        }

        row("Default Value:") {
            textField()
                .bindText(::defaultValue)
                .columns(COLUMNS_MEDIUM)
                .comment("Default value (optional)")
        }
    }

    fun getParameter(): ParameterData {
        return ParameterData(
            name = name,
            displayName = displayName,
            type = type,
            required = required,
            defaultValue = defaultValue,
            description = description
        )
    }
}

