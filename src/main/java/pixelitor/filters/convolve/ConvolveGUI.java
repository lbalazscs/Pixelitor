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

package pixelitor.filters.convolve;

import pixelitor.filters.gui.FilterGUI;
import pixelitor.gui.GUIText;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.layers.Filterable;
import pixelitor.utils.Messages;

import javax.swing.*;
import java.awt.GridLayout;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

import static javax.swing.BorderFactory.createTitledBorder;
import static javax.swing.BoxLayout.X_AXIS;

/**
 * An adjustment panel for customizable convolution filters.
 */
public class ConvolveGUI extends FilterGUI {
    private static final float EPSILON = 0.003f;

    private JTextField[] kernelTextFields;
    private JPanel kernelPanel;
    private JButton normalizeButton;
    private Box presetsBox;
    private final int kernelSize;

    public ConvolveGUI(Convolve filter, Filterable layer, boolean reset) {
        super(filter, layer);
        setLayout(new BoxLayout(this, X_AXIS));

        kernelSize = filter.getKernelSize();

        initLeftPanel();
        initPresetPanel();

        if (reset) {
            resetKernel(kernelSize);
        } else {
            // use the last values
            float[] matrix = filter.getKernelMatrix();
            if (matrix == null) {
                resetKernel(kernelSize);
            } else {
                loadKernel(matrix);
            }
        }
    }

    private static float parseUserInput(String s) throws NumberFormatException {
        String trimmed = s.trim();
        if (trimmed.isEmpty()) {
            return 0.0f;
        }

        NumberFormat nf = NumberFormat.getInstance();
        Number number;
        try {
            // try locale-specific parsing
            number = nf.parse(trimmed);
        } catch (ParseException e) {
            NumberFormat englishFormat = NumberFormat.getInstance(Locale.ENGLISH);
            try {
                // second chance: English
                number = englishFormat.parse(trimmed);
            } catch (ParseException e1) {
                throw new NumberFormatException('\"' + s + "\" is not a number.");
            }
        }
        return number.floatValue();
    }

    private void initLeftPanel() {
        Box leftPanel = Box.createVerticalBox();

        setupKernelPanel(leftPanel);
        addNormalizeButton(leftPanel);
        addExecuteButton(leftPanel);

        leftPanel.add(Box.createVerticalStrut(20));
        leftPanel.setMaximumSize(leftPanel.getPreferredSize());
        leftPanel.setAlignmentY(TOP_ALIGNMENT);

        add(leftPanel);
    }

    private void setupKernelPanel(Box parentBox) {
        kernelPanel = new JPanel(new GridLayout(kernelSize, kernelSize));

        kernelTextFields = new JTextField[kernelSize * kernelSize];
        for (int i = 0; i < kernelTextFields.length; i++) {
            kernelTextFields[i] = createKernelTextField();
        }

        kernelPanel.setBorder(createTitledBorder("Kernel"));
        kernelPanel.setAlignmentX(LEFT_ALIGNMENT);
        parentBox.add(kernelPanel);
    }

    private void addNormalizeButton(Box parentBox) {
        normalizeButton = new JButton("Normalize (preserve brightness)");
        normalizeButton.addActionListener(e -> normalizeKernel());
        normalizeButton.setAlignmentX(LEFT_ALIGNMENT);
        parentBox.add(normalizeButton);
    }

    private void addExecuteButton(Box parentBox) {
        JButton runButton = new JButton("Apply Filter");
        runButton.setToolTipText("Applies the filter with the current kernel values.");
        runButton.addActionListener(e -> onUserAction());
        parentBox.add(runButton);
    }

