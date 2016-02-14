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

package pixelitor.filters;

import net.jafama.FastMath;
import pixelitor.filters.gui.GradientParam;
import pixelitor.filters.gui.GroupedRangeParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.utils.BasicProgressTracker;
import pixelitor.utils.ProgressTracker;
import pixelitor.utils.ReseedSupport;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.Random;

import static pixelitor.filters.gui.RandomizePolicy.IGNORE_RANDOMIZE;

/**
 * Renders a fractal tree
 */
public class FractalTree extends FilterWithParametrizedGUI {
    public static final String NAME = "Fractal Tree";

    private static final Color BROWN = new Color(140, 100, 73);
    private static final Color GREEN = new Color(31, 125, 42);
    private static final int QUALITY_BETTER = 1;
    private static final int QUALITY_FASTER = 2;

    private final RangeParam iterations = new RangeParam("Age (Iterations)", 1, 10, 17);
    private final RangeParam angle = new RangeParam("Angle", 1, 20, 45);
    private final RangeParam randomnessParam = new RangeParam("Randomness", 0, 40, 100);
    private final GroupedRangeParam width = new GroupedRangeParam("Width",
            new RangeParam[]{
                    new RangeParam("Overall", 100, 100, 300),
                    new RangeParam("Trunk", 100, 200, 500),
            },
            false);

    private final RangeParam zoom = new RangeParam("Zoom", 10, 100, 200);
    private final RangeParam curvedness = new RangeParam("Curvedness", 0, 10, 50);
    private final GroupedRangeParam physics = new GroupedRangeParam("Physics",
            "Gravity", "Wind", -100, 0, 100, false);
    private final IntChoiceParam quality = new IntChoiceParam("Quality",
            new IntChoiceParam.Value[]{
                    new IntChoiceParam.Value("Better", QUALITY_BETTER),
                    new IntChoiceParam.Value("Faster", QUALITY_FASTER)
            }, IGNORE_RANDOMIZE);

    // precalculated objects for the various depths
    private Stroke[] widthLookup;
    private Color[] colorLookup;
    private Physics[] physicsLookup;
    private boolean doPhysics;
    private boolean leftFirst;
    private boolean hasRandomness;

    private final GradientParam colors = new GradientParam("Colors",
            new float[]{0.25f, 0.75f},
            new Color[]{BROWN, GREEN}, IGNORE_RANDOMIZE);
    private double defaultLength;
    private double randPercent;
    private double lengthDeviation;
    private double angleDeviation;

    private ProgressTracker pt;

    public FractalTree() {
        super(ShowOriginal.NO);
        setParamSet(new ParamSet(
                iterations,
                zoom,
                randomnessParam,
                curvedness,
                angle,
                physics.setShowLinkedCB(false),
                width.setShowLinkedCB(false),
                colors,
                quality
        ).withAction(ReseedSupport.createAction()));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        ReseedSupport.reInitialize();
        Random rand = ReseedSupport.getRand();
        leftFirst = true;

        defaultLength = zoom.getValue() / 10.0;
        randPercent = randomnessParam.getValue() / 100.0;
        hasRandomness = randomnessParam.getValue() > 0;
        lengthDeviation = defaultLength * randPercent;
        angleDeviation = 10.0 * randPercent;

        Graphics2D g = dest.createGraphics();
        if (quality.getValue() == QUALITY_BETTER) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }

        int maxDepth = iterations.getValue();
        widthLookup = new Stroke[maxDepth + 1];
        colorLookup = new Color[maxDepth + 1];

        int gravity = physics.getValue(0);
        int wind = physics.getValue(1);
        if (gravity != 0 || wind != 0) {
            doPhysics = true;
            physicsLookup = new Physics[maxDepth + 1];
        } else {
            doPhysics = false;
            physicsLookup = null;
        }

        for (int depth = 1; depth <= maxDepth; depth++) {
            float w1 = depth * width.getValueAsPercentage(0);
            double trunkWidth = (double) width.getValueAsPercentage(1);
            double base = Math.pow(trunkWidth, 1.0 / (maxDepth - 1));
            double w2 = Math.pow(base, depth - 1);
            float strokeWidth = (float) (w1 * w2);
            float zoomedStrokeWidth = (strokeWidth * zoom.getValue()) / zoom.getDefaultValue();
            widthLookup[depth] = new BasicStroke(zoomedStrokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
            // colors
            float where = ((float) depth) / iterations.getValue();
            int rgb = colors.getValue().getColor(1.0f - where);
            colorLookup[depth] = new Color(rgb);

            float strokeWidth2 = strokeWidth;
            if (doPhysics) {
                physicsLookup[depth] = new Physics(gravity, wind, strokeWidth2);
            }
        }

        float c = curvedness.getValueAsPercentage();
        if (rand.nextBoolean()) {
            c = -c;
        }

        int drawTreeCalls = 2;
        for(int i = 1; i < maxDepth; i++) {
            drawTreeCalls *= 2;
        }
        drawTreeCalls--;
        pt = new BasicProgressTracker(NAME, drawTreeCalls);

        drawTree(g, src.getWidth() / 2.0, src.getHeight(), 270 + calcAngleRandomness(rand), maxDepth, rand, c);

        g.dispose();
        pt.finish();

        return dest;
    }

