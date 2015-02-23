/*
 * Copyright 2015 Laszlo Balazs-Csiki
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
package pixelitor.tools;

import pixelitor.AppLogic;
import pixelitor.utils.IconUtils;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * The button that activates a tool
 */
public class ToolButton extends JToggleButton implements ActionListener {
    private final Tool tool;

    public ToolButton(Tool tool) {
        this.tool = tool;
        tool.setButton(this);

        // used for component lookup when testing
        String buttonName = tool.getName() + " Tool Button";
        setName(buttonName);

        putClientProperty("JComponent.sizeVariant", "mini");

        Icon icon = IconUtils.loadIcon(tool.getIconFileName());
        setIcon(icon);

        char c = tool.getActivationKeyChar();
        String s = new String(new char[]{c}).toUpperCase();
        setToolTipText(tool.getName() + " Tool (" + s + ')');

        setMargin(new Insets(0, 0, 0, 0));
        setBorderPainted(true);
        setRolloverEnabled(false);
        addActionListener(this);

        int size = 46; // the icons are 32*32
        Dimension preferredSize = new Dimension(size, size);
        setPreferredSize(preferredSize);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Tools.setCurrentTool(tool);
        AppLogic.setStatusMessage(tool.getName() + " Tool: " + tool.getToolMessage());
    }
}
