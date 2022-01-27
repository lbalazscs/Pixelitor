/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.tools.gui;

import pixelitor.filters.gui.UserPreset;
import pixelitor.tools.Tool;
import pixelitor.tools.Tools;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import static java.awt.RenderingHints.KEY_INTERPOLATION;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR;

/**
 * The button that activates a tool
 */
public class ToolButton extends JToggleButton {
    public static final int TOOL_ICON_SIZE = 28;

    private final Tool tool;

    private List<UserPreset> startupPresets;
    private int numPresets;
    private JPopupMenu popup;

    public ToolButton(Tool tool, Dimension preferredSize) {
        this.tool = tool;
        tool.setButton(this);

        // used for component lookup when testing
        String buttonName = tool.getName() + " Button";
        setName(buttonName);

        putClientProperty("JComponent.sizeVariant", "mini");

        Icon icon = tool.createIcon();
        setIcon(icon);

        assert icon.getIconWidth() == TOOL_ICON_SIZE;
        assert icon.getIconHeight() == TOOL_ICON_SIZE;

        char c = tool.getActivationKey();
        setToolTipText("<html>" + tool.getName() + " (<b>" + c + "</b>)");

        setMargin(new Insets(0, 0, 0, 0));
        setBorderPainted(true);
        setRolloverEnabled(false);
        addActionListener(e -> Tools.start(tool));

        if (tool.canHaveUserPresets()) {
            startupPresets = UserPreset.loadPresets(tool.getPresetDirName());
            numPresets = startupPresets.size();

            popup = new JPopupMenu();
            popup.add(tool.createSavePresetAction(this,
                this::addPreset, this::removePreset));
            if (!startupPresets.isEmpty()) {
                popup.add(tool.createManagePresetsAction());
                popup.addSeparator();
                for (UserPreset preset : startupPresets) {
                    popup.add(preset.asAction(tool));
                }
            }

            setComponentPopupMenu(popup);
        }

        setPreferredSize(preferredSize);
    }

    private void addPreset(UserPreset preset) {
        if (numPresets == 0) {
            popup.add(tool.createManagePresetsAction());
            popup.addSeparator();
        }
        popup.add(preset.asAction(tool));
        numPresets++;
    }

    private void removePreset(UserPreset preset) {
        Component[] menuComponents = popup.getComponents();
        for (Component item : menuComponents) {
            if (item instanceof JMenuItem menuItem) {
                if (menuItem.getText().equals(preset.getName())) {
                    popup.remove(menuItem);
                    numPresets--;
                    break;
                }
            }
        }
    }

    public Tool getTool() {
        return tool;
    }

    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        if (tool == Tools.BRUSH) {
            g2d.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR);
        }
        super.paintComponent(g2d);
    }
}
