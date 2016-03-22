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

import pixelitor.colors.FgBgColors;
import pixelitor.filters.gui.DialogParam;
import pixelitor.filters.gui.EffectsParam;
import pixelitor.filters.gui.GroupedRangeParam;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.filters.gui.StrokeParam;
import pixelitor.filters.painters.AreaEffects;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Utils;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static pixelitor.filters.gui.RandomizePolicy.IGNORE_RANDOMIZE;

/**
 * Abstract superclass for shape filters
 */
public abstract class ShapeFilter extends FilterWithParametrizedGUI {
    private static final int BG_BLACK = 1;
    private static final int BG_ORIGINAL = 2;
    private static final int BG_TRANSPARENT = 3;
    private static final int BG_TOOL = 4;

    private static final int FG_WHITE = 5;
    private static final int FG_TOOL = 6;
    private static final int FG_GRADIENT = 7;
    private static final int FG_TRANSPARENT = 8;

    private static final Color DARK_GREEN = new Color(0, 120, 0);
    private static final Color PURPLE = new Color(155, 0, 155);

    private final StrokeParam strokeParam = new StrokeParam("Stroke Settings");
    private final EffectsParam effectsParam = new EffectsParam("Effects");

    private final IntChoiceParam background = new IntChoiceParam("Background",
            new IntChoiceParam.Value[]{
                    new IntChoiceParam.Value("Black", BG_BLACK),
                    new IntChoiceParam.Value("Original Image", BG_ORIGINAL),
                    new IntChoiceParam.Value("Transparent", BG_TRANSPARENT),
                    new IntChoiceParam.Value("Tool Background", BG_TOOL),
            }, IGNORE_RANDOMIZE);

    private final IntChoiceParam foreground = new IntChoiceParam("Foreground",
            new IntChoiceParam.Value[]{
                    new IntChoiceParam.Value("White", FG_WHITE),
                    new IntChoiceParam.Value("Radial Gradient", FG_GRADIENT),
                    new IntChoiceParam.Value("Tool Foreground", FG_TOOL),
                    new IntChoiceParam.Value("Transparent", FG_TRANSPARENT),
            }, IGNORE_RANDOMIZE);

    protected final ImagePositionParam center = new ImagePositionParam("Center");
    private final GroupedRangeParam scale = new GroupedRangeParam("Scale (%)", 1, 100, 500, false);
    private final DialogParam transformSettings = new DialogParam("Transform", center, scale);

    protected ShapeFilter() {
        super(ShowOriginal.NO);
        setParamSet(new ParamSet(
                foreground,
                background,
                transformSettings,
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

        dest = ImageUtils.createImageWithSameColorModel(src);
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
            case FG_GRADIENT:
                float cx = srcWidth / 2.0f;
                float cy = srcHeight / 2.0f;
                float radius = getGradientRadius(cx, cy);
                float[] fractions = {0.0f, 1.0f};
                Color[] colors = {DARK_GREEN, PURPLE};
                g2.setPaint(new RadialGradientPaint(cx, cy, radius, fractions, colors));
                break;
            case FG_TRANSPARENT:
                g2.setComposite(AlphaComposite.Clear);
                break;
        }

        Stroke stroke = strokeParam.createStroke();
        g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

        Shape shape = createShape(srcWidth, srcHeight);
        if (shape != null) {
            double scaleX = scale.getValueAsPercentage(0);
            double scaleY = scale.getValueAsPercentage(1);

            if (scaleX != 1.0 || scaleY != 1.0) {
                double cx = srcWidth * center.getRelativeX();
                double cy = srcHeight * center.getRelativeY();

                // http://stackoverflow.com/questions/17113234/affine-transform-scale-around-a-point
                AffineTransform at = AffineTransform.getTranslateInstance
                        (cx - scaleX * cx, cy - scaleY * cy);
                at.scale(scaleX, scaleY);

                shape = at.createTransformedShape(shape);
            }

//            g2.setStroke(stroke);

            // work with the outline so that we can have "inner glow"
            Shape outline = stroke.createStrokedShape(shape);

            g2.fill(outline);

            AreaEffects effects = effectsParam.getEffects();
            effects.apply(g2, outline);
        }

        g2.dispose();

        return dest;
    }

    protected float getGradientRadius(float cx, float cy) {
        return (float) Math.sqrt(cx * cx + cy * cy);
    }

    protected abstract Shape createShape(int width, int height);
}