    private void drawTree(Graphics2D g, double x1, double y1, double angle, int depth, Random rand, float c) {
        if (depth == 0) {
            return;
        }

        int nextDepth = depth - 1;
        c = -c; // change the direction of the curvature in each iteration

        if (doPhysics) {
            angle = adjustPhysics(angle, depth);
        }

        double angleRad = Math.toRadians(angle);
        double x2 = x1 + FastMath.cos(angleRad) * depth * calcRandomLength(rand);
        double y2 = y1 + FastMath.sin(angleRad) * depth * calcRandomLength(rand);

        g.setStroke(widthLookup[depth]);
        if (quality.getValue() == QUALITY_BETTER) {
            if (depth == 1) {
                g.setColor(colorLookup[depth]);
            } else {
                g.setPaint(new GradientPaint(
                        (float) x1, (float) y1, colorLookup[depth],
                        (float) x2, (float) y2, colorLookup[(nextDepth)]));
            }
        } else {
            g.setColor(colorLookup[depth]);
        }

        connectPoints(g, x1, y1, x2, y2, c);

        int split = this.angle.getValue();

        double leftBranchAngle = angle - split + calcAngleRandomness(rand);
        double rightBranchAngle = angle + split + calcAngleRandomness(rand);

        pt.unitDone();

        leftFirst = !leftFirst;
        if (leftFirst) {
            drawTree(g, x2, y2, leftBranchAngle, nextDepth, rand, c);
            drawTree(g, x2, y2, rightBranchAngle, nextDepth, rand, c);
        } else {
            drawTree(g, x2, y2, rightBranchAngle, nextDepth, rand, c);
            drawTree(g, x2, y2, leftBranchAngle, nextDepth, rand, c);
        }
    }

    private double adjustPhysics(double angle, int depth) {
        assert doPhysics;

        // make sure we have the angle in the range 0-360
        angle += 720;
        angle = angle % 360;

        Physics p = physicsLookup[depth];

        if (angle < 90) {
            angle += (90 - angle) * p.gravityStrength;
            angle -= (angle / 90.0) * p.windStrength;
        } else if (angle < 180) {
            angle -= (angle - 90) * p.gravityStrength;
            angle -= (180 - angle) * p.windStrength;
        } else if (angle < 270) {
            angle -= (270 - angle) * p.gravityStrength;
            angle += (angle - 180) * p.windStrength;
        } else if (angle <= 360) {
            angle += (angle - 270) * p.gravityStrength;
            angle += (360 - angle) * p.windStrength;
        } else {
            throw new IllegalStateException("angle = " + angle);
        }

        return angle;
    }

    private static void connectPoints(Graphics2D g, double x1, double y1, double x2, double y2, float c) {
        if (c == 0) {
            Line2D.Double line = new Line2D.Double(x1, y1, x2, y2);
            g.draw(line);
        } else {
            Path2D.Double path = new Path2D.Double();
            path.moveTo(x1, y1);

            double dx = x2 - x1;
            double dy = y2 - y1;

            // center point
            double cx = x1 + dx / 2.0;
            double cy = y1 + dy / 2.0;

            // We calculate only one Bezier control point,
            // and use it for both.
            // The normal vector is -dy, dx.
            double ctrlX = cx - dy * c;
            double ctrlY = cy + dx * c;

            path.curveTo(ctrlX, ctrlY, ctrlX, ctrlY, x2, y2);
            g.draw(path);
        }
    }

    private double calcAngleRandomness(Random rand) {
        if (!hasRandomness) {
            return 0;
        }

        double retVal = -angleDeviation + rand.nextDouble() * 2 * angleDeviation;
//        System.out.println(String.format("FractalTree::calcAngleRandomness: retVal = %.2f", retVal));
        return retVal;
    }

    private double calcRandomLength(Random rand) {
        if (!hasRandomness) {
            return defaultLength;
        }

        double minLength = defaultLength - lengthDeviation;

        return (minLength + 2 * lengthDeviation * rand.nextDouble());
    }

    private static class Physics {
        public final double gravityStrength;
        public final double windStrength;

        private Physics(int gravity, int wind, float strokeWidth2) {
            double effectStrength = 0.02 / strokeWidth2;

            gravityStrength = effectStrength * gravity;
            windStrength = effectStrength * wind;
        }
    }
}