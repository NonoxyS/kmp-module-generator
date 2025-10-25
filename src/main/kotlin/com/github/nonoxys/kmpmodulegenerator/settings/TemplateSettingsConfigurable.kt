package com.github.nonoxys.kmpmodulegenerator.settings

import com.github.nonoxys.kmpmodulegenerator.services.TemplateService
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.ui.layout.intToBinding
import java.awt.Desktop
import java.io.File
import javax.swing.JButton

/**
 * Settings UI for template configuration using Kotlin UI DSL
 */
class TemplateSettingsConfigurable(private val project: Project) : BoundConfigurable("KMP Module Templates") {
    
    private var useCustomFolder = TemplateSettings.isUsingCustomFolder(project)
    private var customFolderPath = TemplateSettings.getCustomFolderPath(project) ?: ""
    
    override fun createPanel(): DialogPanel = panel {
        val defaultFolder = TemplateSettings.getDefaultTemplateFolder(project)
        
        group("Template Storage Location") {
            row {
                label("Default location:")
                    .bold()
            }
            row {
                comment(defaultFolder.absolutePath)
            }
            
            row {
                checkBox("Use custom template folder")
                    .bindSelected(::useCustomFolder)
                    .onChanged { checkbox ->
                        // Enable/disable custom folder field
                    }
            }

            row("Custom folder:") {
                textFieldWithBrowseButton(
                    FileChooserDescriptorFactory.createSingleFolderDescriptor()
                        .withTitle("Select Template Folder"),
                    project
                )
                    .bindText(::customFolderPath)
                    .columns(COLUMNS_LARGE)
                    .enabledIf(ComponentPredicate.fromValue(useCustomFolder))
            }.comment("Choose folder where templates are stored")
            
            row {
                button("Open Templates Folder") {
                    val folder = TemplateSettings.getTemplateFolder(project)
                    folder.mkdirs()
                    try {
                        Desktop.getDesktop().open(folder)
                    } catch (e: Exception) {
                        // Silently ignore if Desktop is not supported
                    }
                }
            }
            
            row {
                comment("Templates are .ftl files organized in folders.<br>" +
                        "Each folder should contain template.xml and root/ directory.")
            }
        }
    }
    
    override fun apply() {
        super.apply()
        
        if (useCustomFolder && customFolderPath.isNotBlank()) {
            TemplateSettings.setCustomTemplateFolder(project, File(customFolderPath))
        } else {
            TemplateSettings.setCustomTemplateFolder(project, null)
        }
        
        // Reload templates from new location
        TemplateService.getInstance(project).reloadTemplates()
    }
    
    override fun reset() {
        useCustomFolder = TemplateSettings.isUsingCustomFolder(project)
        customFolderPath = TemplateSettings.getCustomFolderPath(project) ?: ""
        super.reset()
    }
}

