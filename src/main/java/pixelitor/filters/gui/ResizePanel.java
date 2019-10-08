/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.filters.comp.Resize;
import pixelitor.gui.OpenComps;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.GridBagHelper;
import pixelitor.gui.utils.TFValidationLayerUI;
import pixelitor.gui.utils.TextFieldValidator;
import pixelitor.gui.utils.ValidatedPanel;
import pixelitor.gui.utils.ValidationResult;
import pixelitor.utils.Utils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import static java.lang.Integer.parseInt;

/**
 * The GUI for the resize dialog
 */
@SuppressWarnings("SuspiciousNameCombination")
public class ResizePanel extends ValidatedPanel implements KeyListener, ItemListener {
    private static final NumberFormat doubleFormatter = new DecimalFormat("#0.00");

    private final JCheckBox constrainProportionsCB;
    private final JComboBox<String> pixelPercentChooser1;
    private final JTextField heightTF;
    private final JTextField widthTF;
    private final double origProportion;
    private int newWidth;
    private int newHeight;
    private double newWidthInPercent;
    private double newHeightInPercent;
    private final int oldWidth;
    private final int oldHeight;
    private static final int NR_OF_COLUMNS = 5;

    private final Validator widthValidator = new Validator("Width");
    private final Validator heightValidator = new Validator("Height");
    private final TitledBorder titledBorder;

    private ResizePanel(Canvas canvas) {
        oldWidth = canvas.getImWidth();
        oldHeight = canvas.getImHeight();

        origProportion = ((double) oldWidth) / oldHeight;
        newWidth = oldWidth;
        newHeight = oldHeight;
        newWidthInPercent = 100.0;
        newHeightInPercent = 100.0;

        String[] items = {"pixels", "percent"};
        ComboBoxModel<String> comboBoxModel = new DefaultComboBoxModel<>(items);
        JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());

        GridBagHelper gbh = new GridBagHelper(p);

        widthTF = new JTextField(NR_OF_COLUMNS);
        widthTF.setName("widthTF");
        widthTF.addKeyListener(this);
        widthTF.setText(String.valueOf(oldWidth));
        pixelPercentChooser1 = new JComboBox<>(comboBoxModel);
        JLayer<JTextField> widthLayer = new JLayer<>(widthTF,
                new TFValidationLayerUI(widthValidator));
        gbh.addLabelWithTwoControls("Width:", widthLayer, pixelPercentChooser1);

        heightTF = new JTextField(NR_OF_COLUMNS);
        heightTF.setName("heightTF");
        heightTF.setText(String.valueOf(oldHeight));
        heightTF.addKeyListener(this);
        JComboBox<String> pixelPercentChooser2 = new JComboBox<>(comboBoxModel);
        JLayer<JTextField> heightLayer = new JLayer<>(heightTF,
                new TFValidationLayerUI(heightValidator));
        gbh.addLabelWithTwoControls("Height:", heightLayer, pixelPercentChooser2);

        titledBorder = BorderFactory.createTitledBorder("");
        updateInfo();
        p.setBorder(titledBorder);
        Box verticalBox = Box.createVerticalBox();
        verticalBox.add(p);

        JPanel p2 = new JPanel();
        constrainProportionsCB = new JCheckBox("Constrain Proportions");
        constrainProportionsCB.setSelected(true);
        p2.add(constrainProportionsCB);
        p2.setLayout(new FlowLayout(FlowLayout.LEFT));
        verticalBox.add(p2);
        add(verticalBox);

        pixelPercentChooser1.addItemListener(this);
        pixelPercentChooser2.addItemListener(this);

