package com.github.nonoxys.kmpmodulegenerator.services

import com.github.nonoxys.kmpmodulegenerator.models.ModuleConfiguration
import com.github.nonoxys.kmpmodulegenerator.models.ModuleTemplate
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project

/**
 * Service for managing module templates
 * Supports both built-in templates and FreeMarker-based user templates
 */
@Service(Service.Level.PROJECT)
class TemplateService(private val project: Project) {

    private val log = Logger.getInstance(TemplateService::class.java)
    private val templates = mutableMapOf<String, ModuleTemplate>()
    private val ftlService: FtlTemplateService by lazy { FtlTemplateService.getInstance(project) }

    init {
        // Load FTL templates from configured folder
        loadFtlTemplates()
    }

    /**
     * Get all available templates (built-in + FTL)
     */
    fun getAllTemplates(): List<ModuleTemplate> {
        return templates.values.toList()
    }

    /**
     * Get template by ID
     */
    fun getTemplate(id: String): ModuleTemplate? {
        return templates[id]
    }


    /**
     * Register a new template
     */
    private fun registerTemplate(template: ModuleTemplate) {
        templates[template.id] = template
    }

    /**
     * Unregister template
     */
    fun unregisterTemplate(id: String) {
        templates.remove(id)
    }

    /**
     * Reload all templates
     */
    fun reloadTemplates() {
        templates.clear()
        loadFtlTemplates()
    }

    /**
     * Load FTL templates from .idea/kmp-templates/
     */
    private fun loadFtlTemplates() {
        try {
            val templateDir = ftlService.getTemplateDirectory()
            log.info("Loading FTL templates from: ${templateDir.absolutePath}")

            templateDir.listFiles()?.forEach { folder ->
                if (folder.isDirectory) {
                    try {
                        val template = ftlService.loadTemplate(folder)
                        if (template != null) {
                            registerTemplate(template)
                            log.info("Loaded FTL template: ${template.id}")
                        }
                    } catch (e: Exception) {
                        log.warn("Failed to load template from ${folder.name}", e)
                    }
                }
            }
        } catch (e: Exception) {
            log.error("Failed to load FTL templates", e)
        }
    }

    /**
     * Create a module configuration from template
     */
    fun createConfiguration(
        template: ModuleTemplate,
        variables: Map<String, String>,
        targetPath: String
    ): ModuleConfiguration {
        return ModuleConfiguration(template, variables, targetPath)
    }

    /**
     * Validate a configuration
     */
    fun validateConfiguration(configuration: ModuleConfiguration): List<String> {
        return configuration.validate()
    }

    companion object {
        fun getInstance(project: Project): TemplateService {
            return project.getService(TemplateService::class.java)
        }
    }
}

