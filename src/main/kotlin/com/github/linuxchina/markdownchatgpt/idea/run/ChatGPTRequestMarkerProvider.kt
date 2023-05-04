package com.github.linuxchina.markdownchatgpt.idea.run

import com.github.linuxchina.markdownchatgpt.idea.ChatGPTDocument
import com.github.linuxchina.markdownchatgpt.idea.chatGPTIcon
import com.github.linuxchina.markdownchatgpt.idea.chatGPTResponseMarker
import com.github.linuxchina.markdownchatgpt.idea.openAIIcon
import com.github.linuxchina.markdownchatgpt.model.ChatCompletionRequest
import com.github.linuxchina.markdownchatgpt.model.ChatCompletionResponse
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.execution.lineMarker.RunLineMarkerProvider
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.intellij.plugins.markdown.lang.psi.impl.MarkdownHeader
import javax.swing.Icon

class ChatGPTRequestMarkerProvider : RunLineMarkerProvider() {
    val client = OkHttpClient()
    val gson = com.google.gson.Gson()

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
                        chatGPTIcon,
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
                        openAIIcon,
                        { _: PsiElement? ->
                            "Response from ChatGPT"
                        },
                        null,
                        GutterIconRenderer.Alignment.CENTER,
                        { "Response from ChatGPT" }
                    )
                }
            }
        }
        return null
    }

    private fun callChatGPT(project: Project, psiElement: MarkdownHeader) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("ChatGPT Notification Group")
            .createNotification("Send requests to OpenAI", NotificationType.IDE_UPDATE)
            .notify(project)
        val openAIToken = System.getenv("OPENAI_API_KEY")
        val chatRequest = ChatCompletionRequest.of("What's java")
        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $openAIToken")
            .post(RequestBody.create(MediaType.get("application/json"), gson.toJson(chatRequest)))
            .build()
        ApplicationManager.getApplication().runWriteAction {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val chatResponse = gson.fromJson(response.body()!!.string(), ChatCompletionResponse::class.java)
                    val reply = chatResponse.choices[0]?.message?.content ?: ""
                    updateChatGPTResponse(project, psiElement, reply)
                }
            }
        }

    }

    private fun updateChatGPTResponse(project: Project, psiElement: MarkdownHeader, reply: String) {
        val chatGPTDocument = ChatGPTDocument(psiElement.parent)
        val request = chatGPTDocument.findRequest(psiElement)!!
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
            val endOffset =
                psiElement.textRange.startOffset + psiElement.text.length + request.responseContentEndOffset
            document.replaceString(
                startOffset,
                endOffset,
                "${chatGPTResponseMarker}${chatGPTReply}"
            )
            caretOffset = startOffset + chatGPTResponseMarker.length + 1
        }
        FileEditorManager.getInstance(project).selectedEditor?.let {
            if (it is TextEditor) {
                it.editor.caretModel.moveToOffset(caretOffset + 1)
            }
        }
    }
}