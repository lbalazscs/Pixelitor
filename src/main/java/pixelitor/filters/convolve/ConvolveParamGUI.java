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

import pixelitor.filters.gui.ParamGUI;
import pixelitor.utils.Messages;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.GridLayout;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static javax.swing.BorderFactory.createTitledBorder;
import static javax.swing.BoxLayout.X_AXIS;

/**
 * The GUI for a {@link ConvolveParam}: a grid of text fields for the
 * kernel values, and a column of buttons for the built-in kernels.
 * <p>
 * Valid edits in the text fields are copied into the model immediately
 * (without triggering the filter), so that presets and animation states
 * always see the typed values. The filter is triggered on Enter or "Apply Filter".
 * The model keeps full precision: the text fields only show rounded values,
 * and a cell overwrites its model value only when the user edits that cell.
 */
public class ConvolveParamGUI extends JPanel implements ParamGUI {
    private static final float EPSILON = 0.003f;

    private final ConvolveParam model;
    private final int kernelSize;

    private JTextField[] kernelTextFields;
    private JPanel kernelPanel;
    private JButton normalizeButton;
    private JButton applyButton;
    private final List<JButton> kernelButtons = new ArrayList<>();

    // true while the text fields are updated from the model,
    // to ignore the document events caused by this update
    private boolean updatingFromModel = false;

    public ConvolveParamGUI(ConvolveParam model) {
        this.model = model;
        kernelSize = model.getKernelSize();

        setLayout(new BoxLayout(this, X_AXIS));

        initLeftPanel();
        initKernelsPanel();

        updateGUI();
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
        addApplyButton(leftPanel);

        leftPanel.add(Box.createVerticalStrut(20));
        leftPanel.setMaximumSize(leftPanel.getPreferredSize());
        leftPanel.setAlignmentY(TOP_ALIGNMENT);

        add(leftPanel);
    }

    private void setupKernelPanel(Box parentBox) {
        kernelPanel = new JPanel(new GridLayout(kernelSize, kernelSize));

        kernelTextFields = new JTextField[kernelSize * kernelSize];
        for (int i = 0; i < kernelTextFields.length; i++) {
            kernelTextFields[i] = createKernelTextField(i);
        }

        kernelPanel.setBorder(createTitledBorder("Kernel"));
        kernelPanel.setAlignmentX(LEFT_ALIGNMENT);
        parentBox.add(kernelPanel);
    }

    private JTextField createKernelTextField(int index) {
        JTextField textField = new JTextField();
        textField.setColumns(5);
        textField.setHorizontalAlignment(JTextField.RIGHT);
        kernelPanel.add(textField);

        textField.addActionListener(_ -> applyIfInputValid());
        textField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                cellEdited(index);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                cellEdited(index);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                cellEdited(index);
            }
        });
        return textField;
    }

    private void addNormalizeButton(Box parentBox) {
        normalizeButton = new JButton("Normalize (preserve brightness)");
        normalizeButton.addActionListener(_ -> normalizeKernel());
        normalizeButton.setAlignmentX(LEFT_ALIGNMENT);
        parentBox.add(normalizeButton);
    }

    private void addApplyButton(Box parentBox) {
        applyButton = new JButton("Apply Filter");
        applyButton.setToolTipText("Applies the filter with the current kernel values.");
        applyButton.addActionListener(_ -> applyIfInputValid());
        parentBox.add(applyButton);
    }

    private void initKernelsPanel() {
        Box kernelsBox = Box.createVerticalBox();
        kernelsBox.setBorder(createTitledBorder("Common Kernels"));

        for (ConvolvePresets.KernelPreset preset : ConvolvePresets.forSize(kernelSize)) {
            JButton button = new JButton(preset.name());
            button.addActionListener(_ -> model.setKernel(preset.kernel(), true));
            kernelsBox.add(button);
            kernelButtons.add(button);
        }

        kernelsBox.setMaximumSize(kernelsBox.getPreferredSize());
        kernelsBox.setAlignmentY(TOP_ALIGNMENT);
        add(kernelsBox);
    }

    /**
     * Copies the edited cell into the model if the text is valid.
     * Invalid text is ignored here, and reported on Enter or "Apply Filter".
     */
    private void cellEdited(int index) {
        if (updatingFromModel) {
            return;
        }

        float value;
        try {
            value = parseUserInput(kernelTextFields[index].getText());
        } catch (NumberFormatException e) {
            return;
        }

        float[] values = model.getKernel();
        if (values[index] != value) {
            values[index] = value;
            model.setKernelFromGUI(values);
        }
        updateNormalizeButtonEnabled(model.getSum());
    }

    /**
     * Checks that all text fields contain valid numbers.
     * If not, shows an error message and returns false.
     */
    private boolean validateInput() {
        for (JTextField textField : kernelTextFields) {
            try {
                parseUserInput(textField.getText());
            } catch (NumberFormatException ex) {
                Messages.showError("Invalid Input", ex.getMessage(), this);
                return false;
            }
        }
        return true;
    }

    private void applyIfInputValid() {
        if (validateInput()) {
            // runs the filter even if nothing changed
            model.triggerFilter();
        }
    }

    private void normalizeKernel() {
        if (validateInput()) {
            model.normalize(true);
        }
    }

    @Override
    public void updateGUI() {
        float[] values = model.getKernel();

        updatingFromModel = true;
        try {
            for (int i = 0; i < kernelTextFields.length; i++) {
                JTextField textField = kernelTextFields[i];
                // keep the text if it already represents the model value
                // (avoids rewriting what the user has typed, like "0.5" => "0.500")
                if (!textMatches(textField, values[i])) {
                    textField.setText(formatValue(values[i]));
                }
            }
        } finally {
            updatingFromModel = false;
        }

        updateNormalizeButtonEnabled(model.getSum());
    }

    private static boolean textMatches(JTextField textField, float value) {
        try {
            return parseUserInput(textField.getText()) == value;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static String formatValue(float value) {
        return value == 0.0f ? "" : String.format("%.3f", value);
    }

    /**
     * Enables or disables the normalize button based on the kernel sum.
     */
    private void updateNormalizeButtonEnabled(float sum) {
        boolean notZero = Math.abs(sum) > EPSILON;
        boolean notOne = Math.abs(sum - 1.0f) > EPSILON;

        normalizeButton.setEnabled(isEnabled() && notZero && notOne);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled); // so that isEnabled() works

        for (JTextField textField : kernelTextFields) {
            textField.setEnabled(enabled);
        }
        applyButton.setEnabled(enabled);
        for (JButton button : kernelButtons) {
            button.setEnabled(enabled);
        }
        updateNormalizeButtonEnabled(model.getSum());
    }

    @Override
    public void setToolTip(String tip) {
        kernelPanel.setToolTipText(tip);
        for (JTextField textField : kernelTextFields) {
            textField.setToolTipText(tip);
        }
    }

    @Override
    public int getNumLayoutColumns() {
        return 1;
    }
}
