package com.github.nonoxys.kmpmodulegenerator.ui

import com.github.nonoxys.kmpmodulegenerator.models.VariableType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.dsl.builder.*
import java.awt.BorderLayout
import javax.swing.*

/**
 * Dialog for editing a single parameter using Kotlin UI DSL
 */
class ParameterEditorDialog(
    project: Project,
    existingParameter: ParameterData?
) : DialogWrapper(project) {

    private var name: String = existingParameter?.name ?: ""
    private var displayName: String = existingParameter?.displayName ?: ""
    private var description: String = existingParameter?.description ?: ""
    private var type: VariableType = existingParameter?.type ?: VariableType.TEXT
    private var required: Boolean = existingParameter?.required ?: true
    private var defaultValue: String = existingParameter?.defaultValue ?: ""
    private var options: String = existingParameter?.options ?: ""

    // UI models for dropdown options editing
    private val optionListModel = DefaultListModel<String>()
    private val optionList = JList(optionListModel)
    private val defaultDropdownModel = DefaultComboBoxModel<String>()
    private val defaultDropdown = JComboBox(defaultDropdownModel)

    init {
        title = if (existingParameter != null) "Edit Parameter" else "Add Parameter"

        // Pre-fill options list from comma-separated string
        if (options.isNotBlank()) {
            options.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .forEach { opt ->
                    optionListModel.addElement(opt)
                }
        }
        rebuildDefaultDropdownModel()
        updateOptionsListSize()

        init()
    }

    override fun createCenterPanel(): JComponent {
        return panel {
            lateinit var typeCombo: JComboBox<VariableType>
            lateinit var optionsRow: Row
            lateinit var defaultTextRow: Row
            lateinit var defaultDropdownRow: Row

            row("Name:") {
                textField()
                    .bindText(::name)
                    .columns(COLUMNS_MEDIUM)
                    .validationOnInput {
                        when {
                            it.text.isBlank() -> error("Parameter name is required")
                            !it.text.matches(Regex("^[a-zA-Z][a-zA-Z0-9_]*$")) ->
                                error("Must start with letter and contain only letters, numbers, and underscores")

                            else -> null
                        }
                    }
                    .comment("Internal name used in templates (e.g., 'moduleName')")
            }

            row("Display Name:") {
                textField()
                    .bindText(::displayName)
                    .columns(COLUMNS_MEDIUM)
                    .validationOnInput {
                        if (it.text.isBlank()) error("Display name is required") else null
                    }
                    .comment("Name shown in UI (e.g., 'Module Name')")
            }

            row("Description:") {
                textArea()
                    .bindText(::description)
                    .rows(3)
                    .comment("Help text for users")
            }

            row("Type:") {
                val cell = comboBox(VariableType.entries)
                    .bindItem(::type.toNullableProperty())
                typeCombo = cell.component
            }

            row {
                val cell = checkBox("Required")
                    .bindSelected(::required)

                // When required flag changes, sync backing field and update default dropdown model
                val checkBox = cell.component
                checkBox.addActionListener {
                    required = checkBox.isSelected
                    rebuildDefaultDropdownModel()
                }
            }

            // Default value for non-DROPDOWN types (simple text)
            defaultTextRow = row("Default Value:") {
                textField()
                    .bindText(::defaultValue)
                    .columns(COLUMNS_MEDIUM)
            }

            // Default value for DROPDOWN: select from options
            defaultDropdownRow = row("Default Value:") {
                cell(defaultDropdown)
            }

            // Explicit options editor for DROPDOWN
            optionsRow = row("Options:") {
                cell(createOptionsEditorPanel())
            }

            fun updateVisibility(currentType: VariableType) {
                val isDropdown = currentType == VariableType.DROPDOWN
                optionsRow.visible(isVisible = isDropdown)
                defaultDropdownRow.visible(isVisible = isDropdown)
                defaultTextRow.visible(isVisible = !isDropdown)
            }

            // Initial state
            updateVisibility(type)

            typeCombo.addActionListener {
                val selected = typeCombo.selectedItem as? VariableType ?: VariableType.TEXT
                type = selected
                updateVisibility(selected)
            }
        }
    }

    fun getParameter(): ParameterData {
        // For DROPDOWN, take default from combo; for others – from text field
        val finalDefault = if (type == VariableType.DROPDOWN) {
            defaultDropdown.selectedItem as? String ?: ""
        } else {
            defaultValue
        }

        // Serialize options as comma-separated string for template.xml
        val normalizedOptions = if (type == VariableType.DROPDOWN) {
            buildString {
                for (i in 0 until optionListModel.size()) {
                    val value = optionListModel.getElementAt(i)
                    if (isNotEmpty()) append(',')
                    append(value)
                }
            }
        } else {
            ""
        }

        return ParameterData(
            name = name,
            displayName = displayName,
            type = type,
            required = required,
            defaultValue = finalDefault,
            description = description,
            options = normalizedOptions
        )
    }

    /**
     * Create panel for managing dropdown options (list + Add/Edit/Delete buttons)
     */
    private fun createOptionsEditorPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        val listScroll = JScrollPane(optionList)
        panel.add(listScroll, BorderLayout.CENTER)

        val buttonsPanel = JPanel()

        val addButton = JButton("Add")
        val editButton = JButton("Edit")
        val removeButton = JButton("Delete")

        addButton.addActionListener {
            val value = Messages.showInputDialog(
                "Enter option value",
                "Add Option",
                null
            )?.trim()

            if (!value.isNullOrEmpty()) {
                if ((0 until optionListModel.size()).any { optionListModel.getElementAt(it) == value }) {
                    Messages.showErrorDialog(
                        "Option '$value' already exists",
                        "Duplicate Option"
                    )
                } else {
                    optionListModel.addElement(value)
                    rebuildDefaultDropdownModel()
                    updateOptionsListSize()
                }
            }
        }

        editButton.addActionListener {
            val index = optionList.selectedIndex
            if (index < 0) {
                Messages.showWarningDialog(
                    "Please select an option to edit",
                    "No Selection"
                )
                return@addActionListener
            }

            val current = optionListModel.getElementAt(index)
            val value = Messages.showInputDialog(
                "Edit option value",
                "Edit Option",
                null,
                current,
                null
            )?.trim()

            if (!value.isNullOrEmpty()) {
                // Allow keeping same value, but prevent duplicates with others
                val exists = (0 until optionListModel.size())
                    .filter { it != index }
                    .any { optionListModel.getElementAt(it) == value }
                if (exists) {
                    Messages.showErrorDialog(
                        "Option '$value' already exists",
                        "Duplicate Option"
                    )
                } else {
                    optionListModel.setElementAt(value, index)
                    rebuildDefaultDropdownModel()
                    updateOptionsListSize()
                }
            }
        }

        removeButton.addActionListener {
            val index = optionList.selectedIndex
            if (index < 0) {
                Messages.showWarningDialog(
                    "Please select an option to delete",
                    "No Selection"
                )
                return@addActionListener
            }

            optionListModel.remove(index)
            rebuildDefaultDropdownModel()
            updateOptionsListSize()
        }

        buttonsPanel.add(addButton)
        buttonsPanel.add(editButton)
        buttonsPanel.add(removeButton)

        panel.add(buttonsPanel, BorderLayout.SOUTH)

        return panel
    }

    /**
     * Rebuild default value dropdown from option list model.
     * Keeps current defaultValue selection when possible.
     */
    private fun rebuildDefaultDropdownModel() {
        val current = defaultDropdown.selectedItem as? String ?: defaultValue

        defaultDropdownModel.removeAllElements()
        // For non-required parameters we allow an explicit "no default" empty value
        if (!required) {
            defaultDropdownModel.addElement("")
        }

        for (i in 0 until optionListModel.size()) {
            defaultDropdownModel.addElement(optionListModel.getElementAt(i))
        }

        fun indexOf(value: String): Int {
            for (i in 0 until defaultDropdownModel.size) {
                if (defaultDropdownModel.getElementAt(i) == value) return i
            }
            return -1
        }

        val currentIndex = if (current.isNotEmpty()) indexOf(current) else -1
        when {
            currentIndex >= 0 -> {
                defaultDropdown.selectedIndex = currentIndex
            }

            required && defaultDropdownModel.size > 0 -> {
                // Required parameter cannot have empty default; pick first available option
                defaultDropdown.selectedIndex = 0
            }

            !required -> {
                // Non-required: select empty value if present
                val emptyIndex = indexOf("")
                defaultDropdown.selectedIndex = emptyIndex
            }

            else -> {
                // No options and required: leave unselected
                defaultDropdown.selectedIndex = -1
            }
        }
    }

    /**
     * Adjust visible rows of options list so that:
     * - with 0 options it's as small as possible (1 row height),
     * - grows up to 5 rows as options are added,
     * - beyond 5 uses scroll.
     */
    private fun updateOptionsListSize() {
        val size = optionListModel.size()
        val rows = when {
            size <= 0 -> 1
            size <= 5 -> size
            else -> 5
        }
        optionList.visibleRowCount = rows
        optionList.revalidate()
        optionList.repaint()
    }
}

