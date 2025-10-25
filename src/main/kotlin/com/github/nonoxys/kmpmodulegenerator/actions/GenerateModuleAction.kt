package com.github.nonoxys.kmpmodulegenerator.actions

import com.github.nonoxys.kmpmodulegenerator.services.TemplateService
import com.github.nonoxys.kmpmodulegenerator.ui.ModuleGeneratorDialog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * Action to open module generator dialog
 */
class GenerateModuleAction : AnAction() {
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val selectedFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        
        val initialPath = getInitialPath(project, selectedFile)
        
        // Reload templates to ensure we have the latest list
        TemplateService.getInstance(project).reloadTemplates()
        
        val dialog = ModuleGeneratorDialog(project, initialPath)
        dialog.show()
    }
    
    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabledAndVisible = project != null
    }
    
    /**
     * Get initial path for module generation
     */
    private fun getInitialPath(project: Project, selectedFile: VirtualFile?): String {
        return when {
            selectedFile != null && selectedFile.isDirectory -> selectedFile.path
            selectedFile != null -> selectedFile.parent?.path ?: project.basePath ?: ""
            else -> project.basePath ?: ""
        }
    }
}

