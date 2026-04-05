package com.github.nonoxys.kmpmodulegenerator.models

/**
 * Preview of what will be generated before committing to disk
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
        val size: Int,
        val level: Int
    )
}
