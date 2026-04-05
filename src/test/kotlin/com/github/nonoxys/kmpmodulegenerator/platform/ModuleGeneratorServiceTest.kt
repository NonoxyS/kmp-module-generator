package com.github.nonoxys.kmpmodulegenerator.platform

import com.github.nonoxys.kmpmodulegenerator.models.GenerationResult
import com.github.nonoxys.kmpmodulegenerator.services.ModuleGeneratorService
import com.github.nonoxys.kmpmodulegenerator.services.TemplateService
import com.intellij.openapi.application.ApplicationManager
import java.io.File

class ModuleGeneratorServiceTest : KmpPluginTestCase() {

    private lateinit var templateService: TemplateService
    private lateinit var generatorService: ModuleGeneratorService
    private lateinit var outputDir: File

    override fun setUp() {
        super.setUp()
        useFixturesAsTemplateFolder()
        templateService = TemplateService.getInstance(project)
        templateService.reloadTemplates()
        generatorService = ModuleGeneratorService.getInstance(project)
        outputDir = tempDir("kmp-gen-test")
    }

    override fun tearDown() {
        outputDir.deleteRecursively()
        resetTemplateFolder()
        super.tearDown()
    }

    private fun generate(moduleName: String, targetDir: File = File(outputDir, moduleName)): GenerationResult {
        val template = templateService.getTemplate("minimal-template")!!
        val config = templateService.createConfiguration(
            template, mapOf("moduleName" to moduleName), targetDir.absolutePath
        )
        var result: GenerationResult? = null
        ApplicationManager.getApplication().invokeAndWait { result = generatorService.generateModule(config) }
        return result!!
    }

    // region generateModule

    fun `test generateModule returns success for valid input`() {
        val result = generate("Auth")
        assertTrue(result is GenerationResult.Success || result is GenerationResult.Warning)
    }

    fun `test generateModule creates files on disk`() {
        val targetDir = File(outputDir, "login")
        generate("Login", targetDir)
        assertTrue(File(targetDir, "Module.kt").exists())
    }

    fun `test generateModule file content contains module name`() {
        val targetDir = File(outputDir, "profile")
        generate("Profile", targetDir)
        assertTrue(File(targetDir, "Module.kt").readText().contains("Profile"))
    }

    fun `test generateModule returns failure when required variable is missing`() {
        val template = templateService.getTemplate("minimal-template")!!
        val config = templateService.createConfiguration(template, emptyMap(), outputDir.absolutePath)
        var result: GenerationResult? = null
        ApplicationManager.getApplication().invokeAndWait { result = generatorService.generateModule(config) }
        assertTrue(result is GenerationResult.Failure)
    }

    // endregion

    // region generatePreview

    fun `test generatePreview lists expected output files`() {
        val template = templateService.getTemplate("minimal-template")!!
        val config = templateService.createConfiguration(
            template, mapOf("moduleName" to "Auth"), outputDir.absolutePath
        )
        val preview = generatorService.generatePreview(config)

        assertFalse(preview.files.isEmpty())
        assertTrue(preview.files.any { it.path.contains("Module.kt") })
    }

    // endregion
}
