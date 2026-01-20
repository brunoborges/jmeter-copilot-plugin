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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for ChatMessage class.
 */
@DisplayName("ChatMessage Tests")
class ChatMessageTest {

    @Test
    @DisplayName("should create a user message with correct role and content")
    void shouldCreateUserMessage() {
        ChatMessage message = new ChatMessage(ChatMessage.Role.USER, "Hello, Copilot!");

        assertThat(message.getRole()).isEqualTo(ChatMessage.Role.USER);
        assertThat(message.getContent()).isEqualTo("Hello, Copilot!");
        assertThat(message.isFromUser()).isTrue();
        assertThat(message.isFromAssistant()).isFalse();
    }

    @Test
    @DisplayName("should create an assistant message with correct role and content")
    void shouldCreateAssistantMessage() {
        ChatMessage message = new ChatMessage(ChatMessage.Role.ASSISTANT, "Here is your test plan");

        assertThat(message.getRole()).isEqualTo(ChatMessage.Role.ASSISTANT);
        assertThat(message.getContent()).isEqualTo("Here is your test plan");
        assertThat(message.isFromUser()).isFalse();
        assertThat(message.isFromAssistant()).isTrue();
    }

    @Test
    @DisplayName("should set timestamp automatically when not provided")
    void shouldSetTimestampAutomatically() {
        long before = System.currentTimeMillis();
        ChatMessage message = new ChatMessage(ChatMessage.Role.USER, "Test");
        long after = System.currentTimeMillis();

        assertThat(message.getTimestamp()).isBetween(before, after);
    }

    @Test
    @DisplayName("should use provided timestamp when specified")
    void shouldUseProvidedTimestamp() {
        long customTimestamp = 1234567890L;
        ChatMessage message = new ChatMessage(ChatMessage.Role.USER, "Test", customTimestamp);

        assertThat(message.getTimestamp()).isEqualTo(customTimestamp);
    }

    @Test
    @DisplayName("should provide meaningful toString representation")
    void shouldProvideMeaningfulToString() {
        ChatMessage message = new ChatMessage(ChatMessage.Role.USER, "Hello!");

        assertThat(message.toString()).contains("USER");
        assertThat(message.toString()).contains("Hello!");
    }

    @Test
    @DisplayName("should create system message correctly")
    void shouldCreateSystemMessage() {
        ChatMessage message = new ChatMessage(ChatMessage.Role.SYSTEM, "System prompt");

        assertThat(message.getRole()).isEqualTo(ChatMessage.Role.SYSTEM);
        assertThat(message.isFromUser()).isFalse();
        assertThat(message.isFromAssistant()).isFalse();
    }
}
