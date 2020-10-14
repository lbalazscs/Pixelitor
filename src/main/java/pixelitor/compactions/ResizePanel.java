/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.compactions;

import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.OpenImages;
import pixelitor.gui.utils.*;
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

import static java.awt.FlowLayout.LEFT;
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
    private static final int NUM_TF_COLUMNS = 5;

    private final Validator widthValidator = new Validator("Width");
    private final Validator heightValidator = new Validator("Height");
    private final TitledBorder titledBorder;
    private String arrowString;

    private ResizePanel(Canvas canvas) {
        oldWidth = canvas.getWidth();
        oldHeight = canvas.getHeight();

        origProportion = ((double) oldWidth) / oldHeight;
        newWidth = oldWidth;
        newHeight = oldHeight;
        newWidthInPercent = 100.0;
        newHeightInPercent = 100.0;

        String[] items = {"pixels", "percent"};
        var sharedComboBoxModel = new DefaultComboBoxModel<>(items);
        var p = new JPanel();
        p.setLayout(new GridBagLayout());

        var gbh = new GridBagHelper(p);

        widthTF = new JTextField(NUM_TF_COLUMNS);
        widthTF.setName("widthTF");
        widthTF.addKeyListener(this);
        updateWidthTextPixels();
        pixelPercentChooser1 = new JComboBox<>(sharedComboBoxModel);
        var widthLayer = new JLayer<>(widthTF,
            TFValidationLayerUI.fromValidator(widthValidator));
        gbh.addLabelAndTwoControls("Width:", widthLayer, pixelPercentChooser1);

        heightTF = new JTextField(NUM_TF_COLUMNS);
        heightTF.setName("heightTF");
        updateHeightTextPixels();
        heightTF.addKeyListener(this);
        var pixelPercentChooser2 = new JComboBox<>(sharedComboBoxModel);
        var heightLayer = new JLayer<>(heightTF,
            TFValidationLayerUI.fromValidator(heightValidator));
        gbh.addLabelAndTwoControls("Height:", heightLayer, pixelPercentChooser2);

        titledBorder = BorderFactory.createTitledBorder("");
        updateStatusLine();
        p.setBorder(titledBorder);
        Box verticalBox = Box.createVerticalBox();
        verticalBox.add(p);

        JPanel p2 = new JPanel();
        constrainProportionsCB = new JCheckBox("Keep Proportions");
        constrainProportionsCB.setSelected(true);
        p2.add(constrainProportionsCB);
        p2.setLayout(new FlowLayout(LEFT));
        verticalBox.add(p2);
        add(verticalBox);

        pixelPercentChooser1.addItemListener(this);
        pixelPercentChooser2.addItemListener(this);

        constrainProportionsCB.addItemListener(this);
    }

    @Override
    public void updateUI() {
        super.updateUI();

        if (Themes.getCurrent().isNimbus()) {
            arrowString = " \u2794 ";
        } else {
            arrowString = " to ";
        }
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
                updateHeightTextPixels();
            } else {
                updateHeightTextPercent();
            }
        }
    }

    private void unitChanged() {
        if (unitsArePixels()) {
            widthValidator.setPixels(true);
            heightValidator.setPixels(true);
            updateWidthTextPixels();
            updateHeightTextPixels();
        } else {
            widthValidator.setPixels(false);
            heightValidator.setPixels(false);
            updateWidthTextPercent();
            updateHeightTextPercent();
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
            updateStatusLine();
        } else if (e.getSource() == heightTF) {
            keyReleasedInHeightTF();
            updateStatusLine();
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
                    updateHeightTextPixels();
//                    newHeightInPercent = ((double) newHeight) * 100 / oldHeight;
                    newHeightInPercent = newWidthInPercent;
                }
            } catch (NumberFormatException ex) {
                resetWidthPixels();
            }
        } else { // percent was selected
            try {
                newWidthInPercent = parseDouble(getWidthText());
                newWidth = (int) (oldWidth * newWidthInPercent / 100);
                if (constrainProportions()) {
                    newHeight = (int) (newWidth / origProportion);
                    newHeightInPercent = newWidthInPercent;
                    updateHeightTextPercent();
                }
            } catch (NumberFormatException e) {
                resetWidthInPercent();
            }
        }
    }

    private void resetWidthPixels() {
        if (widthIsEmpty()) {
            newWidth = -1;
        } else if (newWidth > 0) {
            updateWidthTextPixels();
        }
    }

    private void resetWidthInPercent() {
        if (widthIsEmpty()) {
            newWidth = -1;
        } else if (newWidthInPercent > 0) {
            updateWidthTextPercent();
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
                    updateWidthTextPixels();
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
                    updateWidthTextPercent();
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
            updateHeightTextPixels();
        }
    }

    private void resetHeightInPercent() {
        if (heightIsEmpty()) {
            newHeight = -1;
        } else if (newHeightInPercent > 0) {
            updateHeightTextPercent();
        }
    }

    private void updateWidthTextPixels() {
        widthTF.setText(String.valueOf(newWidth));
    }

    private void updateHeightTextPixels() {
        heightTF.setText(String.valueOf(newHeight));
    }

    private void updateWidthTextPercent() {
        widthTF.setText(doubleFormatter.format(newWidthInPercent));
    }

    private void updateHeightTextPercent() {
        heightTF.setText(doubleFormatter.format(newHeightInPercent));
    }

    private void updateStatusLine() {
        String oldSize = oldWidth + "\u00d7" + oldHeight + arrowString;
        if (newWidth > 0 && newHeight > 0) {
            titledBorder.setTitle(oldSize + newWidth + "\u00d7" + newHeight);
        } else {
            titledBorder.setTitle(oldSize + "??");
        }

        repaint();
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
        var comp = OpenImages.getActiveCompOpt()
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
                return TextFieldValidator.hasPositiveInt(textField, label, false);
            } else {
                return TextFieldValidator.hasPositiveDouble(textField, label);
            }
        }
    }
}