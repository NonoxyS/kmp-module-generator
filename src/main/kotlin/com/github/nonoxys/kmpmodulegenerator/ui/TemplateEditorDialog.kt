package com.github.nonoxys.kmpmodulegenerator.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.*
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import java.awt.Dimension
import java.io.File
import javax.swing.*
import javax.swing.table.DefaultTableModel

/**
 * Visual editor for template.xml
 */
class TemplateEditorDialog(
    private val project: Project,
    private val templateFile: File
) : DialogWrapper(project) {
    
    private val nameField = JBTextField()
    private val descriptionArea = JTextArea(3, 40)
    
    private val parametersTableModel = DefaultTableModel(
        arrayOf("Name", "Display Name", "Type", "Required", "Default"),
        0
    )
    private val parametersTable = JBTable(parametersTableModel)
    
    private var parameters = mutableListOf<ParameterData>()
    
    init {
        title = "Edit Template: ${templateFile.parentFile.name}"
        parseExistingTemplate()
        init()
    }
    
    override fun createCenterPanel(): JComponent {
        val mainPanel = JPanel(BorderLayout(10, 10))
        mainPanel.preferredSize = Dimension(700, 500)
        
        // Top panel - basic info
        val infoPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent("Template ID:", JBLabel(templateFile.parentFile.name))
            .addLabeledComponent("Name:", nameField)
            .addLabeledComponent("Description:", JBScrollPane(descriptionArea))
            .panel
        
        // Parameters panel
        val parametersPanel = JPanel(BorderLayout())
        parametersPanel.border = BorderFactory.createTitledBorder("Parameters")
        
        val tableScrollPane = JBScrollPane(parametersTable)
        parametersPanel.add(tableScrollPane, BorderLayout.CENTER)
        
        // Buttons for parameters
        val buttonPanel = JPanel()
        val addButton = JButton("Add Parameter")
        val editButton = JButton("Edit")
        val deleteButton = JButton("Delete")
        
        addButton.addActionListener { addParameter() }
        editButton.addActionListener { editParameter() }
        deleteButton.addActionListener { deleteParameter() }
        
        buttonPanel.add(addButton)
        buttonPanel.add(editButton)
        buttonPanel.add(deleteButton)
        parametersPanel.add(buttonPanel, BorderLayout.SOUTH)
        
        mainPanel.add(infoPanel, BorderLayout.NORTH)
        mainPanel.add(parametersPanel, BorderLayout.CENTER)
        
        return mainPanel
    }
    
    private fun parseExistingTemplate() {
        if (!templateFile.exists()) return
        
        val content = templateFile.readText()
        
        // Parse basic info
        nameField.text = extractXmlTag(content, "name") ?: ""
        descriptionArea.text = extractXmlTag(content, "description") ?: ""
        
        // Parse parameters
        val parameterPattern = Regex("<parameter[^>]*name=\"([^\"]+)\"[^>]*>(.*?)</parameter>", RegexOption.DOT_MATCHES_ALL)
        parameterPattern.findAll(content).forEach { match ->
            val name = match.groupValues[1]
            val paramContent = match.groupValues[2]
            
            val displayName = extractXmlTag(paramContent, "displayName") ?: name
            val paramType = extractXmlTag(paramContent, "type") ?: "TEXT"
            val required = extractXmlTag(paramContent, "required")?.toBoolean() ?: true
            val default = extractXmlTag(paramContent, "default") ?: ""
            val description = extractXmlTag(paramContent, "description") ?: ""
            
            val param = ParameterData(name, displayName, paramType, required, default, description)
            parameters.add(param)
            parametersTableModel.addRow(arrayOf(name, displayName, paramType, required, default))
        }
    }
    
    private fun addParameter() {
        val dialog = ParameterEditorDialog(project, null)
        if (dialog.showAndGet()) {
            val param = dialog.getParameter()
            parameters.add(param)
            parametersTableModel.addRow(arrayOf(
                param.name,
                param.displayName,
                param.type,
                param.required,
                param.defaultValue
            ))
        }
    }
    
    private fun editParameter() {
        val selectedRow = parametersTable.selectedRow
        if (selectedRow < 0) {
            Messages.showWarningDialog(project, "Please select a parameter to edit", "No Selection")
            return
        }
        
        val param = parameters[selectedRow]
        val dialog = ParameterEditorDialog(project, param)
        if (dialog.showAndGet()) {
            val edited = dialog.getParameter()
            parameters[selectedRow] = edited
            
            parametersTableModel.setValueAt(edited.name, selectedRow, 0)
            parametersTableModel.setValueAt(edited.displayName, selectedRow, 1)
            parametersTableModel.setValueAt(edited.type, selectedRow, 2)
            parametersTableModel.setValueAt(edited.required, selectedRow, 3)
            parametersTableModel.setValueAt(edited.defaultValue, selectedRow, 4)
        }
    }
    
    private fun deleteParameter() {
        val selectedRow = parametersTable.selectedRow
        if (selectedRow < 0) {
            Messages.showWarningDialog(project, "Please select a parameter to delete", "No Selection")
            return
        }
        
        parameters.removeAt(selectedRow)
        parametersTableModel.removeRow(selectedRow)
    }
    
    override fun doOKAction() {
        if (nameField.text.isBlank()) {
            Messages.showErrorDialog(project, "Template name is required", "Validation Error")
            return
        }
        
        saveTemplate()
        super.doOKAction()
    }
    
    private fun saveTemplate() {
        val xml = buildString {
            appendLine("<?xml version=\"1.0\"?>")
            appendLine("<template>")
            appendLine("    <id>${templateFile.parentFile.name}</id>")
            appendLine("    <name>${nameField.text}</name>")
            appendLine("    <description>${descriptionArea.text}</description>")
            appendLine()
            appendLine("    <parameters>")
            
            parameters.forEach { param ->
                appendLine("        <parameter name=\"${param.name}\">")
                appendLine("            <displayName>${param.displayName}</displayName>")
                if (param.description.isNotBlank()) {
                    appendLine("            <description>${param.description}</description>")
                }
                appendLine("            <type>${param.type}</type>")
                if (param.defaultValue.isNotBlank()) {
                    appendLine("            <default>${param.defaultValue}</default>")
                }
                appendLine("            <required>${param.required}</required>")
                appendLine("        </parameter>")
            }
            
            appendLine("    </parameters>")
            appendLine("</template>")
        }
        
        templateFile.writeText(xml)
    }
    
    private fun extractXmlTag(xml: String, tag: String): String? {
        val pattern = Regex("<$tag>(.*?)</$tag>", RegexOption.DOT_MATCHES_ALL)
        return pattern.find(xml)?.groupValues?.get(1)?.trim()
    }
}

data class ParameterData(
    var name: String,
    var displayName: String,
    var type: String,
    var required: Boolean,
    var defaultValue: String,
    var description: String = ""
)

