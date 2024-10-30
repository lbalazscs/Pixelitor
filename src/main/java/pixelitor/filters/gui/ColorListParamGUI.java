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

package pixelitor.filters.gui;

import com.bric.swing.ColorSwatch;
import org.jdesktop.swingx.VerticalLayout;
import pixelitor.colors.Colors;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.gui.utils.SliderSpinner;

import javax.swing.*;
import java.awt.Color;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

import static java.awt.FlowLayout.LEFT;
import static javax.swing.BorderFactory.createTitledBorder;
import static pixelitor.filters.gui.ColorParamGUI.BUTTON_SIZE;
import static pixelitor.gui.utils.SliderSpinner.LabelPosition.NONE;

/**
 * The GUI for a {@link ColorParam}
 */
public class ColorListParamGUI extends JPanel implements ParamGUI {
    private final ColorListParam model;
    private final Color[] candidateColors;
    private final List<ColorSwatch> swatches;

    private final SliderSpinner numColorsSlider;
    private final ResetButton resetButton;
    private final JPanel colorsPanel;

    private boolean sliderMovedByUser = true;
    private int numVisibleSwatches;

    public ColorListParamGUI(ColorListParam model, Color[] candidateColors, int minNumColors) {
        super(new VerticalLayout());
        this.model = model;
        this.candidateColors = candidateColors;

        Color[] colors = model.getColors();
        this.numVisibleSwatches = colors.length;

        colorsPanel = new JPanel(new FlowLayout(LEFT));

        swatches = new ArrayList<>(numVisibleSwatches);
        for (int i = 0; i < numVisibleSwatches; i++) {
            createAndAddSwatch(colors[i], i);
        }

        resetButton = new ResetButton(model);

        RangeParam numColorsModel = new RangeParam("Number",
            minNumColors, numVisibleSwatches, candidateColors.length);
        numColorsSlider = new SliderSpinner(numColorsModel, NONE, false);
        numColorsSlider.setupTicks(1, 0);
        numColorsSlider.addExplicitResetButton(resetButton);
        add(numColorsSlider);
        add(colorsPanel);

        numColorsModel.setAdjustmentListener(() ->
            numColorsChanged(numColorsModel.getValue()));

        setBorder(createTitledBorder(model.getName()));
    }

    private ColorSwatch createAndAddSwatch(Color color, int index) {
        ColorSwatch swatch = new ColorSwatch(color, BUTTON_SIZE);
        swatches.add(swatch);
        colorsPanel.add(swatch);

        GUIUtils.addClickAction(swatch, () -> showColorDialog(index));
        Colors.setupFilterColorsPopupMenu(this, swatch,
            () -> model.getColor(index), c -> updateColor(c, index));

        return swatch;
    }

    private void numColorsChanged(int newNum) {
        changeNumVisibleSwatches(newNum);
        updateModelColors();
    }

    private void changeNumVisibleSwatches(int newNum) {
        if (newNum == numVisibleSwatches) {
            return;
        } else if (newNum < numVisibleSwatches) {
            for (int i = newNum; i < numVisibleSwatches; i++) {
                swatches.get(i).setVisible(false);
            }
        } else { // newNum > numVisibleSwatches
            int numInstantiated = swatches.size();
            for (int i = numVisibleSwatches; i < newNum; i++) {
                if (i < numInstantiated) {
                    swatches.get(i).setVisible(true);
                } else {
                    ColorSwatch swatch = createAndAddSwatch(candidateColors[i], i);
                    swatch.revalidate();
                }
            }
        }
        numVisibleSwatches = newNum;
    }

    private void updateModelColors() {
        Color[] newColors = new Color[numVisibleSwatches];
        for (int i = 0; i < numVisibleSwatches; i++) {
            newColors[i] = swatches.get(i).getForeground();
        }
        model.setColors(newColors, sliderMovedByUser);
    }

    private void showColorDialog(int index) {
        Colors.selectColorWithDialog(this, model.getName(),
            model.getColor(index), false,
            color -> updateColor(color, index));
    }

    private void updateColor(Color color, int index) {
        ColorSwatch swatch = swatches.get(index);
        swatch.setForeground(color);
        swatch.paintImmediately(0, 0, BUTTON_SIZE, BUTTON_SIZE);

        model.setColor(index, color, true);
    }

    @Override
    public void updateGUI() {
        Color[] colors = model.getColors();
        int numColors = colors.length;

        changeNumVisibleSwatches(numColors);

        for (int i = 0; i < numColors; i++) {
            Color color = colors[i];
            ColorSwatch swatch = swatches.get(i);
            if (!swatch.getForeground().equals(color)) {
                swatch.setForeground(color);
            }
        }

        // do this only after setting the foreground colors,
        // because it calls setColors on the model
        sliderMovedByUser = false;
        numColorsSlider.setValue(numColors);
        sliderMovedByUser = true;

        if (resetButton != null) {
            resetButton.updateIcon();
        }
    }

    @Override
    public void setToolTip(String tip) {
        for (ColorSwatch swatch : swatches) {
            swatch.setToolTipText(tip);
        }

    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled); // so that isEnabled() works
        numColorsSlider.setEnabled(enabled);
        for (ColorSwatch swatch : swatches) {
            swatch.setEnabled(enabled);
        }
    }

    @Override
    public int getNumLayoutColumns() {
        return 1;
    }
}
