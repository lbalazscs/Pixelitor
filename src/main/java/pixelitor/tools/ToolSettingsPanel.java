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

import org.jdesktop.swingx.combobox.EnumComboBoxModel;
import pixelitor.tools.brushes.CopyBrushType;
import pixelitor.tools.brushes.CopyBrushTypeChangedListener;
import pixelitor.utils.ToolSettingsLayout;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

/**
 * The upper horizontal panel with the settings of the active tool.
 */
public class ToolSettingsPanel extends JPanel {
    public ToolSettingsPanel() {
        super(new ToolSettingsLayout());
//        super(new FlowLayout(FlowLayout.LEFT, 5, 0));
    }

    public void addSeparator() {
        JSeparator separator = new JSeparator(JSeparator.VERTICAL);
        separator.setPreferredSize(new Dimension(
                separator.getPreferredSize().width,
                26));
        add(separator);
    }

    public void addWithLabel(String labelText, JComponent component) {
        add(new JLabel(labelText));
        add(component);
    }

    public void addWithLabel(String labelText, JComponent component, String name) {
        addWithLabel(labelText, component);
        component.setName(name);
    }

    public JButton addButton(String text, ActionListener actionListener) {
        JButton button = new JButton(text);
        add(button);
        button.addActionListener(actionListener);
        return button;
    }

    public JButton addButton(Action action) {
        JButton button = new JButton(action);
        add(button);
        return button;
    }

    public JCheckBox addCheckBox(String text, boolean selected, String name, Consumer<Boolean> consumer) {
        JCheckBox checkBox = new JCheckBox(text, selected);
        checkBox.setName(name);
        checkBox.addActionListener(e -> consumer.accept(checkBox.isSelected()));
        add(checkBox);
        return checkBox;
    }

    public void addCopyBrushTypeSelector(CopyBrushType defaultSelection,
                                         CopyBrushTypeChangedListener listener) {
        EnumComboBoxModel<CopyBrushType> typeModel = new EnumComboBoxModel<>(CopyBrushType.class);
        typeModel.setSelectedItem(defaultSelection);
        JComboBox<CopyBrushType> typeCB = new JComboBox<>(typeModel);

        addWithLabel("Brush:", typeCB, "typeCB");
        typeCB.addActionListener(e -> {
            CopyBrushType brushType = (CopyBrushType) typeCB.getSelectedItem();
            listener.copyBrushTypeChanged(brushType);
        });
    }

    public void addAutoZoomButtons() {
        JButton actualPixelsButton = new JButton(AutoZoomButtons.ACTUAL_PIXELS_ACTION);
        add(actualPixelsButton);

        JButton fitScreenButton = new JButton(AutoZoomButtons.FIT_SCREEN_ACTION);
        add(fitScreenButton);
    }
}
