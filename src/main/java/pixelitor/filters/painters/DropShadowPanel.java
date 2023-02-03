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

import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.AngleUI;
import pixelitor.filters.gui.ParamAdjustmentListener;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.utils.SliderSpinner;
import pixelitor.utils.Geometry;

import javax.swing.event.ChangeListener;
import java.awt.Color;
import java.awt.geom.Point2D;

/**
 * A GUI for configuring the "drop shadow" shape effect
 */
public class DropShadowPanel extends EffectPanel {
    private final AngleParam angleParam;
    private final RangeParam distanceParam;
    private final RangeParam spreadParam;

    DropShadowPanel(boolean defaultEnabled, Color defaultColor,
                    int defaultDistance, double defaultAngle,
                    double defaultSpread, float opacity) {
        super("Drop Shadow", defaultEnabled, defaultColor, opacity);

        if (defaultDistance < 1) { // can happen due to rounding errors
            defaultDistance = 1;
        }

        distanceParam = new RangeParam("Distance:", 1, defaultDistance, 100);
        var distanceSlider = SliderSpinner.from(distanceParam);
        gbh.addLabelAndControl("Distance:", distanceSlider);

        angleParam = new AngleParam("Angle", defaultAngle);
        var angleUI = new AngleUI(angleParam);
        gbh.addLabelAndControl("Angle:", angleUI);

        spreadParam = new RangeParam("Spread:", 1, defaultSpread, 100);
        var spreadSlider = SliderSpinner.from(spreadParam);
        gbh.addLabelAndControl("Spread:", spreadSlider);

        ChangeListener changeListener = e -> updateResetButtonIcon();
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

        return Geometry.polarToCartesian(distance, angle);
    }

    @Override
    public double getBrushWidth() {
        return spreadParam.getValueAsDouble();
    }

    @Override
    public void setBrushWidth(double value) {
        spreadParam.setValue(value, false);
    }

    public void setAngle(double rad) {
        angleParam.setValue(rad, false);
    }

    public void setDistance(double value) {
        distanceParam.setValueNoTrigger(value);
    }

    @Override
    public boolean isSetToDefault() {
        return super.isSetToDefault()
            && angleParam.isSetToDefault()
            && distanceParam.isSetToDefault()
            && spreadParam.isSetToDefault();
    }

    @Override
    public void reset(boolean trigger) {
        super.reset(false);
        angleParam.reset(false);
        distanceParam.reset(false);
        spreadParam.reset(false);

        if (trigger && adjustmentListener != null) {
            adjustmentListener.paramAdjusted();
        }
    }
}
