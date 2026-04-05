package com.github.nonoxys.kmpmodulegenerator.unit

import com.github.nonoxys.kmpmodulegenerator.models.FileStructure
import com.github.nonoxys.kmpmodulegenerator.models.ModuleConfiguration
import com.github.nonoxys.kmpmodulegenerator.models.ModuleTemplate
import com.github.nonoxys.kmpmodulegenerator.models.TemplateVariable
import com.github.nonoxys.kmpmodulegenerator.models.ValidationResult
import com.github.nonoxys.kmpmodulegenerator.models.VariableType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModuleConfigurationValidationTest {

    // Helpers

    private fun textVar(
        name: String,
        displayName: String = name,
        required: Boolean = true,
        validator: ((String) -> ValidationResult)? = null,
    ) = TemplateVariable(name, displayName, "", VariableType.TEXT, required = required, validator = validator)

    private fun validate(
        vararg variables: TemplateVariable,
        values: Map<String, String> = emptyMap(),
    ): List<String> {
        val template = ModuleTemplate(
            id = "test", name = "Test", description = "",
            variables = variables.toList(),
            fileStructure = FileStructure(emptyList(), emptyList()),
            buildGradleTemplate = ""
        )
        return ModuleConfiguration(template, values, "/tmp/target").validate()
    }

    // Tests

    @Test
    fun `required variable missing produces error with display name`() {
        val errors = validate(textVar("moduleName", "Module Name"))
        assertEquals(1, errors.size)
        assertTrue(errors[0].contains("Module Name"))
    }

    @Test
    fun `optional variable missing produces no error`() {
        val errors = validate(textVar("description", required = false))
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `all required variables provided produces no errors`() {
        val errors = validate(
            textVar("moduleName"),
            textVar("packageName"),
            values = mapOf("moduleName" to "auth", "packageName" to "com.example")
        )
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `each missing required variable produces its own error`() {
        val errors = validate(textVar("a"), textVar("b"))
        assertEquals(2, errors.size)
    }

    @Test
    fun `custom validator error is included in results`() {
        val errors = validate(
            textVar("moduleName", validator = { v ->
                if (v.contains(" ")) ValidationResult.Invalid("No spaces allowed") else ValidationResult.Valid
            }),
            values = mapOf("moduleName" to "my module")
        )
        assertTrue(errors.contains("No spaces allowed"))
    }

    @Test
    fun `defaultValue is not used by validate — missing required still fails`() {
        val varWithDefault = TemplateVariable(
            "moduleName", "Module Name", "", VariableType.TEXT,
            defaultValue = "defaultModule", required = true
        )
        val errors = validate(varWithDefault)
        assertEquals(1, errors.size)
    }
}
