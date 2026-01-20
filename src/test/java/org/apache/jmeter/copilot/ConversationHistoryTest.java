/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jmeter.copilot;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for ConversationHistory class.
 */
@DisplayName("ConversationHistory Tests")
class ConversationHistoryTest {

    private ConversationHistory history;

    @BeforeEach
    void setUp() {
        history = new ConversationHistory();
    }

    @Test
    @DisplayName("should start empty")
    void shouldStartEmpty() {
        assertThat(history.size()).isZero();
        assertThat(history.getMessages()).isEmpty();
        assertThat(history.getLastMessage()).isEmpty();
    }

    @Test
    @DisplayName("should add messages correctly")
    void shouldAddMessages() {
        ChatMessage msg = new ChatMessage(ChatMessage.Role.USER, "Hello");
        history.addMessage(msg);

        assertThat(history.size()).isEqualTo(1);
        assertThat(history.getMessages()).contains(msg);
    }

    @Test
    @DisplayName("should return last message")
    void shouldReturnLastMessage() {
        ChatMessage msg1 = new ChatMessage(ChatMessage.Role.USER, "First");
        ChatMessage msg2 = new ChatMessage(ChatMessage.Role.ASSISTANT, "Second");

        history.addMessage(msg1);
        history.addMessage(msg2);

        assertThat(history.getLastMessage())
            .isPresent()
            .hasValue(msg2);
    }

    @Test
    @DisplayName("should return last assistant message")
    void shouldReturnLastAssistantMessage() {
        history.addMessage(new ChatMessage(ChatMessage.Role.USER, "Q1"));
        history.addMessage(new ChatMessage(ChatMessage.Role.ASSISTANT, "A1"));
        history.addMessage(new ChatMessage(ChatMessage.Role.USER, "Q2"));

        assertThat(history.getLastAssistantMessage())
            .isPresent()
            .hasValueSatisfying(msg ->
                assertThat(msg.getContent()).isEqualTo("A1"));
    }

    @Test
    @DisplayName("should return empty when no assistant message exists")
    void shouldReturnEmptyWhenNoAssistantMessage() {
        history.addMessage(new ChatMessage(ChatMessage.Role.USER, "Q1"));

        assertThat(history.getLastAssistantMessage()).isEmpty();
    }

    @Test
    @DisplayName("should clear all messages")
    void shouldClearAllMessages() {
        history.addMessage(new ChatMessage(ChatMessage.Role.USER, "Hello"));
        history.addMessage(new ChatMessage(ChatMessage.Role.ASSISTANT, "Hi"));

        history.clear();

        assertThat(history.size()).isZero();
        assertThat(history.getMessages()).isEmpty();
    }

    @Test
    @DisplayName("should filter messages by role")
    void shouldFilterMessagesByRole() {
        history.addMessage(new ChatMessage(ChatMessage.Role.USER, "Q1"));
        history.addMessage(new ChatMessage(ChatMessage.Role.ASSISTANT, "A1"));
        history.addMessage(new ChatMessage(ChatMessage.Role.USER, "Q2"));
        history.addMessage(new ChatMessage(ChatMessage.Role.ASSISTANT, "A2"));

        var userMessages = history.getMessagesByRole(ChatMessage.Role.USER);
        var assistantMessages = history.getMessagesByRole(ChatMessage.Role.ASSISTANT);

        assertThat(userMessages).hasSize(2);
        assertThat(assistantMessages).hasSize(2);
    }

    @Test
    @DisplayName("should trim oldest messages when exceeding max")
    void shouldTrimOldestMessagesWhenExceedingMax() {
        ConversationHistory limitedHistory = new ConversationHistory(3);

        limitedHistory.addMessage(new ChatMessage(ChatMessage.Role.USER, "First"));
        limitedHistory.addMessage(new ChatMessage(ChatMessage.Role.ASSISTANT, "Second"));
        limitedHistory.addMessage(new ChatMessage(ChatMessage.Role.USER, "Third"));
        limitedHistory.addMessage(new ChatMessage(ChatMessage.Role.ASSISTANT, "Fourth"));

        assertThat(limitedHistory.size()).isEqualTo(3);
        assertThat(limitedHistory.getMessages().get(0).getContent()).isEqualTo("Second");
    }

    @Test
    @DisplayName("should return unmodifiable list")
    void shouldReturnUnmodifiableList() {
        history.addMessage(new ChatMessage(ChatMessage.Role.USER, "Test"));

        var messages = history.getMessages();

        org.junit.jupiter.api.Assertions.assertThrows(
            UnsupportedOperationException.class,
            () -> messages.add(new ChatMessage(ChatMessage.Role.USER, "New"))
        );
    }
}
