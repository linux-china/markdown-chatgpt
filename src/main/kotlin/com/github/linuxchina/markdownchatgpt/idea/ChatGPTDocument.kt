package com.github.linuxchina.markdownchatgpt.idea

import com.intellij.psi.PsiElement
import com.intellij.psi.util.childrenOfType
import org.intellij.plugins.markdown.lang.psi.impl.MarkdownHeader

class ChatGPTRequest {
    var title: String? = null
    var mdText: String? = null
    var response: String? = null
    var source: PsiElement? = null
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
                mdText.substring(h1Offset, mdText.indexOf(nextH1Text, h1Offset)).trim()
            } else {
                mdText.substring(h1Offset).trim()
            }
            val responseOffset = requestMdText.indexOf("\n##### ChatGPT")
            if (responseOffset > 0) {
                val responseContentOffset = requestMdText.indexOf('\n', responseOffset + 3)
                if (responseContentOffset > 0) {
                    response = requestMdText.substring(responseContentOffset).trim()
                }
                requestMdText = requestMdText.substring(responseOffset).trim()
            }
            requests[markdownHeader] = ChatGPTRequest().apply {
                this.title = title
                this.mdText = requestMdText
                this.response = response
                this.source = markdownHeader
            }
        }
    }

    fun findRequest(h1: MarkdownHeader): ChatGPTRequest? {
        return requests[h1]
    }
}