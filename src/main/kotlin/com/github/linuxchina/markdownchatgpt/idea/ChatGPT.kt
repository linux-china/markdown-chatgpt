package com.github.linuxchina.markdownchatgpt.idea

import com.intellij.icons.AllIcons
import com.intellij.openapi.util.IconLoader

val sendRequestIcon = AllIcons.RunConfigurations.TestState.Run
val chatGPTIcon = IconLoader.getIcon("icons/chatgpt.svg", MarkdownGPTFileIconProvider::class.java)
val openAIIcon = IconLoader.getIcon("icons/openai-16.png", MarkdownGPTFileIconProvider::class.java)

const val chatGPTResponseMarker = "\n##### ChatGPT"