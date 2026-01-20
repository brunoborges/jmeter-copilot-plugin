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

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JEditorPane;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

/**
 * Renders chat messages as styled HTML in a JEditorPane.
 */
public class ChatMessageRenderer {

    private static final String CSS_STYLE = """
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
            font-size: 13px;
            line-height: 1.5;
            margin: 0;
            padding: 10px;
            background-color: #f5f5f5;
        }
        .message {
            margin-bottom: 15px;
            padding: 12px 16px;
            border-radius: 12px;
            max-width: 85%;
            clear: both;
        }
        .user-message {
            background-color: #007AFF;
            color: white;
            float: right;
            border-bottom-right-radius: 4px;
        }
        .assistant-message {
            background-color: white;
            color: #333;
            float: left;
            border-bottom-left-radius: 4px;
            border: 1px solid #e0e0e0;
        }
        .system-message {
            background-color: #fff3cd;
            color: #856404;
            text-align: center;
            font-style: italic;
        }
        .timestamp {
            font-size: 10px;
            color: #999;
            margin-top: 4px;
        }
        .user-message .timestamp {
            color: rgba(255,255,255,0.7);
        }
        pre {
            background-color: #1e1e1e;
            color: #d4d4d4;
            padding: 12px;
            border-radius: 6px;
            overflow-x: auto;
            font-family: 'SF Mono', Consolas, 'Liberation Mono', Menlo, monospace;
            font-size: 12px;
            margin: 10px 0;
        }
        code {
            background-color: rgba(0,0,0,0.05);
            padding: 2px 6px;
            border-radius: 4px;
            font-family: 'SF Mono', Consolas, 'Liberation Mono', Menlo, monospace;
            font-size: 12px;
        }
        pre code {
            background-color: transparent;
            padding: 0;
        }
        .clearfix {
            clear: both;
        }
        .xml-collapsed {
            background-color: #e8f4e8;
            border: 1px solid #28a745;
            border-radius: 6px;
            padding: 10px 14px;
            margin: 10px 0;
            color: #155724;
        }
        .xml-collapsed-icon {
            font-size: 16px;
            margin-right: 8px;
        }
        .xml-collapsed-text {
            font-weight: 500;
        }
        .xml-collapsed-hint {
            font-size: 11px;
            color: #6c757d;
            margin-top: 4px;
        }
        """;

    // Pattern to match XML code blocks in markdown (```xml ... ```)
    private static final Pattern XML_CODE_BLOCK_PATTERN = Pattern.compile(
        "```xml\\s*\\n([\\s\\S]*?)```",
        Pattern.CASE_INSENSITIVE
    );

    private final Parser markdownParser;
    private final HtmlRenderer htmlRenderer;
    private final DateTimeFormatter timeFormatter;
    private final AtomicBoolean showXmlExpanded = new AtomicBoolean(false);

    public ChatMessageRenderer() {
        List<org.commonmark.Extension> extensions = List.of(TablesExtension.create());
        this.markdownParser = Parser.builder().extensions(extensions).build();
        this.htmlRenderer = HtmlRenderer.builder().extensions(extensions).build();
        this.timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
            .withZone(ZoneId.systemDefault());
    }

    /**
     * Sets whether XML code blocks should be shown expanded or collapsed.
     */
    public void setShowXmlExpanded(boolean expanded) {
        showXmlExpanded.set(expanded);
    }

    /**
     * Returns whether XML code blocks are shown expanded.
     */
    public boolean isShowXmlExpanded() {
        return showXmlExpanded.get();
    }

    /**
     * Toggles the XML expanded state and returns the new state.
     */
    public boolean toggleXmlExpanded() {
        boolean newState = !showXmlExpanded.get();
        showXmlExpanded.set(newState);
        return newState;
    }

    /**
     * Creates a configured JEditorPane for displaying chat messages.
     */
    public JEditorPane createMessagePane() {
        JEditorPane pane = new JEditorPane();
        pane.setEditable(false);
        pane.setContentType("text/html");

        HTMLEditorKit kit = new HTMLEditorKit();
        StyleSheet styleSheet = kit.getStyleSheet();
        styleSheet.addRule(CSS_STYLE);
        pane.setEditorKit(kit);

        return pane;
    }

