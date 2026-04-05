package com.github.nonoxys.kmpmodulegenerator.platform

import com.github.nonoxys.kmpmodulegenerator.services.FtlTemplateService

class FtlTemplateServicePlatformTest : KmpPluginTestCase() {

    private lateinit var service: FtlTemplateService

    override fun setUp() {
        super.setUp()
        service = FtlTemplateService.getInstance(project)
    }

    // region processTemplate

    fun `test processTemplate substitutes single variable`() {
        val result = service.processTemplate("Hello \${name}!", mapOf("name" to "World"))
        assertEquals("Hello World!", result)
    }

    fun `test processTemplate substitutes multiple variables`() {
        val result = service.processTemplate(
            "package \${pkg}\n\nclass \${cls} {}",
            mapOf("pkg" to "com.example", "cls" to "MyClass")
        )
        assertEquals("package com.example\n\nclass MyClass {}", result)
    }

    fun `test processTemplate returns static content unchanged`() {
        val result = service.processTemplate("No variables here.", emptyMap())
        assertEquals("No variables here.", result)
    }

    // endregion

    // region loadTemplate

    fun `test loadTemplate parses minimal-template correctly`() {
        val template = service.loadTemplate(fixturesDir().resolve("minimal-template"))!!

        assertEquals("minimal-template", template.id)
        assertEquals("Minimal Template", template.name)
        assertEquals(1, template.variables.size)
        assertEquals("moduleName", template.variables[0].name)
        assertTrue(template.variables[0].required)
    }

    fun `test loadTemplate parses full-template with all variable types`() {
        val template = service.loadTemplate(fixturesDir().resolve("full-template"))!!

        assertEquals("full-template", template.id)
        assertEquals(3, template.variables.size)
        val dropdown = template.variables.first { it.name == "platform" }
        assertEquals(listOf("android", "kmp"), dropdown.options)
    }

    fun `test loadTemplate detects build gradle path in full-template`() {
        val template = service.loadTemplate(fixturesDir().resolve("full-template"))!!
        assertTrue(template.modulePaths.any { it.contains("build.gradle.kts") })
    }

    fun `test loadTemplate returns null when template xml is missing`() {
        val emptyDir = tempDir("kmp-no-config")
        try {
            assertNull(service.loadTemplate(emptyDir))
        } finally {
            emptyDir.deleteRecursively()
        }
    }

    // endregion

    // region getAvailableTemplates

    fun `test getAvailableTemplates finds all valid fixture templates`() {
        useFixturesAsTemplateFolder()
        val ids = service.getAvailableTemplates().map { it.id }

        assertTrue(ids.contains("minimal-template"))
        assertTrue(ids.contains("full-template"))
    }

    fun `test getAvailableTemplates skips directory without template xml`() {
        useFixturesAsTemplateFolder()
        val ids = service.getAvailableTemplates().map { it.id }

        assertFalse(ids.contains("no-config-dir"))
    }

    override fun tearDown() {
        resetTemplateFolder()
        super.tearDown()
    }

    // endregion
}
