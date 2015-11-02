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

package pixelitor.filters.jhlabsproxies;

import com.jhlabs.image.CausticsFilter;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.ColorParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ReseedNoiseFilterAction;

import java.awt.Color;
import java.awt.image.BufferedImage;

import static pixelitor.filters.gui.ColorParam.OpacitySetting.NO_OPACITY;
import static pixelitor.filters.gui.RandomizePolicy.IGNORE_RANDOMIZE;
import static pixelitor.utils.SliderSpinner.TextPosition.BORDER;

/**
 * Renders caustics based on the JHLabs CausticsFilter
 */
public class JHCaustics extends FilterWithParametrizedGUI {
    private final ColorParam bgColor = new ColorParam("Background Color", new Color(0, 200, 175), NO_OPACITY);
    private final RangeParam scale = new RangeParam("Zoom", 1, 500, 100);
    private final RangeParam brightness = new RangeParam("Brightness", 0, 20, 7);
    private final RangeParam focus = new RangeParam("Focus", 0, 100, 50);
    private final RangeParam dispersion = new RangeParam("Dispersion (Color Separation)", 0, 100, 0);
    private final RangeParam turbulence = new RangeParam("Turbulence", 0, 8, 0);
    private final RangeParam time = new RangeParam("Time", 0, 999, 0);
    private final RangeParam samples = new RangeParam("Samples (Quality)", 1, 10, 1, true, BORDER, IGNORE_RANDOMIZE);

    private CausticsFilter filter;

    public JHCaustics() {
        super("Caustics", false, false);
        setParamSet(new ParamSet(
                bgColor,
                scale.adjustRangeToImageSize(0.5),
                brightness,
                turbulence,
                time,
                focus,
                dispersion,
                samples
        ).withAction(new ReseedNoiseFilterAction()));
        listNamePrefix = "Render ";
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new CausticsFilter();
        }

        filter.setAmount(focus.getValueAsPercentage());
        filter.setBgColor(bgColor.getColor().getRGB());
        filter.setBrightness(brightness.getValue());
        filter.setDispersion(dispersion.getValueAsPercentage());
        filter.setSamples(samples.getValue());
        filter.setScale(scale.getValueAsFloat());
        filter.setTime(time.getValueAsPercentage());
        filter.setTurbulence(turbulence.getValueAsFloat());

        dest = filter.filter(src, dest);
        return dest;
    }
}