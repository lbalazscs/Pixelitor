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
import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.AngleSelectorComponent;
import pixelitor.filters.gui.ParamAdjustmentListener;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.utils.SliderSpinner;
import pixelitor.utils.Utils;

import javax.swing.event.ChangeListener;
import java.awt.Color;
import java.awt.geom.Point2D;

import static pixelitor.gui.utils.SliderSpinner.TextPosition.NONE;

/**
 *
 */
public class DropShadowEffectConfiguratorPanel extends EffectConfiguratorPanel {
    private final AngleParam angleParam;
    private final RangeParam distanceParam;
    private final RangeParam spreadParam;

    DropShadowEffectConfiguratorPanel(boolean defaultEnabled, Color defaultColor,
                                      int defaultDistance, double defaultAngle,
                                      int defaultSpread) {
        super("Drop Shadow", defaultEnabled, defaultColor);

        distanceParam = new RangeParam("Distance:", 1, defaultDistance, 100);
        SliderSpinner distanceSlider = new SliderSpinner(distanceParam, NONE, AddDefaultButton.NO);
        gbHelper.addLabelWithControl("Distance:", distanceSlider);

        angleParam = new AngleParam("Angle", defaultAngle);
        AngleSelectorComponent angleSelectorComponent = new AngleSelectorComponent(angleParam);
        gbHelper.addLabelWithControl("Angle:", angleSelectorComponent);

        spreadParam = new RangeParam("Spread:", 1, defaultSpread, 100);
        SliderSpinner spreadSlider = new SliderSpinner(spreadParam, NONE, AddDefaultButton.NO);
        gbHelper.addLabelWithControl("Spread:", spreadSlider);

        ChangeListener changeListener = e -> updateDefaultButtonState();
        distanceParam.addChangeListener(changeListener);
        angleParam.addChangeListener(changeListener);
        spreadParam.addChangeListener(changeListener);
    }

    @Override
    public void setAdjustmentListener(ParamAdjustmentListener adjustmentListener) {
        super.setAdjustmentListener(adjustmentListener);
        angleParam.setAdjustmentListener(adjustmentListener);
        distanceParam.setAdjustmentListener(adjustmentListener);
        spreadParam.setAdjustmentListener(adjustmentListener);
    }

    public Point2D getOffset() {
        double distance = distanceParam.getValueAsDouble();
        double angle = angleParam.getValueInRadians();

        return Utils.calculateOffset(distance, angle);
    }


    @Override
    public int getBrushWidth() {
        return spreadParam.getValue();
    }

    @Override
    public boolean isSetToDefault() {
        return super.isSetToDefault()
                && angleParam.isSetToDefault()
                && distanceParam.isSetToDefault()
                && spreadParam.isSetToDefault();
    }

    @Override
    public void reset(boolean triggerAction) {
        super.reset(false);
        angleParam.reset(false);
        distanceParam.reset(false);
        spreadParam.reset(false);

        if (triggerAction && adjustmentListener != null) {
            adjustmentListener.paramAdjusted();
        }
    }
}
