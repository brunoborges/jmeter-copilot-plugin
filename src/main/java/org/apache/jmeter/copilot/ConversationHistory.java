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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Manages the conversation history for the Copilot chat session.
 */
public class ConversationHistory {

    private final List<ChatMessage> messages;
    private final int maxMessages;

    public ConversationHistory() {
        this(100); // Default max messages
    }

    public ConversationHistory(int maxMessages) {
        this.messages = new ArrayList<>();
        this.maxMessages = maxMessages;
    }

    /**
     * Adds a message to the conversation history.
     * If the history exceeds maxMessages, the oldest messages are removed.
     */
    public void addMessage(ChatMessage message) {
        messages.add(message);
        trimIfNeeded();
    }

    /**
     * Returns an unmodifiable view of all messages.
     */
    public List<ChatMessage> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    /**
     * Returns the number of messages in the history.
     */
    public int size() {
        return messages.size();
    }

    /**
     * Clears all messages from the history.
     */
    public void clear() {
        messages.clear();
    }

    /**
     * Returns the last message in the history, if any.
     */
    public Optional<ChatMessage> getLastMessage() {
        if (messages.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(messages.get(messages.size() - 1));
    }

    /**
     * Returns the last assistant message in the history, if any.
     */
    public Optional<ChatMessage> getLastAssistantMessage() {
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage msg = messages.get(i);
            if (msg.isFromAssistant()) {
                return Optional.of(msg);
            }
        }
        return Optional.empty();
    }

    /**
     * Returns messages filtered by role.
     */
    public List<ChatMessage> getMessagesByRole(ChatMessage.Role role) {
        return messages.stream()
            .filter(m -> m.getRole() == role)
            .toList();
    }

    private void trimIfNeeded() {
        while (messages.size() > maxMessages) {
            messages.remove(0);
        }
    }
}
