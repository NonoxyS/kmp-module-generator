package com.github.nonoxys.kmpmodulegenerator.models

/**
 * Represents the file and folder structure to be generated
 */
data class FileStructure(
    val directories: List<Directory>,
    val files: List<FileTemplate>
)

/**
 * Represents a directory in the structure.
 * [path] supports variable interpolation: `${moduleName}/src/main` or `{{moduleName}}/src/main`
 */
data class Directory(
    val path: String,
) {
    fun getResolvedPath(variables: Map<String, String>): String = resolveVariables(path, variables)
}

/**
 * Represents a file template.
 * Both [path] and [content] support variable interpolation.
 */
data class FileTemplate(
    val path: String,
    val content: String,
    val encoding: String = "UTF-8"
) {
    fun getResolvedPath(variables: Map<String, String>): String = resolveVariables(path, variables)
    fun getResolvedContent(variables: Map<String, String>): String = resolveVariables(content, variables)
}

private fun resolveVariables(template: String, variables: Map<String, String>): String {
    var result = template
    variables.forEach { (key, value) ->
        result = result.replace("\${$key}", value)
        result = result.replace("{{$key}}", value)
    }
    return result
}
