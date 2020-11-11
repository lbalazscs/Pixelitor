/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

import org.jdesktop.swingx.painter.effects.GlowPathEffect;
import org.jdesktop.swingx.painter.effects.NeonBorderEffect;
import org.jdesktop.swingx.painter.effects.ShadowPathEffect;
import pixelitor.colors.Colors;
import pixelitor.filters.gui.*;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.painters.AreaEffects;
import pixelitor.gui.GUIText;
import pixelitor.utils.ImageUtils;

import java.awt.*;
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

    private final StrokeParam strokeParam = new StrokeParam("Stroke Settings");
    private final EffectsParam effectsParam = new EffectsParam("Effects");

    private final IntChoiceParam background = new IntChoiceParam("Background", new Item[]{
        new Item("Black", BG_BLACK),
        new Item("Original Image", BG_ORIGINAL),
        new Item("Transparent", BG_TRANSPARENT),
        new Item(GUIText.BG_COLOR, BG_TOOL),
    }, IGNORE_RANDOMIZE);

    private final IntChoiceParam foreground = new IntChoiceParam("Foreground", new Item[]{
        new Item("White", FG_WHITE),
        new Item("Black", FG_BLACK),
        new Item("Radial Gradient", FG_GRADIENT),
        new Item(GUIText.FG_COLOR, FG_TOOL),
        new Item("Transparent", FG_TRANSPARENT),
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

        boolean watermarking = waterMark.isChecked();
        if (watermarking) {
            bumpImage = new BufferedImage(srcWidth, srcHeight, TYPE_INT_RGB);
            g2 = bumpImage.createGraphics();
            Colors.fillWith(BLACK, g2, srcWidth, srcHeight);
            g2.setColor(Color.GRAY);
        } else {
            dest = ImageUtils.createImageWithSameCM(src);
            g2 = dest.createGraphics();
            fillBackground(src, g2, srcWidth, srcHeight);
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
                var at = AffineTransform.getTranslateInstance
                        (cx - scaleX * cx, cy - scaleY * cy);
                at.scale(scaleX, scaleY);

                shape = at.createTransformedShape(shape);
            }

            AreaEffects effects = effectsParam.getEffects();
//            if (effects.getInnerGlow() != null) {
            // work with the outline so that we can have "inner glow"
            shape = stroke.createStrokedShape(shape);
            g2.fill(shape);
//            } else {
//                g2.setStroke(stroke);
//                g2.draw(shape);
//            }

            // If there are effects and the foreground is set to transparent,
            // then the effects have to be run on a temporary image, because
            // they also set the composite. Also inner glow is ignored.
            if (!effects.isEmpty()) {
                if (!watermarking && foreground.getValue() == FG_TRANSPARENT) {
                    drawEffectsWithTransparency(effects, g2, shape, srcWidth, srcHeight);
                } else { // the simple case
                    effects.drawOn(g2, shape);
                }
            }
        }

        g2.dispose();

        if (watermarking) {
            dest = ImageUtils.bumpMap(src, bumpImage, getName());
            bumpImage.flush();
        }

        return dest;
    }

    private static void drawEffectsWithTransparency(AreaEffects effects, Graphics2D g2, Shape shape, int srcWidth, int srcHeight) {
        GlowPathEffect glow = effects.getGlow();
        NeonBorderEffect neonBorder = effects.getNeonBorder();
        ShadowPathEffect dropShadow = effects.getDropShadow();
        if (glow != null || neonBorder != null || dropShadow != null) {
            var maskImage = ImageUtils.createSysCompatibleImage(srcWidth, srcHeight);
            Graphics2D tmpG = maskImage.createGraphics();
            // fill with transparent pixels
            tmpG.setComposite(AlphaComposite.Clear);
            tmpG.fillRect(0, 0, srcWidth, srcHeight);

            // reset the composite to normal.
            tmpG.setComposite(AlphaComposite.SrcOver);

            // check the effects individually in order to skip inner glow.
            if (glow != null) {
                glow.apply(tmpG, shape, 0, 0);
            }
            if (neonBorder != null) {
                neonBorder.apply(tmpG, shape, 0, 0);
            }
            if (dropShadow != null) {
                dropShadow.apply(tmpG, shape, 0, 0);
            }
            tmpG.dispose();

            // the effect colors won't matter, only their opacity
            g2.setComposite(AlphaComposite.DstOut);
            g2.drawImage(maskImage, 0, 0, null);
            maskImage.flush();
        }
    }

    private void fillBackground(BufferedImage src, Graphics2D g2, int srcWidth, int srcHeight) {
        int bg = background.getValue();
        switch (bg) {
            case BG_BLACK -> Colors.fillWith(BLACK, g2, srcWidth, srcHeight);
            case BG_TOOL -> Colors.fillWith(getBGColor(), g2, srcWidth, srcHeight);
            case BG_ORIGINAL -> g2.drawImage(src, 0, 0, null);
            case BG_TRANSPARENT -> {
            }
            default -> throw new IllegalStateException("Unexpected value: " + bg);
        }
    }

    private void setupForeground(int srcWidth, int srcHeight, Graphics2D g2) {
        int fg = foreground.getValue();
        switch (fg) {
            case FG_WHITE -> g2.setColor(WHITE);
            case FG_BLACK -> g2.setColor(BLACK);
            case FG_TOOL -> g2.setColor(getFGColor());
            case FG_GRADIENT -> setupGradientForeground(g2, srcWidth, srcHeight);
            case FG_TRANSPARENT -> g2.setComposite(AlphaComposite.Clear);
            default -> throw new IllegalStateException("Unexpected value: " + fg);
        }
    }

    private void setupGradientForeground(Graphics2D g2, int srcWidth, int srcHeight) {
        float cx = srcWidth / 2.0f;
        float cy = srcHeight / 2.0f;
        float radius = getGradientRadius(cx, cy);
        float[] fractions = {0.0f, 1.0f};
        Color[] colors = {getFGColor(), getBGColor()};
        g2.setPaint(new RadialGradientPaint(cx, cy, radius, fractions, colors));
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