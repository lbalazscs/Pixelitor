/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.filters.gui.DialogMenuBar;
import pixelitor.filters.gui.FilterGUI;
import pixelitor.layers.Filterable;
import pixelitor.utils.Messages;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

import static javax.swing.BorderFactory.createTitledBorder;
import static javax.swing.BoxLayout.X_AXIS;

/**
 * An adjustment panel for customizable convolution filters.
 */
public class ConvolveGUI extends FilterGUI {
    private static final int PREFERRED_TEXTFIELD_WIDTH = 70;

    private JTextField[] kernelTextFields;
    private JPanel kernelPanel;
    private JButton normalizeButton;
    private Box presetsBox;
    private final int matrixOrder;

    private Object lastEventSource;

    public ConvolveGUI(Convolve filter, Filterable layer, boolean reset) {
        super(filter, layer);
        setLayout(new BoxLayout(this, X_AXIS));

        matrixOrder = filter.getMatrixOrder();

        initLeftPanel();
        initPresetPanel();

        if (reset) {
            resetKernel(matrixOrder);
        } else {
            // use the last values
            float[] matrix = filter.getKernelMatrix();
            if (matrix == null) {
                resetKernel(matrixOrder);
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
        kernelPanel = new JPanel(new GridLayout(matrixOrder, matrixOrder));
        kernelTextFields = new JTextField[matrixOrder * matrixOrder];
        for (int i = 0; i < kernelTextFields.length; i++) {
            kernelTextFields[i] = new JTextField();
        }
        for (var textField : kernelTextFields) {
            setupKernelTextField(textField);
        }
        kernelPanel.setBorder(createTitledBorder("Kernel"));
        kernelPanel.setAlignmentX(LEFT_ALIGNMENT);
        parentBox.add(kernelPanel);

        // this must come after adding the textFields to the box
        var minimumSize = kernelPanel.getMinimumSize();
        kernelPanel.setPreferredSize(new Dimension(
            matrixOrder * PREFERRED_TEXTFIELD_WIDTH,
            minimumSize.height));
    }

    private void addNormalizeButton(Box parentBox) {
        normalizeButton = new JButton("Normalize (preserve brightness)");
        normalizeButton.addActionListener(this::onUserAction);
        normalizeButton.setAlignmentX(LEFT_ALIGNMENT);
        parentBox.add(normalizeButton);
    }

    private void addExecuteButton(Box parentBox) {
        JButton runButton = new JButton("Apply Filter");
        runButton.setToolTipText("Applies the filter with the current kernel values.");
        runButton.addActionListener(this::onUserAction);
        parentBox.add(runButton);
    }

    private void initPresetPanel() {
        presetsBox = Box.createVerticalBox();
        presetsBox.setBorder(createTitledBorder(DialogMenuBar.PRESETS));

        if (matrixOrder == 3) {
            init3x3Presets();
        } else if (matrixOrder == 5) {
            init5x5Presets();
        } else {
            throw new IllegalStateException("matrixOrder = " + matrixOrder);
        }

        presetsBox.add(Box.createVerticalStrut(20));

        JButton randomizeButton = new JButton("Randomize");
        randomizeButton.addActionListener(e -> {
            loadKernel(Convolve.createRandomKernel(matrixOrder));
            onUserAction(e);
        });
        presetsBox.add(randomizeButton);

        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(e -> {
            resetKernel(matrixOrder);
            onUserAction(e);
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
            onUserAction(e);
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

    private void setupKernelTextField(JTextField textField) {
        kernelPanel.add(textField);
        textField.addActionListener(this::onUserAction);
    }

    private void collectKernelValues() {
        float sum = 0;
        float[] values = new float[matrixOrder * matrixOrder];
        for (int i = 0; i < values.length; i++) {
            String s = kernelTextFields[i].getText();
            try {
                values[i] = parseUserInput(s);
            } catch (NumberFormatException ex) {
                Messages.showError("Invalid Input", ex.getMessage(), this);
                return;
            }
            sum += values[i];
        }
        toggleNormalizeButton(sum);

        if (lastEventSource == normalizeButton && sum != 0.0f) {
            for (int i = 0; i < values.length; i++) {
                values[i] /= sum;
            }
            loadKernel(values);
        }

        ((Convolve) this.filter).setKernelMatrix(values);
    }

    private void onUserAction(ActionEvent event) {
        lastEventSource = event.getSource();
        startPreview(false);
    }

    @Override
    public void startPreview(boolean firstPreview) {
        collectKernelValues();
        super.startPreview(firstPreview);
    }

    private void loadKernel(float[] values) {
        assert values.length == matrixOrder * matrixOrder;

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
        boolean notZero = sum < -0.003f || sum > 0.003f;
        boolean notOne = sum < 0.997f || sum > 1.003f;

        normalizeButton.setEnabled(notZero && notOne);
    }
}
