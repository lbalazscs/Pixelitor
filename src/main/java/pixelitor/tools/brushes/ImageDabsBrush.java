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
 * A {@link DabsBrush} where the dabs are images
 */
public class ImageDabsBrush extends DabsBrush {
    private static final Map<ImageBrushType, BufferedImage> templateImages
        = new EnumMap<>(ImageBrushType.class);
    private final BufferedImage templateImg;
    private BufferedImage coloredBrushImg;
    private BufferedImage finalScaledImg;
    private Color lastColor;

    public ImageDabsBrush(double radius, ImageBrushType imageBrushType,
                          double spacingRatio, AngleSettings angleSettings) {
        super(radius, new RadiusRatioSpacing(spacingRatio),
            angleSettings, false);

        // for each brush type multiple brush instances are created because
        // of the symmetry, but the template image can be shared between them
        templateImg = templateImages.computeIfAbsent(imageBrushType,
            ImageBrushType::createBWBrushImage);
    }

    @Override
    public void setRadius(double radius) {
        super.setRadius(radius);

        // if the radius changes during the brush stroke
        // via hotkeys, the brush image has to be recreated
        if (targetG != null) {
            // the point argument is not used by this class
            setupBrushStamp(null);
        }
    }

    @Override
    void setupBrushStamp(PPoint p) {
        assert diameter > 0 : "zero diameter in " + getClass().getName();
        Color currColor = targetG.getColor();

        if (!currColor.equals(lastColor)) {
            colorizeBrushImage(currColor);
            lastColor = currColor;
            recreateBrushImage(diameter, true);
        } else {
            recreateBrushImage(diameter, false);
        }
    }

    private void recreateBrushImage(double newSize, boolean colorChanged) {
        if (!colorChanged && brushImageHasSize(newSize)) {
            return;
        }

        // if the color changed, then recreate it no matter what the size is
        if (finalScaledImg != null) {
            finalScaledImg.flush();
        }

        int newSizeInt = (int) newSize;
        assert newSizeInt > 0 : "newSize = " + newSize;
        finalScaledImg = new BufferedImage(newSizeInt, newSizeInt, TYPE_INT_ARGB);
        Graphics2D g = finalScaledImg.createGraphics();
        g.drawImage(coloredBrushImg, 0, 0, newSizeInt, newSizeInt, null);
        g.dispose();
    }

    private boolean brushImageHasSize(double newSize) {
        return finalScaledImg != null && finalScaledImg.getWidth() == newSize;
    }

    /**
     * Creates a colorized brush image from the template image
     * according to the given color.
     */
    private void colorizeBrushImage(Color color) {
        coloredBrushImg = new BufferedImage(
            templateImg.getWidth(), templateImg.getHeight(), TYPE_INT_ARGB);
        int[] srcPixels = ImageUtils.getPixelsAsArray(templateImg);
        int[] destPixels = ImageUtils.getPixelsAsArray(coloredBrushImg);

        int destR = color.getRed();
        int destG = color.getGreen();
        int destB = color.getBlue();
        for (int i = 0; i < destPixels.length; i++) {
            int srcRGB = srcPixels[i];

            int srcR = (srcRGB >>> 16) & 0xFF;
            int srcG = (srcRGB >>> 8) & 0xFF;
            int srcB = srcRGB & 0xFF;
            int srcAverage = (srcR + srcG + srcB) / 3;

            // the color comes from the given color,
            // the alpha depends on the source
            destPixels[i] = (0xFF - srcAverage) << 24 | destR << 16 | destG << 8 | destB;
        }
    }

    @Override
    public void putDab(PPoint p, double theta) {
        assert finalScaledImg != null;

        double x = p.getImX();
        double y = p.getImY();
        int drawStartX = (int) (x - radius);
        int drawStartY = (int) (y - radius);
        if (!settings.isAngleAware() || theta == 0) {
            targetG.drawImage(finalScaledImg, drawStartX, drawStartY, null);
        } else {
            var oldTransform = targetG.getTransform();
            targetG.rotate(theta, x, y);
            targetG.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR);
            targetG.drawImage(finalScaledImg, drawStartX, drawStartY, null);
            targetG.setTransform(oldTransform);
        }
        repaintComp(p);
    }
}
