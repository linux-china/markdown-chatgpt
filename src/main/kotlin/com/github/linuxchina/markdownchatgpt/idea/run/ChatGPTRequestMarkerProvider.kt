package com.github.linuxchina.markdownchatgpt.idea.run

import com.github.linuxchina.markdownchatgpt.idea.ChatGPTDocument
import com.github.linuxchina.markdownchatgpt.idea.chatGPTIcon
import com.github.linuxchina.markdownchatgpt.idea.chatGPTResponseMarker
import com.github.linuxchina.markdownchatgpt.idea.openAIIcon
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.execution.lineMarker.RunLineMarkerProvider
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import org.intellij.plugins.markdown.lang.psi.impl.MarkdownHeader
import javax.swing.Icon

class ChatGPTRequestMarkerProvider : RunLineMarkerProvider() {

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
                            updateChatGPTResponse(psiElement.project, psiElement)
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

    private fun updateChatGPTResponse(project: Project, psiElement: MarkdownHeader) {
        val chatGPTDocument = ChatGPTDocument(psiElement.parent)
        val request = chatGPTDocument.findRequest(psiElement)!!
        ApplicationManager.getApplication()
            .runWriteAction {
                val documentManager = PsiDocumentManager.getInstance(project)
                val document = documentManager.getDocument(psiElement.containingFile)!!
                var caretOffset = 0
                val chatGPTReply = "\n\nChatGPT response\n\n"
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
}