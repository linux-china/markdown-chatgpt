package com.github.linuxchina.markdownchatgpt.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ChatCompletionChoice {
    Integer index;
    ChatMessage message;
    @JsonProperty("finish_reason")
    String finishReason;

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public ChatMessage getMessage() {
        return message;
    }

    public void setMessage(ChatMessage message) {
        this.message = message;
    }

    public String getFinishReason() {
        return finishReason;
    }

    public void setFinishReason(String finishReason) {
        this.finishReason = finishReason;
    }
}
