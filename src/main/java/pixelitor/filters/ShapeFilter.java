/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.DialogParam;
import pixelitor.filters.gui.EffectsParam;
import pixelitor.filters.gui.GroupedRangeParam;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.IntChoiceParam.Value;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.filters.gui.StrokeParam;
import pixelitor.filters.painters.AreaEffects;
import pixelitor.utils.ImageUtils;

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
import static java.awt.image.BufferedImage.TYPE_INT_RGB;
import static pixelitor.colors.FgBgColors.getBGColor;
import static pixelitor.colors.FgBgColors.getFGColor;
import static pixelitor.filters.gui.RandomizePolicy.IGNORE_RANDOMIZE;

/**
 * Abstract superclass for the shape filters
 */
public abstract class ShapeFilter extends ParametrizedFilter {
    private static final int BG_BLACK = 1;
    private static final int BG_ORIGINAL = 2;
    private static final int BG_TRANSPARENT = 3;
    private static final int BG_TOOL = 4;

    private static final int FG_WHITE = 5;
    private static final int FG_BLACK = 6;
    private static final int FG_TOOL = 7;
    private static final int FG_GRADIENT = 8;
    private static final int FG_TRANSPARENT = 9;

    private static final Color DARK_GREEN = new Color(0, 120, 0);
    private static final Color PURPLE = new Color(155, 0, 155);

    private final StrokeParam strokeParam = new StrokeParam("Stroke Settings");
    private final EffectsParam effectsParam = new EffectsParam("Effects");

    private final IntChoiceParam background = new IntChoiceParam("Background", new Value[]{
            new Value("Black", BG_BLACK),
            new Value("Original Image", BG_ORIGINAL),
            new Value("Transparent", BG_TRANSPARENT),
            new Value("Background Color", BG_TOOL),
    }, IGNORE_RANDOMIZE);

    private final IntChoiceParam foreground = new IntChoiceParam("Foreground", new Value[]{
            new Value("White", FG_WHITE),
            new Value("Black", FG_BLACK),
            new Value("Radial Gradient", FG_GRADIENT),
            new Value("Foreground Color", FG_TOOL),
            new Value("Transparent", FG_TRANSPARENT),
    }, IGNORE_RANDOMIZE);

    private final BooleanParam waterMark = new BooleanParam("Watermarking", false);

    protected final ImagePositionParam center = new ImagePositionParam("Center");
    private final GroupedRangeParam scale = new GroupedRangeParam("Scale (%)", 1, 100, 500, false);

    protected ShapeFilter() {
        super(ShowOriginal.NO);

        setParams(
                background,
                foreground,
                waterMark,
                new DialogParam("Transform", center, scale),
                strokeParam,
                effectsParam
        );

        // disable effects if foreground is set to transparent
        foreground.setupDisableOtherIf(effectsParam,
                selectedValue -> selectedValue.getValue() == FG_TRANSPARENT);

        // disable foreground and background if watermarking is selected
        waterMark.setupDisableOtherIfChecked(foreground);
        waterMark.setupDisableOtherIfChecked(background);
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();

        Graphics2D g2;
        BufferedImage bumpImage = null;

        if (waterMark.isChecked()) {
            bumpImage = new BufferedImage(srcWidth, srcHeight, TYPE_INT_RGB);
            g2 = bumpImage.createGraphics();
            g2.setColor(BLACK);
            g2.fillRect(0, 0, srcWidth, srcHeight);
            g2.setColor(Color.GRAY);
        } else {
            dest = ImageUtils.createImageWithSameCM(src);
            g2 = dest.createGraphics();
            setupBackground(src, srcWidth, srcHeight, g2);
            setupForeground(srcWidth, srcHeight, g2);
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

            // work with the outline so that we can have "inner glow"
            Shape outline = stroke.createStrokedShape(shape);

            g2.fill(outline);

            AreaEffects effects = effectsParam.getEffects();
            effects.drawOn(g2, outline);
        }

        g2.dispose();

        if(waterMark.isChecked()) {
            dest = ImageUtils.bumpMap(src, bumpImage, getName());
        }

        return dest;
    }

    private void setupBackground(BufferedImage src, int srcWidth, int srcHeight, Graphics2D g2) {
        int bg = background.getValue();
        switch (bg) {
            case BG_BLACK:
                g2.setColor(BLACK);
                g2.fillRect(0, 0, srcWidth, srcHeight);
                break;
            case BG_TOOL:
                g2.setColor(getBGColor());
                g2.fillRect(0, 0, srcWidth, srcHeight);
                break;
            case BG_ORIGINAL:
                g2.drawImage(src, 0, 0, null);
                break;
            case BG_TRANSPARENT:
                // do nothing
                break;
        }
    }

    private void setupForeground(int srcWidth, int srcHeight, Graphics2D g2) {
        int fg = foreground.getValue();
        switch (fg) {
            case FG_WHITE:
                g2.setColor(WHITE);
                break;
            case FG_BLACK:
                g2.setColor(BLACK);
                break;
            case FG_TOOL:
                g2.setColor(getFGColor());
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
    }

    protected float getGradientRadius(float cx, float cy) {
        return (float) Math.sqrt(cx * cx + cy * cy);
    }

    protected abstract Shape createShape(int width, int height);

    @Override
    protected boolean createDefaultDestImg() {
        return false;
    }

    @Override
    public boolean supportsGray() {
        return !waterMark.isChecked();
    }
}