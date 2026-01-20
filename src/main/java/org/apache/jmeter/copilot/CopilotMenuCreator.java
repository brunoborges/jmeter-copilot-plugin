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

import java.awt.event.KeyEvent;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.MenuElement;

import org.apache.jmeter.gui.plugin.MenuCreator;

/**
 * Creates menu items for the Copilot panel.
 * This class is discovered by JMeter via ServiceLoader.
 */
public class CopilotMenuCreator implements MenuCreator {

    @Override
    public JMenuItem[] getMenuItemsAtLocation(MENU_LOCATION location) {
        if (location == MENU_LOCATION.TOOLS) {
            JMenuItem copilotMenuItem = new JMenuItem("Copilot Chat");
            copilotMenuItem.setMnemonic(KeyEvent.VK_C);
            copilotMenuItem.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_P,
                    java.awt.event.InputEvent.CTRL_DOWN_MASK | java.awt.event.InputEvent.SHIFT_DOWN_MASK)
            );
            copilotMenuItem.setActionCommand(ToggleCopilotPanelAction.TOGGLE_COPILOT_PANEL);
            copilotMenuItem.addActionListener(e -> {
                CopilotChatWindow.getInstance().showWindow();
            });

            return new JMenuItem[] { copilotMenuItem };
        }
        return new JMenuItem[0];
    }

    @Override
    public JMenu[] getTopLevelMenus() {
        return new JMenu[0];
    }

    @Override
    public boolean localeChanged(MenuElement menu) {
        return false;
    }

    @Override
    public void localeChanged() {
        // No-op: No locale-specific resources
    }
}
