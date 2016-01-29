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

import com.jhlabs.image.ShadowFilter;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.ResizingFilterHelper;
import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.ColorParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.utils.BasicProgressTracker;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.ProgressTracker;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static java.awt.Color.BLACK;
import static pixelitor.filters.ResizingFilterHelper.ScaleUpQuality.BILINEAR_FAST;
import static pixelitor.filters.gui.ColorParam.OpacitySetting.NO_OPACITY;

/**
 * Drop Shadow based on the JHLabs ShadowFilter
 */
public class JHDropShadow extends FilterWithParametrizedGUI {
    public static final String NAME = "Drop Shadow";

    private final AngleParam angle = new AngleParam("Angle", ImageUtils.DEG_315_IN_RADIANS);
    private final RangeParam distance = new RangeParam("Distance", 0, 10, 100);
    private final RangeParam opacity = new RangeParam("Opacity (%)", 0, 90, 100);
    private final RangeParam softness = new RangeParam("Softness", 0, 10, 25);
    private final BooleanParam shadowOnly = new BooleanParam("Shadow Only", false);
    private final ColorParam color = new ColorParam("Color", BLACK, NO_OPACITY);

    private ShadowFilter filter;

    public JHDropShadow() {
        super(ShowOriginal.YES);
        setParamSet(new ParamSet(
                angle,
                distance.adjustRangeToImageSize(0.1),
                opacity,
                softness.adjustRangeToImageSize(0.025),
                color,
                shadowOnly
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new ShadowFilter(NAME);
        }

        filter.setAddMargins(false);
        filter.setAngle((float) angle.getValueInIntuitiveRadians());
        filter.setOpacity(opacity.getValueAsPercentage());
        filter.setShadowColor(color.getColor().getRGB());
        filter.setShadowOnly(shadowOnly.isChecked());

        ResizingFilterHelper r = new ResizingFilterHelper(src);
        boolean shouldResize = r.shouldResize();
        if (shouldResize) {
            boolean addSource = !shadowOnly.isChecked();

            int resizeUnits = r.getResizeWorkUnits(BILINEAR_FAST);
            int filterUnits = 2; // estimated
            int workUnits = resizeUnits + filterUnits;
            if (addSource) {
                workUnits++;
            }
            ProgressTracker pt = new BasicProgressTracker(NAME, workUnits);
//            ProgressTracker pt = new DebugProgressTracker(NAME, workUnits);
            filter.setProgressTracker(ProgressTracker.NULL_TRACKER);

            double resizeFactor = r.getResizeFactor();
            filter.setDistance((float) (distance.getValueAsFloat() / resizeFactor));
            filter.setRadius((float) (softness.getValueAsFloat() / resizeFactor));
            filter.setShadowOnly(true); // we only want to resize the shadow

            dest = r.invoke(BILINEAR_FAST, filter, pt, filterUnits);

            if (addSource) {
                Graphics2D g = dest.createGraphics();
                g.setComposite(AlphaComposite.SrcOver);
                g.drawRenderedImage(src, null);
                g.dispose();

                pt.unitDone();
            }
            pt.finish();
        } else {
            // normal case, no resizing
            filter.setDistance(distance.getValueAsFloat());
            filter.setRadius(softness.getValueAsFloat());
            dest = filter.filter(src, dest);
        }

        return dest;
    }

    public void setSoftness(int newSoftness) {
        softness.setValue(newSoftness);
    }

    public void setDistance(int newDistance) {
        distance.setValue(newDistance);
    }

    public void setOpacity(float newValue) {
        opacity.setValue((int) (100 * newValue));
    }

    @Override
    public boolean supportsGray() {
        return false;
    }
}