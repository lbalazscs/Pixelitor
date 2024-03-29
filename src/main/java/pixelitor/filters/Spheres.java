/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.filters.gui.*;
import pixelitor.gui.GUIText;
import pixelitor.utils.Shapes;
import pixelitor.utils.StatusBarProgressTracker;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.Random;

import static java.awt.MultipleGradientPaint.CycleMethod.NO_CYCLE;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static pixelitor.utils.AngleUnit.CCW_DEGREES;

/**
 * Fills the image with 3D-like circles
 */
public class Spheres extends ParametrizedFilter {
    public static final String NAME = "Spheres";

    @Serial
    private static final long serialVersionUID = -2472078121608102477L;

    enum LayoutType {
        PATTERN("Pattern"), RANDOM("Random");

        private final String guiName;

        LayoutType(String guiName) {
            this.guiName = guiName;
        }

        @Override
        public String toString() {
            return guiName;
        }
    }

    // see http://extremelearning.com.au/unreasonable-effectiveness-of-quasirandom-sequences/
    private static final double PHI_2 = 1.324717957244746;
    private static final double a1 = 1.0 / PHI_2;
    private static final double a2 = 1.0 / (PHI_2 * PHI_2);

    private final EnumParam<LayoutType> layout = new EnumParam<>("Layout", LayoutType.class);
    private final RangeParam minRadius = new RangeParam("Min Radius", 2, 10, 100);
    private final RangeParam maxRadius = new RangeParam("Max Radius", 2, 10, 100);
    private final RangeParam density = new RangeParam("Density (%)", 1, 50, 100);

    private final BooleanParam addHighLightsCB = new BooleanParam(
        "Add 3D Highlights", true);
    private final AngleParam highlightAngleSelector = new AngleParam(
        "Light Direction (Azimuth)", 0);
    private final ElevationAngleParam highlightElevationSelector = new ElevationAngleParam(
        "Highlight Elevation", 45, CCW_DEGREES);

    private final RangeParam opacity = new RangeParam(GUIText.OPACITY, 0, 100, 100);

    public Spheres() {
        super(true);

        // enable "Light Direction" and "Highlight Elevation"
        // only if "Add Highlights" is checked
        addHighLightsCB.setupEnableOtherIfChecked(highlightAngleSelector);
        addHighLightsCB.setupEnableOtherIfChecked(highlightElevationSelector);

        opacity.setPresetKey("Opacity");

        FilterButtonModel reseedAction = paramSet.createReseedAction();
        layout.setupEnableOtherIf(reseedAction, layoutType -> layoutType == LayoutType.RANDOM);

        setParams(
            layout.withAction(reseedAction),
            minRadius.withAdjustedRange(0.1),
            maxRadius.withAdjustedRange(0.1),
            density,
            opacity,
            addHighLightsCB,
            highlightAngleSelector,
            highlightElevationSelector
        );

        maxRadius.ensureHigherValueThan(minRadius);
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        int width = dest.getWidth();
        int height = dest.getHeight();
        double minR = minRadius.getValueAsDouble();
        double maxR = maxRadius.getValueAsDouble();
        double averageR = (minR + maxR) / 2.0;
        int numCircles = (int) (width * height * density.getPercentage() / (averageR * averageR));

        LayoutType type = this.layout.getSelected();

        var pt = new StatusBarProgressTracker(NAME, numCircles);

        Graphics2D g = dest.createGraphics();
        g.setComposite(AlphaComposite.SrcOver.derive((float) opacity.getPercentage()));
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

        double angle = highlightAngleSelector.getValueInRadians() + Math.PI;

        double elevation = highlightElevationSelector.getValueInRadians();
        double cosAngle = Math.cos(angle);
        double sinAngle = Math.sin(angle);
        double cosElevation = Math.cos(elevation);

        Color[] colors = null;

        boolean addHighlights = addHighLightsCB.isChecked();

        Random rand = paramSet.getLastSeedRandom();
        double deltaR = (maxR - minR) / numCircles;
        for (int i = 0; i < numCircles; i++) {
            double r;
            if (minR != maxR) {
                r = maxR - i * deltaR;
            } else {
                r = minR;
            }
            int x, y;
            if (type == LayoutType.PATTERN) {
                // http://extremelearning.com.au/unreasonable-effectiveness-of-quasirandom-sequences/
                x = (int) (width * ((0.5 + a1 * (i + 1)) % 1));
                y = (int) (height * ((0.5 + a2 * (i + 1)) % 1));
            } else if (type == LayoutType.RANDOM) {
                x = rand.nextInt(width);
                y = rand.nextInt(height);
            } else {
                throw new IllegalStateException("type = " + type);
            }

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

                float centerShiftX = (float) (r * cosAngle * cosElevation);
                float centerShiftY = (float) (r * sinAngle * cosElevation);

                Paint gradientPaint = new RadialGradientPaint(
                    x + centerShiftX, y + centerShiftY, (float) r,
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