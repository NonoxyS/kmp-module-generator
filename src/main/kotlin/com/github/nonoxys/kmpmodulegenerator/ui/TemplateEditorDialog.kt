package com.github.nonoxys.kmpmodulegenerator.ui

import com.github.nonoxys.kmpmodulegenerator.models.VariableType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.MouseAdapter
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

    private val parametersTableModel = object : DefaultTableModel(
        arrayOf("Name", "Display Name", "Type", "Required", "Default"),
        0
    ) {
        override fun isCellEditable(row: Int, column: Int): Boolean {
            // read-only - editing only through Edit button/double-click
            return false
        }
    }

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

        // Add double-click listener to edit parameter
        parametersTable.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent) {
                if (e.clickCount == 2) {
                    editParameter()
                }
            }
        })

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
        val parameterPattern =
            Regex("<parameter[^>]*name=\"([^\"]+)\"[^>]*>(.*?)</parameter>", RegexOption.DOT_MATCHES_ALL)
        parameterPattern.findAll(content).forEach { match ->
            val name = match.groupValues[1]
            val paramContent = match.groupValues[2]

            val displayName = extractXmlTag(paramContent, "displayName") ?: name
            val paramTypeString = extractXmlTag(paramContent, "type") ?: VariableType.TEXT.name
            val required = extractXmlTag(paramContent, "required")?.toBoolean() ?: true
            val default = extractXmlTag(paramContent, "default") ?: ""
            val description = extractXmlTag(paramContent, "description") ?: ""
            val options = extractXmlTag(paramContent, "options") ?: ""

            val paramType = try {
                VariableType.valueOf(paramTypeString)
            } catch (_: IllegalArgumentException) {
                VariableType.TEXT
            }

            val param = ParameterData(name, displayName, paramType, required, default, description, options)
            parameters.add(param)
            parametersTableModel.addRow(arrayOf(name, displayName, paramType.name, required, default))
        }
    }

    private fun addParameter() {
        val dialog = ParameterEditorDialog(project, null)
        if (dialog.showAndGet()) {
            val param = dialog.getParameter()
            parameters.add(param)
            parametersTableModel.addRow(
                arrayOf(
                    param.name,
                    param.displayName,
                    param.type.name,
                    param.required,
                    param.defaultValue
                )
            )
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
            parametersTableModel.setValueAt(edited.type.name, selectedRow, 2)
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
                appendLine("            <type>${param.type.name}</type>")
                if (param.defaultValue.isNotBlank()) {
                    appendLine("            <default>${param.defaultValue}</default>")
                }
                if (param.options.isNotBlank() && param.type == VariableType.DROPDOWN) {
                    appendLine("            <options>${param.options}</options>")
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
    var type: VariableType,
    var required: Boolean,
    var defaultValue: String,
    var description: String = "",
    var options: String = ""
)

