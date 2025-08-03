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

import pixelitor.gui.utils.GridBagHelper;
import pixelitor.utils.Icons;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static javax.swing.BorderFactory.createTitledBorder;
import static pixelitor.gui.utils.SliderSpinner.LabelPosition.NONE;

/**
 * The GUI component for a {@link GridParam}.
 */
public class GridParamGUI extends JPanel implements ParamGUI {
    private final GridParam model;
    private final GridEditorPanel gridEditorPanel;
    private final ResetButton resetButton;

    private final RangeParam gridWidthParam;
    private final RangeParam gridHeightParam;
    private final JComboBox<String> presetComboBox;
    private final ActionListener presetListener;
    private final JButton randomizeButton;

    /**
     * Constructs the GUI for the given GridParam model.
     */
    public GridParamGUI(GridParam model) {
        super(new BorderLayout(5, 0));

        this.model = model;
        this.gridEditorPanel = new GridEditorPanel(model, model.getPainters());
        this.resetButton = new ResetButton(model);

        presetComboBox = new JComboBox<>(model.getPresetNames().toArray(new String[0]));
        this.presetListener = e -> {
            String selected = (String) presetComboBox.getSelectedItem();
            if (selected != null && !selected.equals(model.getSelectedPresetName())) {
                model.selectPreset(selected);
            }
        };

        randomizeButton = new JButton(Icons.getRandomizeIcon());
        randomizeButton.addActionListener(e -> {
            model.randomize();
            model.trigger();
        });
        randomizeButton.setToolTipText("Randomize " + model.getName());

        int defaultWidth = model.getGridCols();
        int defaultHeight = model.getGridRows();

        gridWidthParam = new RangeParam("Columns", 2, defaultWidth, 8, false, NONE);
        gridHeightParam = new RangeParam("Rows", 2, defaultHeight, 8, false, NONE);

        add(createSettingsPanel(), BorderLayout.WEST);
        add(gridEditorPanel, BorderLayout.CENTER);

        // wrap the reset button in a panel to align it vertically
        JPanel eastPanel = new JPanel(new GridBagLayout());
        eastPanel.add(resetButton);
        add(eastPanel, BorderLayout.EAST);

        setBorder(createTitledBorder(model.getName()));
    }

    private JPanel createSettingsPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagHelper gbh = new GridBagHelper(p);

        presetComboBox.addActionListener(presetListener);
        JPanel presetPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        presetPanel.add(presetComboBox);
        presetPanel.add(randomizeButton);
        gbh.addLabelAndControlNoStretch("Preset:", presetPanel);

        gridWidthParam.setAdjustmentListener(() ->
            model.setGridWidth(gridWidthParam.getValue()));

        gridHeightParam.setAdjustmentListener(() ->
            model.setGridHeight(gridHeightParam.getValue()));

        gbh.addParam(gridWidthParam);
        gbh.addParam(gridHeightParam);

        return p;
    }

    @Override
    public void updateGUI() {
        // temporarily remove the listener to prevent firing an event when we update the selection
        presetComboBox.removeActionListener(presetListener);
        presetComboBox.setSelectedItem(model.getSelectedPresetName());
        presetComboBox.addActionListener(presetListener);

        gridWidthParam.setValue(model.getGridCols(), false);
        gridHeightParam.setValue(model.getGridRows(), false);
        gridEditorPanel.gridModelChanged();
        resetButton.updateState();
    }

    @Override
    public void setEnabled(boolean b) {
        super.setEnabled(b);

        presetComboBox.setEnabled(b);
        randomizeButton.setEnabled(b);
        gridWidthParam.setEnabled(b);
        gridHeightParam.setEnabled(b);

        gridEditorPanel.setEnabled(b);
        // the reset button's enabled state is also managed by its own logic (isAtDefault)
        resetButton.setEnabled(b);
    }

    @Override
    public void setToolTip(String tip) {
        gridEditorPanel.setToolTipText(tip);
    }

    @Override
    public int getNumLayoutColumns() {
        return 1;
    }
}
