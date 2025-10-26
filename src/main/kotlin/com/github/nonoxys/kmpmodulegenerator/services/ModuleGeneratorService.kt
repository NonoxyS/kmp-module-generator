package com.github.nonoxys.kmpmodulegenerator.services

import com.github.nonoxys.kmpmodulegenerator.models.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.diagnostic.Logger
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
            
            val moduleName = configuration.variables["moduleName"] 
                ?: return GenerationResult.Failure("Module name is required")
            
            // Create module directory
            val moduleDir = createModuleDirectory(configuration.targetPath, moduleName)
                ?: return GenerationResult.Failure("Failed to create module directory")
            
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
            
            // Update settings.gradle
            try {
                updateSettingsGradle(configuration)
            } catch (e: Exception) {
                log.warn("Failed to update settings.gradle", e)
                warnings.add("Failed to update settings.gradle: ${e.message}")
            }
            
            // Refresh file system
            ApplicationManager.getApplication().invokeLater {
                moduleDir.refresh(false, true)
            }
            
            return if (warnings.isEmpty()) {
                GenerationResult.Success(moduleName, moduleDir, generatedFiles)
            } else {
                GenerationResult.Warning(moduleName, moduleDir, generatedFiles, warnings)
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
        
        // Preview gradle changes
        val moduleName = configuration.variables["moduleName"] ?: "moduleName"
        val projectBasePath = project.basePath ?: ""
        val targetPath = File(configuration.targetPath).canonicalPath
        val projectPath = File(projectBasePath).canonicalPath
        
        val relativePath = if (targetPath.startsWith(projectPath)) {
            targetPath.substring(projectPath.length)
                .removePrefix(File.separator)
                .replace(File.separator, ":")
        } else {
            ""
        }
        
        val moduleEntry = if (relativePath.isNotEmpty()) {
            ":$relativePath:$moduleName"
        } else {
            ":$moduleName"
        }
        
        gradleChanges.add("Add to settings.gradle(.kts): include(\"$moduleEntry\")")
        
        return GenerationPreview(directories, files, gradleChanges)
    }
    
    /**
     * Create module directory
     */
    private fun createModuleDirectory(basePath: String, moduleName: String): VirtualFile? {
        return ApplicationManager.getApplication().runWriteAction<VirtualFile?> {
            try {
                val baseDir = File(basePath)
                val moduleDir = File(baseDir, moduleName)
                
                if (!moduleDir.exists()) {
                    moduleDir.mkdirs()
                }
                
                LocalFileSystem.getInstance().refreshAndFindFileByIoFile(moduleDir)
            } catch (e: Exception) {
                log.error("Error creating module directory", e)
                null
            }
        }
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
                
                if (fileTemplate.executable) {
                    file.isWritable = true
                }
                
                file
            } catch (e: Exception) {
                log.error("Error generating file: ${fileTemplate.path}", e)
                null
            }
        }
    }
    
    /**
     * Update settings.gradle file
     */
    private fun updateSettingsGradle(configuration: ModuleConfiguration) {
        ApplicationManager.getApplication().runWriteAction {
            try {
                val projectBasePath = project.basePath ?: return@runWriteAction
                val settingsFile = findSettingsGradleFile(projectBasePath) ?: return@runWriteAction
                
                val moduleName = configuration.variables["moduleName"] ?: return@runWriteAction
                
                // Calculate relative path from project root to target path
                val targetPath = File(configuration.targetPath).canonicalPath
                val projectPath = File(projectBasePath).canonicalPath
                
                val relativePath = if (targetPath.startsWith(projectPath)) {
                    targetPath.substring(projectPath.length)
                        .removePrefix(File.separator)
                        .replace(File.separator, ":")
                } else {
                    ""
                }
                
                // Build module entry (e.g., ":shared:moduleName" or ":moduleName")
                val moduleEntry = if (relativePath.isNotEmpty()) {
                    ":$relativePath:$moduleName"
                } else {
                    ":$moduleName"
                }
                
                // Read current content
                val currentContent = String(settingsFile.contentsToByteArray())
                
                // Check if module already included
                if (currentContent.contains("include(\"$moduleEntry\")") ||
                    currentContent.contains("include('$moduleEntry')")) {
                    return@runWriteAction
                }
                
                // Add include statement
                val newContent = currentContent + "\ninclude(\"$moduleEntry\")\n"
                settingsFile.setBinaryContent(newContent.toByteArray())
                
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
    
    /**
     * Resolve variables in string
     */
    private fun resolveVariables(template: String, variables: Map<String, String>): String {
        var result = template
        variables.forEach { (key, value) ->
            result = result.replace("\${$key}", value)
            result = result.replace("{{$key}}", value)
        }
        return result
    }
    
    companion object {
        fun getInstance(project: Project): ModuleGeneratorService {
            return project.getService(ModuleGeneratorService::class.java)
        }
    }
}

