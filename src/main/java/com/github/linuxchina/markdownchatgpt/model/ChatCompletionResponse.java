package com.github.linuxchina.markdownchatgpt.model;

import java.util.List;


public class ChatCompletionResponse {
    private String id;
    private ChatCompletionUsage usage;
    private List<ChatCompletionChoice> choices;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ChatCompletionUsage getUsage() {
        return usage;
    }

    public void setUsage(ChatCompletionUsage usage) {
        this.usage = usage;
    }

    public List<ChatCompletionChoice> getChoices() {
        return choices;
    }

    public void setChoices(List<ChatCompletionChoice> choices) {
        this.choices = choices;
    }

    public ChatMessage getReply() {
        if (this.choices == null || this.choices.isEmpty()) return null;
        return this.choices.get(0).getMessage();
    }
}
