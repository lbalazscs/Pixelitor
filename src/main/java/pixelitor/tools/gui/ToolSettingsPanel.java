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

import org.jdesktop.swingx.combobox.EnumComboBoxModel;
import pixelitor.gui.AutoZoom;
import pixelitor.gui.GUIText;
import pixelitor.tools.brushes.CopyBrushType;
import pixelitor.utils.ToolSettingsLayout;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

import static javax.swing.SwingConstants.VERTICAL;

/**
 * The upper horizontal panel with the settings of the active tool.
 */
public class ToolSettingsPanel extends JPanel {
    public ToolSettingsPanel() {
        super(new ToolSettingsLayout());
    }

    public void addSeparator() {
        JSeparator separator = new JSeparator(VERTICAL);
        separator.setPreferredSize(new Dimension(
            separator.getPreferredSize().width,
            26));
        add(separator);
    }

    public void addWithLabel(String text, JComponent component, String name) {
        add(new JLabel(text));
        add(component);
        component.setName(name);
    }

    public void addComboBox(String text, JComboBox<?> box, String name) {
        box.setFocusable(false);

        addWithLabel(text, box, name);
    }

    public JButton addButton(Action action, String name, String toolTip) {
        JButton button = new JButton(action);
        button.setName(name);
        button.setToolTipText(toolTip);
        add(button);
        return button;
    }

    public JButton addButton(String text, ActionListener listener,
                             String name, String toolTip) {
        JButton button = new JButton(text);
        button.setName(name);
        button.setToolTipText(toolTip);
        button.addActionListener(listener);
        add(button);
        return button;
    }

    public JCheckBox addCheckBox(String text, boolean selected, String name,
                                 Consumer<Boolean> consumer) {
        JCheckBox checkBox = new JCheckBox(text, selected);
        checkBox.setName(name);
        checkBox.addActionListener(e -> consumer.accept(checkBox.isSelected()));
        add(checkBox);
        return checkBox;
    }

    public JCheckBox addCheckBox(String text, boolean selected, String name,
                                 String toolTip) {
        JCheckBox checkBox = new JCheckBox(text, selected);
        checkBox.setName(name);
        checkBox.setToolTipText(toolTip);
        add(checkBox);
        return checkBox;
    }

    public EnumComboBoxModel<CopyBrushType> addCopyBrushTypeSelector(CopyBrushType defaultSelection,
                                                                     Consumer<CopyBrushType> listener) {
        EnumComboBoxModel<CopyBrushType> typeModel = new EnumComboBoxModel<>(CopyBrushType.class);
        typeModel.setSelectedItem(defaultSelection);

        @SuppressWarnings("unchecked")
        var typeCB = new JComboBox<CopyBrushType>(typeModel);

        addComboBox(GUIText.BRUSH + ":", typeCB, "typeCB");
        typeCB.addActionListener(e -> {
            CopyBrushType brushType = (CopyBrushType) typeCB.getSelectedItem();
            listener.accept(brushType);
        });

        return typeModel;
    }

    public void addAutoZoomButtons() {
        add(new JButton(AutoZoom.ACTUAL_PIXELS_ACTION));
        add(new JButton(AutoZoom.FIT_SPACE_ACTION));
        add(new JButton(AutoZoom.FIT_WIDTH_ACTION));
        add(new JButton(AutoZoom.FIT_HEIGHT_ACTION));
    }
}
