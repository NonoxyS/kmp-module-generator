package com.github.nonoxys.kmpmodulegenerator.mcp.models

import kotlinx.serialization.Serializable

@Serializable
data class TemplateDTO(
    val id: String,
    val name: String,
    val description: String,
    val variables: List<TemplateVariableDTO>,
)
