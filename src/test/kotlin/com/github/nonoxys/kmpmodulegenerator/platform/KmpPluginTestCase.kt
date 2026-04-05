package com.github.nonoxys.kmpmodulegenerator.platform

import com.github.nonoxys.kmpmodulegenerator.settings.TemplateSettings
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File
import kotlin.io.path.createTempDirectory

abstract class KmpPluginTestCase : BasePlatformTestCase() {

    protected fun fixturesDir(): File {
        val url = javaClass.classLoader.getResource("fixtures/templates")
            ?: error("fixtures/templates not found in test resources")
        return File(url.toURI())
    }

    protected fun tempDir(prefix: String = "kmp-test"): File =
        createTempDirectory(prefix).toFile()

    protected fun useFixturesAsTemplateFolder() {
        TemplateSettings.setCustomTemplateFolder(project, fixturesDir())
    }

    protected fun resetTemplateFolder() {
        TemplateSettings.setCustomTemplateFolder(project, null)
    }
}
