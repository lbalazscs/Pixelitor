/*
 * Copyright 2018 Laszlo Balazs-Csiki
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

import pixelitor.Composition;
import pixelitor.filters.comp.Resize;
import pixelitor.gui.ImageComponents;
import pixelitor.gui.utils.Dialogs;
import pixelitor.gui.utils.GridBagHelper;
import pixelitor.gui.utils.OKCancelDialog;
import pixelitor.utils.Messages;

import javax.swing.*;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;

/**
 * The GUI for the resize dialog
 */
public class ResizePanel extends JPanel implements KeyListener, ItemListener {
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
    private boolean validData = true; // the defaults are valid
    private String errorMessage;
    private static final int NR_OF_COLUMNS = 5;

    private ResizePanel(Composition comp) {
        oldWidth = comp.getCanvasWidth();
        oldHeight = comp.getCanvasHeight();

        origProportion = ((double) oldWidth) / oldHeight;
        newWidth = oldWidth;
        newHeight = oldHeight;
        newWidthInPercent = 100.0;
        newHeightInPercent = 100.0;

        String[] items = {"pixels", "percent"};
        ComboBoxModel<String> comboBoxModel = new DefaultComboBoxModel(items);
        JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());

        GridBagHelper gbHelper = new GridBagHelper(p);

        widthTF = new JTextField(NR_OF_COLUMNS);
        widthTF.setName("widthTF");
        widthTF.addKeyListener(this);
        widthTF.setText(String.valueOf(oldWidth));
        pixelPercentChooser1 = new JComboBox<>(comboBoxModel);
        gbHelper.addLabelWithTwoControls("Width:", widthTF, pixelPercentChooser1);

        heightTF = new JTextField(NR_OF_COLUMNS);
        heightTF.setName("heightTF");
        heightTF.setText(String.valueOf(oldHeight));
        heightTF.addKeyListener(this);
        JComboBox<String> pixelPercentChooser2 = new JComboBox<>(comboBoxModel);
        gbHelper.addLabelWithTwoControls("Height:", heightTF, pixelPercentChooser2);

        p.setBorder(BorderFactory.createTitledBorder("Resize from " + oldWidth + 'x' + oldHeight));
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

    private boolean pixelsSelected() {
        return (pixelPercentChooser1.getSelectedIndex() == 0);
    }

    private boolean constrainProportions() {
        return constrainProportionsCB.isSelected();
    }

    private static double parseLocalizedDouble(String s) {
        double retVal = 100.0;
        try {
            Number number = doubleFormatter.parse(s);
            retVal = number.doubleValue();
        } catch (ParseException ex) {
            Messages.showException(ex);
        }
        return retVal;
    }

    // a combo box or a checkbox was used
    @Override
    public void itemStateChanged(ItemEvent e) {
        if (e.getSource() == constrainProportionsCB) {
            if (constrainProportions()) {
                // it just got selected, adjust the height to the width
                newHeight = (int) (newWidth / origProportion);
                //noinspection SuspiciousNameCombination
                newHeightInPercent = newWidthInPercent;
                if (pixelsSelected()) {
                    heightTF.setText(String.valueOf(newHeight));
                } else {
                    heightTF.setText(doubleFormatter.format(newHeightInPercent));
                }
            }
        } else { // one of the combo boxes was selected
            if (pixelsSelected()) {
                widthTF.setText(String.valueOf(newWidth));
                heightTF.setText(String.valueOf(newHeight));
            } else {
                widthTF.setText(doubleFormatter.format(newWidthInPercent));
                heightTF.setText(doubleFormatter.format(newHeightInPercent));
            }
        }
//        updateFilter();
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
        validData = true;
        errorMessage = null;
        if (e.getSource() == widthTF) {
            if (pixelsSelected()) {
                try {
                    newWidth = Integer.parseInt(widthTF.getText());
                    if (constrainProportions()) {
                        newHeight = (int) (newWidth / origProportion);
                        if (newHeight == 0) {
                            newHeight = 1;
                        }
                        heightTF.setText(String.valueOf(newHeight));
                        newHeightInPercent = ((double) newHeight) * 100 / oldHeight;
                    }
                    newWidthInPercent = ((double) newWidth) * 100 / oldWidth;
                } catch (NumberFormatException ex) {
                    if (widthTF.getText().trim().isEmpty()) {
                        validData = false;
                        errorMessage = "the 'width' field is empty";
                    } else {
                        widthTF.setText(String.valueOf(newWidth)); // reset
                        Toolkit.getDefaultToolkit().beep();
                    }
                }
            } else { // percent was selected
                newWidthInPercent = parseLocalizedDouble(widthTF.getText());
                newWidth = (int) (oldWidth * newWidthInPercent / 100);
                if (constrainProportions()) {
                    newHeight = (int) (newWidth / origProportion);
                    //noinspection SuspiciousNameCombination
                    newHeightInPercent = newWidthInPercent;
                    heightTF.setText(doubleFormatter.format(newHeightInPercent));
                }
            }
        } else if (e.getSource() == heightTF) {
            if (pixelsSelected()) {
                try {
                    newHeight = Integer.parseInt(heightTF.getText());
                    if (constrainProportions()) {
                        newWidth = (int) (newHeight * origProportion);
                        if (newWidth == 0) {
                            newWidth = 1;
                        }
                        widthTF.setText(String.valueOf(newWidth));
                        newWidthInPercent = parseLocalizedDouble(widthTF.getText());
                    }
                    newHeightInPercent = ((double) newHeight) * 100 / oldHeight;
                } catch (NumberFormatException ex) {
                    if (heightTF.getText().trim().isEmpty()) {
                        validData = false;
                        errorMessage = "the 'height' field is empty";
                    } else {
                        heightTF.setText(String.valueOf(newHeight)); // reset
                        Toolkit.getDefaultToolkit().beep();
                    }
                }
            } else {  // percent was selected
                newHeightInPercent = parseLocalizedDouble(heightTF.getText());
                newHeight = (int) (oldHeight * newHeightInPercent / 100);
                if (constrainProportions()) {
                    newWidth = (int) (newHeight * origProportion);
                    //noinspection SuspiciousNameCombination
                    newWidthInPercent = newHeightInPercent;
                    widthTF.setText(doubleFormatter.format(newWidthInPercent));
                }
            }
        }
//        updateFilter();
    }

    private boolean validData() {
        if (getNewWidth() == 0 || getNewHeight() == 0) {
            validData = false;
            errorMessage = "Width and height cannot be 0";
        }

        return validData;
    }

    private String getErrorMessage() {
        return errorMessage;
    }

    private int getNewWidth() {
        return newWidth;
    }

    private int getNewHeight() {
        return newHeight;
    }

    public static void showInDialog(Composition comp) {
        ResizePanel p = new ResizePanel(comp);
        OKCancelDialog d = new OKCancelDialog(p, "Resize") {
            @Override
            protected void dialogAccepted() {
                if (!p.validData()) {
                    Dialogs.showErrorDialog(this, "Error", p.getErrorMessage());
                    return;
                }
                new Resize(p.getNewWidth(), p.getNewHeight(), false).process(comp);
                close();
            }
        };
        d.setVisible(true);
    }

    public static void resizeActiveImage() {
        Composition comp = ImageComponents.getActiveComp()
                .orElseThrow(() -> new IllegalStateException("no active image"));
        showInDialog(comp);
    }
}