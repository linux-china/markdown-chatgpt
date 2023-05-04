package com.github.linuxchina.markdownchatgpt.model;

import org.jetbrains.annotations.NotNull;

public class ChatMessage {
    @NotNull String role;
    @NotNull String content;

    public ChatMessage(@NotNull String role, @NotNull String content) {
        this.role = role;
        this.content = content;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public static ChatMessage systemMessage(@NotNull String content) {
        return new ChatMessage("system", content);
    }

    public static ChatMessage userMessage(@NotNull String content) {
        return new ChatMessage("user", content);
    }

    public static ChatMessage assistantMessage(@NotNull String content) {
        return new ChatMessage("assistant", content);
    }
}
