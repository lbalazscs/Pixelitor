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

import pixelitor.filters.gui.*;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.gui.GUIText;
import pixelitor.utils.ReseedSupport;
import pixelitor.utils.Shapes;
import pixelitor.utils.StatusBarProgressTracker;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

import static java.awt.MultipleGradientPaint.CycleMethod.NO_CYCLE;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static pixelitor.colors.FgBgColors.getBGColor;
import static pixelitor.colors.FgBgColors.getFGColor;
import static pixelitor.gui.GUIText.OPACITY;
import static pixelitor.utils.AngleUnit.CCW_DEGREES;

/**
 * Fills the image with random circles
 */
public class RandomSpheres extends ParametrizedFilter {
    public static final String NAME = "Random Spheres";

    private static final int COLORS_SAMPLE_IMAGE = 1;
    private static final int COLORS_FG_BG = 2;

    private final RangeParam radius = new RangeParam(GUIText.RADIUS, 2, 10, 100);
    private final RangeParam density = new RangeParam("Density (%)", 1, 50, 221);

    private final IntChoiceParam colorSource = new IntChoiceParam("Colors Source",
        new Item[]{
            new Item("Sample Image", COLORS_SAMPLE_IMAGE),
            new Item("Use FG, BG Colors", COLORS_FG_BG),
        });
    private final BooleanParam addHighLightsCB = new BooleanParam(
        "Add Highlights", true);
    private final AngleParam highlightAngleSelector = new AngleParam(
        "Light Direction (Azimuth)", 0);
    private final ElevationAngleParam highlightElevationSelector = new ElevationAngleParam(
        "Highlight Elevation", 45, CCW_DEGREES);

    private final RangeParam opacity = new RangeParam(OPACITY, 0, 100, 100);

    public RandomSpheres() {
        super(true);

        // enable "Light Direction" and "Highlight Elevation"
        // only if "Add Highlights" is checked
        addHighLightsCB.setupEnableOtherIfChecked(highlightAngleSelector);
        addHighLightsCB.setupEnableOtherIfChecked(highlightElevationSelector);

        setParams(
            radius.withAdjustedRange(0.1),
            density,
            opacity,
            colorSource,
            addHighLightsCB,
            highlightAngleSelector,
            highlightElevationSelector
        ).withAction(ReseedSupport.createAction());
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        int width = dest.getWidth();
        int height = dest.getHeight();
        float r = radius.getValueAsFloat();
        int numCircles = (int) (width * height * density.getPercentageValD() / (r * r));

        var pt = new StatusBarProgressTracker(NAME, numCircles);

        Random rand = ReseedSupport.reInitialize();

        Graphics2D g = dest.createGraphics();
        g.setComposite(AlphaComposite.SrcOver.derive(opacity.getPercentageValF()));
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

        int colorSrc = colorSource.getValue();

        double angle = highlightAngleSelector.getValueInRadians();
        angle += Math.PI;

        double elevation = highlightElevationSelector.getValueInRadians();
        int centerShiftX = (int) (r * Math.cos(angle) * Math.cos(elevation));
        int centerShiftY = (int) (r * Math.sin(angle) * Math.cos(elevation));

        Color[] colors = null;
        Color c = null;

        if (colorSrc == COLORS_FG_BG) {
            colors = new Color[]{getFGColor(), getBGColor()};
            c = colors[0];
        }

        boolean addHighlights = addHighLightsCB.isChecked();

        for (int i = 0; i < numCircles; i++) {
            int x = rand.nextInt(width);
            int y = rand.nextInt(height);

            // could be faster, but the main bottleneck is
            // the highlights generation anyway
            int srcColor = src.getRGB(x, y);
            int alpha = (srcColor >>> 24) & 0xFF;
            if (alpha == 0) {
                continue;
            }

            if (colorSrc == COLORS_SAMPLE_IMAGE) {
                c = new Color(srcColor);
                if (addHighlights) {
                    colors = new Color[]{c.brighter().brighter(), c};
                }
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