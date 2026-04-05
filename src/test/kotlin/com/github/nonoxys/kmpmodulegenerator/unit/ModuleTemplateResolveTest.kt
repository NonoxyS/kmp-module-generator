package com.github.nonoxys.kmpmodulegenerator.unit

import com.github.nonoxys.kmpmodulegenerator.models.Directory
import com.github.nonoxys.kmpmodulegenerator.models.FileTemplate
import org.junit.Assert.assertEquals
import org.junit.Test

class ModuleTemplateResolveTest {

    @Test
    fun `Directory resolves dollar-brace variable`() {
        val dir = Directory("src/\${moduleName}/kotlin")
        val result = dir.getResolvedPath(mapOf("moduleName" to "auth"))
        assertEquals("src/auth/kotlin", result)
    }

    @Test
    fun `Directory resolves double-brace variable`() {
        val dir = Directory("src/{{moduleName}}/kotlin")
        val result = dir.getResolvedPath(mapOf("moduleName" to "auth"))
        assertEquals("src/auth/kotlin", result)
    }

    @Test
    fun `Directory leaves unknown variable unchanged`() {
        val dir = Directory("src/\${unknown}/kotlin")
        val result = dir.getResolvedPath(mapOf("moduleName" to "auth"))
        assertEquals("src/\${unknown}/kotlin", result)
    }

    @Test
    fun `Directory with no variables returns path as-is`() {
        val dir = Directory("src/main/kotlin")
        val result = dir.getResolvedPath(emptyMap())
        assertEquals("src/main/kotlin", result)
    }

    @Test
    fun `FileTemplate path resolves dollar-brace variable`() {
        val file = FileTemplate("src/\${moduleName}/Module.kt", "content")
        val result = file.getResolvedPath(mapOf("moduleName" to "auth"))
        assertEquals("src/auth/Module.kt", result)
    }

    @Test
    fun `FileTemplate path resolves double-brace variable`() {
        val file = FileTemplate("src/{{moduleName}}/Module.kt", "content")
        val result = file.getResolvedPath(mapOf("moduleName" to "auth"))
        assertEquals("src/auth/Module.kt", result)
    }

    @Test
    fun `FileTemplate content resolves variable`() {
        val file = FileTemplate("Module.kt", "class \${moduleName} {}")
        val result = file.getResolvedContent(mapOf("moduleName" to "Auth"))
        assertEquals("class Auth {}", result)
    }

    @Test
    fun `FileTemplate content resolves multiple variables`() {
        val file = FileTemplate("Module.kt", "package \${packageName}\n\nclass \${moduleName} {}")
        val result = file.getResolvedContent(mapOf("packageName" to "com.example", "moduleName" to "Auth"))
        assertEquals("package com.example\n\nclass Auth {}", result)
    }

    @Test
    fun `FileTemplate content leaves unknown variable unchanged`() {
        val file = FileTemplate("Module.kt", "class \${unknown} {}")
        val result = file.getResolvedContent(mapOf("moduleName" to "Auth"))
        assertEquals("class \${unknown} {}", result)
    }
}
