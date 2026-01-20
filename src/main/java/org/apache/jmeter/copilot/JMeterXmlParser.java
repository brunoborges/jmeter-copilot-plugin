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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jmeter.save.SaveService;
import org.apache.jorphan.collections.HashTree;

/**
 * Parses JMeter XML test plan content and converts it to a HashTree.
 * This class handles extracting XML from Copilot responses which may include
 * markdown code blocks and other text.
 */
public class JMeterXmlParser {

    private static final Pattern XML_CODE_BLOCK_PATTERN =
        Pattern.compile("```(?:xml)?\\s*([\\s\\S]*?)```", Pattern.MULTILINE);

    private static final Pattern JMETER_TEST_PLAN_PATTERN =
        Pattern.compile("<jmeterTestPlan[^>]*>[\\s\\S]*</jmeterTestPlan>", Pattern.MULTILINE);

    private static final Pattern JMX_FILE_PATH_PATTERN =
        Pattern.compile("(?:`([^`]+\\.jmx)`|\\b([a-zA-Z]:)?[/\\\\]?(?:[\\w.\\-]+[/\\\\])*[\\w.\\-]+\\.jmx\\b)", Pattern.CASE_INSENSITIVE);

    /**
     * Result of parsing XML content.
     */
    public record ParseResult(HashTree tree, String extractedXml, String errorMessage) {
        public boolean isSuccess() {
            return tree != null && errorMessage == null;
        }

        public static ParseResult success(HashTree tree, String xml) {
            return new ParseResult(tree, xml, null);
        }

        public static ParseResult failure(String errorMessage) {
            return new ParseResult(null, null, errorMessage);
        }
    }

    /**
     * Extracts XML content from text that may contain markdown code blocks.
     *
     * @param text The text potentially containing XML in code blocks
     * @return The extracted XML string, or empty if no valid XML found
     */
    public Optional<String> extractXmlFromText(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }

        // First try to find XML in code blocks
        Matcher codeBlockMatcher = XML_CODE_BLOCK_PATTERN.matcher(text);
        while (codeBlockMatcher.find()) {
            String blockContent = codeBlockMatcher.group(1).trim();
            if (blockContent.contains("<jmeterTestPlan")) {
                return Optional.of(blockContent);
            }
        }

        // Then try to find raw jmeterTestPlan XML
        Matcher rawXmlMatcher = JMETER_TEST_PLAN_PATTERN.matcher(text);
        if (rawXmlMatcher.find()) {
            String xmlContent = rawXmlMatcher.group();
            // Add XML declaration if not present
            if (!xmlContent.startsWith("<?xml")) {
                xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + xmlContent;
            }
            return Optional.of(xmlContent);
        }

        return Optional.empty();
    }

    /**
     * Parses JMeter XML content into a HashTree.
     * The content can be either raw XML or embedded in markdown code blocks.
     *
     * @param content The content containing JMeter XML
     * @return ParseResult containing the HashTree or error message
     */
    public ParseResult parseXml(String content) {
        Optional<String> extractedXml = extractXmlFromText(content);

        if (extractedXml.isEmpty()) {
            return ParseResult.failure("No valid JMeter test plan XML found in the response");
        }

        String xml = extractedXml.get();

        try {
            HashTree tree = loadFromXml(xml);
            return ParseResult.success(tree, xml);
        } catch (Exception e) {
            return ParseResult.failure("Failed to parse JMeter XML: " + e.getMessage());
        }
    }

    /**
     * Parses raw JMeter XML content directly.
     *
     * @param xml The raw JMeter XML content
     * @return The parsed HashTree
     * @throws IOException if parsing fails
     */
    public HashTree loadFromXml(String xml) throws IOException {
        // SaveService.loadTree requires a File, so we create a temp file
        java.io.File tempFile = java.io.File.createTempFile("jmeter-copilot-", ".jmx");
        try {
            java.nio.file.Files.writeString(tempFile.toPath(), xml, StandardCharsets.UTF_8);
            return SaveService.loadTree(tempFile);
        } finally {
            tempFile.delete();
        }
    }

    /**
     * Validates that the given XML is a valid JMeter test plan structure.
     *
     * @param xml The XML content to validate
     * @return true if the XML appears to be a valid JMeter test plan
     */
    public boolean isValidJMeterXml(String xml) {
        if (xml == null || xml.isBlank()) {
            return false;
        }

        // Basic structural validation
        return xml.contains("<jmeterTestPlan")
            && xml.contains("</jmeterTestPlan>")
            && xml.contains("<hashTree>");
    }

    /**
     * Checks if the content contains any JMeter XML.
     *
     * @param content The content to check
     * @return true if the content appears to contain JMeter XML
     */
    public boolean containsJMeterXml(String content) {
        return extractXmlFromText(content).isPresent();
    }

    /**
     * Extracts a .jmx file path from text content.
     * Detects paths in backticks or bare paths ending with .jmx
     *
     * @param text The text potentially containing a file path
     * @return The extracted file path, or empty if none found
     */
    public Optional<String> extractJmxFilePath(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }

        Matcher matcher = JMX_FILE_PATH_PATTERN.matcher(text);
        while (matcher.find()) {
            // Group 1 is for backtick-wrapped paths, group 0 is the full match
            String path = matcher.group(1) != null ? matcher.group(1) : matcher.group(0);
            java.io.File file = new java.io.File(path);
            if (file.exists() && file.isFile()) {
                return Optional.of(path);
            }
        }
        return Optional.empty();
    }

    /**
     * Loads a JMeter test plan from a .jmx file.
     *
     * @param filePath The path to the .jmx file
     * @return ParseResult containing the HashTree or error message
     */
    public ParseResult parseXmlFile(String filePath) {
        java.io.File file = new java.io.File(filePath);
        if (!file.exists()) {
            return ParseResult.failure("File not found: " + filePath);
        }

        try {
            HashTree tree = SaveService.loadTree(file);
            String xml = java.nio.file.Files.readString(file.toPath(), StandardCharsets.UTF_8);
            return ParseResult.success(tree, xml);
        } catch (Exception e) {
            return ParseResult.failure("Failed to load JMeter file: " + e.getMessage());
        }
    }
}
