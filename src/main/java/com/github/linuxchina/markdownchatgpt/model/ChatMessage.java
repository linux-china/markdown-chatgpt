package com.github.linuxchina.markdownchatgpt.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessage {
    private String role;
    private String content;
    /**
     * the name of the author of this message
     */
    private String name;

    @JsonProperty("function_call")
    private FunctionCall functionCall;

    public ChatMessage() {
    }

    public ChatMessage(@NotNull String role, @Nullable String content) {
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FunctionCall getFunctionCall() {
        return functionCall;
    }

    public void setFunctionCall(FunctionCall functionCall) {
        this.functionCall = functionCall;
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
