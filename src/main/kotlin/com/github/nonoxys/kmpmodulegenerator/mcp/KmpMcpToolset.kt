package com.github.nonoxys.kmpmodulegenerator.mcp

import com.github.nonoxys.kmpmodulegenerator.models.GenerationResult
import com.github.nonoxys.kmpmodulegenerator.models.VariableType
import com.github.nonoxys.kmpmodulegenerator.services.ModuleGeneratorService
import com.github.nonoxys.kmpmodulegenerator.services.TemplateService
import com.intellij.mcpserver.McpExpectedError
import com.intellij.mcpserver.McpToolset
import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool
import com.intellij.mcpserver.project
import com.intellij.openapi.application.ApplicationManager
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class KmpMcpToolset : McpToolset {

    @McpTool(name = "kmp_list_templates")
    @McpDescription("List all available KMP module templates with their variables. Use this before calling kmp_generate_module to discover templateId values and required variables.")
    suspend fun listTemplates(): List<TemplateDto> {
        val project = currentCoroutineContext().project
        val templates = TemplateService.getInstance(project).getAllTemplates()

        if (templates.isEmpty()) {
            throw McpExpectedError("No templates found. Create templates in .idea/kmp-templates/ or configure a custom path in Settings > KMP Module Templates.")
        }

        return templates.map { template ->
            TemplateDto(
                id = template.id,
                name = template.name,
                description = template.description,
                variables = template.variables.map { v ->
                    TemplateVariableDto(
                        name = v.name,
                        displayName = v.displayName,
                        description = v.description,
                        type = v.type.name,
                        required = v.required,
                        defaultValue = v.defaultValue,
                        options = if (v.type == VariableType.DROPDOWN) v.options else null,
                    )
                }
            )
        }
    }

    @McpTool(name = "kmp_generate_module")
    @McpDescription("Generate a KMP/Android module from a template. Use kmp_list_templates first to get available templateId values and required variables. targetPath should be an absolute filesystem path.")
    suspend fun generateModule(
        @McpDescription("Template ID from kmp_list_templates") templateId: String,
        @McpDescription("Absolute filesystem path where the module will be created") targetPath: String,
        @McpDescription("JSON object with variable values, e.g. {\"key\": \"value\"}") variables: String = "{}",
    ): GenerationResultDto {
        val project = currentCoroutineContext().project
        val templateService = TemplateService.getInstance(project)
        val generatorService = ModuleGeneratorService.getInstance(project)

        val template = templateService.getTemplate(templateId)
            ?: throw McpExpectedError("Template '$templateId' not found. Use kmp_list_templates to see available templates.")

        val variablesMap = try {
            if (variables.isBlank() || variables == "{}") emptyMap()
            else Json.decodeFromString<Map<String, String>>(variables)
        } catch (e: Exception) {
            throw McpExpectedError("Invalid variables JSON: ${e.message}. Expected format: {\"key\": \"value\"}")
        }

        val configuration = templateService.createConfiguration(template, variablesMap, targetPath)

        val validationErrors = templateService.validateConfiguration(configuration)
        if (validationErrors.isNotEmpty()) {
            throw McpExpectedError("Validation failed:\n${validationErrors.joinToString("\n") { "- $it" }}")
        }

        var result: GenerationResult? = null
        ApplicationManager.getApplication().invokeAndWait {
            result = generatorService.generateModule(configuration)
        }

        return when (val r = result) {
            is GenerationResult.Success -> GenerationResultDto(
                moduleName = r.moduleName,
                location = r.moduleDirectory.path,
                files = r.generatedFiles.map { it.path },
            )
            is GenerationResult.Warning -> GenerationResultDto(
                moduleName = r.moduleName,
                location = r.moduleDirectory.path,
                files = r.generatedFiles.map { it.path },
                warnings = r.warnings,
            )
            is GenerationResult.Failure -> throw McpExpectedError(r.error)
            null -> throw McpExpectedError("Generation produced no result (unexpected internal error)")
        }
    }
}

@Serializable
data class TemplateDto(
    val id: String,
    val name: String,
    val description: String,
    val variables: List<TemplateVariableDto>,
)

@Serializable
data class TemplateVariableDto(
    val name: String,
    val displayName: String,
    val description: String,
    val type: String,
    val required: Boolean,
    val defaultValue: String,
    val options: List<String>? = null,
)

@Serializable
data class GenerationResultDto(
    val moduleName: String,
    val location: String,
    val files: List<String>,
    val warnings: List<String> = emptyList(),
)
