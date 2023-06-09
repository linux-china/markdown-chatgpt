package com.github.linuxchina.markdownchatgpt.idea

import com.intellij.icons.AllIcons
import com.intellij.openapi.util.IconLoader

val sendRequestIcon = AllIcons.Nodes.RunnableMark
val chatGPTIcon = IconLoader.getIcon("icons/chatgpt.svg", MarkdownGPTFileIconProvider::class.java)
val openAIIcon = IconLoader.getIcon("icons/openai-16.png", MarkdownGPTFileIconProvider::class.java)
val chatGPTFunctionsIcon = AllIcons.Nodes.Function

const val chatGPTResponseMarker = "\n##### ChatGPT"
const val chatGPTFunctionsFenceLanguage = "json {.functions}"