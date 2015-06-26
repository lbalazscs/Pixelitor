/*
 * Copyright 2015 Laszlo Balazs-Csiki
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

package pixelitor.filters.painters;

import com.bric.swing.ColorPicker;
import com.bric.swing.ColorSwatch;
import org.jdesktop.swingx.painter.effects.AbstractAreaEffect;
import pixelitor.PixelitorWindow;
import pixelitor.filters.gui.ParamAdjustmentListener;
import pixelitor.filters.gui.RangeParam;
import pixelitor.utils.GridBagHelper;
import pixelitor.utils.SliderSpinner;

import javax.swing.*;
import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static pixelitor.utils.SliderSpinner.TextPosition.NONE;

/**
 * An effect configurator panel...
 */
public abstract class EffectConfiguratorPanel extends JPanel {
    private final JCheckBox enabledCB;
    private final ColorSwatch colorSwatch;
    private Color color;
    static final int BUTTON_SIZE = 20;
    ParamAdjustmentListener adjustmentListener;

    private final RangeParam opacityRange;

    protected final GridBagHelper gbHelper;

    EffectConfiguratorPanel(String effectName, boolean defaultEnabled, Color defaultColor) {
        setBorder(BorderFactory.createTitledBorder('"' + effectName + "\" Configuration"));

        opacityRange = new RangeParam("Width:", 1, 100, 100);
        SliderSpinner opacitySlider = new SliderSpinner(opacityRange, NONE, false);

        enabledCB = new JCheckBox();
        enabledCB.setName("enabledCB");
        enabledCB.setSelected(defaultEnabled);

        colorSwatch = new ColorSwatch(defaultColor, BUTTON_SIZE);
        color = defaultColor;

        colorSwatch.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                changeColor();
            }
        });

        setLayout(new GridBagLayout());

        gbHelper = new GridBagHelper(this);
        gbHelper.addLabelWithControl("Enabled:", enabledCB);
        gbHelper.addLabelWithControlNoFill("Color:", colorSwatch);
        gbHelper.addLabelWithControlNoFill("Opacity:", opacitySlider);
    }

    private void changeColor() {
        Color selectedColor = ColorPicker.showDialog(PixelitorWindow.getInstance(), "Select Color", color, true);
        if (selectedColor != null) { // ok was pressed
            color = selectedColor;
            colorSwatch.setForeground(color);
            colorSwatch.paintImmediately(0, 0, BUTTON_SIZE, BUTTON_SIZE);

            if (adjustmentListener != null) {
                adjustmentListener.paramAdjusted();
            }
        }
    }

    ButtonModel getEnabledModel() {
        return enabledCB.getModel();
    }

    public boolean isSelected() {
        return enabledCB.isSelected();
    }

    public Color getColor() {
        return color;
    }

    public float getOpacity() {
        return opacityRange.getValueAsPercentage();
    }

    public abstract int getBrushWidth();

    public void setAdjustmentListener(ParamAdjustmentListener adjustmentListener) {
        if (this.adjustmentListener != null) {
            throw new IllegalStateException("only one is allowed");
        }

        this.adjustmentListener = adjustmentListener;
        enabledCB.addActionListener(e -> adjustmentListener.paramAdjusted());

        opacityRange.setAdjustmentListener(adjustmentListener);
    }

    public void updateEffectColorAndBrush(AbstractAreaEffect effect) {
        effect.setBrushColor(getColor());

        int brushWidth = getBrushWidth();
        effect.setEffectWidth(brushWidth);

        effect.setBrushSteps(calculateBrushSteps(brushWidth));
    }

    public static int calculateBrushSteps(int brushWidth) {
        int brushSteps = 1 + brushWidth / 3;
        return brushSteps;
    }
}
