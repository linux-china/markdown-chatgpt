package com.github.linuxchina.markdownchatgpt.idea

import com.github.linuxchina.markdownchatgpt.model.ChatMessage
import com.intellij.psi.PsiElement
import com.intellij.psi.util.childrenOfType
import org.intellij.plugins.markdown.lang.psi.impl.MarkdownHeader
import org.yaml.snakeyaml.Yaml

class OpenAISettings {
    var model = "gpt-3.5-turbo"
    var temperature = 1.0
    var n = 1
    var url = "https://api.openai.com/v1/chat/completions"
    var openaiApiKey: String? = null

    fun getOpenAIToken(): String? {
        return if (openaiApiKey != null && openaiApiKey!!.length > 10) {
            openaiApiKey
        } else {
            System.getenv("OPENAI_API_KEY")
        }
    }
}

class ChatGPTRequest {
    var title: String? = null
    var mdText: String? = null
    var response: String? = null
    var source: PsiElement? = null
    var responseContentOffset: Int = 0
    var responseContentEndOffset: Int = 0

    fun convertBodyToMessages(): List<ChatMessage> {
        val messages = mutableListOf<ChatMessage>()
        var userMsgContent = mdText!!
        //system message
        val systemMsgPattern = Regex("(\\S.+\\n)*.+\\{\\.system}")
        systemMsgPattern.find(userMsgContent)?.let {
            it.groups[0]?.let { group ->
                val matchedText = group.value
                userMsgContent = userMsgContent.replace(matchedText, "").trim()
                val systemMsgContent = matchedText.replace("{.system}", "").trim()
                messages.add(ChatMessage.systemMessage(systemMsgContent))
            }
        }
        //assistant messages
        val assistantMessages = mutableListOf<ChatMessage>()
        val assistantMsgPattern = Regex("(\\S.+\\n)*.+\\{\\.assistant}")
        assistantMsgPattern.findAll(userMsgContent).forEach {
            val matchedText = it.value
            userMsgContent = userMsgContent.replace(matchedText, "").trim()
            val assistantMsgContent = matchedText.replace("{.assistant}", "").trim()
            assistantMessages.add(ChatMessage.assistantMessage(assistantMsgContent))
        }
        // user message
        messages.add(ChatMessage.userMessage(userMsgContent))
        // append assistant messages
        if (assistantMessages.isNotEmpty()) {
            messages.addAll(assistantMessages)
        }
        return messages
    }
}

class ChatGPTDocument(val root: PsiElement) {
    val requests: MutableMap<MarkdownHeader, ChatGPTRequest> = mutableMapOf()
    var frontMatter: String? = null

    init {
        val mdText = root.text
        if (mdText.startsWith("---")) {
            val frontMatterEndOffset = mdText.indexOf("\n---", 3)
            if (frontMatterEndOffset > 0) {
                frontMatter = mdText.substring(4, frontMatterEndOffset).trim()
            }
        }
        val childrenOfH1 = root.childrenOfType<MarkdownHeader>().filter { it.level == 1 }
        var h1Offset = 0
        var response = ""
        childrenOfH1.forEachIndexed { index, markdownHeader ->
            val title = markdownHeader.name
            val headerText = markdownHeader.text
            h1Offset = mdText.indexOf(headerText, h1Offset) + headerText.length
            var requestMdText = if (index < childrenOfH1.size - 1) {
                val nextH1Text = childrenOfH1[index + 1].text
                mdText.substring(h1Offset, mdText.indexOf(nextH1Text, h1Offset))
            } else {
                mdText.substring(h1Offset)
            }
            val responseOffset = requestMdText.indexOf(chatGPTResponseMarker)
            var responseContentOffset = 0
            val responseContentEndOffset = requestMdText.length
            if (responseOffset > 0) {
                responseContentOffset = requestMdText.indexOf('\n', responseOffset + 3)
                if (responseContentOffset > 0) {
                    response = requestMdText.substring(responseContentOffset)
                }
                requestMdText = requestMdText.substring(0, responseOffset)
            }
            requests[markdownHeader] = ChatGPTRequest().apply {
                this.title = title
                this.mdText = requestMdText.trim()
                this.response = response
                this.source = markdownHeader
                this.responseContentOffset = responseContentOffset
                this.responseContentEndOffset = responseContentEndOffset
            }
        }
    }

    fun findRequest(h1: MarkdownHeader): ChatGPTRequest? {
        return requests[h1]
    }

    fun getFrontMatter(): OpenAISettings {
        if (!frontMatter.isNullOrEmpty()) {
            try {
                Yaml().loadAs(frontMatter, Map::class.java)?.let {
                    val settings = OpenAISettings()
                    settings.model = it["model"] as? String ?: "gpt-3.5-turbo"
                    settings.temperature = it["temperature"] as? Double ?: 1.0
                    settings.n = it["n"] as? Int ?: 1
                    if (it.contains("openai_api_key")) {
                        settings.openaiApiKey = it["openai_api_key"] as? String
                    } else if (it.contains("openai-api-key")) {
                        settings.openaiApiKey = it["openai-api-key"] as? String
                    } else if (it.contains("OPENAI_API_KEY")) {
                        settings.openaiApiKey = it["OPENAI_API_KEY"] as? String
                    } else if (it.contains("OPENAI-API-KEY")) {
                        settings.openaiApiKey = it["OPENAI-API-KEY"] as? String
                    }
                    return settings
                }
            } catch (ignore: Exception) {

            }
        }
        return OpenAISettings()
    }
}