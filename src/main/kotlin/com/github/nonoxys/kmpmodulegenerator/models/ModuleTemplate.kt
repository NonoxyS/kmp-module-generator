package com.github.nonoxys.kmpmodulegenerator.models

/**
 * Represents a module template with configurable parameters.
 *
 * @param modulePaths paths to build.gradle files within the template root (relative to root/)
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
