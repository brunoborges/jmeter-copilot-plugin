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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.logging.Logger;

import javax.swing.JSplitPane;
import javax.swing.UIManager;

import org.apache.jmeter.exceptions.IllegalUserActionException;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.action.ActionNames;
import org.apache.jmeter.gui.action.Load;
import org.apache.jorphan.collections.HashTree;

/**
 * Manages the Copilot Chat panel within JMeter's MainFrame.
 * Provides methods to show/hide the panel and handles integration
 * with JMeter's test tree.
 */
public class CopilotPanelManager {

    private static final Logger LOG = Logger.getLogger(CopilotPanelManager.class.getName());

    private static CopilotPanelManager instance;

    private CopilotChatPanel chatPanel;
    private JSplitPane mainSplitPane;
    private boolean panelVisible = false;
    private int dividerLocation = -1;

    private CopilotPanelManager() {
        // Private constructor for singleton
    }

    /**
     * Gets the singleton instance of CopilotPanelManager.
     */
    public static synchronized CopilotPanelManager getInstance() {
        if (instance == null) {
            instance = new CopilotPanelManager();
        }
        return instance;
    }

    /**
     * Initializes the Copilot panel and integrates it into the given split pane.
     * This should be called during MainFrame initialization.
     *
     * @param existingContent The existing content pane (typically the topAndDown split pane)
     * @return A new split pane containing both the existing content and the Copilot panel
     */
    public JSplitPane initializePanel(Component existingContent) {
        chatPanel = new CopilotChatPanel();

        // Set up the callback to load test plans
        chatPanel.setOnLoadTestPlan(this::loadTestPlan);

        // Create a horizontal split pane with the existing content on the left
        // and the Copilot panel on the right
        mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setLeftComponent(existingContent);
        mainSplitPane.setRightComponent(chatPanel);
        mainSplitPane.setResizeWeight(1.0); // Give all extra space to the left component
        mainSplitPane.setContinuousLayout(true);
        mainSplitPane.setOneTouchExpandable(true);

        // Initially hide the Copilot panel
        chatPanel.setVisible(false);
        mainSplitPane.setDividerSize(0);

        return mainSplitPane;
    }

    /**
     * Toggles the visibility of the Copilot panel.
     */
    public void togglePanel() {
        if (panelVisible) {
            hidePanel();
        } else {
            showPanel();
        }
    }

    /**
     * Shows the Copilot panel.
     */
    public void showPanel() {
        if (mainSplitPane == null || chatPanel == null) {
            LOG.warning("Copilot panel not initialized");
            return;
        }

        chatPanel.setVisible(true);
        mainSplitPane.setDividerSize(UIManager.getInt("SplitPane.dividerSize"));

        // Set divider location to show the panel
        if (dividerLocation > 0) {
            mainSplitPane.setDividerLocation(dividerLocation);
        } else {
            // Default: show panel taking about 25% of width
            int totalWidth = mainSplitPane.getWidth();
            mainSplitPane.setDividerLocation(totalWidth - chatPanel.getPreferredSize().width);
        }

        panelVisible = true;

        // Connect to Copilot if not already connected
        if (!chatPanel.isConnected()) {
            chatPanel.connect();
        }
    }

    /**
     * Hides the Copilot panel.
     */
    public void hidePanel() {
        if (mainSplitPane == null || chatPanel == null) {
            return;
        }

        // Remember the divider location
        dividerLocation = mainSplitPane.getDividerLocation();

        chatPanel.setVisible(false);
        mainSplitPane.setDividerSize(0);
        panelVisible = false;
    }

    /**
     * Returns whether the panel is currently visible.
     */
    public boolean isPanelVisible() {
        return panelVisible;
    }

    /**
     * Returns the chat panel instance.
     */
    public CopilotChatPanel getChatPanel() {
        return chatPanel;
    }

    /**
     * Loads a test plan into JMeter's test tree.
     */
    @SuppressWarnings("MethodCanBeStatic")
    private void loadTestPlan(HashTree tree) {
        try {
            GuiPackage guiPackage = GuiPackage.getInstance();
            if (guiPackage != null) {
                // Use JMeter's Load.insertLoadedTree which properly handles
                // GUI component initialization and tree insertion
                ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, ActionNames.OPEN);
                Load.insertLoadedTree(event.getID(), tree);

                LOG.info("Test plan loaded from Copilot");
            }
        } catch (IllegalUserActionException e) {
            LOG.severe("Failed to load test plan: " + e.getMessage());
            throw new RuntimeException("Failed to load test plan: " + e.getMessage(), e);
        } catch (Exception e) {
            LOG.severe("Failed to load test plan: " + e.getMessage());
            throw new RuntimeException("Failed to load test plan", e);
        }
    }

    /**
     * Cleans up resources when JMeter is closing.
     */
    public void cleanup() {
        if (chatPanel != null) {
            chatPanel.disconnect();
        }
    }
}