    /**
     * Renders a list of messages as HTML.
     */
    public String renderMessages(java.util.List<ChatMessage> messages) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body>");

        for (ChatMessage message : messages) {
            html.append(renderMessage(message));
        }

        html.append("<div class=\"clearfix\"></div>");
        html.append("</body></html>");
        return html.toString();
    }

    /**
     * Renders a single message as HTML.
     */
    public String renderMessage(ChatMessage message) {
        String content = message.getContent();
        
        // Skip rendering messages with empty content
        if (content == null || content.isBlank()) {
            return "";
        }

        String cssClass = switch (message.getRole()) {
            case USER -> "message user-message";
            case ASSISTANT -> "message assistant-message";
            case SYSTEM -> "message system-message";
        };

        String renderedContent;

        if (message.isFromAssistant()) {
            // Collapse XML code blocks if not expanded
            String processedContent = content;
            if (!showXmlExpanded.get()) {
                processedContent = collapseXmlCodeBlocks(content);
            }

            // Parse markdown for assistant messages
            Node document = markdownParser.parse(processedContent);
            renderedContent = htmlRenderer.render(document);
        } else {
            // Escape HTML for user messages
            renderedContent = escapeHtml(content).replace("\n", "<br>");
        }

        String timestamp = timeFormatter.format(Instant.ofEpochMilli(message.getTimestamp()));

        return String.format("""
            <div class="%s">
                %s
                <div class="timestamp">%s</div>
            </div>
            <div class="clearfix"></div>
            """, cssClass, renderedContent, timestamp);
    }

    /**
     * Collapses XML code blocks, replacing them with a summary placeholder.
     */
    private static String collapseXmlCodeBlocks(String content) {
        Matcher matcher = XML_CODE_BLOCK_PATTERN.matcher(content);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String xmlContent = matcher.group(1);
            int lineCount = xmlContent.split("\n").length;
            String summary = extractXmlSummary(xmlContent);

            // Replace with collapsed placeholder (using special markers that won't be escaped)
            String placeholder = String.format(
                "\n\n<div class=\"xml-collapsed\">" +
                "<span class=\"xml-collapsed-icon\">📄</span>" +
                "<span class=\"xml-collapsed-text\">JMeter Test Plan Generated</span><br>" +
                "<span class=\"xml-collapsed-hint\">%s (%d lines) — Click \"Show XML\" button below to view</span>" +
                "</div>\n\n",
                summary, lineCount
            );
            matcher.appendReplacement(result, Matcher.quoteReplacement(placeholder));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Extracts a brief summary from XML content.
     */
    private static String extractXmlSummary(String xmlContent) {
        // Try to extract test plan name
        Pattern testPlanPattern = Pattern.compile("testname=\"([^\"]+)\"");
        Matcher testPlanMatcher = testPlanPattern.matcher(xmlContent);
        if (testPlanMatcher.find()) {
            return testPlanMatcher.group(1);
        }

        // Fallback: count major elements
        int threadGroups = countOccurrences(xmlContent, "ThreadGroup");
        int samplers = countOccurrences(xmlContent, "Sampler");

        if (threadGroups > 0 || samplers > 0) {
            return String.format("%d thread group(s), %d sampler(s)", threadGroups, samplers);
        }

        return "XML Test Plan";
    }

    private static int countOccurrences(String text, String pattern) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(pattern, index)) != -1) {
            count++;
            index += pattern.length();
        }
        return count;
    }

    /**
     * Renders streaming content that is still being received.
     */
    public String renderStreamingMessage(String partialContent) {
        Node document = markdownParser.parse(partialContent);
        String renderedContent = htmlRenderer.render(document);

        return String.format("""
            <html><body>
            <div class="message assistant-message">
                %s
                <span class="streaming-indicator">▌</span>
            </div>
            <div class="clearfix"></div>
            </body></html>
            """, renderedContent);
    }

    private static String escapeHtml(String text) {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
}
