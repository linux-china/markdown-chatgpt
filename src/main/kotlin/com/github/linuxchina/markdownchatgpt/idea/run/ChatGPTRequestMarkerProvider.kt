package com.github.linuxchina.markdownchatgpt.idea.run

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.linuxchina.markdownchatgpt.idea.*
import com.github.linuxchina.markdownchatgpt.model.ChatCompletionRequest
import com.github.linuxchina.markdownchatgpt.model.ChatCompletionResponse
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.execution.lineMarker.RunLineMarkerProvider
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import okhttp3.*
import org.intellij.plugins.markdown.lang.psi.impl.MarkdownCodeFence
import org.intellij.plugins.markdown.lang.psi.impl.MarkdownHeader
import javax.swing.Icon

class ChatGPTRequestMarkerProvider : RunLineMarkerProvider() {
    val client = OkHttpClient.Builder().apply {
        this.connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        this.readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        this.writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
    }.build()
    val objectMapper = ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    override fun getName(): String {
        return "ChatGPT"
    }

    override fun getIcon(): Icon {
        return chatGPTIcon
    }

    override fun getLineMarkerInfo(psiElement: PsiElement): LineMarkerInfo<*>? {
        val fileName = psiElement.containingFile.name
        if (fileName.endsWith(".gpt")) {
            if (psiElement is MarkdownHeader) {
                if (psiElement.level == 1) {
                    return LineMarkerInfo(
                        psiElement,
                        psiElement.textRange,
                        sendRequestIcon,
                        { _: PsiElement? ->
                            "Talk with ChatGPT"
                        },
                        { e, elt ->
                            callChatGPT(psiElement.project, psiElement)
                        },
                        GutterIconRenderer.Alignment.CENTER,
                        { "Talk with ChatGPT" }
                    )
                } else if (psiElement.level == 5 && (psiElement.name?.startsWith("ChatGPT") == true)) {
                    return LineMarkerInfo(
                        psiElement,
                        psiElement.textRange,
                        chatGPTIcon,
                        { _: PsiElement? ->
                            "Response from ChatGPT"
                        },
                        null,
                        GutterIconRenderer.Alignment.CENTER,
                        { "Response from ChatGPT" }
                    )
                }
            } else if (psiElement is MarkdownCodeFence) {
                val fenceLanguage = psiElement.fenceLanguage
                if (fenceLanguage != null && fenceLanguage == chatGPTFunctionsFenceLanguage) {
                    return LineMarkerInfo(
                        psiElement,
                        psiElement.textRange,
                        chatGPTFunctionsIcon,
                        { _: PsiElement? ->
                            "ChatGPT functions"
                        },
                        null,
                        GutterIconRenderer.Alignment.CENTER,
                        { "ChatGPT functions" }
                    )
                }
            }
        }
        return null
    }

