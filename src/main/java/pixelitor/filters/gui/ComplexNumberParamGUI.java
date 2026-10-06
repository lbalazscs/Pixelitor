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

import javax.swing.*;
import java.awt.*;
import java.util.Locale;
import java.util.Objects;

import static javax.swing.BorderFactory.createTitledBorder;

/**
 * The GUI component for a {@link ComplexNumberParam}.
 */
public class ComplexNumberParamGUI extends JPanel implements ParamGUI {
    public static final int DEFAULT_SIZE = 150;
    private static final int DISPLAY_WIDTH = 150;

    private final ComplexNumberParam model;
    private final ComplexNumberSelector selector;
    private final JPanel displayPanel;
    private final ResetButton resetButton;
    private JLabel realLabel;
    private JLabel imagLabel;

    public ComplexNumberParamGUI(ComplexNumberParam model) {
        super(new BorderLayout(10, 0));
        setBorder(createTitledBorder(model.getName()));

        this.model = Objects.requireNonNull(model);

        this.selector = new ComplexNumberSelector(model);
        this.displayPanel = createDisplayPanel();

        JPanel east = new JPanel();
        east.add(displayPanel);
        resetButton = new ResetButton(model);
        east.add(resetButton);

        add(selector, BorderLayout.CENTER);
        add(east, BorderLayout.EAST);

        if (model.paramGUI == null) {
            model.paramGUI = this;
        }

        updateDisplay();
    }

    private JPanel createDisplayPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1));
        panel.setPreferredSize(new Dimension(DISPLAY_WIDTH, DEFAULT_SIZE));

        Font font = new Font(Font.MONOSPACED, Font.PLAIN, 20);

        realLabel = new JLabel();
        realLabel.setVerticalAlignment(SwingConstants.BOTTOM);
        realLabel.setFont(font);
        panel.add(realLabel);

        imagLabel = new JLabel();
        imagLabel.setVerticalAlignment(SwingConstants.TOP);
        imagLabel.setFont(font);
        panel.add(imagLabel);

        return panel;
    }

    public void updateDisplay() {
        updateDisplay(model.getRe(), model.getIm());
    }

    public void updateDisplay(double real, double imag) {
        realLabel.setText(String.format(Locale.ROOT, "re = %.2f", real));
        imagLabel.setText(String.format(Locale.ROOT, "im = %.2f", imag));
    }

    @Override
    public void updateGUI() {
        updateDisplay();
        selector.repaint();
        resetButton.updateState();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        selector.setEnabled(enabled);
        displayPanel.setEnabled(enabled);
        for (Component c : displayPanel.getComponents()) {
            c.setEnabled(enabled);
        }
    }

    @Override
    public void setToolTip(String tip) {
        setToolTipText(tip);
        selector.setToolTipText(tip);
    }

    @Override
    public int getNumLayoutColumns() {
        return 1;
    }

    public ComplexNumberParam getModel() {
        return model;
    }

    public ComplexNumberSelector getSelector() {
        return selector;
    }
}
