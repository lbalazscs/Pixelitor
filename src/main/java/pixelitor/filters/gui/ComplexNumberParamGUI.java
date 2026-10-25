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

package pixelitor.filters.gui;

import pixelitor.filters.util.ComplexBounds;

import javax.swing.*;
import java.awt.*;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

import static javax.swing.BorderFactory.createTitledBorder;

/**
 * The GUI component for a {@link ComplexNumberParam}.
 */
public class ComplexNumberParamGUI extends JPanel implements ParamGUI {
    private static final int SPINNER_PANEL_WIDTH = 150;
    private static final double SPINNER_STEP = 0.01;
    private static final String NUMBER_FORMAT = "0.00";

    private final ComplexNumberParam model;
    private final ComplexNumberSelector selector;
    private final JPanel spinnerPanel;
    private final ResetButton resetButton;
    private final JSpinner reSpinner;
    private final JSpinner imSpinner;

    // prevents feedback loops between model updates and spinner change listeners
    private boolean updatingFromModel = false;

    public ComplexNumberParamGUI(ComplexNumberParam model) {
        super(new BorderLayout(10, 0));
        setBorder(createTitledBorder(model.getName()));

        this.model = model;

        ComplexBounds bounds = model.getBounds();
        selector = new ComplexNumberSelector(model);
        reSpinner = createSpinner(bounds.minRe(), bounds.maxRe(), model::getRe, model::setRe);
        imSpinner = createSpinner(bounds.minIm(), bounds.maxIm(), model::getIm, model::setIm);
        spinnerPanel = createSpinnerPanel();

        JPanel east = new JPanel();
        east.add(spinnerPanel);
        resetButton = new ResetButton(model);
        east.add(resetButton);

        add(selector, BorderLayout.CENTER);
        add(east, BorderLayout.EAST);
    }

    public void updateBounds() {
        syncSpinnersWithBounds();
        selector.updateBounds();
        updateGUI();
    }

    private void syncSpinnersWithBounds() {
        ComplexBounds bounds = model.getBounds();

        updatingFromModel = true;
        try {
            var reModel = (SpinnerNumberModel) reSpinner.getModel();
            var imModel = (SpinnerNumberModel) imSpinner.getModel();

            // temporarily remove the min/max bounds so that setting the value does not throw IllegalArgumentException
            reModel.setMinimum(null);
            reModel.setMaximum(null);
            reSpinner.setValue(model.getRe());
            reModel.setMinimum(bounds.minRe());
            reModel.setMaximum(bounds.maxRe());

            imModel.setMinimum(null);
            imModel.setMaximum(null);
            imSpinner.setValue(model.getIm());
            imModel.setMinimum(bounds.minIm());
            imModel.setMaximum(bounds.maxIm());
        } finally {
            updatingFromModel = false;
        }
    }

    private JPanel createSpinnerPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setPreferredSize(new Dimension(SPINNER_PANEL_WIDTH, ComplexNumberSelector.DEFAULT_SIZE));

        addRow(panel, 0, "Re:", reSpinner, 0, 5);
        addRow(panel, 1, "Im:", imSpinner, 5, 0);

        return panel;
    }

    private static void addRow(JPanel panel, int row, String text, JSpinner spinner, int top, int bottom) {
        var gbc = new GridBagConstraints();
        gbc.weightx = 1.0;
        gbc.gridy = row;
        gbc.insets = new Insets(top, 0, bottom, 4);

        var label = new JLabel(text);
        label.setLabelFor(spinner);

        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.LINE_END;
        panel.add(label, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.insets.right = 0;
        panel.add(spinner, gbc);
    }

    private JSpinner createSpinner(double min, double max,
                                   DoubleSupplier getter, DoubleConsumer setter) {
        var spinnerModel = new SpinnerNumberModel(getter.getAsDouble(), min, max, SPINNER_STEP);
        var spinner = new JSpinner(spinnerModel);
        spinner.setEditor(new JSpinner.NumberEditor(spinner, NUMBER_FORMAT));

        spinnerModel.addChangeListener(_ -> {
            if (updatingFromModel) {
                return;
            }
            double val = spinnerModel.getNumber().doubleValue();
            if (Double.compare(val, getter.getAsDouble()) != 0) {
                setter.accept(val);
            }
        });

        return spinner;
    }

    private void updateSpinners() {
        updatingFromModel = true;
        try {
            reSpinner.setValue(model.getRe());
            imSpinner.setValue(model.getIm());
        } finally {
            updatingFromModel = false;
        }
    }

    @Override
    public void updateGUI() {
        updateSpinners();
        selector.repaint();
        resetButton.updateState();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        selector.setEnabled(enabled);
        spinnerPanel.setEnabled(enabled);
        for (Component c : spinnerPanel.getComponents()) {
            c.setEnabled(enabled);
        }
        resetButton.setEnabled(enabled);
    }

    @Override
    public void setToolTip(String tip) {
        setToolTipText(tip);
        selector.setToolTipText(tip);
        reSpinner.setToolTipText(tip);
        imSpinner.setToolTipText(tip);
    }

    @Override
    public int getNumLayoutColumns() {
        return 1;
    }
}
