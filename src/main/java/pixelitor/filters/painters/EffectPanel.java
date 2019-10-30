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

package pixelitor.filters.painters;

import com.bric.swing.ColorSwatch;
import org.jdesktop.swingx.painter.effects.AbstractAreaEffect;
import pixelitor.colors.ColorUtils;
import pixelitor.filters.gui.DefaultButton;
import pixelitor.filters.gui.ParamAdjustmentListener;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.Resettable;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.gui.utils.GridBagHelper;
import pixelitor.gui.utils.SliderSpinner;

import javax.swing.*;
import java.awt.Color;
import java.awt.GridBagLayout;
import java.util.Objects;

import static javax.swing.BorderFactory.createTitledBorder;

/**
 * A GUI for configuring an area effect
 */
public abstract class EffectPanel extends JPanel implements Resettable {
    private final JCheckBox enabledCB;
    private final ColorSwatch colorSwatch;

    private final boolean defaultEnabled;
    private final Color defaultColor;

    private Color color;
    static final int BUTTON_SIZE = 20;
    ParamAdjustmentListener adjustmentListener;

    private DefaultButton defaultButton;

    private final RangeParam opacityRange;

    protected final GridBagHelper gbh;

    EffectPanel(String effectName, boolean defaultEnabled, Color defaultColor) {
        this.defaultEnabled = defaultEnabled;
        this.defaultColor = defaultColor;

        setBorder(createTitledBorder('"' + effectName + "\" Configuration"));

        opacityRange = new RangeParam("Width:", 1, 100, 100);
        SliderSpinner opacitySlider = SliderSpinner.simpleFrom(opacityRange);

        enabledCB = new JCheckBox();
        enabledCB.setName("enabledCB");
        setTabEnabled(defaultEnabled);
        enabledCB.addActionListener(e -> updateDefaultButtonIcon());

        colorSwatch = new ColorSwatch(defaultColor, BUTTON_SIZE);
        color = defaultColor;

        GUIUtils.addColorDialogListener(colorSwatch, this::showColorDialog);

        ColorUtils.setupFilterColorsPopupMenu(this, colorSwatch,
                this::getColor, c -> setColor(c, true));

        setLayout(new GridBagLayout());

        gbh = new GridBagHelper(this);
        gbh.addLabelWithControl("Enabled:", enabledCB);
        gbh.addLabelWithControlNoStretch("Color:", colorSwatch);
        gbh.addLabelWithControl("Opacity:", opacitySlider);
    }

    public void setTabEnabled(boolean defaultEnabled) {
        enabledCB.setSelected(defaultEnabled);
    }

    private void showColorDialog() {
        ColorUtils.selectColorWithDialog(this,
                "Select Color", color, true,
                c -> setColor(c, true));
    }

    public void setColor(Color newColor, boolean trigger) {
        color = newColor;
        colorSwatch.setForeground(color);
        colorSwatch.paintImmediately(0, 0, BUTTON_SIZE, BUTTON_SIZE);

        updateDefaultButtonIcon();

        if (trigger && adjustmentListener != null) {
            adjustmentListener.paramAdjusted();
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

    public abstract void setBrushWidth(int value);

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

    private static int calculateBrushSteps(int brushWidth) {
        return 1 + brushWidth / 3;
    }

    @Override
    public boolean isSetToDefault() {
        boolean enabled = enabledCB.isSelected();

        return (enabled == defaultEnabled)
                && Objects.equals(color, defaultColor);
    }

    @Override
    public void reset(boolean trigger) {
        setTabEnabled(defaultEnabled);
        setColor(defaultColor, trigger);
    }

    public void setDefaultButton(DefaultButton defaultButton) {
        this.defaultButton = defaultButton;
    }

    protected void updateDefaultButtonIcon() {
        if (defaultButton != null) {
            defaultButton.updateIcon();
        }
    }
}
