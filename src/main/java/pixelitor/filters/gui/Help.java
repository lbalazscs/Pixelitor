/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor. If not, see <http://www.gnu.org/licenses/>.
 */

package pixelitor.filters.gui;

import pixelitor.gui.GUIText;
import pixelitor.gui.utils.Dialogs;
import pixelitor.utils.OpenInBrowserAction;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class Help {
    private final String content;
    private final boolean isWikipediaURL;

    private Help(String content, boolean isWikipediaURL) {
        this.content = content;
        this.isWikipediaURL = isWikipediaURL;
    }

    public static Help fromWikiURL(String helpURL) {
        return new Help(helpURL, true);
    }

    public static Help fromHTML(String htmlText) {
        return new Help(htmlText, false);
    }

    public JMenu createMenu() {
        JMenu helpMenu = new JMenu(GUIText.HELP);
        if (isWikipediaURL) {
            if (OpenInBrowserAction.CAN_BROWSE) {
                helpMenu.add(new OpenInBrowserAction("Wikipedia", content));
            } else {
                // fallback to just showing the URL
                addDialogHelp(helpMenu, content);
            }
        } else {
            addDialogHelp(helpMenu, content);
        }
        return helpMenu;
    }

    private static void addDialogHelp(JMenu helpMenu, String helpText) {
        helpMenu.add(new AbstractAction("Help") {
            @Override
            public void actionPerformed(ActionEvent e) {
                Dialogs.showInfoDialog("Help", helpText);
            }
        });
    }
}
