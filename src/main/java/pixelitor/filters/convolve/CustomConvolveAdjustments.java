/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

import org.jdesktop.swingx.combobox.EnumComboBoxModel;
import pixelitor.filters.gui.AdjustPanel;
import pixelitor.layers.ImageLayer;
import pixelitor.utils.Messages;
import pixelitor.utils.NotANumberException;
import pixelitor.utils.Utils;

import javax.swing.*;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * An adjustment panel for customizable convolutions
 */
public class CustomConvolveAdjustments extends AdjustPanel implements ActionListener {
    private static final int TEXTFIELD_PREFERRED_WIDTH = 70;

    private JTextField[] textFields;

    private JPanel textFieldsPanel;
    private JButton normalizeButton;
    private Box presetsBox;
    private final int size;

    public CustomConvolveAdjustments(Convolve filter, ImageLayer layer) {
        super(filter, layer);
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        size = filter.getSize();

        initLeftVerticalBox(filter);
        initPresetBox();

        reset(size);
    }

    private void initLeftVerticalBox(Convolve filter) {
        Box leftVerticalBox = Box.createVerticalBox();

        textFieldsPanel = new JPanel();
        textFields = new JTextField[size * size];
        for (int i = 0; i < textFields.length; i++) {
            textFields[i] = new JTextField();
        }
        textFieldsPanel.setLayout(new GridLayout(size, size));
        for (JTextField textField : textFields) {
            setupTextField(textField);
        }
        textFieldsPanel.setBorder(BorderFactory.createTitledBorder("Kernel"));
        textFieldsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftVerticalBox.add(textFieldsPanel);

        // these two lines must come after adding the textFieldsPanel to the box
        Dimension minimumSize = textFieldsPanel.getMinimumSize();
        textFieldsPanel.setPreferredSize(new Dimension(size * TEXTFIELD_PREFERRED_WIDTH, minimumSize.height));

        normalizeButton = new JButton("Normalize (preserve brightness)");
        normalizeButton.addActionListener(this);
        normalizeButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftVerticalBox.add(normalizeButton);

        JButton tryButton = new JButton("Try");
        tryButton.addActionListener(this);
        leftVerticalBox.add(tryButton);

        leftVerticalBox.add(Box.createVerticalStrut(20));

        JLabel cmLabel = new JLabel("Convolution method:", JLabel.LEFT);
        cmLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftVerticalBox.add(cmLabel);
        EnumComboBoxModel<ConvolveMethod> convolveMethodModel = filter.getConvolveMethodModel();
        JComboBox<ConvolveMethod> convolveMethodCB = new JComboBox<>(convolveMethodModel);

        convolveMethodCB.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftVerticalBox.add(convolveMethodCB);
        convolveMethodCB.addActionListener(this);

        leftVerticalBox.setMaximumSize(leftVerticalBox.getPreferredSize());
        leftVerticalBox.setAlignmentY(Component.TOP_ALIGNMENT);

        add(leftVerticalBox);
    }

    private void initPresetBox() {
        presetsBox = Box.createVerticalBox();
        presetsBox.setBorder(BorderFactory.createTitledBorder("Presets"));

        if (size == 3) {
            init3x3Presets();
        } else if (size == 5) {
            init5x5Presets();
        } else {
            throw new IllegalStateException("size = " + size);
        }

        presetsBox.add(Box.createVerticalStrut(20));

        JButton randomizeButton = new JButton("Randomize");
        randomizeButton.addActionListener(e -> {
            setValues(Convolve.getRandomKernelMatrix(size));
            actionPerformed(e);
        });
        presetsBox.add(randomizeButton);

        JButton doNothingButton = new JButton("Do Nothing");
        doNothingButton.addActionListener(e -> {
            reset(size);
            actionPerformed(e);
        });
        presetsBox.add(doNothingButton);

        presetsBox.setMaximumSize(presetsBox.getPreferredSize());
        presetsBox.setAlignmentY(Component.TOP_ALIGNMENT);

        add(presetsBox);
    }

    private void initPreset(String name, float[] kernel) {
        JButton button = new JButton(name);
        button.addActionListener(e -> {
            setValues(kernel);
            actionPerformed(e);
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
                0.0f, 0.0f, 0.0f, 0.0f,  -1.0f,
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
        initPreset("\"Corner\" Blur", (new float[]{
                0.25f, 0.0f, 0.25f,
                0.0f, 0.0f, 0.0f,
                0.25f, 0.0f, 0.25f}));

        initPreset("\"Gaussian\" Blur", new float[]{
                1 / 16.0f, 2 / 16.0f, 1 / 16.0f,
                2 / 16.0f, 4 / 16.0f, 2 / 16.0f,
                1 / 16.0f, 2 / 16.0f, 1 / 16.0f});

        initPreset("Mean Filter", new float[]{
                0.1115f, 0.1115f, 0.1115f,
                0.1115f, 0.1115f, 0.1115f,
                0.1115f, 0.1115f, 0.1115f});


        initPreset("Sharpen", new float[]{
                0, -1, 0,
                -1, 5, -1,
                0, -1, 0});

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
                0,  0,  0,
                1,  1,  1});

        initPreset("Vertical Edge Detection", new float[]{
                -1,  0,  1,
                -1,  0,  1,
                -1,  0,  1});

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
        for (int i = 0; i < defaultValues.length; i++) {
            defaultValues[i] = 0.0f;
        }
        defaultValues[defaultValues.length / 2] = 1.0f;

        setValues(defaultValues);
    }

    private void setupTextField(JTextField textField) {
        textFieldsPanel.add(textField);
        textField.addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        float sum = 0;
        float[] values = new float[size * size];
        for (int i = 0; i < values.length; i++) {
            String s = textFields[i].getText();
            try {
                values[i] = Utils.string2float(s);
            } catch (NotANumberException ex) {
                Messages.showError("Wrong Number Format", ex.getMessage());
            }
            sum += values[i];
        }
        enableNormalizeButton(sum);

        if (e.getSource() == normalizeButton) {
            if (sum != 0.0f) {
                for (int i = 0; i < values.length; i++) {
                    values[i] /= sum;
                }

                setValues(values);
            }
        }
        Convolve kernelFilter = (Convolve) filter;
        kernelFilter.setKernelMatrix(values);
        super.executeFilterPreview();
    }

    private void setValues(float[] values) {
        assert values.length == size * size;

        float sum = 0;
        for (int i = 0; i < textFields.length; i++) {
            JTextField textField = textFields[i];
            textField.setText(Utils.float2String(values[i]));
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
