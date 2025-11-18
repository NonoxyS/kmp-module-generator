package com.github.nonoxys.kmpmodulegenerator.services

import com.github.nonoxys.kmpmodulegenerator.models.*
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import freemarker.template.Configuration
import freemarker.template.Template
import freemarker.template.TemplateExceptionHandler
import java.io.File
import java.io.StringWriter

/**
 * Service for working with FreeMarker templates
 *
 * Templates are stored as .ftl files and can be easily edited by users
 */
@Service(Service.Level.PROJECT)
class FtlTemplateService(private val project: Project) {

    private val log = Logger.getInstance(FtlTemplateService::class.java)
    private val freemarkerConfig: Configuration = Configuration(Configuration.VERSION_2_3_32).apply {
        defaultEncoding = "UTF-8"
        templateExceptionHandler = TemplateExceptionHandler.RETHROW_HANDLER
        logTemplateExceptions = false
        wrapUncheckedExceptions = true
    }

    /**
     * Get template directory for the project
     * Uses configured location from settings or default .idea/kmp-templates/
     */
    fun getTemplateDirectory(): File {
        val templatesDir = com.github.nonoxys.kmpmodulegenerator.settings.TemplateSettings.getTemplateFolder(project)
        if (!templatesDir.exists()) {
            templatesDir.mkdirs()
        }
        return templatesDir
    }

    /**
     * Get all available template folders
     */
    fun getAvailableTemplates(): List<TemplateInfo> {
        val templates = mutableListOf<TemplateInfo>()
        val templateDir = getTemplateDirectory()

        templateDir.listFiles()?.forEach { folder ->
            if (folder.isDirectory) {
                val configFile = File(folder, "template.xml")
                if (configFile.exists()) {
                    try {
                        val info = parseTemplateConfig(configFile)
                        templates.add(info)
                    } catch (e: Exception) {
                        log.warn("Failed to parse template config: ${folder.name}", e)
                    }
                }
            }
        }

        return templates
    }

    /**
     * Process FreeMarker template with variables
     */
    fun processTemplate(templateContent: String, variables: Map<String, Any>): String {
        return try {
            val template = Template("inline", templateContent, freemarkerConfig)
            val writer = StringWriter()
            template.process(variables, writer)
            writer.toString()
        } catch (e: Exception) {
            log.error("Failed to process template", e)
            throw e
        }
    }

    /**
     * Process template file
     */
    fun processTemplateFile(templateFile: File, variables: Map<String, Any>): String {
        return processTemplate(templateFile.readText(), variables)
    }

    /**
     * Create template from FTL files
     */
    fun loadTemplate(templateFolder: File): ModuleTemplate? {
        val configFile = File(templateFolder, "template.xml")
        if (!configFile.exists()) {
            log.warn("template.xml not found in ${templateFolder.name}")
            return null
        }

        return try {
            parseTemplateFromFolder(templateFolder, configFile)
        } catch (e: Exception) {
            log.error("Failed to load template from ${templateFolder.name}", e)
            null
        }
    }

    /**
     * Parse template configuration
     */
    private fun parseTemplateConfig(configFile: File): TemplateInfo {
        // Simple XML parsing for template metadata
        val content = configFile.readText()

        val id = extractXmlTag(content, "id") ?: configFile.parentFile.name
        val name = extractXmlTag(content, "name") ?: id
        val description = extractXmlTag(content, "description") ?: ""

        return TemplateInfo(id, name, description)
    }

    /**
     * Parse full template from folder
     */
    private fun parseTemplateFromFolder(folder: File, configFile: File): ModuleTemplate {
        val config = parseTemplateConfig(configFile)
        val variables = parseVariables(configFile)

        val rootDir = File(folder, "root")
        val fileStructure = if (rootDir.exists()) {
            scanTemplateStructure(rootDir)
        } else {
            FileStructure(emptyList(), emptyList())
        }

        // Find all build.gradle files in the template
        val modulePaths = if (rootDir.exists()) {
            findBuildGradleFiles(rootDir)
        } else {
            emptyList()
        }

        val buildGradleFile = File(folder, "build.gradle.kts.ftl")
        val buildGradleTemplate = if (buildGradleFile.exists()) {
            buildGradleFile.readText()
        } else ""

        return ModuleTemplate(
            id = config.id,
            name = config.name,
            description = config.description,
            variables = variables,
            fileStructure = fileStructure,
            buildGradleTemplate = buildGradleTemplate,
            modulePaths = modulePaths
        )
    }

