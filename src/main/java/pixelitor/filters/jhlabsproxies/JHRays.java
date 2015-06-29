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

import com.jhlabs.composite.MiscComposite;
import com.jhlabs.image.RaysFilter;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.ResizingHelper;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Rays based on the JHLabs RaysFilter
 */
public class JHRays extends FilterWithParametrizedGUI {
    private final ImagePositionParam center = new ImagePositionParam("Light Source");
    private final RangeParam rotation = new RangeParam("Twirl", -90, 90, 0);
    private final RangeParam length = new RangeParam("Length", 0, 200, 20);
    private final RangeParam opacity = new RangeParam("Opacity (%)", 0, 100, 80);
    private final RangeParam strength = new RangeParam("Strength", 0, 500, 200);
    private final RangeParam threshold = new RangeParam("Threshold (%)", 0, 100, 25);
    private final BooleanParam raysOnly = new BooleanParam("Rays Only", false, true);

    // setting a ColorMap does not work properly

    private RaysFilter filter;

    public JHRays() {
        super("Rays", true, false);
        setParamSet(new ParamSet(
                center,
                length,
                threshold,
                strength,
                opacity,
                rotation,
                raysOnly
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new RaysFilter();
        }

        filter.setCentreX(center.getRelativeX());
        filter.setCentreY(center.getRelativeY());
        filter.setStrength(strength.getValueAsPercentage());
        filter.setRotation(rotation.getValueInRadians());
//        filter.setOpacity(opacity.getValueAsPercentage());
        filter.setThreshold(threshold.getValueAsPercentage());
//        filter.setRaysOnly(raysOnly.isChecked());

        // this value should not be divided by resizeFactor because
        // this is a scale and not really a length
        filter.setZoom(length.getValueAsPercentage());

        BufferedImage rays;
        ResizingHelper r = new ResizingHelper(src);
        if (r.shouldResize()) {
            rays = r.invoke(ResizingHelper.BILINEAR_FAST, filter);
        } else {
            // normal case, no resizing
            rays = filter.filter(src, dest);
        }


        if (dest == null) {
            dest = filter.createCompatibleDestImage(src, null);
        }

        // according to "rays only" setting return the rays image or an added image
        Graphics2D g = dest.createGraphics();
        if (!raysOnly.isChecked()) {
            g.setComposite(AlphaComposite.SrcOver);
            g.drawRenderedImage(src, null);
        }
        g.setComposite(MiscComposite.getInstance(MiscComposite.ADD, opacity.getValueAsPercentage()));
        g.drawRenderedImage(rays, null);
        g.dispose();

        return dest;
    }
}