package com.github.nonoxys.kmpmodulegenerator.platform

import com.github.nonoxys.kmpmodulegenerator.mcp.KmpMcpToolset
import com.github.nonoxys.kmpmodulegenerator.services.TemplateService
import com.github.nonoxys.kmpmodulegenerator.settings.TemplateSettings
import com.intellij.mcpserver.McpExpectedError
import com.intellij.openapi.components.ComponentManagerEx
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.io.path.createTempDirectory

class KmpMcpToolsetTest : KmpPluginTestCase() {

    private lateinit var toolset: KmpMcpToolset
    private lateinit var outputDir: File

    override fun setUp() {
        super.setUp()
        useFixturesAsTemplateFolder()
        TemplateService.getInstance(project).reloadTemplates()
        toolset = KmpMcpToolset()
        outputDir = tempDir("kmp-mcp-test")
    }

    override fun tearDown() {
        outputDir.deleteRecursively()
        resetTemplateFolder()
        super.tearDown()
    }

    private fun <T> mcp(block: suspend KmpMcpToolset.() -> T): T {
        val scope = (project as ComponentManagerEx).getCoroutineScope()
        return runBlocking(scope.coroutineContext) { toolset.block() }
    }

    private fun mcpError(block: suspend KmpMcpToolset.() -> Unit): McpExpectedError {
        try {
            mcp(block)
            fail("Expected McpExpectedError to be thrown")
        } catch (e: McpExpectedError) {
            return e
        }
        error("unreachable")
    }

    // region listTemplates

    fun `test listTemplates returns dto list with fixture templates`() {
        val result = mcp { listTemplates() }
        val ids = result.map { it.id }
        assertTrue(ids.contains("minimal-template"))
        assertTrue(ids.contains("full-template"))
    }

    fun `test listTemplates dto contains variables`() {
        val result = mcp { listTemplates() }
        val minimal = result.first { it.id == "minimal-template" }
        assertEquals(1, minimal.variables.size)
        assertEquals("moduleName", minimal.variables[0].name)
        assertTrue(minimal.variables[0].required)
    }

    fun `test listTemplates throws McpExpectedError when no templates`() {
        val emptyDir = tempDir("kmp-empty")
        try {
            TemplateSettings.setCustomTemplateFolder(project, emptyDir)
            TemplateService.getInstance(project).reloadTemplates()
            mcpError { listTemplates() }
        } finally {
            emptyDir.deleteRecursively()
            useFixturesAsTemplateFolder()
            TemplateService.getInstance(project).reloadTemplates()
        }
    }

    // endregion

    // region generateModule

    fun `test generateModule returns dto with created files on success`() {
        val targetDir = File(outputDir, "auth")
        val result = mcp { generateModule("minimal-template", targetDir.absolutePath, """{"moduleName":"Auth"}""") }

        assertTrue(result.moduleName.isNotBlank())
        assertTrue(result.files.isNotEmpty())
        assertTrue(result.warnings.isEmpty())
        assertTrue(File(targetDir, "Module.kt").exists())
    }

    fun `test generateModule throws McpExpectedError for unknown template id`() {
        val error = mcpError { generateModule("nonexistent", outputDir.absolutePath) }
        assertTrue(error.message!!.contains("nonexistent"))
    }

    fun `test generateModule throws McpExpectedError for invalid variables json`() {
        mcpError { generateModule("minimal-template", outputDir.absolutePath, "not-json") }
    }

    fun `test generateModule throws McpExpectedError when required variable is missing`() {
        mcpError { generateModule("minimal-template", outputDir.absolutePath, "{}") }
    }

    // endregion
}
