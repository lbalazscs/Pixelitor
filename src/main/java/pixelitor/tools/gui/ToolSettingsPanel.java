/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.filters.gui.FilterParam;
import pixelitor.filters.gui.ParamGUI;
import pixelitor.gui.AutoZoom;
import pixelitor.gui.GUIText;
import pixelitor.tools.brushes.CopyBrushType;
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
    }

    public void addSeparator() {
        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
        separator.setPreferredSize(new Dimension(
            separator.getPreferredSize().width,
            26));
        add(separator);
    }

    public void addWithLabel(String text, JComponent component, String name) {
        JLabel label = new JLabel(text);
        label.setLabelFor(component);
        add(label);
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
        if (toolTip != null) { // avoid erasing the tooltip set by the Action
            button.setToolTipText(toolTip);
        }
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

    private void addButton(Action action) {
        add(new JButton(action));
    }

    public void addAutoZoomButtons() {
        addButton(AutoZoom.ACTUAL_PIXELS_ACTION);
        addButton(AutoZoom.FIT_SPACE_ACTION);
        addButton(AutoZoom.FIT_WIDTH_ACTION);
        addButton(AutoZoom.FIT_HEIGHT_ACTION);
    }

    public JCheckBox addCheckBox(String text, boolean selected, String name,
                                 String toolTip, Consumer<Boolean> onToggle) {
        JCheckBox checkBox = new JCheckBox(text, selected);
        checkBox.setName(name);
        if (toolTip != null) {
            checkBox.setToolTipText(toolTip);
        }
        if (onToggle != null) {
            checkBox.addActionListener(_ -> onToggle.accept(checkBox.isSelected()));
        }
        add(checkBox);
        return checkBox;
    }

    public JCheckBox addCheckBox(String text, boolean selected, String name, Consumer<Boolean> onToggle) {
        return addCheckBox(text, selected, name, null, onToggle);
    }

    public JCheckBox addCheckBox(String text, boolean selected, String name, String toolTip) {
        return addCheckBox(text, selected, name, toolTip, null);
    }

    public <E extends Enum<E>> EnumComboBoxModel<E> addEnumSelector(Class<E> enumClass, E defaultSelection,
                                                                    String labelText, String name,
                                                                    Consumer<E> listener) {
        var model = new EnumComboBoxModel<>(enumClass);
        model.setSelectedItem(defaultSelection);

        @SuppressWarnings("unchecked")
        var comboBox = new JComboBox<E>(model);
        addComboBox(labelText, comboBox, name);

        comboBox.addActionListener(_ -> {
            @SuppressWarnings("unchecked")
            E selected = (E) comboBox.getSelectedItem();
            listener.accept(selected);
        });

        return model;
    }

    public EnumComboBoxModel<CopyBrushType> addCopyBrushTypeSelector(CopyBrushType defaultSelection,
                                                                     Consumer<CopyBrushType> listener) {
        return addEnumSelector(CopyBrushType.class, defaultSelection,
            GUIText.BRUSH + ":", "typeCB", listener);
    }

    public void addParam(FilterParam param) {
        var gui = (JComponent & ParamGUI) param.createGUI();
        if (gui.getNumLayoutColumns() == 2) {
            add(new JLabel(param.getName() + ":"));
        }
        add(gui);
    }
}
