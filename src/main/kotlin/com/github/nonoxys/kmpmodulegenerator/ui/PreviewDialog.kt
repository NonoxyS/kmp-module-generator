package com.github.nonoxys.kmpmodulegenerator.ui

import com.github.nonoxys.kmpmodulegenerator.models.GenerationPreview
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.treeStructure.Tree
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

/**
 * Dialog for previewing module structure before generation using Kotlin UI DSL
 */
class PreviewDialog(
    project: Project,
    private val preview: GenerationPreview
) : DialogWrapper(project) {
    
    init {
        title = "Preview Module Structure"
        setSize(700, 600)
        init()
    }
    
    override fun createCenterPanel(): JComponent = panel {
        group("Files and Directories") {
            row {
                cell(JBScrollPane(createPreviewTree()))
                    .resizableColumn()
                    .align(com.intellij.ui.dsl.builder.Align.FILL)
            }.resizableRow()
        }
        
        group("Gradle Changes") {
            row {
                val text = preview.gradleChanges.joinToString("\n")
                scrollCell(JTextArea(text).apply {
                    isEditable = false
                    lineWrap = true
                    wrapStyleWord = true
                    rows = 5
                })
                    .resizableColumn()
                    .align(com.intellij.ui.dsl.builder.AlignX.FILL)
            }
        }
    }
    
    /**
     * Create tree view for preview
     */
    private fun createPreviewTree(): Tree {
        val root = DefaultMutableTreeNode("Module Root")
        
        // Build tree structure
        val pathNodes = mutableMapOf<String, DefaultMutableTreeNode>()
        pathNodes[""] = root
        
        // Add directories first
        preview.directories.sortedBy { it.path }.forEach { dir ->
            val parts = dir.path.split("/")
            var currentPath = ""
            var parentNode = root
            
            parts.forEach { part ->
                if (part.isNotEmpty()) {
                    val fullPath = if (currentPath.isEmpty()) part else "$currentPath/$part"
                    
                    val node = pathNodes.getOrPut(fullPath) {
                        val newNode = DefaultMutableTreeNode("📁 $part")
                        parentNode.add(newNode)
                        newNode
                    }
                    
                    parentNode = node
                    currentPath = fullPath
                }
            }
        }
        
        // Add files
        preview.files.sortedBy { it.path }.forEach { file ->
            val parts = file.path.split("/")
            val fileName = parts.last()
            val dirPath = parts.dropLast(1).joinToString("/")
            
            val parentNode = pathNodes[dirPath] ?: root
            val icon = getFileIcon(fileName)
            val sizeText = formatFileSize(file.size)
            parentNode.add(DefaultMutableTreeNode("$icon $fileName ($sizeText)"))
        }
        
        val treeModel = DefaultTreeModel(root)
        return Tree(treeModel).apply {
            // Expand all nodes
            for (i in 0 until rowCount) {
                expandRow(i)
            }
        }
    }
    
    /**
     * Create text for gradle changes
     */
    private fun createGradleChangesText(): JTextArea {
        val text = preview.gradleChanges.joinToString("\n")
        return JTextArea(text).apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
        }
    }
    
    /**
     * Get icon for file based on extension
     */
    private fun getFileIcon(fileName: String): String {
        return when {
            fileName.endsWith(".kt") -> "📄"
            fileName.endsWith(".kts") -> "📝"
            fileName.endsWith(".xml") -> "📋"
            fileName.endsWith(".gradle") -> "🔧"
            fileName.endsWith(".properties") -> "⚙️"
            fileName.endsWith(".json") -> "📊"
            fileName.endsWith(".md") -> "📖"
            fileName == ".gitignore" -> "🚫"
            else -> "📄"
        }
    }
    
    /**
     * Format file size for display
     */
    private fun formatFileSize(bytes: Int): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }
    
    override fun createActions(): Array<Action> {
        return arrayOf(okAction)
    }
    
    override fun getOKAction(): Action {
        return super.getOKAction().apply {
            putValue(Action.NAME, "Close")
        }
    }
}

