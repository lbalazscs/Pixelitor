/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.filters.jhlabsproxies;

import com.jhlabs.image.WoodFilter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.GradientParam;
import pixelitor.filters.gui.RangeParam;

import java.awt.Color;
import java.awt.image.BufferedImage;

import static pixelitor.filters.gui.ReseedActions.reseedNoise;
import static pixelitor.gui.GUIText.ZOOM;

/**
 * Renders wood texture based on the JHLabs WoodFilter
 */
public class JHWood extends ParametrizedFilter {
    public static final String NAME = "Wood";

    private final RangeParam rings = new RangeParam("Rings", 1, 50, 100);
    private final RangeParam scale = new RangeParam(ZOOM, 1, 100, 500);
    private final RangeParam stretch = new RangeParam("Stretch", 0, 10, 50);
    private final AngleParam angle = new AngleParam("Angle", 0);
    private final RangeParam turbulence = new RangeParam("Turbulence", 0, 0, 100);
    private final RangeParam fibres = new RangeParam("Fibres", 0, 10, 100);
    private final RangeParam gain = new RangeParam("Gain", 0, 80, 100);

    private final GradientParam gradient = new GradientParam("Colors",
        new float[]{0.0f, 0.5f, 1.0f},
        new Color[]{
            new Color(229, 196, 148),
            new Color(190, 160, 115),
            new Color(152, 123, 81)});

    private WoodFilter filter;

    public JHWood() {
        super(false);

        scale.setPresetKey("Zoom");
        setParams(
            angle,
            scale.withAdjustedRange(0.5),
            stretch,
            gradient,
            rings,
            turbulence,
            fibres,
            gain
        ).withAction(reseedNoise());

        stretch.setupEnableOtherIfNotZero(angle);
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new WoodFilter(NAME);
        }

        filter.setAngle((float) (angle.getValueInRadians() + Math.PI / 2));
        filter.setScale(scale.getValueAsFloat());
        filter.setStretch(stretch.getValueAsFloat() + 1.0f);
        filter.setRings((float) rings.getPercentage());
        filter.setTurbulence((float) turbulence.getPercentage());
        filter.setFibres((float) fibres.getPercentage());
        filter.setGain((float) gain.getPercentage());
        filter.setColormap(gradient.getValue());

        return filter.filter(src, dest);
    }
}