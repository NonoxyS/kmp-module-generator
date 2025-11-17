package com.github.nonoxys.kmpmodulegenerator.services

import com.github.nonoxys.kmpmodulegenerator.models.FileTemplate
import com.github.nonoxys.kmpmodulegenerator.models.GenerationPreview
import com.github.nonoxys.kmpmodulegenerator.models.GenerationResult
import com.github.nonoxys.kmpmodulegenerator.models.ModuleConfiguration
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

/**
 * Service for generating modules from templates
 */
@Service(Service.Level.PROJECT)
class ModuleGeneratorService(private val project: Project) {

    private val log = Logger.getInstance(ModuleGeneratorService::class.java)

    /**
     * Generate module from configuration
     */
    fun generateModule(configuration: ModuleConfiguration): GenerationResult {
        try {
            // Validate configuration
            val validationErrors = configuration.validate()
            if (validationErrors.isNotEmpty()) {
                return GenerationResult.Failure("Validation failed: ${validationErrors.joinToString(", ")}")
            }

            // Use target path directly
            val moduleDir = ApplicationManager.getApplication().runWriteAction<VirtualFile?> {
                try {
                    val targetDir = File(configuration.targetPath)
                    if (!targetDir.exists()) {
                        targetDir.mkdirs()
                    }
                    LocalFileSystem.getInstance().refreshAndFindFileByIoFile(targetDir)
                } catch (e: Exception) {
                    log.error("Error accessing target directory", e)
                    null
                }
            } ?: return GenerationResult.Failure("Failed to access target directory")

            val generatedFiles = mutableListOf<VirtualFile>()
            val warnings = mutableListOf<String>()

            // Generate directories
            configuration.template.fileStructure.directories.forEach { directory ->
                val dirPath = directory.getResolvedPath(configuration.variables)
                createDirectory(moduleDir, dirPath)
            }

            // Generate files
            configuration.template.fileStructure.files.forEach { fileTemplate ->
                try {
                    val file = generateFile(moduleDir, fileTemplate, configuration.variables)
                    if (file != null) {
                        generatedFiles.add(file)
                    } else {
                        warnings.add("Failed to generate file: ${fileTemplate.path}")
                    }
                } catch (e: Exception) {
                    log.warn("Error generating file ${fileTemplate.path}", e)
                    warnings.add("Error generating file ${fileTemplate.path}: ${e.message}")
                }
            }

            // Extract module paths from build.gradle paths and update settings.gradle
            val modulePaths = extractModulePaths(configuration)
            if (modulePaths.isNotEmpty()) {
                try {
                    updateSettingsGradle(configuration, modulePaths)
                } catch (e: Exception) {
                    log.warn("Failed to update settings.gradle", e)
                    warnings.add("Failed to update settings.gradle: ${e.message}")
                }
            }

            // Refresh file system
            ApplicationManager.getApplication().invokeLater {
                moduleDir.refresh(false, true)
            }

            // Create success message with module names (extract last part of path for display)
            val displayMessage = if (modulePaths.isNotEmpty()) {
                val moduleNames = modulePaths.map { it.split("/").last() }
                if (moduleNames.size == 1) {
                    "Module '${moduleNames[0]}' generated successfully"
                } else {
                    "Modules ${moduleNames.joinToString(", ") { "'$it'" }} generated successfully"
                }
            } else {
                "Files generated successfully"
            }

            val displayName = if (modulePaths.isNotEmpty()) {
                modulePaths.map { it.split("/").last() }.joinToString(", ")
            } else {
                moduleDir.name ?: "Files"
            }

            return if (warnings.isEmpty()) {
                GenerationResult.Success(displayName, moduleDir, generatedFiles, displayMessage)
            } else {
                val warningMessage = if (modulePaths.isNotEmpty()) {
                    val moduleNames = modulePaths.map { it.split("/").last() }
                    if (moduleNames.size == 1) {
                        "Module '${moduleNames[0]}' generated with warnings"
                    } else {
                        "Modules ${moduleNames.joinToString(", ") { "'$it'" }} generated with warnings"
                    }
                } else {
                    "Files generated with warnings"
                }
                GenerationResult.Warning(displayName, moduleDir, generatedFiles, warnings, warningMessage)
            }

        } catch (e: Exception) {
            log.error("Error generating module", e)
            return GenerationResult.Failure("Error generating module: ${e.message}", e)
        }
    }

