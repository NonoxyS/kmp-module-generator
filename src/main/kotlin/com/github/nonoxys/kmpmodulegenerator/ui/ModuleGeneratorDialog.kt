package com.github.nonoxys.kmpmodulegenerator.ui

import com.github.nonoxys.kmpmodulegenerator.models.*
import com.github.nonoxys.kmpmodulegenerator.services.ModuleGeneratorService
import com.github.nonoxys.kmpmodulegenerator.services.TemplateService
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.TextComponentAccessor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Dimension
import javax.swing.*

/**
 * Dialog for module generation wizard
 */
class ModuleGeneratorDialog(
    private val project: Project,
    private val initialPath: String? = null
) : DialogWrapper(project) {
    
    private val templateService = TemplateService.getInstance(project)
    private val generatorService = ModuleGeneratorService.getInstance(project)
    
    // UI Components
    private val templateComboBox: JComboBox<ModuleTemplate>
    private val targetPathField: TextFieldWithBrowseButton
    private val variableFieldsPanel: JPanel
    private val variableFields = mutableMapOf<String, JComponent>()
    private val descriptionLabel: JBLabel
    private val previewButton: JButton
    
    private var selectedTemplate: ModuleTemplate? = null
    private val cardLayout = CardLayout()
    private val variableCardsPanel = JPanel(cardLayout)
    
    init {
        title = "Generate Module"
        
        // Initialize template combo box
        val templates = templateService.getAllTemplates()
        templateComboBox = JComboBox(templates.toTypedArray()).apply {
            renderer = object : DefaultListCellRenderer() {
                override fun getListCellRendererComponent(
                    list: JList<*>?,
                    value: Any?,
                    index: Int,
                    isSelected: Boolean,
                    cellHasFocus: Boolean
                ): java.awt.Component {
                    val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                    if (value is ModuleTemplate) {
                        text = value.name
                    }
                    return component
                }
            }
            addActionListener {
                onTemplateSelected(selectedItem as? ModuleTemplate)
            }
        }
        
        // Initialize target path field
        targetPathField = TextFieldWithBrowseButton().apply {
            text = initialPath ?: project.basePath ?: ""
            addBrowseFolderListener(
                project,
                FileChooserDescriptorFactory.createSingleFolderDescriptor(),
            )
        }
        
        // Initialize description label
        descriptionLabel = JBLabel().apply {
            preferredSize = Dimension(500, 40)
        }
        
        // Initialize variables panel
        variableFieldsPanel = JPanel()
        variableFieldsPanel.layout = BoxLayout(variableFieldsPanel, BoxLayout.Y_AXIS)
        
        // Preview button
        previewButton = JButton("Preview Structure").apply {
            addActionListener { showPreview() }
        }
        
        // Select first template by default
        if (templates.isNotEmpty()) {
            templateComboBox.selectedIndex = 0
            onTemplateSelected(templates.first())
        }
        
        init()
    }
    
    override fun createCenterPanel(): JComponent {
        val mainPanel = JPanel(BorderLayout(10, 10))
        
        // Top panel with template selection
        val topPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent("Template:", templateComboBox)
            .addComponent(descriptionLabel)
            .addSeparator()
            .addLabeledComponent("Target Directory:", targetPathField)
            .panel
        
        // Variables panel with scroll
        val scrollPane = JBScrollPane(variableFieldsPanel).apply {
            preferredSize = Dimension(500, 300)
            border = BorderFactory.createTitledBorder("Module Configuration")
        }
        
        // Bottom panel with preview button
        val bottomPanel = JPanel().apply {
            add(previewButton)
        }
        
        mainPanel.add(topPanel, BorderLayout.NORTH)
        mainPanel.add(scrollPane, BorderLayout.CENTER)
        mainPanel.add(bottomPanel, BorderLayout.SOUTH)
        
        return mainPanel
    }
    
    /**
     * Handle template selection
     */
    private fun onTemplateSelected(template: ModuleTemplate?) {
        selectedTemplate = template
        
        if (template != null) {
            descriptionLabel.text = "<html>${template.description}</html>"
            updateVariableFields(template)
        } else {
            descriptionLabel.text = ""
            variableFieldsPanel.removeAll()
        }
        
        variableFieldsPanel.revalidate()
        variableFieldsPanel.repaint()
    }
    
    /**
     * Update variable input fields based on template
     */
    private fun updateVariableFields(template: ModuleTemplate) {
        variableFieldsPanel.removeAll()
        variableFields.clear()
        
        val formBuilder = FormBuilder.createFormBuilder()
        
        template.variables.forEach { variable ->
            val field = createFieldForVariable(variable)
            variableFields[variable.name] = field
            
            val label = if (variable.required) {
                "${variable.displayName}*:"
            } else {
                "${variable.displayName}:"
            }
            
            formBuilder.addLabeledComponent(label, field)
            
            if (variable.description.isNotEmpty()) {
                formBuilder.addTooltip(variable.description)
            }
        }
        
        variableFieldsPanel.add(formBuilder.panel)
    }
    
    /**
     * Create appropriate input field for variable type
     */
    private fun createFieldForVariable(variable: TemplateVariable): JComponent {
        return when (variable.type) {
            VariableType.BOOLEAN -> {
                JCheckBox().apply {
                    isSelected = variable.defaultValue.toBoolean()
                }
            }
            VariableType.DROPDOWN -> {
                JComboBox(variable.options?.toTypedArray() ?: arrayOf()).apply {
                    if (variable.defaultValue.isNotEmpty()) {
                        selectedItem = variable.defaultValue
                    }
                }
            }
            VariableType.NUMBER -> {
                JBTextField(variable.defaultValue).apply {
                    columns = 10
                }
            }
            VariableType.MULTILINE_TEXT -> {
                JTextArea(variable.defaultValue, 3, 30).apply {
                    lineWrap = true
                    wrapStyleWord = true
                }
            }
            else -> { // TEXT, PACKAGE
                JBTextField(variable.defaultValue).apply {
                    columns = 30
                }
            }
        }
    }
    
    /**
     * Get values from variable fields
     */
    private fun getVariableValues(): Map<String, String> {
        val values = mutableMapOf<String, String>()
        val template = selectedTemplate ?: return values
        
        template.variables.forEach { variable ->
            val field = variableFields[variable.name]
            val value = when (field) {
                is JBTextField -> field.text
                is JCheckBox -> field.isSelected.toString()
                is JComboBox<*> -> field.selectedItem?.toString() ?: ""
                is JTextArea -> field.text
                else -> ""
            }
            values[variable.name] = value
            
            // Add helper variables
            if (variable.name == "packageName") {
                values["packagePath"] = value.replace('.', '/')
            }
        }
        
        return values
    }
    
    /**
     * Show preview of module structure
     */
    private fun showPreview() {
        val template = selectedTemplate ?: return
        val variables = getVariableValues()
        val targetPath = targetPathField.text
        
        val configuration = ModuleConfiguration(template, variables, targetPath)
        val validationErrors = configuration.validate()
        
        if (validationErrors.isNotEmpty()) {
            Messages.showErrorDialog(
                project,
                "Please fix the following errors:\n${validationErrors.joinToString("\n")}",
                "Validation Error"
            )
            return
        }
        
        val preview = generatorService.generatePreview(configuration)
        PreviewDialog(project, preview).show()
    }
    
    /**
     * Validate all fields
     */
    override fun doValidate(): ValidationInfo? {
        val template = selectedTemplate
        if (template == null) {
            return ValidationInfo("Please select a template", templateComboBox)
        }
        
        val targetPath = targetPathField.text
        if (targetPath.isBlank()) {
            return ValidationInfo("Please select target directory", targetPathField)
        }
        
        val variables = getVariableValues()
        
        // Validate each variable
        template.variables.forEach { variable ->
            val value = variables[variable.name] ?: ""
            
            if (variable.required && value.isBlank()) {
                val field = variableFields[variable.name]
                return ValidationInfo("${variable.displayName} is required", field)
            }
            
            if (value.isNotBlank() && variable.validator != null) {
                when (val result = variable.validator.invoke(value)) {
                    is ValidationResult.Invalid -> {
                        val field = variableFields[variable.name]
                        return ValidationInfo(result.message, field)
                    }
                    is ValidationResult.Valid -> { /* OK */ }
                }
            }
        }
        
        return null
    }
    
    /**
     * Execute module generation
     */
    override fun doOKAction() {
        val template = selectedTemplate ?: return
        val variables = getVariableValues()
        val targetPath = targetPathField.text
        
        val configuration = ModuleConfiguration(template, variables, targetPath)
        
        // Generate module
        val result = generatorService.generateModule(configuration)
        
        when (result) {
            is GenerationResult.Success -> {
                Messages.showInfoMessage(
                    project,
                    result.message,
                    "Success"
                )
                super.doOKAction()
            }
            is GenerationResult.Warning -> {
                val message = buildString {
                    appendLine("Module generated with warnings:")
                    result.warnings.forEach { warning ->
                        appendLine("• $warning")
                    }
                }
                Messages.showWarningDialog(project, message, "Warning")
                super.doOKAction()
            }
            is GenerationResult.Failure -> {
                Messages.showErrorDialog(
                    project,
                    "Failed to generate module: ${result.error}",
                    "Error"
                )
            }
        }
    }
}

