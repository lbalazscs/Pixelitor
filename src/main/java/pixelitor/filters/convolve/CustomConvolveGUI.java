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

package pixelitor.filters.convolve;

import pixelitor.filters.gui.DialogMenuBar;
import pixelitor.filters.gui.FilterGUI;
import pixelitor.layers.Drawable;
import pixelitor.utils.Messages;
import pixelitor.utils.NotANumberException;
import pixelitor.utils.Utils;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;

import static javax.swing.BorderFactory.createTitledBorder;
import static javax.swing.BoxLayout.X_AXIS;

/**
 * An adjustment panel for customizable convolutions
 */
public class CustomConvolveGUI extends FilterGUI {
    private static final int TEXTFIELD_PREFERRED_WIDTH = 70;

    private JTextField[] textFields;
    private JPanel matrixPanel;
    private JButton normalizeButton;
    private Box presetsBox;
    private final int matrixOrder;

    public CustomConvolveGUI(Convolve filter, Drawable dr, boolean reset) {
        super(filter, dr);
        setLayout(new BoxLayout(this, X_AXIS));

        matrixOrder = filter.getMatrixOrder();

        initLeftVerticalBox();
        initPresetBox();

        if (reset) {
            reset(matrixOrder);
        } else {
            // use the last values
            float[] kernelMatrix = filter.getKernelMatrix();
            if (kernelMatrix == null) {
                reset(matrixOrder);
            } else {
                setMatrix(kernelMatrix);
                collectValuesAndRun(null);
            }
        }
    }

    private void initLeftVerticalBox() {
        Box box = Box.createVerticalBox();

        addTextFieldsPanel(box);
        addNormalizeButton(box);
        addRunButton(box);

        box.add(Box.createVerticalStrut(20));

        box.setMaximumSize(box.getPreferredSize());
        box.setAlignmentY(TOP_ALIGNMENT);

        add(box);
    }

    private void addTextFieldsPanel(Box leftVerticalBox) {
        matrixPanel = new JPanel(new GridLayout(matrixOrder, matrixOrder));
        textFields = new JTextField[matrixOrder * matrixOrder];
        for (int i = 0; i < textFields.length; i++) {
            textFields[i] = new JTextField();
        }
        for (var textField : textFields) {
            setupTextField(textField);
        }
        matrixPanel.setBorder(createTitledBorder("Kernel"));
        matrixPanel.setAlignmentX(LEFT_ALIGNMENT);
        leftVerticalBox.add(matrixPanel);

        // this must come after adding the textFieldsP to the box
        var minimumSize = matrixPanel.getMinimumSize();
        matrixPanel.setPreferredSize(new Dimension(
            matrixOrder * TEXTFIELD_PREFERRED_WIDTH, minimumSize.height));
    }

    private void addNormalizeButton(Box leftVerticalBox) {
        normalizeButton = new JButton("Normalize (preserve brightness)");
        normalizeButton.addActionListener(this::collectValuesAndRun);
        normalizeButton.setAlignmentX(LEFT_ALIGNMENT);
        leftVerticalBox.add(normalizeButton);
    }

    private void addRunButton(Box leftVerticalBox) {
        JButton runButton = new JButton("Run");
        runButton.setToolTipText("Run the filter with the current values.");
        runButton.addActionListener(this::collectValuesAndRun);
        leftVerticalBox.add(runButton);
    }