    /**
     * Parse variables from template.xml
     */
    private fun parseVariables(configFile: File): List<TemplateVariable> {
        val content = configFile.readText()
        val variables = mutableListOf<TemplateVariable>()

        // Extract <parameter> blocks
        val parameterPattern = Regex("<parameter[^>]*>.*?</parameter>", RegexOption.DOT_MATCHES_ALL)
        parameterPattern.findAll(content).forEach { match ->
            val paramBlock = match.value

            val name = extractXmlAttribute(paramBlock, "name") ?: return@forEach
            val displayName = extractXmlTag(paramBlock, "displayName") ?: name
            val description = extractXmlTag(paramBlock, "description") ?: ""
            val type = extractXmlTag(paramBlock, "type") ?: "TEXT"
            val defaultValue = extractXmlTag(paramBlock, "default") ?: ""
            val required = extractXmlTag(paramBlock, "required")?.toBoolean() ?: true
            val optionsRaw = extractXmlTag(paramBlock, "options") ?: ""
            val options = if (optionsRaw.isNotBlank()) {
                optionsRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            } else {
                null
            }

            variables.add(
                TemplateVariable(
                    name = name,
                    displayName = displayName,
                    description = description,
                    type = try {
                        VariableType.valueOf(type)
                    } catch (e: Exception) {
                        VariableType.TEXT
                    },
                    defaultValue = defaultValue,
                    required = required,
                    options = options
                )
            )
        }

        return variables
    }

    /**
     * Scan template folder structure
     */
    private fun scanTemplateStructure(rootDir: File): FileStructure {
        val directories = mutableListOf<Directory>()
        val files = mutableListOf<FileTemplate>()

        fun scanDirectory(dir: File, basePath: String = "") {
            dir.listFiles()?.forEach { file ->
                val relativePath = if (basePath.isEmpty()) file.name else "$basePath/${file.name}"

                when {
                    file.isDirectory -> {
                        directories.add(Directory(relativePath))
                        scanDirectory(file, relativePath)
                    }

                    file.name.endsWith(".ftl") -> {
                        val targetPath = relativePath.removeSuffix(".ftl")
                        files.add(
                            FileTemplate(
                                path = targetPath,
                                content = file.readText()
                            )
                        )
                    }
                }
            }
        }

        scanDirectory(rootDir)
        return FileStructure(directories, files)
    }

    /**
     * Find all build.gradle files in template structure
     */
    private fun findBuildGradleFiles(rootDir: File): List<String> {
        val buildGradlePaths = mutableListOf<String>()

        fun scanDirectory(dir: File, basePath: String = "") {
            dir.listFiles()?.forEach { file ->
                val relativePath = if (basePath.isEmpty()) file.name else "$basePath/${file.name}"

                when {
                    file.isDirectory -> {
                        scanDirectory(file, relativePath)
                    }

                    file.name.endsWith(".ftl") -> {
                        val targetPath = relativePath.removeSuffix(".ftl")
                        // Check if this is a build.gradle file
                        if (targetPath.endsWith("build.gradle") || targetPath.endsWith("build.gradle.kts")) {
                            buildGradlePaths.add(targetPath)
                        }
                    }
                }
            }
        }

        scanDirectory(rootDir)
        return buildGradlePaths
    }

    /**
     * Extract XML tag content
     */
    private fun extractXmlTag(xml: String, tag: String): String? {
        val pattern = Regex("<$tag>(.*?)</$tag>", RegexOption.DOT_MATCHES_ALL)
        return pattern.find(xml)?.groupValues?.get(1)?.trim()
    }

    /**
     * Extract XML attribute
     */
    private fun extractXmlAttribute(xml: String, attribute: String): String? {
        val pattern = Regex("$attribute=\"([^\"]+)\"")
        return pattern.find(xml)?.groupValues?.get(1)
    }

    companion object {
        fun getInstance(project: Project): FtlTemplateService {
            return project.getService(FtlTemplateService::class.java)
        }
    }
}

/**
 * Template metadata
 */
data class TemplateInfo(
    val id: String,
    val name: String,
    val description: String
)

