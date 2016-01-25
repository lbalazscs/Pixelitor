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

package pixelitor.filters.jhlabsproxies;

import com.jhlabs.image.WoodFilter;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.GradientParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ReseedNoiseActionSetting;
import pixelitor.filters.gui.ShowOriginal;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * Renders wood texture based on the JHLabs WoodFilter
 */
public class JHWood extends FilterWithParametrizedGUI {
    public static final String NAME = "Wood";

    private final RangeParam rings = new RangeParam("Rings", 1, 50, 100);
    private final RangeParam scale = new RangeParam("Zoom", 1, 100, 500);
    private final RangeParam stretch = new RangeParam("Stretch", 1, 10, 50);
    private final AngleParam angle = new AngleParam("Angle", 0);
    private final RangeParam turbulence = new RangeParam("Turbulence", 0, 0, 100);
    private final RangeParam fibres = new RangeParam("Fibres", 0, 10, 100);
    private final RangeParam gain = new RangeParam("Gain", 0, 80, 100);

    private final GradientParam gradient = new GradientParam("Colors", new Color(229, 196, 148), new Color(152, 123, 81));

    private WoodFilter filter;

    public JHWood() {
        super(ShowOriginal.NO);

        setParamSet(new ParamSet(
                angle,
                scale.adjustRangeToImageSize(0.5),
                stretch,
                gradient,
                rings,
                turbulence,
                fibres,
                gain
        ).withAction(new ReseedNoiseActionSetting()));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new WoodFilter(NAME);
        }

        filter.setAngle((float) (angle.getValueInRadians() + Math.PI / 2));
        filter.setScale(scale.getValueAsFloat());
        filter.setStretch(stretch.getValueAsFloat());
        filter.setRings(rings.getValueAsPercentage());
        filter.setTurbulence(turbulence.getValueAsPercentage());
        filter.setFibres(fibres.getValueAsPercentage());
        filter.setGain(gain.getValueAsPercentage());
        filter.setColormap(gradient.getValue());

        dest = filter.filter(src, dest);
        return dest;
    }
}