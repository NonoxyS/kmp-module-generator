package com.github.nonoxys.kmpmodulegenerator.settings

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import java.io.File

/**
 * Settings for template storage location
 */
object TemplateSettings {
    
    private const val TEMPLATE_FOLDER_KEY = "kmp.module.generator.template.folder"
    private const val USE_CUSTOM_FOLDER_KEY = "kmp.module.generator.use.custom.folder"
    
    /**
     * Get template folder for project
     */
    fun getTemplateFolder(project: Project): File {
        val properties = PropertiesComponent.getInstance(project)
        val useCustom = properties.getBoolean(USE_CUSTOM_FOLDER_KEY, false)
        
        return if (useCustom) {
            val customPath = properties.getValue(TEMPLATE_FOLDER_KEY)
            if (customPath != null) {
                File(customPath)
            } else {
                getDefaultTemplateFolder(project)
            }
        } else {
            getDefaultTemplateFolder(project)
        }
    }
    
    /**
     * Get default template folder (.idea/kmp-templates/)
     */
    fun getDefaultTemplateFolder(project: Project): File {
        val projectPath = project.basePath ?: return File(System.getProperty("user.home"), ".kmp-templates")
        return File(projectPath, ".idea/kmp-templates")
    }
    
    /**
     * Set custom template folder
     */
    fun setCustomTemplateFolder(project: Project, folder: File?) {
        val properties = PropertiesComponent.getInstance(project)
        
        if (folder != null) {
            properties.setValue(TEMPLATE_FOLDER_KEY, folder.absolutePath)
            properties.setValue(USE_CUSTOM_FOLDER_KEY, true)
        } else {
            properties.unsetValue(TEMPLATE_FOLDER_KEY)
            properties.setValue(USE_CUSTOM_FOLDER_KEY, false)
        }
    }
    
    /**
     * Check if using custom folder
     */
    fun isUsingCustomFolder(project: Project): Boolean {
        return PropertiesComponent.getInstance(project).getBoolean(USE_CUSTOM_FOLDER_KEY, false)
    }
    
    /**
     * Get custom folder path (if set)
     */
    fun getCustomFolderPath(project: Project): String? {
        return if (isUsingCustomFolder(project)) {
            PropertiesComponent.getInstance(project).getValue(TEMPLATE_FOLDER_KEY)
        } else {
            null
        }
    }
}

