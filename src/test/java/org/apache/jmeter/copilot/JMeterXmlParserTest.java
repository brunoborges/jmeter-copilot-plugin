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

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for JMeterXmlParser class.
 */
@DisplayName("JMeterXmlParser Tests")
class JMeterXmlParserTest {

    private JMeterXmlParser parser;

    private static final String VALID_JMETER_XML = """
        <?xml version="1.0" encoding="UTF-8"?>
        <jmeterTestPlan version="1.2" properties="5.0" jmeter="5.6.3">
          <hashTree>
            <TestPlan guiclass="TestPlanGui" testclass="TestPlan" testname="Test Plan">
              <boolProp name="TestPlan.functional_mode">false</boolProp>
              <boolProp name="TestPlan.serialize_threadgroups">false</boolProp>
              <stringProp name="TestPlan.comments"></stringProp>
              <stringProp name="TestPlan.user_define_classpath"></stringProp>
              <elementProp name="TestPlan.user_defined_variables" elementType="Arguments">
                <collectionProp name="Arguments.arguments"/>
              </elementProp>
            </TestPlan>
            <hashTree>
              <ThreadGroup guiclass="ThreadGroupGui" testclass="ThreadGroup" testname="Thread Group">
                <intProp name="ThreadGroup.num_threads">1</intProp>
                <intProp name="ThreadGroup.ramp_time">1</intProp>
                <boolProp name="ThreadGroup.same_user_on_next_iteration">true</boolProp>
                <stringProp name="ThreadGroup.on_sample_error">continue</stringProp>
                <elementProp name="ThreadGroup.main_controller" elementType="LoopController">
                  <stringProp name="LoopController.loops">1</stringProp>
                  <boolProp name="LoopController.continue_forever">false</boolProp>
                </elementProp>
              </ThreadGroup>
              <hashTree/>
            </hashTree>
          </hashTree>
        </jmeterTestPlan>
        """;

    private static final String MARKDOWN_WITH_XML = """
        Here's a JMeter test plan for load testing your API:

        ```xml
        <?xml version="1.0" encoding="UTF-8"?>
        <jmeterTestPlan version="1.2" properties="5.0">
          <hashTree>
            <TestPlan guiclass="TestPlanGui" testclass="TestPlan" testname="API Load Test">
              <boolProp name="TestPlan.functional_mode">false</boolProp>
            </TestPlan>
            <hashTree/>
          </hashTree>
        </jmeterTestPlan>
        ```

        This test plan includes a basic structure. You can add HTTP samplers as needed.
        """;

    @BeforeEach
    void setUp() {
        parser = new JMeterXmlParser();
    }

    @Test
    @DisplayName("should extract XML from markdown code block")
    void shouldExtractXmlFromMarkdownCodeBlock() {
        Optional<String> result = parser.extractXmlFromText(MARKDOWN_WITH_XML);

        assertThat(result).isPresent();
        assertThat(result.get()).contains("<jmeterTestPlan");
        assertThat(result.get()).contains("</jmeterTestPlan>");
    }

    @Test
    @DisplayName("should extract raw XML without code blocks")
    void shouldExtractRawXml() {
        Optional<String> result = parser.extractXmlFromText(VALID_JMETER_XML);

        assertThat(result).isPresent();
        assertThat(result.get()).contains("<jmeterTestPlan");
    }

    @Test
    @DisplayName("should return empty for text without JMeter XML")
    void shouldReturnEmptyForTextWithoutJMeterXml() {
        String noXml = "This is just some text without any XML content.";
        Optional<String> result = parser.extractXmlFromText(noXml);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should return empty for null input")
    void shouldReturnEmptyForNull() {
        Optional<String> result = parser.extractXmlFromText(null);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should return empty for blank input")
    void shouldReturnEmptyForBlank() {
        Optional<String> result = parser.extractXmlFromText("   ");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should validate correct JMeter XML structure")
    void shouldValidateCorrectJMeterXml() {
        assertThat(parser.isValidJMeterXml(VALID_JMETER_XML)).isTrue();
    }

    @Test
    @DisplayName("should reject invalid XML structure")
    void shouldRejectInvalidXmlStructure() {
        String invalidXml = "<notJmeter><something/></notJmeter>";
        assertThat(parser.isValidJMeterXml(invalidXml)).isFalse();
    }

    @Test
    @DisplayName("should reject null XML")
    void shouldRejectNullXml() {
        assertThat(parser.isValidJMeterXml(null)).isFalse();
    }

    @Test
    @DisplayName("should reject incomplete JMeter XML")
    void shouldRejectIncompleteJMeterXml() {
        String incomplete = "<jmeterTestPlan><hashTree>";
        assertThat(parser.isValidJMeterXml(incomplete)).isFalse();
    }

    @Test
    @DisplayName("should detect JMeter XML in content")
    void shouldDetectJMeterXmlInContent() {
        assertThat(parser.containsJMeterXml(MARKDOWN_WITH_XML)).isTrue();
        assertThat(parser.containsJMeterXml("No XML here")).isFalse();
    }

    @Test
    @DisplayName("should handle code block with xml tag")
    void shouldHandleCodeBlockWithXmlTag() {
        String withXmlTag = """
            ```xml
            <?xml version="1.0" encoding="UTF-8"?>
            <jmeterTestPlan version="1.2">
              <hashTree>
                <TestPlan testname="Test"/>
                <hashTree/>
              </hashTree>
            </jmeterTestPlan>
            ```
            """;

        Optional<String> result = parser.extractXmlFromText(withXmlTag);
        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("should handle code block without xml tag")
    void shouldHandleCodeBlockWithoutXmlTag() {
        String withoutXmlTag = """
            ```
            <?xml version="1.0" encoding="UTF-8"?>
            <jmeterTestPlan version="1.2">
              <hashTree>
                <TestPlan testname="Test"/>
                <hashTree/>
              </hashTree>
            </jmeterTestPlan>
            ```
            """;

        Optional<String> result = parser.extractXmlFromText(withoutXmlTag);
        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("should parse XML and return failure for invalid content")
    void shouldReturnFailureForInvalidContent() {
        JMeterXmlParser.ParseResult result = parser.parseXml("No XML content here");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.errorMessage()).isNotNull();
        assertThat(result.tree()).isNull();
    }

    @Test
    @DisplayName("should add XML declaration if missing")
    void shouldAddXmlDeclarationIfMissing() {
        String xmlWithoutDeclaration = """
            <jmeterTestPlan version="1.2">
              <hashTree>
                <TestPlan testname="Test"/>
                <hashTree/>
              </hashTree>
            </jmeterTestPlan>
            """;

        Optional<String> result = parser.extractXmlFromText(xmlWithoutDeclaration);

        assertThat(result).isPresent();
        assertThat(result.get()).startsWith("<?xml");
    }
}
