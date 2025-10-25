package com.github.nonoxys.kmpmodulegenerator.utils

import com.github.nonoxys.kmpmodulegenerator.models.ValidationResult

/**
 * Validator for module names
 */
object ModuleNameValidator {
    
    private val MODULE_NAME_REGEX = Regex("^[a-z][a-z0-9-]*$")
    
    /**
     * Validate module name format
     */
    fun validate(moduleName: String): ValidationResult {
        if (moduleName.isBlank()) {
            return ValidationResult.Invalid("Module name cannot be empty")
        }
        
        if (moduleName.length < 2) {
            return ValidationResult.Invalid("Module name must be at least 2 characters long")
        }
        
        if (moduleName.length > 50) {
            return ValidationResult.Invalid("Module name should not exceed 50 characters")
        }
        
        if (!MODULE_NAME_REGEX.matches(moduleName)) {
            return ValidationResult.Invalid(
                "Module name must start with lowercase letter and contain only lowercase letters, numbers, and hyphens"
            )
        }
        
        if (moduleName.startsWith("-") || moduleName.endsWith("-")) {
            return ValidationResult.Invalid("Module name cannot start or end with a hyphen")
        }
        
        if (moduleName.contains("--")) {
            return ValidationResult.Invalid("Module name cannot contain consecutive hyphens")
        }
        
        return ValidationResult.Valid
    }
}

