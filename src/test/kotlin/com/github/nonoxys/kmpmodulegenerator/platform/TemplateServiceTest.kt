package com.github.nonoxys.kmpmodulegenerator.platform

import com.github.nonoxys.kmpmodulegenerator.services.TemplateService

class TemplateServiceTest : KmpPluginTestCase() {

    private lateinit var service: TemplateService

    override fun setUp() {
        super.setUp()
        useFixturesAsTemplateFolder()
        service = TemplateService.getInstance(project)
        service.reloadTemplates()
    }

    override fun tearDown() {
        resetTemplateFolder()
        super.tearDown()
    }

    // region getAllTemplates

    fun `test getAllTemplates returns both fixture templates`() {
        val ids = service.getAllTemplates().map { it.id }
        assertTrue(ids.contains("minimal-template"))
        assertTrue(ids.contains("full-template"))
    }

    // endregion

    // region getTemplate

    fun `test getTemplate returns correct template by id`() {
        val template = service.getTemplate("minimal-template")
        assertNotNull(template)
        assertEquals("Minimal Template", template!!.name)
    }

    fun `test getTemplate returns null for unknown id`() {
        assertNull(service.getTemplate("does-not-exist"))
    }

    // endregion

    // region createConfiguration / validateConfiguration

    fun `test createConfiguration fills template variables and path`() {
        val template = service.getTemplate("minimal-template")!!
        val config = service.createConfiguration(template, mapOf("moduleName" to "auth"), "/tmp/target")

        assertEquals(template, config.template)
        assertEquals("auth", config.variables["moduleName"])
        assertEquals("/tmp/target", config.targetPath)
    }

    fun `test validateConfiguration passes when all required variables are provided`() {
        val template = service.getTemplate("minimal-template")!!
        val config = service.createConfiguration(template, mapOf("moduleName" to "auth"), "/tmp/target")
        assertTrue(service.validateConfiguration(config).isEmpty())
    }

    fun `test validateConfiguration fails when required variable is missing`() {
        val template = service.getTemplate("minimal-template")!!
        val config = service.createConfiguration(template, emptyMap(), "/tmp/target")
        assertEquals(1, service.validateConfiguration(config).size)
    }

    // endregion

    // region reloadTemplates

    fun `test reloadTemplates preserves template count`() {
        val before = service.getAllTemplates().size
        service.reloadTemplates()
        assertEquals(before, service.getAllTemplates().size)
    }

    // endregion
}
