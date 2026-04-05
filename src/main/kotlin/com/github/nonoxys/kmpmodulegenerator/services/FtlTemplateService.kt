package com.github.nonoxys.kmpmodulegenerator.services

import com.github.nonoxys.kmpmodulegenerator.models.*
import com.github.nonoxys.kmpmodulegenerator.settings.TemplateSettings
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import freemarker.template.Configuration
import freemarker.template.Template
import freemarker.template.TemplateExceptionHandler
import java.io.File
import java.io.StringWriter

/**
 * Service for working with FreeMarker templates.
 * Templates are stored as .ftl files and can be edited directly by users.
 */
@Service(Service.Level.PROJECT)
class FtlTemplateService(private val project: Project) {

    private val log = Logger.getInstance(FtlTemplateService::class.java)

    private val freemarkerConfig: Configuration = Configuration(Configuration.VERSION_2_3_32).apply {
        defaultEncoding = ENCODING
        templateExceptionHandler = TemplateExceptionHandler.RETHROW_HANDLER
        logTemplateExceptions = false
        wrapUncheckedExceptions = true
    }

    /**
     * Returns the template directory for this project.
     * Uses the path from settings or falls back to `.idea/kmp-templates/`.
     */
    fun getTemplateDirectory(): File {
        val templatesDir = TemplateSettings.getTemplateFolder(project)
        if (!templatesDir.exists()) templatesDir.mkdirs()
        return templatesDir
    }

    /**
     * Returns lightweight metadata for all valid templates in the template directory.
     */
    fun getAvailableTemplates(): List<TemplateInfo> {
        val templateDir = getTemplateDirectory()
        return templateDir.listFiles().orEmpty()
            .filter { it.isDirectory }
            .mapNotNull { folder ->
                val configFile = File(folder, TEMPLATE_CONFIG_FILE)
                if (!configFile.exists()) return@mapNotNull null
                try {
                    parseTemplateConfig(configFile)
                } catch (e: Exception) {
                    log.warn("Failed to parse template config: ${folder.name}", e)
                    null
                }
            }
    }

    /**
     * Processes a FreeMarker template string with the given variables.
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
     * Reads and processes a .ftl file with the given variables.
     */
    fun processTemplateFile(templateFile: File, variables: Map<String, Any>): String {
        return processTemplate(templateFile.readText(), variables)
    }

    /**
     * Loads a [ModuleTemplate] from a template folder. Returns null if the folder
     * is missing `template.xml` or if parsing fails.
     */
    fun loadTemplate(templateFolder: File): ModuleTemplate? {
        val configFile = File(templateFolder, TEMPLATE_CONFIG_FILE)
        if (!configFile.exists()) {
            log.warn("$TEMPLATE_CONFIG_FILE not found in ${templateFolder.name}")
            return null
        }
        return try {
            parseTemplateFromFolder(templateFolder, configFile)
        } catch (e: Exception) {
            log.error("Failed to load template from ${templateFolder.name}", e)
            null
        }
    }

    private fun parseTemplateConfig(configFile: File): TemplateInfo {
        val content = configFile.readText()
        val id = extractXmlTag(content, TAG_ID)?.takeIf { it.isNotBlank() } ?: configFile.parentFile.name
        val name = extractXmlTag(content, TAG_NAME)?.takeIf { it.isNotBlank() } ?: id
        val description = extractXmlTag(content, TAG_DESCRIPTION).orEmpty()
        return TemplateInfo(id, name, description)
    }

