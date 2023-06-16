package com.github.linuxchina.markdownchatgpt.idea.json

import com.github.linuxchina.markdownchatgpt.idea.chatGPTFunctionsFenceLanguage
import com.intellij.json.psi.JsonFile
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.jetbrains.jsonSchema.extension.ContentAwareJsonSchemaFileProvider
import org.intellij.plugins.markdown.lang.psi.impl.MarkdownCodeFence


@Suppress("UnstableApiUsage")
class GPTFunctionsJsonSchemaProviderFactory : ContentAwareJsonSchemaFileProvider {

    override fun getSchemaFile(psiFile: PsiFile): VirtualFile? {
        if (psiFile is JsonFile) {
            val injectedLanguageManager = InjectedLanguageManager.getInstance(psiFile.project)
            val psiLanguageInjectionHost = injectedLanguageManager.getInjectionHost(psiFile)
            if (psiLanguageInjectionHost is MarkdownCodeFence) {
                val codeFence: MarkdownCodeFence = psiLanguageInjectionHost
                if(codeFence.fenceLanguage == chatGPTFunctionsFenceLanguage) {
                    return VfsUtil.findFileByURL(this.javaClass.getResource("/gpt-functions-schema.json")!!)
                }
            }
        }
        return null
    }
}