package com.github.nonoxys.kmpmodulegenerator.mcp

import com.github.nonoxys.kmpmodulegenerator.mcp.models.GenerationResultDTO
import com.github.nonoxys.kmpmodulegenerator.mcp.models.TemplateDTO
import com.github.nonoxys.kmpmodulegenerator.mcp.models.TemplateVariableDTO
import com.github.nonoxys.kmpmodulegenerator.models.GenerationResult
import com.github.nonoxys.kmpmodulegenerator.models.VariableType
import com.github.nonoxys.kmpmodulegenerator.services.ModuleGeneratorService
import com.github.nonoxys.kmpmodulegenerator.services.TemplateService
import com.intellij.mcpserver.McpToolset
import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool
import com.intellij.mcpserver.mcpFail
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.project.Project
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.serialization.json.Json
import kotlin.coroutines.CoroutineContext

@Suppress("UnstableApiUsage")
class KmpMcpToolset(
    // Non-null only in tests — bypasses MCP coroutine context resolution
    private val projectForTesting: Project? = null,
) : McpToolset {

    @McpTool(name = "kmp_list_templates")
    @McpDescription("List all available KMP module templates with their variables. Use this before calling kmp_generate_module to discover templateId values and required variables.")
    suspend fun listTemplates(): List<TemplateDTO> {
        val project = getProject()
        val templates = TemplateService.getInstance(project).getAllTemplates()

        if (templates.isEmpty()) {
            mcpFail("No templates found. Create templates in .idea/kmp-templates/ or configure a custom path in Settings > KMP Module Templates.")
        }

        return templates.map { template ->
            TemplateDTO(
                id = template.id,
                name = template.name,
                description = template.description,
                variables = template.variables.map { v ->
                    TemplateVariableDTO(
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
    ): GenerationResultDTO {
        val project = getProject()
        val templateService = TemplateService.getInstance(project)
        val generatorService = ModuleGeneratorService.getInstance(project)

        val template = templateService.getTemplate(templateId)
            ?: mcpFail("Template '$templateId' not found. Use kmp_list_templates to see available templates.")

        val variablesMap = try {
            if (variables.isBlank()) emptyMap()
            else Json.decodeFromString<Map<String, String>>(variables)
        } catch (e: Exception) {
            mcpFail("Invalid variables JSON: ${e.message}. Expected format: {\"key\": \"value\"}")
        }

        val configuration = templateService.createConfiguration(template, variablesMap, targetPath)

        val validationErrors = templateService.validateConfiguration(configuration)
        if (validationErrors.isNotEmpty()) {
            mcpFail("Validation failed:\n${validationErrors.joinToString("\n") { "- $it" }}")
        }

        val result = invokeAndWaitIfNeeded { generatorService.generateModule(configuration) }

        return when (result) {
            is GenerationResult.Success -> GenerationResultDTO(
                moduleName = result.moduleName,
                location = result.moduleDirectory.path,
                files = result.generatedFiles.map { it.path },
            )

            is GenerationResult.Warning -> GenerationResultDTO(
                moduleName = result.moduleName,
                location = result.moduleDirectory.path,
                files = result.generatedFiles.map { it.path },
                warnings = result.warnings,
            )

            is GenerationResult.Failure -> mcpFail(result.error)
        }
    }

    private suspend fun getProject(): Project =
        projectForTesting
            ?: resolveProject(currentCoroutineContext())
            ?: mcpFail("No project found")

    private fun resolveProject(context: CoroutineContext): Project? {
        return try {
            val helperClass = Class.forName("com.intellij.mcpserver.McpCallInfoKt")
            val helperMethod = helperClass.getMethod(
                "getProjectOrNull",
                CoroutineContext::class.java
            )
            helperMethod.invoke(null, context) as? Project
        } catch (_: ReflectiveOperationException) {
            null
        }
    }
}
