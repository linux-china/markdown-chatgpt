package com.github.linuxchina.markdownchatgpt.idea

import com.github.linuxchina.markdownchatgpt.model.ChatMessage
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.childrenOfType
import io.github.cdimascio.dotenv.Dotenv
import org.intellij.plugins.markdown.lang.psi.impl.MarkdownHeader
import org.yaml.snakeyaml.Yaml
import java.util.*


class OpenAISettings {
    var model = "gpt-4o-mini"
    var temperature = 1.0
    var n = 1
    var url = "https://api.openai.com/v1/chat/completions"
    var openaiApiKey: String? = null

    fun getOpenAIToken(project: Project): String? {
        return if (openaiApiKey != null && openaiApiKey!!.length > 10) {
            openaiApiKey
        } else {
            var token: String? = null
            val projectDir = project.guessProjectDir()
            if (projectDir != null) {
                val dotEnvFile = projectDir.findChild(".env")
                if (dotEnvFile != null) {
                    val dotEnv =
                        Dotenv.configure().directory(projectDir.path).ignoreIfMalformed().ignoreIfMissing().load()
                    token = dotEnv["OPENAI_API_KEY"]
                }
                if (token == null) {
                    val envPropertiesFile = projectDir.findChild(".env.properties")
                    if (envPropertiesFile != null) {
                        val properties = Properties()
                        properties.load(envPropertiesFile.inputStream)
                        token = properties.getProperty("openai.api.key")
                    }
                }
            }
            if (token.isNullOrEmpty()) {
                token = System.getenv("OPENAI_API_KEY");
            }
            return token
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
    var functions: String? = null

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
        // GPT functions
        if (userMsgContent.contains("```${chatGPTFunctionsFenceLanguage}")) {
            val offset = userMsgContent.indexOf("```${chatGPTFunctionsFenceLanguage}")
            val endOffset = userMsgContent.indexOf("```", offset + 20)
            if (endOffset > 0) {
                functions = userMsgContent.substring(offset + 20, endOffset).trim()
                userMsgContent = userMsgContent.substring(0, offset) + userMsgContent.substring(endOffset + 3)
            }
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

    fun insertOpenAIToken() {
        val project = root.project
        val mdText = root.text
        var caretOffset = 0
        if (!mdText.contains("\nopenai_api_key:")) {
            WriteCommandAction.runWriteCommandAction(project) {
                val document = PsiDocumentManager.getInstance(project).getDocument(root.containingFile)
                if (document != null) {
                    if (mdText.startsWith("---")) {
                        val offset = mdText.indexOf("\n---", 3)
                        if (offset > 0) {
                            val content = "\nopenai_api_key: xxx"
                            document.insertString(offset, content)
                            caretOffset = offset + content.length
                        }
                    } else {
                        val content = "---\nopenai_api_key: xxx\n---\n\n"
                        document.insertString(0, content)
                        caretOffset = content.length - 6
                    }
                }
            }
        } else {
            caretOffset = mdText.indexOf("openai_api_key:") + 16
        }
        if (caretOffset > 0) {
            val fileEditors =
                FileEditorManager.getInstance(project).getAllEditors(root.containingFile.virtualFile)
            if (fileEditors.isNotEmpty()) {
                val lastEditor = fileEditors.last()
                if (lastEditor is TextEditor) {
                    lastEditor.editor.caretModel.moveToOffset(caretOffset)
                    lastEditor.editor.scrollingModel.scrollToCaret(ScrollType.CENTER)
                }
            }
        }
    }
}