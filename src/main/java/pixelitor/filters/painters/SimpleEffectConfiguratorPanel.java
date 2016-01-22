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

package pixelitor.filters.painters;

import pixelitor.filters.gui.AddDefaultButton;
import pixelitor.filters.gui.ParamAdjustmentListener;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.utils.SliderSpinner;

import java.awt.Color;

import static pixelitor.gui.utils.SliderSpinner.TextPosition.NONE;

/**
 * An EffectConfiguratorPanel that has a width parameter.
 * Most effect configurator panels need this as the superclass
 */
public class SimpleEffectConfiguratorPanel extends EffectConfiguratorPanel {
    private final RangeParam widthRange;
    private final SliderSpinner widthSlider;

    public SimpleEffectConfiguratorPanel(String effectName, boolean defaultSelected, Color defaultColor, int defaultWidth) {
        super(effectName, defaultSelected, defaultColor);

        widthRange = new RangeParam("Width:", 1, defaultWidth, 100);
        widthSlider = new SliderSpinner(widthRange, NONE, AddDefaultButton.NO);

        gbHelper.addLabelWithControl("Width:", widthSlider);

        widthRange.addChangeListener(e -> updateDefaultButtonState());
    }

    @Override
    public int getBrushWidth() {
        return widthSlider.getCurrentValue();
    }

    @Override
    public void setAdjustmentListener(ParamAdjustmentListener adjustmentListener) {
        super.setAdjustmentListener(adjustmentListener);
        widthRange.setAdjustmentListener(adjustmentListener);
    }

    @Override
    public boolean isSetToDefault() {
        return super.isSetToDefault()
                && widthRange.isSetToDefault();
    }

    @Override
    public void reset(boolean triggerAction) {
        super.reset(false);
        widthRange.reset(triggerAction);
    }
}
