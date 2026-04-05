package com.github.nonoxys.kmpmodulegenerator.mcp.models

import kotlinx.serialization.Serializable

@Serializable
data class TemplateVariableDTO(
    val name: String,
    val displayName: String,
    val description: String,
    val type: String,
    val required: Boolean,
    val defaultValue: String,
    val options: List<String>? = null,
)