    private void initPresetPanel() {
        presetsBox = Box.createVerticalBox();
        presetsBox.setBorder(createTitledBorder(GUIText.PRESETS));

        switch (kernelSize) {
            case 3 -> init3x3Presets();
            case 5 -> init5x5Presets();
            default -> throw new IllegalStateException("kernelSize = " + kernelSize);
        }

        presetsBox.add(Box.createVerticalStrut(20));

        JButton randomizeButton = GUIUtils.createRandomizeSettingsButton(e -> {
            loadKernel(Convolve.createRandomKernel(kernelSize));
            onUserAction();
        });
        presetsBox.add(randomizeButton);

        JButton resetButton = GUIUtils.createResetAllButton(e -> {
            resetKernel(kernelSize);
            onUserAction();
        });
        presetsBox.add(resetButton);

        presetsBox.setMaximumSize(presetsBox.getPreferredSize());
        presetsBox.setAlignmentY(TOP_ALIGNMENT);
        add(presetsBox);
    }

    private void createPresetButton(String name, float[] kernel) {
        JButton button = new JButton(name);
        button.addActionListener(e -> {
            loadKernel(kernel);
            onUserAction();
        });
        presetsBox.add(button);
    }

    private void init3x3Presets() {
        createPresetButton("Corner Blur", new float[]{
            0.25f, 0.0f, 0.25f,
            0.0f, 0.0f, 0.0f,
            0.25f, 0.0f, 0.25f});

        createPresetButton("\"Gaussian\" Blur", new float[]{
            1 / 16.0f, 2 / 16.0f, 1 / 16.0f,
            2 / 16.0f, 4 / 16.0f, 2 / 16.0f,
            1 / 16.0f, 2 / 16.0f, 1 / 16.0f});

        createPresetButton("Mean Blur", new float[]{
            0.1115f, 0.1115f, 0.1115f,
            0.1115f, 0.1115f, 0.1115f,
            0.1115f, 0.1115f, 0.1115f});

        createPresetButton("Sharpen", new float[]{
            0, -1, 0,
            -1, 5, -1,
            0, -1, 0});

        createPresetButton("Sharpen 2", new float[]{
            -1, -1, -1,
            -1, 9, -1,
            -1, -1, -1});

        createPresetButton("Edge Detection", new float[]{
            0, -1, 0,
            -1, 4, -1,
            0, -1, 0});

        createPresetButton("Edge Detection 2", new float[]{
            -1, -1, -1,
            -1, 8, -1,
            -1, -1, -1});

        createPresetButton("Horizontal Edge Detection", new float[]{
            -1, -1, -1,
            0, 0, 0,
            1, 1, 1});

        createPresetButton("Vertical Edge Detection", new float[]{
            -1, 0, 1,
            -1, 0, 1,
            -1, 0, 1});

        createPresetButton("Emboss", new float[]{
            -2, -2, 0,
            -2, 6, 0,
            0, 0, 0});

        createPresetButton("Emboss 2", new float[]{
            -2, 0, 0,
            0, 0, 0,
            0, 0, 2});

        createPresetButton("Color Emboss", new float[]{
            -1, -1, 0,
            -1, 1, 1,
            0, 1, 1});
    }

