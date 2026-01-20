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

import java.util.List;

import javax.swing.JEditorPane;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for ChatMessageRenderer class.
 */
@DisplayName("ChatMessageRenderer Tests")
class ChatMessageRendererTest {

    private ChatMessageRenderer renderer;

    @BeforeEach
    void setUp() {
        renderer = new ChatMessageRenderer();
    }

    @Test
    @DisplayName("should create message pane with correct content type")
    void shouldCreateMessagePaneWithCorrectContentType() {
        JEditorPane pane = renderer.createMessagePane();

        assertThat(pane).isNotNull();
        assertThat(pane.getContentType()).isEqualTo("text/html");
        assertThat(pane.isEditable()).isFalse();
    }

    @Test
    @DisplayName("should render user message with correct CSS class")
    void shouldRenderUserMessageWithCorrectCssClass() {
        ChatMessage message = new ChatMessage(ChatMessage.Role.USER, "Hello Copilot");
        String html = renderer.renderMessage(message);

        assertThat(html).contains("user-message");
        assertThat(html).contains("Hello Copilot");
    }

    @Test
    @DisplayName("should render assistant message with correct CSS class")
    void shouldRenderAssistantMessageWithCorrectCssClass() {
        ChatMessage message = new ChatMessage(ChatMessage.Role.ASSISTANT, "Hello! How can I help?");
        String html = renderer.renderMessage(message);

        assertThat(html).contains("assistant-message");
        assertThat(html).contains("Hello!");
    }

    @Test
    @DisplayName("should render system message with correct CSS class")
    void shouldRenderSystemMessageWithCorrectCssClass() {
        ChatMessage message = new ChatMessage(ChatMessage.Role.SYSTEM, "Connected to Copilot");
        String html = renderer.renderMessage(message);

        assertThat(html).contains("system-message");
        assertThat(html).contains("Connected to Copilot");
    }

    @Test
    @DisplayName("should collapse XML code blocks by default")
    void shouldCollapseXmlCodeBlocksByDefault() {
        String markdown = "Here is some code:\n```xml\n<test>value</test>\n```";
        ChatMessage message = new ChatMessage(ChatMessage.Role.ASSISTANT, markdown);
        String html = renderer.renderMessage(message);

        // By default, XML should be collapsed
        assertThat(html).contains("xml-collapsed");
        assertThat(html).contains("JMeter Test Plan Generated");
    }

    @Test
    @DisplayName("should show XML code blocks when expanded")
    void shouldShowXmlCodeBlocksWhenExpanded() {
        renderer.setShowXmlExpanded(true);
        String markdown = "Here is some code:\n```xml\n<test>value</test>\n```";
        ChatMessage message = new ChatMessage(ChatMessage.Role.ASSISTANT, markdown);
        String html = renderer.renderMessage(message);

        assertThat(html).contains("<pre>");
        assertThat(html).containsPattern("<code.*>");
    }

    @Test
    @DisplayName("should escape HTML in user messages")
    void shouldEscapeHtmlInUserMessages() {
        ChatMessage message = new ChatMessage(ChatMessage.Role.USER, "<script>alert('xss')</script>");
        String html = renderer.renderMessage(message);

        assertThat(html).doesNotContain("<script>");
        assertThat(html).contains("&lt;script&gt;");
    }

    @Test
    @DisplayName("should render multiple messages")
    void shouldRenderMultipleMessages() {
        List<ChatMessage> messages = List.of(
            new ChatMessage(ChatMessage.Role.USER, "Create a test plan"),
            new ChatMessage(ChatMessage.Role.ASSISTANT, "Here is a test plan...")
        );

        String html = renderer.renderMessages(messages);

        assertThat(html).contains("<html>");
        assertThat(html).contains("user-message");
        assertThat(html).contains("assistant-message");
        assertThat(html).contains("Create a test plan");
        assertThat(html).contains("Here is a test plan...");
    }

    @Test
    @DisplayName("should include timestamp in rendered message")
    void shouldIncludeTimestampInRenderedMessage() {
        ChatMessage message = new ChatMessage(ChatMessage.Role.USER, "Test");
        String html = renderer.renderMessage(message);

        assertThat(html).contains("timestamp");
        // Should contain time in HH:mm format
        assertThat(html).containsPattern("\\d{2}:\\d{2}");
    }

    @Test
    @DisplayName("should render streaming message with indicator")
    void shouldRenderStreamingMessageWithIndicator() {
        String partialContent = "Here is the test plan...";
        String html = renderer.renderStreamingMessage(partialContent);

        assertThat(html).contains("assistant-message");
        assertThat(html).contains("streaming-indicator");
        assertThat(html).contains("Here is the test plan...");
    }

    @Test
    @DisplayName("should handle empty message list")
    void shouldHandleEmptyMessageList() {
        String html = renderer.renderMessages(List.of());

        assertThat(html).contains("<html>");
        assertThat(html).contains("</html>");
    }

    @Test
    @DisplayName("should preserve newlines in user messages")
    void shouldPreserveNewlinesInUserMessages() {
        ChatMessage message = new ChatMessage(ChatMessage.Role.USER, "Line 1\nLine 2");
        String html = renderer.renderMessage(message);

        assertThat(html).contains("<br>");
    }
}
