/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.tools.brushes;

import pixelitor.tools.util.PPoint;
import pixelitor.utils.ImageUtils;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;

import static java.awt.RenderingHints.KEY_INTERPOLATION;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

/**
 * A {@link DabsBrush} that uses images as dabs.
 */
public class ImageDabsBrush extends DabsBrush {
    private static final Map<ImageBrushType, BufferedImage> templateImages
        = new EnumMap<>(ImageBrushType.class);

    // a black-and-white, unchanging image, which defines the brush's texture
    private final BufferedImage templateImg;

    // a colorized version of the template, using the current color for
    // the pixels and the template's grayscale values for the transparency
    private BufferedImage coloredBrushImg;

    // the colorized image, scaled to match the current
    // brush diameter, and used as the stamp/dab of the brush
    private BufferedImage finalScaledImg;

    private Color lastColor;

    public ImageDabsBrush(double radius, ImageBrushType imageBrushType,
                          double spacingRatio, RotationSettings rotationSettings) {
        super(radius, new RadiusRatioSpacing(spacingRatio),
            rotationSettings, false);

        // share template images between instances of the same brush type
        templateImg = templateImages.computeIfAbsent(imageBrushType,
            ImageBrushType::createTemplateImage);
    }

    @Override
    public void setRadius(double radius) {
        super.setRadius(radius);

        // if the radius changes during the brush stroke
        // via hotkeys, recreate the brush image
        if (targetG != null) {
            // the point argument isn't used by this class
            initBrushStamp(null);
        }
    }

    @Override
    void initBrushStamp(PPoint p) {
        assert diameter > 0 : "zero diameter in " + getClass().getName();
        Color currentColor = targetG.getColor();

        boolean colorChanged = !currentColor.equals(lastColor);
        if (colorChanged) {
            recreateColoredBrushImage(currentColor);
            lastColor = currentColor;
        }

        // always check size, as it might have changed even if color hasn't
        recreateFinalScaledImage(diameter, colorChanged);
    }

    /**
     * Recreates the final scaled image from the colorized image.
     */
    private void recreateFinalScaledImage(double diameter, boolean colorChanged) {
        int newSize = (int) diameter;
        assert newSize > 0 : "newSize = " + newSize;
        if (!colorChanged && isBrushImageSize(newSize)) {
            return; // nothing changed
        }

        if (finalScaledImg != null) {
            finalScaledImg.flush();
        }
        finalScaledImg = new BufferedImage(newSize, newSize, TYPE_INT_ARGB);
        Graphics2D g = finalScaledImg.createGraphics();
        g.drawImage(coloredBrushImg, 0, 0, newSize, newSize, null);
        g.dispose();
    }

    private boolean isBrushImageSize(double size) {
        return finalScaledImg != null && finalScaledImg.getWidth() == size;
    }

    /**
     * Recreates a colorized brush image from the template image
     * using the given color.
     */
    private void recreateColoredBrushImage(Color color) {
        int colorNoAlpha = color.getRGB() & 0x00_FF_FF_FF;
        coloredBrushImg = ImageUtils.maskToTransparency(templateImg, colorNoAlpha);
    }

    @Override
    public void putDab(PPoint currentPoint, double angle) {
        assert finalScaledImg != null;

        double x = currentPoint.getImX();
        double y = currentPoint.getImY();
        int drawStartX = (int) (x - radius);
        int drawStartY = (int) (y - radius);

        if (!settings.isDirectional() || angle == 0) {
            targetG.drawImage(finalScaledImg, drawStartX, drawStartY, null);
        } else {
            // draw rotated image
            var origTransform = targetG.getTransform();
            targetG.rotate(angle, x, y);
            targetG.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR);
            targetG.drawImage(finalScaledImg, drawStartX, drawStartY, null);
            targetG.setTransform(origTransform);
        }

        repaintComp(currentPoint);
    }

    @Override
    public void dispose() {
        super.dispose();
        if (coloredBrushImg != null) {
            coloredBrushImg.flush();
            coloredBrushImg = null;
        }
        if (finalScaledImg != null) {
            finalScaledImg.flush();
            finalScaledImg = null;
        }
    }    
}