    /**
     * Generate preview of module structure
     */
    fun generatePreview(configuration: ModuleConfiguration): GenerationPreview {
        val directories = mutableListOf<GenerationPreview.PreviewDirectory>()
        val files = mutableListOf<GenerationPreview.PreviewFile>()
        val gradleChanges = mutableListOf<String>()

        // Preview directories
        configuration.template.fileStructure.directories.forEach { directory ->
            val path = directory.getResolvedPath(configuration.variables)
            val level = path.count { it == '/' }
            directories.add(GenerationPreview.PreviewDirectory(path, level))
        }

        // Preview files
        configuration.template.fileStructure.files.forEach { fileTemplate ->
            val path = fileTemplate.getResolvedPath(configuration.variables)
            val content = fileTemplate.getResolvedContent(configuration.variables)
            val level = path.count { it == '/' }
            files.add(GenerationPreview.PreviewFile(path, content.length, level))
        }

        val modulePaths = extractModulePaths(configuration)

        if (modulePaths.isNotEmpty()) {
            val projectBasePath = project.basePath ?: ""
            val targetPath = File(configuration.targetPath).canonicalPath
            val projectPath = File(projectBasePath).canonicalPath

            val baseRelativePath = if (targetPath.startsWith(projectPath)) {
                targetPath.substring(projectPath.length)
                    .removePrefix(File.separator)
            } else {
                ""
            }

            modulePaths.forEach { modulePath ->
                // Build full path: baseRelativePath + modulePath
                // Normalize paths to use / separator, then convert to Gradle format
                val normalizedBasePath = baseRelativePath.replace(File.separator, "/")
                val normalizedModulePath = modulePath.replace(File.separator, "/")
                val fullPath = if (normalizedBasePath.isNotEmpty()) {
                    "$normalizedBasePath/$normalizedModulePath"
                } else {
                    normalizedModulePath
                }
                val moduleEntry = ":" + fullPath.replace("/", ":")
                gradleChanges.add("Add to settings.gradle(.kts): include(\"$moduleEntry\")")
            }
        }

        return GenerationPreview(directories, files, gradleChanges)
    }

    /**
     * Create directory structure
     */
    private fun createDirectory(baseDir: VirtualFile, relativePath: String) {
        ApplicationManager.getApplication().runWriteAction {
            try {
                val parts = relativePath.split("/")
                var currentDir = baseDir

                parts.forEach { part ->
                    if (part.isNotBlank()) {
                        val child = currentDir.findChild(part)
                        currentDir = child ?: currentDir.createChildDirectory(this, part)
                    }
                }
            } catch (e: Exception) {
                log.error("Error creating directory: $relativePath", e)
            }
        }
    }

    /**
     * Generate a file from template with FreeMarker processing
     */
    private fun generateFile(
        baseDir: VirtualFile,
        fileTemplate: FileTemplate,
        variables: Map<String, String>
    ): VirtualFile? {
        return ApplicationManager.getApplication().runWriteAction<VirtualFile?> {
            try {
                // Process path with FreeMarker
                val ftlService = FtlTemplateService.getInstance(project)
                val processedPath = ftlService.processTemplate(fileTemplate.path, variables)

                // Process content with FreeMarker
                val processedContent = ftlService.processTemplate(fileTemplate.content, variables)

                // Navigate to parent directory
                val parts = processedPath.split("/")
                var currentDir = baseDir

                for (i in 0 until parts.size - 1) {
                    val part = parts[i]
                    if (part.isNotBlank()) {
                        val child = currentDir.findChild(part)
                        currentDir = child ?: currentDir.createChildDirectory(this, part)
                    }
                }

                // Create file
                val fileName = parts.last()
                val file = currentDir.findChild(fileName) ?: currentDir.createChildData(this, fileName)
                file.setBinaryContent(processedContent.toByteArray(charset(fileTemplate.encoding)))
                file
            } catch (e: Exception) {
                log.error("Error generating file: ${fileTemplate.path}", e)
                null
            }
        }
    }