    private void init5x5Presets() {
        createPresetButton("Diamond Blur", new float[]{
            0.0f, 0.0f, 0.077f, 0.0f, 0.0f,
            0.0f, 0.077f, 0.077f, 0.077f, 0.0f,
            0.077f, 0.077f, 0.077f, 0.077f, 0.077f,
            0.0f, 0.077f, 0.077f, 0.077f, 0.0f,
            0.0f, 0.0f, 0.077f, 0.0f, 0.0f,
        });

        createPresetButton("Motion Blur", new float[]{
            0.0f, 0.0f, 0.0f, 0.0f, 0.2f,
            0.0f, 0.0f, 0.0f, 0.2f, 0.0f,
            0.0f, 0.0f, 0.2f, 0.0f, 0.0f,
            0.0f, 0.2f, 0.0f, 0.0f, 0.0f,
            0.2f, 0.0f, 0.0f, 0.0f, 0.0f,
        });

        createPresetButton("Find Horizontal Edges", new float[]{
            0.0f, 0.0f, -1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, -2.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 6.0f, 0.0f, 0.0f,
            0.0f, 0.0f, -2.0f, 0.0f, 0.0f,
            0.0f, 0.0f, -1.0f, 0.0f, 0.0f,
        });

        createPresetButton("Find Vertical Edges", new float[]{
            0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
            -1.0f, -2.0f, 6.0f, -2.0f, -1.0f,
            0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
        });

        createPresetButton("Find \\ Edges", new float[]{
            0.0f, 0.0f, 0.0f, 0.0f, -1.0f,
            0.0f, 0.0f, 0.0f, -2.0f, 0.0f,
            0.0f, 0.0f, 6.0f, 0.0f, 0.0f,
            0.0f, -2.0f, 0.0f, 0.0f, 0.0f,
            -1.0f, 0.0f, 0.0f, 0.0f, 0.0f,
        });

        createPresetButton("Find / Edges", new float[]{
            -1.0f, 0.0f, 0.0f, 0.0f, 0.0f,
            0.0f, -2.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 6.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 0.0f, -2.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 0.0f, -1.0f,
        });

        createPresetButton("Sharpen", new float[]{
            -0.125f, -0.125f, -0.125f, -0.125f, -0.125f,
            -0.125f, 0.25f, 0.25f, 0.25f, -0.125f,
            -0.125f, 0.25f, 1.0f, 0.25f, -0.125f,
            -0.125f, 0.25f, 0.25f, 0.25f, -0.125f,
            -0.125f, -0.125f, -0.125f, -0.125f, -0.125f,
        });
    }

    private void resetKernel(int size) {
        float[] defaultValues = new float[size * size];
        defaultValues[defaultValues.length / 2] = 1.0f;

        loadKernel(defaultValues);
    }

    private JTextField createKernelTextField() {
        JTextField textField = new JTextField();
        textField.setColumns(5);
        textField.setHorizontalAlignment(JTextField.RIGHT);
        kernelPanel.add(textField);
        textField.addActionListener(e -> onUserAction());
        return textField;
    }

    private void collectKernelValues() {
        float[] values = getCurrentKernelValues();
        if (values == null) {
            return; // an error occurred during parsing
        }
        ((Convolve) this.filter).setKernelMatrix(values);
    }

    private float[] getCurrentKernelValues() {
        float[] values = new float[kernelSize * kernelSize];
        float sum = 0.0f;
        for (int i = 0; i < values.length; i++) {
            String s = kernelTextFields[i].getText();
            try {
                values[i] = parseUserInput(s);
            } catch (NumberFormatException ex) {
                Messages.showError("Invalid Input", ex.getMessage(), this);
                return null;
            }
            sum += values[i];
        }
        toggleNormalizeButton(sum);
        return values;
    }

    private void onUserAction() {
        startPreview(false);
    }

    private void normalizeKernel() {
        float[] values = getCurrentKernelValues();
        if (values == null) {
            return; // an error occurred during parsing
        }

        float sum = 0.0f;
        for (float value : values) {
            sum += value;
        }

        if (sum != 0.0f) {
            for (int i = 0; i < values.length; i++) {
                values[i] /= sum;
            }
            loadKernel(values);
            startPreview(false);
        }
    }

    @Override
    public void startPreview(boolean initialPreview) {
        collectKernelValues();
        super.startPreview(initialPreview);
    }

    private void loadKernel(float[] values) {
        assert values.length == kernelSize * kernelSize;

        float sum = 0;
        for (int i = 0; i < kernelTextFields.length; i++) {
            String valueAsString = "";
            if (values[i] != 0.0f) {
                valueAsString = String.format("%.3f", values[i]);
            }

            kernelTextFields[i].setText(valueAsString);
            sum += values[i];
        }

        toggleNormalizeButton(sum);
    }

    /**
     * Enables or disables the normalize button based on the kernel sum.
     */
    private void toggleNormalizeButton(float sum) {
        boolean notZero = Math.abs(sum) > EPSILON;
        boolean notOne = Math.abs(sum - 1.0f) > EPSILON;

        normalizeButton.setEnabled(notZero && notOne);
    }
}
