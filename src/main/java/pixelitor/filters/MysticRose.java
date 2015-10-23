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

package pixelitor.filters;

import pixelitor.FgBgColors;
import pixelitor.filters.gui.EffectsParam;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.StrokeParam;
import pixelitor.filters.painters.AreaEffects;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Utils;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;

/**
 * Mystic Rose
 */
public class MysticRose extends FilterWithParametrizedGUI {
    private static final int BG_BLACK = 1;
    private static final int BG_ORIGINAL = 2;
    private static final int BG_TRANSPARENT = 3;
    private static final int BG_TOOL = 4;

    private static final int FG_WHITE = 5;
    private static final int FG_TOOL = 6;
    private static final int FG_TRANSPARENT = 7;

    private final ImagePositionParam center = new ImagePositionParam("Center");
    private final RangeParam nrPoints = new RangeParam("Number of Points", 3, 42, 10);
    private final RangeParam radius = new RangeParam("Radius (Height %)", 1, 200, 45);
    private final RangeParam rotate = new RangeParam("Rotate", 0, 100, 0);
    private final StrokeParam strokeParam = new StrokeParam("Stroke Settings");
    private final EffectsParam effectsParam = new EffectsParam("Effects", false, true);

    private final IntChoiceParam background = new IntChoiceParam("Background",
            new IntChoiceParam.Value[]{
                    new IntChoiceParam.Value("Black", BG_BLACK),
                    new IntChoiceParam.Value("Original Image", BG_ORIGINAL),
                    new IntChoiceParam.Value("Transparent", BG_TRANSPARENT),
                    new IntChoiceParam.Value("Tool Background", BG_TOOL),
            });

    private final IntChoiceParam foreground = new IntChoiceParam("Foreground",
            new IntChoiceParam.Value[]{
                    new IntChoiceParam.Value("White", FG_WHITE),
                    new IntChoiceParam.Value("Tool Foreground", FG_TOOL),
                    new IntChoiceParam.Value("Transparent", FG_TRANSPARENT),
            });

    public MysticRose() {
        super("Mystic Rose", false, false);
        setParamSet(new ParamSet(
                nrPoints,
                rotate,
                center,
                radius,
                background,
                foreground,
                strokeParam,
                effectsParam
        ));

        // disable effects if foreground is set to transparent
        Utils.setupDisableOtherIf(foreground, effectsParam,
                selectedValue -> selectedValue.getIntValue() == FG_TRANSPARENT);
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();

        dest = ImageUtils.createCompatibleDest(src);
        Graphics2D g2 = dest.createGraphics();

        int bgVal = background.getValue();
        switch (bgVal) {
            case BG_BLACK:
                g2.setColor(BLACK);
                g2.fillRect(0, 0, srcWidth, srcHeight);
                break;
            case BG_TOOL:
                g2.setColor(FgBgColors.getBG());
                g2.fillRect(0, 0, srcWidth, srcHeight);
                break;
            case BG_ORIGINAL:
                g2.drawImage(src, 0, 0, null);
                break;
            case BG_TRANSPARENT:
                // do nothing
                break;
        }

        int fgVal = foreground.getValue();
        switch (fgVal) {
            case FG_WHITE:
                g2.setColor(WHITE);
                break;
            case FG_TOOL:
                g2.setColor(FgBgColors.getFG());
                break;
            case FG_TRANSPARENT:
                g2.setComposite(AlphaComposite.Clear);
                break;
        }

        Stroke stroke = strokeParam.createStroke();
//        g2.setStroke(stroke);
        g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

        Path2D shape = createShape(srcWidth, srcHeight);
        // work with the outline so that we can have "inner glow"
        Shape outline = stroke.createStrokedShape(shape);

        g2.fill(outline);

        AreaEffects effects = effectsParam.getEffects();
        effects.apply(g2, outline);

        g2.dispose();

        return dest;
    }

    private Path2D createShape(int srcWidth, int srcHeight) {
        Point2D[] points = calcPoints(srcWidth, srcHeight);

        Path2D shape = new Path2D.Double();
        for (int i = 0; i < points.length; i++) {
            for (int j = 0; j < points.length; j++) {
                if(i > j) { // draw only in one direction
                    shape.moveTo(points[i].getX(), points[i].getY());
                    shape.lineTo(points[j].getX(), points[j].getY());
                }
            }
        }
        return shape;
    }

    private Point2D[] calcPoints(int srcWidth, int srcHeight) {
        int numPoints = nrPoints.getValue();
        Point2D[] points = new Point2D[numPoints];
        double r = radius.getValueAsPercentage() * srcHeight;
        double startAngle = 2 * Math.PI / numPoints * rotate.getValueAsPercentage();
        double cx = srcWidth * center.getRelativeX();
        double cy = srcHeight * center.getRelativeY();

        for (int i = 0; i < points.length; i++) {
            double theta = startAngle + 2 * Math.PI * i / numPoints;
            points[i] = new Point2D.Double((cx + r * Math.cos(theta)), (cy + r * Math.sin(theta)));
        }
        return points;
    }
}