package com.github.nonoxys.kmpmodulegenerator.models

/**
 * Represents a module template with configurable parameters
 *
 * [modulePaths] - Paths to build.gradle files (relative to root)
 */
data class ModuleTemplate(
    val id: String,
    val name: String,
    val description: String,
    val variables: List<TemplateVariable>,
    val fileStructure: FileStructure,
    val buildGradleTemplate: String,
    val modulePaths: List<String> = emptyList()
)

/**
 * Represents a template variable that can be configured by user
 */
data class TemplateVariable(
    val name: String,
    val displayName: String,
    val description: String,
    val type: VariableType,
    val defaultValue: String = "",
    val required: Boolean = true,
    val validator: ((String) -> ValidationResult)? = null,
    val options: List<String>? = null // For dropdown type
)

/**
 * Type of variable
 */
enum class VariableType {
    TEXT,
    PACKAGE,
    BOOLEAN,
    DROPDOWN,
    NUMBER,
    MULTILINE_TEXT
}

/**
 * Validation result for template variables
 */
sealed interface ValidationResult {
    data object Valid : ValidationResult
    data class Invalid(val message: String) : ValidationResult
}

/**
 * Represents the file and folder structure to be generated
 */
data class FileStructure(
    val directories: List<Directory>,
    val files: List<FileTemplate>
)

/**
 * Represents a directory in the structure
 */
data class Directory(
    val path: String, // Support for variables like "${moduleName}/src/main"
) {
    fun getResolvedPath(variables: Map<String, String>): String {
        return resolveVariables(path, variables)
    }
}

/**
 * Represents a file template
 */
data class FileTemplate(
    val path: String, // Path with variables like "${moduleName}/build.gradle.kts"
    val content: String, // Content with variables
    val encoding: String = "UTF-8"
) {
    fun getResolvedPath(variables: Map<String, String>): String {
        return resolveVariables(path, variables)
    }

    fun getResolvedContent(variables: Map<String, String>): String {
        return resolveVariables(content, variables)
    }
}

/**
 * Helper function to resolve variables in strings
 */
private fun resolveVariables(template: String, variables: Map<String, String>): String {
    var result = template
    variables.forEach { (key, value) ->
        result = result.replace("\${$key}", value)
        result = result.replace("{{$key}}", value) // Support both ${} and {{}} syntax
    }
    return result
}

/**
 * Represents user configuration for module generation
 */
data class ModuleConfiguration(
    val template: ModuleTemplate,
    val variables: Map<String, String>,
    val targetPath: String
) {
    fun validate(): List<String> {
        val errors = mutableListOf<String>()

        template.variables.forEach { variable ->
            val value = variables[variable.name]

            if (variable.required && value.isNullOrBlank()) {
                errors.add("${variable.displayName} is required")
            }

            if (value != null && variable.validator != null) {
                when (val result = variable.validator.invoke(value)) {
                    is ValidationResult.Invalid -> errors.add(result.message)
                    is ValidationResult.Valid -> { /* OK */
                    }
                }
            }
        }

        return errors
    }
}

