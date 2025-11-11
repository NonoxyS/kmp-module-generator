package com.github.nonoxys.kmpmodulegenerator.actions

import com.github.nonoxys.kmpmodulegenerator.ui.TemplateWizardDialog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

/**
 * Action to create new template using wizard
 */
class CreateTemplateAction : AnAction(
    "New Template...",
    "Create a new module template using wizard",
    null
) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val wizard = TemplateWizardDialog(project)
        wizard.show()
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }
}

