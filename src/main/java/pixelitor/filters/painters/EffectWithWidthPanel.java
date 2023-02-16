/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.filters.gui.ParamAdjustmentListener;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.utils.SliderSpinner;

import java.awt.Color;

/**
 * An {@link EffectPanel} that has a width parameter.
 * Most effect panels use this as the superclass.
 */
public class EffectWithWidthPanel extends EffectPanel {
    private final RangeParam widthRange;

    public EffectWithWidthPanel(String effectName, boolean selected,
                                Color color, double width, float opacity) {
        super(effectName, selected, color, opacity);

        widthRange = new RangeParam("Width:", 1, width, 100);
        SliderSpinner widthSlider = SliderSpinner.from(widthRange);

        gbh.addLabelAndControl("Width:", widthSlider);

        widthRange.addChangeListener(e -> updateResetButtonIcon());
    }

    @Override
    public double getBrushWidth() {
        return widthRange.getValueAsDouble();
    }

    @Override
    public void setBrushWidth(double value) {
        widthRange.setValue(value, false);
    }

    @Override
    public void setAdjustmentListener(ParamAdjustmentListener adjustmentListener) {
        super.setAdjustmentListener(adjustmentListener);
        widthRange.setAdjustmentListener(adjustmentListener);
    }

    @Override
    public boolean hasDefault() {
        return super.hasDefault() && widthRange.hasDefault();
    }

    @Override
    public void reset(boolean trigger) {
        super.reset(false);
        widthRange.reset(trigger);
    }

    @Override
    public boolean randomize() {
        boolean enable = super.randomize();
        if (enable) {
            widthRange.randomize();
        }
        return enable;
    }
}
