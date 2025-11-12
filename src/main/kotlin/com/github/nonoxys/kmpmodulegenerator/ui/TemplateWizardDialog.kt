package com.github.nonoxys.kmpmodulegenerator.ui

import com.github.nonoxys.kmpmodulegenerator.services.FtlTemplateService
import com.github.nonoxys.kmpmodulegenerator.services.TemplateService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import java.awt.Dimension
import java.io.File
import javax.swing.*
import javax.swing.table.DefaultTableModel

/**
 * Wizard for creating new template from scratch
 */
class TemplateWizardDialog(private val project: Project) : DialogWrapper(project) {

    private val idField = JBTextField()
    private val nameField = JBTextField()
    private val descriptionArea = JTextArea(3, 40)

    private val parametersTableModel = DefaultTableModel(
        arrayOf("Name", "Display Name", "Type", "Required", "Default"),
        0
    )
    private val parametersTable = JBTable(parametersTableModel)
    private val parameters = mutableListOf<ParameterData>()

    init {
        title = "Create New Template"

        // Add standard parameters by default
        addStandardParameter("moduleName", "Module Name", "TEXT", true, "")
        addStandardParameter("packageName", "Package Name", "PACKAGE", true, "")

        init()
    }

    override fun createCenterPanel(): JComponent {
        val mainPanel = JPanel(BorderLayout(10, 10))
        mainPanel.preferredSize = Dimension(700, 500)

        // Info panel
        val infoPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent("Template ID:", idField)
            .addTooltip("Unique identifier (lowercase, hyphens allowed)")
            .addLabeledComponent("Display Name:", nameField)
            .addTooltip("Name shown in UI")
            .addLabeledComponent("Description:", JScrollPane(descriptionArea))
            .panel

        // Parameters panel
        val parametersPanel = JPanel(BorderLayout())
        parametersPanel.border = BorderFactory.createTitledBorder("Parameters (Variables)")

        val tableScrollPane = JScrollPane(parametersTable)
        parametersPanel.add(tableScrollPane, BorderLayout.CENTER)

        // Buttons
        val buttonPanel = JPanel()
        val addButton = JButton("Add Parameter")
        val editButton = JButton("Edit")
        val deleteButton = JButton("Delete")
        val moveUpButton = JButton("↑")
        val moveDownButton = JButton("↓")

        addButton.addActionListener { addParameter() }
        editButton.addActionListener { editParameter() }
        deleteButton.addActionListener { deleteParameter() }
        moveUpButton.addActionListener { moveParameterUp() }
        moveDownButton.addActionListener { moveParameterDown() }

        buttonPanel.add(addButton)
        buttonPanel.add(editButton)
        buttonPanel.add(deleteButton)
        buttonPanel.add(JSeparator(SwingConstants.VERTICAL))
        buttonPanel.add(moveUpButton)
        buttonPanel.add(moveDownButton)

        parametersPanel.add(buttonPanel, BorderLayout.SOUTH)

        // Help panel
        val helpPanel = JPanel(BorderLayout())
        helpPanel.border = BorderFactory.createTitledBorder("What's Next?")
        val helpText = JBLabel(
            "<html>" +
                    "After creating this template:<br>" +
                    "1. Template folder will be created in your templates directory<br>" +
                    "2. Edit template.xml if needed<br>" +
                    "3. Add files to root/ folder (use .ftl extension for FreeMarker)<br>" +
                    "4. Use \${variableName} in files to insert parameter values" +
                    "</html>"
        )
        helpPanel.add(helpText, BorderLayout.CENTER)

        mainPanel.add(infoPanel, BorderLayout.NORTH)
        mainPanel.add(parametersPanel, BorderLayout.CENTER)
        mainPanel.add(helpPanel, BorderLayout.SOUTH)

        return mainPanel
    }

    private fun addStandardParameter(
        name: String,
        displayName: String,
        type: String,
        required: Boolean,
        default: String
    ) {
        val param = ParameterData(name, displayName, type, required, default)
        parameters.add(param)
        parametersTableModel.addRow(arrayOf(name, displayName, type, required, default))
    }

