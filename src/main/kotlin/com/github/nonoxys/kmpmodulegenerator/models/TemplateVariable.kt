package com.github.nonoxys.kmpmodulegenerator.models

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
 * Type of variable shown in the template UI.
 *
 * - TEXT           — single-line free text
 * - BOOLEAN        — checkbox (true/false)
 * - DROPDOWN       — value from a predefined list of options
 * - MULTILINE_TEXT — multi-line text (descriptions, JSON, etc.)
 */
enum class VariableType {
    TEXT,
    BOOLEAN,
    DROPDOWN,
    MULTILINE_TEXT,
}

/**
 * Validation result for template variables
 */
sealed interface ValidationResult {
    data object Valid : ValidationResult
    data class Invalid(val message: String) : ValidationResult
}