    /**
     * Extract module paths from build.gradle paths
     * Returns full paths to modules (without build.gradle.kts)
     * e.g., "cool-feature/api/build.gradle.kts" -> "cool-feature/api"
     * e.g., "api/build.gradle.kts" -> "api"
     */
    private fun extractModulePaths(configuration: ModuleConfiguration): List<String> {
        val ftlService = FtlTemplateService.getInstance(project)
        val modulePaths = mutableSetOf<String>()

        configuration.template.modulePaths.forEach { modulePath ->
            // Process path with FreeMarker to resolve variables
            val resolvedPath = ftlService.processTemplate(modulePath, configuration.variables)

            // Remove build.gradle or build.gradle.kts from the end
            val modulePathWithoutBuildGradle = when {
                resolvedPath.endsWith("/build.gradle.kts") -> {
                    resolvedPath.removeSuffix("/build.gradle.kts")
                }

                resolvedPath.endsWith("/build.gradle") -> {
                    resolvedPath.removeSuffix("/build.gradle")
                }

                resolvedPath == "build.gradle.kts" || resolvedPath == "build.gradle" -> {
                    // build.gradle is in root, use empty path (will use target directory name)
                    ""
                }

                else -> {
                    // If path doesn't end with build.gradle, assume it's already a module path
                    resolvedPath
                }
            }

            if (modulePathWithoutBuildGradle.isNotBlank()) {
                modulePaths.add(modulePathWithoutBuildGradle)
            } else {
                // build.gradle is in root, use target directory name as module path
                val targetDir = File(configuration.targetPath)
                val dirName = targetDir.name
                if (dirName.isNotBlank()) {
                    modulePaths.add(dirName)
                }
            }
        }

        return modulePaths.toList()
    }

    /**
     * Update settings.gradle file with all module paths
     * modulePaths are relative paths from targetPath (e.g., "cool-feature/api")
     */
    private fun updateSettingsGradle(configuration: ModuleConfiguration, modulePaths: List<String>) {
        if (modulePaths.isEmpty()) return

        ApplicationManager.getApplication().runWriteAction {
            try {
                val projectBasePath = project.basePath ?: return@runWriteAction
                val settingsFile = findSettingsGradleFile(projectBasePath) ?: return@runWriteAction

                // Calculate relative path from project root to target path
                val targetPath = File(configuration.targetPath).canonicalPath
                val projectPath = File(projectBasePath).canonicalPath

                val baseRelativePath = if (targetPath.startsWith(projectPath)) {
                    targetPath.substring(projectPath.length)
                        .removePrefix(File.separator)
                } else {
                    ""
                }

                // Read current content
                val currentContent = String(settingsFile.contentsToByteArray())
                val newIncludes = mutableListOf<String>()

                // Build module entries for each module path
                modulePaths.forEach { modulePath ->
                    // Build full path: baseRelativePath + modulePath
                    // Normalize paths to use / separator, then convert to Gradle format
                    val normalizedBasePath = baseRelativePath.replace(File.separator, "/")
                    val normalizedModulePath = modulePath.replace(File.separator, "/")
                    val fullPath = if (normalizedBasePath.isNotEmpty()) {
                        "$normalizedBasePath/$normalizedModulePath"
                    } else {
                        normalizedModulePath
                    }
                    // Convert to Gradle module path format (replace / with :)
                    val moduleEntry = ":" + fullPath.replace("/", ":")

                    // Check if module already included
                    if (!currentContent.contains("include(\"$moduleEntry\")") &&
                        !currentContent.contains("include('$moduleEntry')")
                    ) {
                        newIncludes.add("include(\"$moduleEntry\")")
                    }
                }

                // Add new include statements
                if (newIncludes.isNotEmpty()) {
                    val newContent = currentContent + "\n" + newIncludes.joinToString("\n") + "\n"
                    settingsFile.setBinaryContent(newContent.toByteArray())
                }

            } catch (e: Exception) {
                log.error("Error updating settings.gradle", e)
                throw e
            }
        }
    }

    /**
     * Find settings.gradle or settings.gradle.kts file
     */
    private fun findSettingsGradleFile(projectPath: String): VirtualFile? {
        val fs = LocalFileSystem.getInstance()
        return fs.findFileByPath("$projectPath/settings.gradle.kts")
            ?: fs.findFileByPath("$projectPath/settings.gradle")
    }

    companion object {
        fun getInstance(project: Project): ModuleGeneratorService {
            return project.getService(ModuleGeneratorService::class.java)
        }
    }
}