        constrainProportionsCB.addItemListener(this);
    }

    private void updateInfo() {
        String oldSize = oldWidth + "\u00d7" + oldHeight + " \u2794 ";
        if (newWidth > 0 && newHeight > 0) {
            titledBorder.setTitle(oldSize + newWidth + "\u00d7" + newHeight);
        } else {
            titledBorder.setTitle(oldSize + "??");
        }

        repaint();
    }

    private boolean unitsArePixels() {
        return pixelPercentChooser1.getSelectedIndex() == 0;
    }

    private boolean constrainProportions() {
        return constrainProportionsCB.isSelected();
    }

    private static double parseDouble(String s) {
        if (s.isEmpty()) {
            return 0;
        }
        return Utils.parseDouble(s);
    }

    // a combo box or a checkbox was used
    @Override
    public void itemStateChanged(ItemEvent e) {
        if (e.getSource() == constrainProportionsCB) {
            constrainProportionsSettingChanged();
        } else { // one of the combo boxes was selected
            unitChanged();
        }
    }

    private void constrainProportionsSettingChanged() {
        if (constrainProportions()) {
            // it just got selected, adjust the height to the width
            newHeight = (int) (newWidth / origProportion);
            newHeightInPercent = newWidthInPercent;
            if (unitsArePixels()) {
                heightTF.setText(String.valueOf(newHeight));
            } else {
                heightTF.setText(doubleFormatter.format(newHeightInPercent));
            }
        }
    }

    private void unitChanged() {
        if (unitsArePixels()) {
            widthValidator.setPixels(true);
            heightValidator.setPixels(true);
            widthTF.setText(String.valueOf(newWidth));
            heightTF.setText(String.valueOf(newHeight));
        } else {
            widthValidator.setPixels(false);
            heightValidator.setPixels(false);
            widthTF.setText(doubleFormatter.format(newWidthInPercent));
            heightTF.setText(doubleFormatter.format(newHeightInPercent));
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getSource() == widthTF) {
            keyReleasedInWidthTF();
            updateInfo();
        } else if (e.getSource() == heightTF) {
            keyReleasedInHeightTF();
            updateInfo();
        }
    }

    private void keyReleasedInWidthTF() {
        if (unitsArePixels()) {
            try {
                newWidth = parseInt(getWidthText());
                newWidthInPercent = ((double) newWidth) * 100 / oldWidth;
                if (constrainProportions()) {
                    newHeight = (int) (newWidth / origProportion);
                    if (newHeight == 0) {
                        newHeight = 1;
                    }
                    heightTF.setText(String.valueOf(newHeight));
//                    newHeightInPercent = ((double) newHeight) * 100 / oldHeight;
                    newHeightInPercent = newWidthInPercent;
                }
            } catch (NumberFormatException ex) {
                resetWidth();
            }
        } else { // percent was selected
            try {
                newWidthInPercent = parseDouble(getWidthText());
                newWidth = (int) (oldWidth * newWidthInPercent / 100);
                if (constrainProportions()) {
                    newHeight = (int) (newWidth / origProportion);
                    newHeightInPercent = newWidthInPercent;
                    heightTF.setText(doubleFormatter.format(newHeightInPercent));
                }
            } catch (NumberFormatException e) {
                resetWidthInPercent();
            }
        }
    }

    private void resetWidth() {
        if (widthIsEmpty()) {
            newWidth = -1;
        } else if (newWidth > 0) {
            widthTF.setText(String.valueOf(newWidth));
        }
    }

    private void resetWidthInPercent() {
        if (widthIsEmpty()) {
            newWidth = -1;
        } else if (newWidthInPercent > 0) {
            widthTF.setText(doubleFormatter.format(newWidthInPercent));
        }
    }

    private void keyReleasedInHeightTF() {
        if (unitsArePixels()) {
            try {
                newHeight = parseInt(getHeightText());
                newHeightInPercent = ((double) newHeight) * 100 / oldHeight;
                if (constrainProportions()) {
                    newWidth = (int) (newHeight * origProportion);
                    if (newWidth == 0) {
                        newWidth = 1;
                    }
                    widthTF.setText(String.valueOf(newWidth));
                    newWidthInPercent = newHeightInPercent;
                }
            } catch (NumberFormatException ex) {
                resetHeight();
            }
        } else {  // percent was selected
            try {
                newHeightInPercent = parseDouble(getHeightText());
                newHeight = (int) (oldHeight * newHeightInPercent / 100);
                if (constrainProportions()) {
                    newWidth = (int) (newHeight * origProportion);
                    newWidthInPercent = newHeightInPercent;
                    widthTF.setText(doubleFormatter.format(newWidthInPercent));
                }
            } catch (NumberFormatException e) {
                resetHeightInPercent();
            }
        }
    }

    private void resetHeight() {
        if (heightIsEmpty()) {
            newHeight = -1;
        } else if (newHeight > 0) {
            heightTF.setText(String.valueOf(newHeight)); // reset
        }
    }

    private void resetHeightInPercent() {
        if (heightIsEmpty()) {
            newHeight = -1;
        } else if (newHeightInPercent > 0) {
            heightTF.setText(doubleFormatter.format(newHeightInPercent));
        }
    }

    @Override
    public ValidationResult checkValidity() {
        return widthValidator.check(widthTF)
                .and(heightValidator.check(heightTF));
    }

    private String getWidthText() {
        return widthTF.getText().trim();
    }

    private String getHeightText() {
        return heightTF.getText().trim();
    }

    private boolean widthIsEmpty() {
        return getWidthText().isEmpty();
    }

    private boolean heightIsEmpty() {
        return getHeightText().isEmpty();
    }

    private int getNewWidth() {
        return newWidth;
    }

    private int getNewHeight() {
        return newHeight;
    }

    public static void showInDialog(Composition comp) {
        ResizePanel p = new ResizePanel(comp.getCanvas());
        new DialogBuilder()
                .validatedContent(p)
                .title("Resize")
                .okAction(() -> new Resize(p.getNewWidth(), p.getNewHeight(), false)
                        .process(comp))
                .show();
    }

    public static void resizeActiveImage() {
        Composition comp = OpenComps.getActiveComp()
                .orElseThrow(() -> new IllegalStateException("no active image"));
        showInDialog(comp);
    }

    /**
     * A textfield validator that can validate
     * either int or double textfields
     */
    static class Validator implements TextFieldValidator {
        private boolean pixels = true;
        private final String label;

        public Validator(String label) {
            this.label = label;
        }

        public void setPixels(boolean pixels) {
            this.pixels = pixels;
        }

        @Override
        public ValidationResult check(JTextField textField) {
            if (pixels) {
                return TextFieldValidator.hasValidPositiveInt(label, textField, false);
            } else {
                return TextFieldValidator.hasValidPositiveDouble(label, textField);
            }
        }
    }
}