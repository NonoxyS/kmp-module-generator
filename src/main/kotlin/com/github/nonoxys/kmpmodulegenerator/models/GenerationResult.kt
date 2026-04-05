package com.github.nonoxys.kmpmodulegenerator.models

import com.intellij.openapi.vfs.VirtualFile

/**
 * Result of module generation operation
 */
sealed class GenerationResult {
    data class Success(
        val moduleName: String,
        val moduleDirectory: VirtualFile,
        val generatedFiles: List<VirtualFile>,
        val message: String = "Module '$moduleName' generated successfully"
    ) : GenerationResult()

    data class Failure(
        val error: String,
        val exception: Throwable? = null
    ) : GenerationResult()

    data class Warning(
        val moduleName: String,
        val moduleDirectory: VirtualFile,
        val generatedFiles: List<VirtualFile>,
        val warnings: List<String>,
        val message: String = "Module '$moduleName' generated with warnings"
    ) : GenerationResult()
}