    private fun callChatGPT(project: Project, psiElement: MarkdownHeader) {
        val root = psiElement.parent
        val chatGPTDocument = ChatGPTDocument(root)
        val openAISettings = chatGPTDocument.getFrontMatter()
        val mdChatRequest = chatGPTDocument.findRequest(psiElement)!!
        displayTextInBar(project, "Sending request to OpenAI")
        val openAIToken = openAISettings.getOpenAIToken(project)
        if (openAIToken.isNullOrEmpty()) {
            popupErrorBalloon(
                project,
                "OpenAI token is empty. Please set it in the front matter, such as `openai_api_key: xxx`"
            )
            // fill/modify openai_api_key in front matter
            chatGPTDocument.insertOpenAIToken()
            return
        }
        val chatRequest = ChatCompletionRequest()
        chatRequest.model = openAISettings.model
        chatRequest.temperature = openAISettings.temperature
        chatRequest.n = openAISettings.n
        chatRequest.messages = mdChatRequest.convertBodyToMessages()
        if (!mdChatRequest.functions.isNullOrEmpty()) {
            chatRequest.functions = mdChatRequest.functions
        }
        val request = Request.Builder()
            .url(openAISettings.url)
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $openAIToken")
            .post(RequestBody.create(MediaType.get("application/json"), objectMapper.writeValueAsString(chatRequest)))
            .build()
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Sending request to OpenAI") {
            var reply: StringBuilder = StringBuilder()
            var startedAt = System.currentTimeMillis()
            var httpCall: Call? = null
            override fun run(indicator: ProgressIndicator) {
                try {
                    httpCall = client.newCall(request)
                    httpCall!!.execute().use { response ->
                        if (response.isSuccessful) {
                            val chatResponse =
                                objectMapper.readValue(response.body()!!.string(), ChatCompletionResponse::class.java)
                            val choices = chatResponse.choices
                            if (choices != null && choices.isNotEmpty()) {
                                for (choice in choices) {
                                    val message = choice.message
                                    if (message != null) {
                                        if (message.content != null) {
                                            reply.append(message.content + "\n")
                                        }
                                        if (message.functionCall != null) {
                                            val functionCall = message.functionCall!!
                                            val call = mutableMapOf<String, Any>()
                                            call["name"] = functionCall.name
                                            if (functionCall.arguments.startsWith("{")) {
                                                call["arguments"] =
                                                    objectMapper.readValue(functionCall.arguments, Map::class.java)
                                            } else if (functionCall.arguments.startsWith("[")) {
                                                call["arguments"] =
                                                    objectMapper.readValue(functionCall.arguments, List::class.java)
                                            } else {
                                                call["arguments"] = functionCall.arguments
                                            }
                                            val jsonText =
                                                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(call)
                                            reply.append("\n```json {.function_call}\n$jsonText\n```\n")
                                        }
                                    }
                                }
                            }

                        } else {
                            popupErrorBalloon(project, "Failed to talk to ChatGPT. Response code: ${response.code()} \n ${response.body()!!.string()}")
                        }
                    }
                } catch (e: Exception) {
                    popupErrorBalloon(project, "Failed to talk to ChatGPT. Stacktrace: ${e.message}")
                }
            }

            override fun onSuccess() {
                if (reply.isNotEmpty()) {
                    WriteCommandAction.writeCommandAction(project, psiElement.containingFile).run<Exception> {
                        updateChatGPTResponse(project, psiElement, mdChatRequest, reply.toString().trim())
                    }
                    val duration = System.currentTimeMillis() - startedAt
                    displayTextInBar(project, "ChatGPT query finished, execution time: $duration ms")
                }
            }

            override fun onCancel() {
                httpCall?.cancel()
            }
        })

    }

    private fun updateChatGPTResponse(
        project: Project,
        psiElement: MarkdownHeader,
        request: ChatGPTRequest,
        reply: String
    ) {
        val chatGPTReply = "\n\n${reply.trim()}\n\n"
        val documentManager = PsiDocumentManager.getInstance(project)
        val document = documentManager.getDocument(psiElement.containingFile)!!
        var caretOffset = 0
        if (request.responseContentOffset > 0) { // `#### ChatGPT` found
            val startOffset =
                psiElement.textRange.startOffset + psiElement.text.length + request.responseContentOffset
            val endOffset =
                psiElement.textRange.startOffset + psiElement.text.length + request.responseContentEndOffset
            document.replaceString(
                startOffset,
                endOffset,
                chatGPTReply
            )
            caretOffset = startOffset
        } else {
            val startOffset =
                psiElement.textRange.startOffset + psiElement.text.length + request.responseContentEndOffset
            document.insertString(
                startOffset,
                "${chatGPTResponseMarker}${chatGPTReply}"
            )
            caretOffset = startOffset + chatGPTResponseMarker.length + 1
        }
        val fileEditors = FileEditorManager.getInstance(project).getAllEditors(psiElement.containingFile.virtualFile)
        if (fileEditors.isNotEmpty()) {
            val lastEditor = fileEditors.last()
            if (lastEditor is TextEditor) {
                lastEditor.editor.caretModel.moveToOffset(caretOffset + 2)
                lastEditor.editor.scrollingModel.scrollToCaret(ScrollType.CENTER)
            }
        }
    }

    private fun displayTextInBar(project: Project, text: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("ChatGPT Info Group")
            .createNotification(text, NotificationType.INFORMATION)
            .notify(project)
    }

    private fun popupErrorBalloon(project: Project, text: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("ChatGPT Notification Group")
            .createNotification(text, NotificationType.ERROR)
            .notify(project)
    }
}