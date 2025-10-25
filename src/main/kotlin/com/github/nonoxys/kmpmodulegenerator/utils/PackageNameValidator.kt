package com.github.nonoxys.kmpmodulegenerator.utils

import com.github.nonoxys.kmpmodulegenerator.models.ValidationResult

/**
 * Validator for package names
 */
object PackageNameValidator {
    
    private val PACKAGE_NAME_REGEX = Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*$")
    private val RESERVED_KEYWORDS = setOf(
        "abstract", "as", "break", "class", "continue", "do", "else", "false",
        "for", "fun", "if", "in", "interface", "is", "null", "object", "package",
        "return", "super", "this", "throw", "true", "try", "typealias", "typeof",
        "val", "var", "when", "while"
    )
    
    /**
     * Validate package name format
     */
    fun validate(packageName: String): ValidationResult {
        if (packageName.isBlank()) {
            return ValidationResult.Invalid("Package name cannot be empty")
        }
        
        if (!PACKAGE_NAME_REGEX.matches(packageName)) {
            return ValidationResult.Invalid(
                "Package name must start with lowercase letter and contain only lowercase letters, numbers, underscores, and dots"
            )
        }
        
        // Check for reserved keywords
        val parts = packageName.split(".")
        val reservedPart = parts.firstOrNull { it in RESERVED_KEYWORDS }
        if (reservedPart != null) {
            return ValidationResult.Invalid("Package name contains reserved keyword: $reservedPart")
        }
        
        // Check part length
        if (parts.any { it.length > 100 }) {
            return ValidationResult.Invalid("Package name parts should not exceed 100 characters")
        }
        
        return ValidationResult.Valid
    }
    
    /**
     * Convert package name to file path
     */
    fun toPath(packageName: String): String {
        return packageName.replace('.', '/')
    }
    
    /**
     * Convert file path to package name
     */
    fun fromPath(path: String): String {
        return path.replace('/', '.')
    }
}

