package com.github.linuxchina.markdownchatgpt.idea.run

import com.github.linuxchina.markdownchatgpt.idea.chatGPTIcon
import com.github.linuxchina.markdownchatgpt.idea.openAIIcon
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.lineMarker.RunLineMarkerProvider
import com.intellij.ide.actions.runAnything.activity.RunAnythingCommandProvider
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.project.Project
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
                            runDxTaskByRunAnything(psiElement.project, psiElement)
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

    private fun runDxTaskByRunAnything(project: Project, psiElement: MarkdownHeader) {
        RunAnythingCommandProvider.runCommand(
            psiElement.containingFile.virtualFile.parent,
            "dx xxx",
            DefaultRunExecutor.getRunExecutorInstance(),
            SimpleDataContext.getProjectContext(project)
        )
    }
}