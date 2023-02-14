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

package pixelitor.filters;

import pixelitor.colors.Colors;
import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.DialogParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.utils.ReseedSupport;
import pixelitor.utils.StatusBarProgressTracker;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static pixelitor.filters.gui.RandomizePolicy.IGNORE_RANDOMIZE;

/**
 * The "Abstract Lights" filter.
 * The algorithm is based on <a href="https://codepen.io/tsuhre/pen/BYbjyg">this codepen by Ben Matthews</a>.
 */
public class AbstractLights extends ParametrizedFilter {
    @Serial
    private static final long serialVersionUID = 1L;
    private static final float CRITICAL_ALPHA = 0.002f;

    public static String NAME = "Abstract Lights";

    private final RangeParam iterationsParam = new RangeParam("Iterations", 1, 1000, 5000);
    private final RangeParam complexityParam = new RangeParam("Complexity", 1, 10, 20);
    private final RangeParam brightnessParam = new RangeParam("Brightness", 1, 6, 10);
    private final AngleParam hueParam = new AngleParam("Hue", 0);
    private final RangeParam hueRandomnessParam = new RangeParam("Hue Variability", 0, 25, 100);
    private final RangeParam whiteParam = new RangeParam("Mix White", 0, 0, 100);
    private final RangeParam blurParam = new RangeParam("Blur", 0, 0, 7);
    private final RangeParam speedParam = new RangeParam("Particle Speed", 1, 1, 10);
    private final BooleanParam bounceParam = new BooleanParam("Edge Bounce", true);

    public AbstractLights() {
        super(false);

        // there can be no hue variation if the complexity is 1
        complexityParam.setupDisableOtherIf(hueRandomnessParam, value -> value == 1);

        // the hue doesn't matter if it's all white
        whiteParam.setupDisableOtherIf(hueParam, value -> value == 100);
        whiteParam.setupDisableOtherIf(hueRandomnessParam, value -> value == 100);

        DialogParam advancedParam = new DialogParam("Advanced",
                hueRandomnessParam, whiteParam, blurParam, speedParam, bounceParam);
        advancedParam.setRandomizePolicy(IGNORE_RANDOMIZE);

        setParams(iterationsParam,
                complexityParam,
                brightnessParam,
                hueParam,
                advancedParam
        ).withAction(ReseedSupport.createAction());
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        Random random = ReseedSupport.getLastSeedRandom();
        int iterations = iterationsParam.getValue();

        var pt = new StatusBarProgressTracker(NAME, iterations);

        Graphics2D g2 = dest.createGraphics();

        double lineWidth = blurParam.getValueAsDouble() + 1.0;
        g2.setStroke(new BasicStroke((float) lineWidth));

        g2.setColor(Color.BLACK);
        int width = dest.getWidth();
        int height = dest.getHeight();
        g2.fillRect(0, 0, width, height);

        float darkening = 1.0f;
        // the sqrt is an attempt to use linear light calculations
        float alpha = (float) (brightnessParam.getValueAsDouble() / (200.0 * Math.sqrt(lineWidth)));
        if (alpha < CRITICAL_ALPHA) {
            // if a smaller alpha is used, nothing is drawn, therefore
            // use this alpha and compensate by darkening the color.
            darkening = CRITICAL_ALPHA / alpha;
            alpha = CRITICAL_ALPHA;
        }

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

        float colorBri = 1.0f / darkening;
        List<FPoint> points = createPoints(width, height, random, colorBri);

        for (int i = 0; i < iterations; i++) {
            for (FPoint p : points) {
                p.updateAndDraw(width, height, g2);
            }
            pt.unitDone();
        }
        pt.finished();

        return dest;
    }

    private List<FPoint> createPoints(int width, int height, Random random, float bri) {
        int numPoints = complexityParam.getValue() + 1;
        int baseHue = hueParam.getValueInNonIntuitiveDegrees();
        int hueRandomness = (int) (hueRandomnessParam.getValue() * 3.6);

        boolean bounce = bounceParam.isChecked();
        double speed = speedParam.getValueAsDouble();

        ArrayList<FPoint> points = new ArrayList<>();
        for (int i = 0; i < numPoints; i++) {
            int x = random.nextInt(width);
            int y = random.nextInt(height);
//            int x = width / 2;
//            int y = height / 2;

            int hue;
            if (hueRandomness > 0) {
                hue = (baseHue + random.nextInt(hueRandomness) - hueRandomness / 2) % 360;
            } else {
                hue = baseHue;
                random.nextInt(); // keep the same image 
            }

            Color color = Color.getHSBColor(hue / 360.0f, 1.0f, bri);
            if (whiteParam.getValue() > 0) {
                color = Colors.rgbInterpolate(color, Color.WHITE, (float) whiteParam.getPercentage());
            }

            double angle = 2 * random.nextDouble() * Math.PI;
//            double angle = 2.0 * i * Math.PI / numPoints;

            points.add(new FPoint(x, y, speed, angle, color, bounce));
        }

        for (int i = 0; i < numPoints; i++) {
            int siblingIndex = i - 1;
            if (i == 0) {
                siblingIndex = numPoints - 1;
            }
            points.get(i).sibling = points.get(siblingIndex);
        }

        return points;
    }

    static class FPoint {
        private double x, y;
        private double vx, vy;
        private final Color color;
        public FPoint sibling;
        private final boolean bounce;

        public FPoint(int x, int y, double speed, double angle, Color color, boolean bounce) {
            this.x = x;
            this.y = y;
            this.vx = speed * Math.cos(angle);
            this.vy = speed * Math.sin(angle);
            this.color = color;
            this.bounce = bounce;
        }

        public void updateAndDraw(int width, int height, Graphics2D g) {
            x += vx;
            y += vy;

            if (bounce) {
                if (x < 0 || x >= width) {
                    vx = -vx;
                }
                if (y < 0 || y >= height) {
                    vy = -vy;
                }
            }

            g.setColor(color);
            g.draw(new Line2D.Double(x, y, sibling.x, sibling.y));
        }
    }
}
