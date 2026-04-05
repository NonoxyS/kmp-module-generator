package com.github.nonoxys.kmpmodulegenerator.mcp.models

import kotlinx.serialization.Serializable

@Serializable
data class GenerationResultDTO(
    val moduleName: String,
    val location: String,
    val files: List<String>,
    val warnings: List<String> = emptyList(),
)
