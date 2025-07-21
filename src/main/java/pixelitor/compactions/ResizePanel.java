/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.filters.gui.DialogMenuBar;
import pixelitor.filters.gui.DialogMenuOwner;
import pixelitor.filters.gui.UserPreset;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.DimensionHelper;
import pixelitor.gui.utils.GridBagHelper;
import pixelitor.gui.utils.ValidatedPanel;
import pixelitor.gui.utils.ValidationResult;
import pixelitor.utils.ResizeUnit;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import static java.awt.FlowLayout.LEFT;

/**
 * The GUI for the resize settings.
 */
public class ResizePanel extends ValidatedPanel implements KeyListener, ItemListener, DialogMenuOwner, DimensionHelper.DimensionChangeCallback {
    private final JCheckBox keepProportionsCB;
    private final TitledBorder titleBorder;

    private final double origAspectRatio;
    private final int origWidth;
    private final int origHeight;

    private final DimensionHelper dimensions;

    private ResizePanel(Composition comp) {
        Canvas canvas = comp.getCanvas();
        origWidth = canvas.getWidth();
        origHeight = canvas.getHeight();
        origAspectRatio = ((double) origWidth) / origHeight;

        dimensions = new DimensionHelper(this, ResizeUnit.values(),
            origWidth, origHeight, comp.getDpi(), origWidth, origHeight);

        var inputPanel = new JPanel(new GridBagLayout());
        var gbh = new GridBagHelper(inputPanel);

        var widthLayer = dimensions.createWidthTextField(this);
        var widthUnitChooser = dimensions.createUnitChooser(this);
        gbh.addLabelAndTwoControls("Width:", widthLayer, widthUnitChooser);

        var heightLayer = dimensions.createHeightTextField(this);
        var heightUnitChooser = dimensions.createUnitChooser(this);
        gbh.addLabelAndTwoControls("Height:", heightLayer, heightUnitChooser);

        titleBorder = BorderFactory.createTitledBorder("");
        updateBorderText();
        inputPanel.setBorder(titleBorder);

        JPanel optionsPanel = new JPanel(new FlowLayout(LEFT));
        keepProportionsCB = new JCheckBox("Keep Proportions");
        keepProportionsCB.setSelected(true);
        keepProportionsCB.addItemListener(this);
        optionsPanel.add(keepProportionsCB);

        JPanel dpiPanel = new JPanel(new FlowLayout(LEFT));
        dpiPanel.add(new JLabel("DPI:"));
        var dpiChooser = dimensions.getDpiChooser();
        dpiChooser.addItemListener(this);
        dpiChooser.setEnabled(false);
        dpiPanel.add(dpiChooser);

        Box verticalBox = Box.createVerticalBox();
        verticalBox.add(inputPanel);
        verticalBox.add(optionsPanel);
        verticalBox.add(dpiPanel);
        add(verticalBox);

        dimensions.setInitialValues();
    }

    private boolean shouldKeepProportions() {
        return keepProportionsCB.isSelected();
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        Object source = e.getSource();
        if (e.getStateChange() != ItemEvent.SELECTED) {
            return;
        }

        if (source == keepProportionsCB) {
            if (shouldKeepProportions()) {
                adjustHeightToKeepProportions();
            }
        } else {
            dimensions.itemStateChanged(e);
        }
    }

    @Override
    public void dpiChanged() {
        if (shouldKeepProportions()) {
            adjustHeightToKeepProportions();
        } else {
            dimensions.updateHeightText();
        }
        dimensions.updateWidthText();
        updateBorderText();
    }

    @Override
    public void dimensionChanged(boolean isWidthChanged) {
        if (shouldKeepProportions()) {
            if (isWidthChanged) {
                adjustHeightToKeepProportions();
            } else { // height was changed
                adjustWidthToKeepProportions();
            }
        }
        updateBorderText();
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
        dimensions.keyReleased(e);
    }

    private void adjustHeightToKeepProportions() {
        int targetWidth = dimensions.getTargetWidth();
        int targetHeight = (int) Math.round(targetWidth / origAspectRatio);
        if (targetHeight == 0) {
            targetHeight = 1;
        }
        dimensions.setTargetHeight(targetHeight);
        dimensions.updateHeightText();
    }

    private void adjustWidthToKeepProportions() {
        int targetHeight = dimensions.getTargetHeight();
        int targetWidth = (int) Math.round(targetHeight * origAspectRatio);
        if (targetWidth == 0) {
            targetWidth = 1;
        }
        dimensions.setTargetWidth(targetWidth);
        dimensions.updateWidthText();
    }

    private void updateBorderText() {
        int targetWidth = dimensions.getTargetWidth();
        int targetHeight = dimensions.getTargetHeight();
        String origSize = origWidth + "x" + origHeight + " to ";
        if (targetWidth > 0 && targetHeight > 0) {
            titleBorder.setTitle(origSize + targetWidth + "x" + targetHeight);
        } else {
            // -1 signals invalid input
            titleBorder.setTitle(origSize + "??");
        }

        repaint();
    }

    @Override
    public ValidationResult validateSettings() {
        return dimensions.checkWidthAndHeight();
    }

    public static void showInDialog(Composition comp, String dialogTitle) {
        ResizePanel p = new ResizePanel(comp);
        new DialogBuilder()
            .validatedContent(p)
            .title(dialogTitle)
            .menuBar(new DialogMenuBar(p))
            .okAction(() -> {
                comp.setDpi(p.dimensions.getDpi());
                new Resize(
                    p.dimensions.getTargetWidth(),
                    p.dimensions.getTargetHeight()).process(comp);
            })
            .show();
    }

    @Override
    public boolean supportsUserPresets() {
        return true;
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        dimensions.saveStateTo(preset);

        preset.putBoolean("Constrain", shouldKeepProportions());
    }

    @Override
    public void loadUserPreset(UserPreset preset) {
        boolean keep = preset.getBoolean("Constrain");
        keepProportionsCB.setSelected(keep);

        dimensions.loadUserPreset(preset, keep);
        updateBorderText();
    }

    @Override
    public String getPresetDirName() {
        return "Resize";
    }
}
