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

/**
 * Preview of what will be generated
 */
data class GenerationPreview(
    val directories: List<PreviewDirectory>,
    val files: List<PreviewFile>,
    val gradleChanges: List<String>
) {
    data class PreviewDirectory(
        val path: String,
        val level: Int
    )
    
    data class PreviewFile(
        val path: String,
        val size: Int, // Estimated size in bytes
        val level: Int
    )
}

