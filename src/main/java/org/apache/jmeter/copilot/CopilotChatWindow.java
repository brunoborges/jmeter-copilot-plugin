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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

/**
 * A standalone window for the Copilot Chat panel.
 * Opens the Copilot Chat in a separate window instead of embedded in the main frame.
 */
public class CopilotChatWindow extends JFrame {

    private static CopilotChatWindow instance;

    private final CopilotChatPanel chatPanel;

    private CopilotChatWindow() {
        super("JMeter Copilot Chat");
        chatPanel = new CopilotChatPanel();

        initializeWindow();
    }

    /**
     * Gets or creates the singleton instance of the Copilot Chat window.
     */
    public static synchronized CopilotChatWindow getInstance() {
        if (instance == null) {
            instance = new CopilotChatWindow();
        }
        return instance;
    }

    private void initializeWindow() {
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setLayout(new BorderLayout());
        add(chatPanel, BorderLayout.CENTER);

        setMinimumSize(new Dimension(450, 500));
        setPreferredSize(new Dimension(500, 700));
        pack();
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Don't disconnect on close, just hide
            }
        });
    }

    /**
     * Shows the Copilot Chat window and connects if not already connected.
     */
    public void showWindow() {
        setVisible(true);
        toFront();
        requestFocus();

        if (!chatPanel.isConnected()) {
            chatPanel.connect();
        }
    }

    /**
     * Returns the chat panel instance.
     */
    public CopilotChatPanel getChatPanel() {
        return chatPanel;
    }

    /**
     * Cleans up resources when the application is closing.
     */
    public void cleanup() {
        chatPanel.disconnect();
        dispose();
    }
}