    private void initPresetBox() {
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
            setMatrix(Convolve.createRandomKernelMatrix(matrixOrder));
            collectValuesAndRun(e);
        });
        presetsBox.add(randomizeButton);

        JButton doNothingButton = new JButton("Do Nothing");
        doNothingButton.addActionListener(e -> {
            reset(matrixOrder);
            collectValuesAndRun(e);
        });
        presetsBox.add(doNothingButton);

        presetsBox.setMaximumSize(presetsBox.getPreferredSize());
        presetsBox.setAlignmentY(TOP_ALIGNMENT);

        add(presetsBox);
    }

    private void initPreset(String name, float[] kernel) {
        JButton button = new JButton(name);
        button.addActionListener(e -> {
            setMatrix(kernel);
            collectValuesAndRun(e);
        });
        presetsBox.add(button);
    }

    private void init5x5Presets() {
        initPreset("Diamond Blur", new float[]{
            0.0f, 0.0f, 0.077f, 0.0f, 0.0f,
            0.0f, 0.077f, 0.077f, 0.077f, 0.0f,
            0.077f, 0.077f, 0.077f, 0.077f, 0.077f,
            0.0f, 0.077f, 0.077f, 0.077f, 0.0f,
            0.0f, 0.0f, 0.077f, 0.0f, 0.0f,
        });

        initPreset("Motion Blur", new float[]{
            0.0f, 0.0f, 0.0f, 0.0f, 0.2f,
            0.0f, 0.0f, 0.0f, 0.2f, 0.0f,
            0.0f, 0.0f, 0.2f, 0.0f, 0.0f,
            0.0f, 0.2f, 0.0f, 0.0f, 0.0f,
            0.2f, 0.0f, 0.0f, 0.0f, 0.0f,
        });

        initPreset("Find Horizontal Edges", new float[]{
            0.0f, 0.0f, -1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, -2.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 6.0f, 0.0f, 0.0f,
            0.0f, 0.0f, -2.0f, 0.0f, 0.0f,
            0.0f, 0.0f, -1.0f, 0.0f, 0.0f,
        });

        initPreset("Find Vertical Edges", new float[]{
            0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
            -1.0f, -2.0f, 6.0f, -2.0f, -1.0f,
            0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
        });

        initPreset("Find Diagonal Edges", new float[]{
            0.0f, 0.0f, 0.0f, 0.0f, -1.0f,
            0.0f, 0.0f, 0.0f, -2.0f, 0.0f,
            0.0f, 0.0f, 6.0f, 0.0f, 0.0f,
            0.0f, -2.0f, 0.0f, 0.0f, 0.0f,
            -1.0f, 0.0f, 0.0f, 0.0f, 0.0f,
        });

        initPreset("Find Diagonal Edges 2", new float[]{
            -1.0f, 0.0f, 0.0f, 0.0f, 0.0f,
            0.0f, -2.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 6.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 0.0f, -2.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 0.0f, -1.0f,
        });

        initPreset("Sharpen", new float[]{
            -0.125f, -0.125f, -0.125f, -0.125f, -0.125f,
            -0.125f, 0.25f, 0.25f, 0.25f, -0.125f,
            -0.125f, 0.25f, 1.0f, 0.25f, -0.125f,
            -0.125f, 0.25f, 0.25f, 0.25f, -0.125f,
            -0.125f, -0.125f, -0.125f, -0.125f, -0.125f,
        });
    }

    private void init3x3Presets() {
        initPreset("Corner Blur", new float[]{
            0.25f, 0.0f, 0.25f,
            0.0f, 0.0f, 0.0f,
            0.25f, 0.0f, 0.25f});

        initPreset("\"Gaussian\" Blur", new float[]{
            1 / 16.0f, 2 / 16.0f, 1 / 16.0f,
            2 / 16.0f, 4 / 16.0f, 2 / 16.0f,
            1 / 16.0f, 2 / 16.0f, 1 / 16.0f});

        initPreset("Mean Blur", new float[]{
            0.1115f, 0.1115f, 0.1115f,
            0.1115f, 0.1115f, 0.1115f,
            0.1115f, 0.1115f, 0.1115f});

        initPreset("Sharpen", new float[]{
            0, -1, 0,
            -1, 5, -1,
            0, -1, 0});

        initPreset("Sharpen 2", new float[]{
            -1, -1, -1,
            -1, 9, -1,
            -1, -1, -1});

        initPreset("Edge Detection", new float[]{
            0, -1, 0,
            -1, 4, -1,
            0, -1, 0});

        initPreset("Edge Detection 2", new float[]{
            -1, -1, -1,
            -1, 8, -1,
            -1, -1, -1});

        initPreset("Horizontal Edge Detection", new float[]{
            -1, -1, -1,
            0, 0, 0,
            1, 1, 1});

        initPreset("Vertical Edge Detection", new float[]{
            -1, 0, 1,
            -1, 0, 1,
            -1, 0, 1});

        initPreset("Emboss", new float[]{
            -2, -2, 0,
            -2, 6, 0,
            0, 0, 0});

        initPreset("Emboss 2", new float[]{
            -2, 0, 0,
            0, 0, 0,
            0, 0, 2});

        initPreset("Color Emboss", new float[]{
            -1, -1, 0,
            -1, 1, 1,
            0, 1, 1});
    }

    private void reset(int size) {
        float[] defaultValues = new float[size * size];
        defaultValues[defaultValues.length / 2] = 1.0f;

        setMatrix(defaultValues);
    }

    private void setupTextField(JTextField textField) {
        matrixPanel.add(textField);
        textField.addActionListener(this::collectValuesAndRun);
    }

    private void collectValuesAndRun(ActionEvent e) {
        float sum = 0;
        float[] values = new float[matrixOrder * matrixOrder];
        for (int i = 0; i < values.length; i++) {
            String s = textFields[i].getText();
            try {
                values[i] = Utils.string2float(s);
            } catch (NotANumberException ex) {
                Messages.showError("Wrong Number Format", ex.getMessage(), this);
                return;
            }
            sum += values[i];
        }
        enableNormalizeButton(sum);

        if (e != null && e.getSource() == normalizeButton && sum != 0.0f) {
            for (int i = 0; i < values.length; i++) {
                values[i] /= sum;
            }

            setMatrix(values);
        }

        Convolve convolve = (Convolve) this.filter;
        convolve.setKernelMatrix(values);
        runFilterPreview();
    }

    private void setMatrix(float[] values) {
        assert values.length == matrixOrder * matrixOrder;

        float sum = 0;
        for (int i = 0; i < textFields.length; i++) {
            textFields[i].setText(Utils.float2String(values[i]));
            sum += values[i];
        }

        enableNormalizeButton(sum);
    }

    private void enableNormalizeButton(float sum) {
        boolean notZero = sum < -0.003f || sum > 0.003f;
        boolean notOne = sum < 0.997f || sum > 1.003f;

        normalizeButton.setEnabled(notZero && notOne);
    }
}
