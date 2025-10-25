package com.github.nonoxys.kmpmodulegenerator.utils

/**
 * Utility for resolving template variables
 */
object TemplateVariableResolver {
    
    /**
     * Resolve variables in a template string
     * Supports both ${var} and {{var}} syntax
     */
    fun resolve(template: String, variables: Map<String, String>): String {
        var result = template
        
        variables.forEach { (key, value) ->
            result = result.replace("\${$key}", value)
            result = result.replace("{{$key}}", value)
        }
        
        return result
    }
    
    /**
     * Find all variables in a template string
     */
    fun findVariables(template: String): Set<String> {
        val variables = mutableSetOf<String>()
        
        // Find ${var} pattern
        val dollarPattern = Regex("\\\$\\{([^}]+)\\}")
        dollarPattern.findAll(template).forEach { match ->
            variables.add(match.groupValues[1])
        }
        
        // Find {{var}} pattern
        val bracePattern = Regex("\\{\\{([^}]+)\\}\\}")
        bracePattern.findAll(template).forEach { match ->
            variables.add(match.groupValues[1])
        }
        
        return variables
    }
    
    /**
     * Validate that all required variables are present
     */
    fun validateVariables(template: String, variables: Map<String, String>): List<String> {
        val required = findVariables(template)
        val missing = required.filter { !variables.containsKey(it) }
        
        return if (missing.isEmpty()) {
            emptyList()
        } else {
            listOf("Missing variables: ${missing.joinToString(", ")}")
        }
    }
}

