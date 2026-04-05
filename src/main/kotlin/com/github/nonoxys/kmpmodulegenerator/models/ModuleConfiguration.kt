package com.github.nonoxys.kmpmodulegenerator.models

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
                val result = variable.validator.invoke(value)
                if (result is ValidationResult.Invalid) errors.add(result.message)
            }
        }

        return errors
    }
}