    private fun addParameter() {
        val dialog = ParameterEditorDialog(project, null)
        if (dialog.showAndGet()) {
            val param = dialog.getParameter()

            // Check for duplicate names
            if (parameters.any { it.name == param.name }) {
                Messages.showErrorDialog(
                    project,
                    "Parameter with name '${param.name}' already exists",
                    "Duplicate Parameter"
                )
                return
            }

            parameters.add(param)
            parametersTableModel.addRow(
                arrayOf(
                    param.name,
                    param.displayName,
                    param.type,
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

    private fun moveParameterUp() {
        val selectedRow = parametersTable.selectedRow
        if (selectedRow <= 0) return

        // Swap in list
        val temp = parameters[selectedRow]
        parameters[selectedRow] = parameters[selectedRow - 1]
        parameters[selectedRow - 1] = temp

        // Swap in table
        parametersTableModel.moveRow(selectedRow, selectedRow, selectedRow - 1)
        parametersTable.setRowSelectionInterval(selectedRow - 1, selectedRow - 1)
    }

    private fun moveParameterDown() {
        val selectedRow = parametersTable.selectedRow
        if (selectedRow < 0 || selectedRow >= parameters.size - 1) return

        // Swap in list
        val temp = parameters[selectedRow]
        parameters[selectedRow] = parameters[selectedRow + 1]
        parameters[selectedRow + 1] = temp

        // Swap in table
        parametersTableModel.moveRow(selectedRow, selectedRow, selectedRow + 1)
        parametersTable.setRowSelectionInterval(selectedRow + 1, selectedRow + 1)
    }

    override fun doValidate(): com.intellij.openapi.ui.ValidationInfo? {
        if (idField.text.isBlank()) {
            return com.intellij.openapi.ui.ValidationInfo("Template ID is required", idField)
        }

        if (!idField.text.matches(Regex("^[a-z][a-z0-9-]*$"))) {
            return com.intellij.openapi.ui.ValidationInfo(
                "Template ID must start with lowercase letter and contain only lowercase, numbers, and hyphens",
                idField
            )
        }

        if (nameField.text.isBlank()) {
            return com.intellij.openapi.ui.ValidationInfo("Template name is required", nameField)
        }

        // Check if template already exists
        val ftlService = FtlTemplateService.getInstance(project)
        val templateDir = File(ftlService.getTemplateDirectory(), idField.text)
        if (templateDir.exists()) {
            return com.intellij.openapi.ui.ValidationInfo("Template with ID '${idField.text}' already exists", idField)
        }

        return null
    }

    override fun doOKAction() {
        createTemplate()
        super.doOKAction()
    }

    private fun createTemplate() {
        val ftlService = FtlTemplateService.getInstance(project)
        val templateDir = File(ftlService.getTemplateDirectory(), idField.text)
        templateDir.mkdirs()

        // Create template.xml
        val templateXml = File(templateDir, "template.xml")
        val xml = buildString {
            appendLine("<?xml version=\"1.0\"?>")
            appendLine("<template>")
            appendLine("    <id>${idField.text}</id>")
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
        templateXml.writeText(xml)

        // Create root directory with example
        val rootDir = File(templateDir, "root")
        rootDir.mkdirs()

        // Create example build.gradle.kts.ftl
        val buildGradleFile = File(rootDir, "build.gradle.kts.ftl")
        buildGradleFile.writeText(
            """
            plugins {
                kotlin("jvm")
            }
            
            dependencies {
                implementation(kotlin("stdlib"))
            }
        """.trimIndent()
        )

        // Create README
        val readmeFile = File(templateDir, "README.md")
        readmeFile.writeText(
            """
            # ${nameField.text}
            
            ${descriptionArea.text}
            
            ## Parameters
            
            ${parameters.joinToString("\n") { "- **${it.displayName}** (${it.name}): ${it.description}" }}
            
            ## Files
            
            Add your template files to the `root/` directory.
            Use `.ftl` extension for FreeMarker templates.
            
            Available variables:
            ${parameters.joinToString("\n") { "- `\${${it.name}}`" }}
        """.trimIndent()
        )

        // Reload templates
        TemplateService.getInstance(project).reloadTemplates()

        Messages.showInfoMessage(
            project,
            "Template created successfully!\n\nLocation: ${templateDir.absolutePath}\n\nAdd your files to the root/ folder.",
            "Template Created"
        )
    }
}