    private fun parseTemplateFromFolder(folder: File, configFile: File): ModuleTemplate {
        val config = parseTemplateConfig(configFile)
        val variables = parseVariables(configFile)

        val rootDir = File(folder, TEMPLATE_ROOT_DIR)
        val fileStructure: FileStructure
        val modulePaths: List<String>

        if (rootDir.exists()) {
            fileStructure = scanTemplateStructure(rootDir)
            modulePaths = findBuildGradleFiles(rootDir)
        } else {
            fileStructure = FileStructure(emptyList(), emptyList())
            modulePaths = emptyList()
        }

        val buildGradleTemplate = File(folder, BUILD_GRADLE_KTS_FTL)
            .takeIf { it.exists() }?.readText().orEmpty()

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

    private fun parseVariables(configFile: File): List<TemplateVariable> {
        val content = configFile.readText()
        val parameterPattern = Regex("<parameter[^>]*>.*?</parameter>", RegexOption.DOT_MATCHES_ALL)

        return parameterPattern.findAll(content).mapNotNull { match ->
            val paramBlock = match.value
            val name = extractXmlAttribute(paramBlock, ATTR_NAME) ?: return@mapNotNull null
            val displayName = extractXmlTag(paramBlock, TAG_DISPLAY_NAME) ?: name
            val description = extractXmlTag(paramBlock, TAG_DESCRIPTION).orEmpty()
            val type = extractXmlTag(paramBlock, TAG_TYPE) ?: VariableType.TEXT.name
            val defaultValue = extractXmlTag(paramBlock, TAG_DEFAULT).orEmpty()
            val required = extractXmlTag(paramBlock, TAG_REQUIRED)?.toBoolean() ?: true
            val optionsRaw = extractXmlTag(paramBlock, TAG_OPTIONS).orEmpty()
            val options = optionsRaw.takeIf { it.isNotBlank() }
                ?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }

            TemplateVariable(
                name = name,
                displayName = displayName,
                description = description,
                type = VariableType.entries.find { it.name == type } ?: VariableType.TEXT,
                defaultValue = defaultValue,
                required = required,
                options = options
            )
        }.toList()
    }

    private fun scanTemplateStructure(rootDir: File): FileStructure {
        val directories = mutableListOf<Directory>()
        val files = mutableListOf<FileTemplate>()

        fun scan(dir: File, basePath: String = "") {
            dir.listFiles().orEmpty().forEach { file ->
                val relativePath = if (basePath.isEmpty()) file.name else "$basePath/${file.name}"
                when {
                    file.isDirectory -> {
                        directories.add(Directory(relativePath))
                        scan(file, relativePath)
                    }

                    file.name.endsWith(FTL_EXTENSION) -> {
                        files.add(
                            FileTemplate(
                                path = relativePath.removeSuffix(FTL_EXTENSION),
                                content = file.readText()
                            )
                        )
                    }
                }
            }
        }

        scan(rootDir)
        return FileStructure(directories, files)
    }

    private fun findBuildGradleFiles(rootDir: File): List<String> {
        val buildGradlePaths = mutableListOf<String>()

        fun scan(dir: File, basePath: String = "") {
            dir.listFiles().orEmpty().forEach { file ->
                val relativePath = if (basePath.isEmpty()) file.name else "$basePath/${file.name}"
                when {
                    file.isDirectory -> scan(file, relativePath)
                    file.name.endsWith(FTL_EXTENSION) -> {
                        val targetPath = relativePath.removeSuffix(FTL_EXTENSION)
                        if (targetPath.endsWith(BUILD_GRADLE) || targetPath.endsWith(BUILD_GRADLE_KTS)) {
                            buildGradlePaths.add(targetPath)
                        }
                    }
                }
            }
        }

        scan(rootDir)
        return buildGradlePaths
    }

    private fun extractXmlTag(xml: String, tag: String): String? =
        Regex("<$tag>(.*?)</$tag>", RegexOption.DOT_MATCHES_ALL)
            .find(xml)?.groupValues?.get(1)?.trim()

    private fun extractXmlAttribute(xml: String, attribute: String): String? =
        Regex("$attribute=\"([^\"]+)\"")
            .find(xml)?.groupValues?.get(1)

    companion object {
        private const val ENCODING = "UTF-8"
        private const val TEMPLATE_CONFIG_FILE = "template.xml"
        private const val TEMPLATE_ROOT_DIR = "root"
        private const val FTL_EXTENSION = ".ftl"
        private const val BUILD_GRADLE = "build.gradle"
        private const val BUILD_GRADLE_KTS = "build.gradle.kts"
        private const val BUILD_GRADLE_KTS_FTL = "build.gradle.kts.ftl"

        private const val TAG_ID = "id"
        private const val TAG_NAME = "name"
        private const val TAG_DISPLAY_NAME = "displayName"
        private const val TAG_DESCRIPTION = "description"
        private const val TAG_TYPE = "type"
        private const val TAG_DEFAULT = "default"
        private const val TAG_REQUIRED = "required"
        private const val TAG_OPTIONS = "options"
        private const val ATTR_NAME = "name"

        fun getInstance(project: Project): FtlTemplateService =
            project.getService(FtlTemplateService::class.java)
    }
}
