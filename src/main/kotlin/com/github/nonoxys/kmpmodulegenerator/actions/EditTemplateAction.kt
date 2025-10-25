package com.github.nonoxys.kmpmodulegenerator.actions

import com.github.nonoxys.kmpmodulegenerator.services.FtlTemplateService
import com.github.nonoxys.kmpmodulegenerator.services.TemplateService
import com.github.nonoxys.kmpmodulegenerator.ui.TemplateEditorDialog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import java.io.File

/**
 * Action to edit existing template
 */
class EditTemplateAction : AnAction(
    "Edit Template...",
    "Edit an existing template configuration",
    null
) {
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        // Reload templates to ensure we have the latest list   
        TemplateService.getInstance(project).reloadTemplates()
        
        val ftlService = FtlTemplateService.getInstance(project)
        val templateDir = ftlService.getTemplateDirectory()
        
        // Get list of templates
        val templates = templateDir.listFiles()
            ?.filter { it.isDirectory && File(it, "template.xml").exists() }
            ?: emptyList()
        
        if (templates.isEmpty()) {
            Messages.showInfoMessage(
                project,
                "No templates found.\n\nCreate a new template first using 'New Template' action.",
                "No Templates"
            )
            return
        }
        
        // Show selection dialog
        val templateNames = templates.map { it.name }.toTypedArray()
        val selectedIndex = Messages.showChooseDialog(
            project,
            "Select template to edit:",
            "Edit Template",
            Messages.getQuestionIcon(),
            templateNames,
            templateNames.firstOrNull()
        )

        if (selectedIndex >= 0) {
            val selectedTemplate = templates[selectedIndex]
            val templateXml = File(selectedTemplate, "template.xml")

            val editor = TemplateEditorDialog(project, templateXml)
            if (editor.showAndGet()) {
                // Reload templates
                TemplateService.getInstance(project).reloadTemplates()

                Messages.showInfoMessage(
                    project,
                    "Template updated successfully!",
                    "Success"
                )
            }
        }
    }
    
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }
}

