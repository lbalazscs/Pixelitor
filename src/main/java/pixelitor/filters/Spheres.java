/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.filters;

import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.ElevationAngleParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.GUIText;
import pixelitor.utils.Shapes;
import pixelitor.utils.StatusBarProgressTracker;

import java.awt.*;
import java.awt.image.BufferedImage;

import static java.awt.MultipleGradientPaint.CycleMethod.NO_CYCLE;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static pixelitor.gui.GUIText.OPACITY;
import static pixelitor.utils.AngleUnit.CCW_DEGREES;

/**
 * Fills the image with 3D-like circles
 */
public class Spheres extends ParametrizedFilter {
    public static final String NAME = "Spheres";

    // see http://extremelearning.com.au/unreasonable-effectiveness-of-quasirandom-sequences/
    private static final double PHI_2 = 1.32471795724474602596;
    private static final double a1 = 1.0 / PHI_2;
    private static final double a2 = 1.0 / (PHI_2 * PHI_2);

    private final RangeParam radius = new RangeParam(GUIText.RADIUS, 2, 10, 100);
    private final RangeParam density = new RangeParam("Density (%)", 1, 50, 100);

    private final BooleanParam addHighLightsCB = new BooleanParam(
        "Add 3D Highlights", true);
    private final AngleParam highlightAngleSelector = new AngleParam(
        "Light Direction (Azimuth)", 0);
    private final ElevationAngleParam highlightElevationSelector = new ElevationAngleParam(
        "Highlight Elevation", 45, CCW_DEGREES);

    private final RangeParam opacity = new RangeParam(OPACITY, 0, 100, 100);

    public Spheres() {
        super(true);

        // enable "Light Direction" and "Highlight Elevation"
        // only if "Add Highlights" is checked
        addHighLightsCB.setupEnableOtherIfChecked(highlightAngleSelector);
        addHighLightsCB.setupEnableOtherIfChecked(highlightElevationSelector);

        setParams(
            radius.withAdjustedRange(0.1),
            density,
            opacity,
            addHighLightsCB,
            highlightAngleSelector,
            highlightElevationSelector
        );
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        int width = dest.getWidth();
        int height = dest.getHeight();
        float r = radius.getValueAsFloat();
        int numCircles = (int) (width * height * density.getPercentageValD() / (r * r));

        var pt = new StatusBarProgressTracker(NAME, numCircles);

        Graphics2D g = dest.createGraphics();
        g.setComposite(AlphaComposite.SrcOver.derive(opacity.getPercentageValF()));
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

        double angle = highlightAngleSelector.getValueInRadians() + Math.PI;

        double elevation = highlightElevationSelector.getValueInRadians();
        int centerShiftX = (int) (r * Math.cos(angle) * Math.cos(elevation));
        int centerShiftY = (int) (r * Math.sin(angle) * Math.cos(elevation));

        Color[] colors = null;

        boolean addHighlights = addHighLightsCB.isChecked();

        for (int i = 0; i < numCircles; i++) {
//            int x = rand.nextInt(width);
//            int y = rand.nextInt(height);
            // http://extremelearning.com.au/unreasonable-effectiveness-of-quasirandom-sequences/
            int x = (int) (width * ((0.5 + a1 * (i + 1)) % 1));
            int y = (int) (height * ((0.5 + a2 * (i + 1)) % 1));

            // could be faster, but the main bottleneck is
            // the highlights' generation anyway
            int srcColor = src.getRGB(x, y);
            int alpha = (srcColor >>> 24) & 0xFF;
            if (alpha == 0) {
                continue;
            }

            Color c = new Color(srcColor);
            if (addHighlights) {
                colors = new Color[]{c.brighter().brighter(), c};
            }

            // setup paint
            if (addHighlights) {
                float[] fractions = {0.0f, 1.0f};
                Paint gradientPaint = new RadialGradientPaint(
                    x + centerShiftX, y + centerShiftY, r,
                    fractions, colors, NO_CYCLE);

                g.setPaint(gradientPaint);
            } else {
                g.setColor(c);
            }

            // render the spheres
            g.fill(Shapes.createCircle(x, y, r));

            pt.unitDone();
        }
        pt.finished();

        g.dispose();
        return dest;
    }
}