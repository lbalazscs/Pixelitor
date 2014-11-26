/*
 * Copyright 2009-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.tools.brushes;

import pixelitor.tools.AbstractBrushTool;
import pixelitor.utils.ImageUtils;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

public class UniformImageBrush extends UniformDabsBrush {
    private final BufferedImage templateImage;
    private BufferedImage coloredBrushImage;
    private BufferedImage finalScaledImage;
    private Color lastColor;

    public UniformImageBrush(ImageBrushType imageBrushType, double spacingRatio, boolean angleAware) {
        super(spacingRatio, angleAware);

        templateImage = imageBrushType.createTemplateBrush(AbstractBrushTool.MAX_BRUSH_RADIUS);
    }


    @Override
    void setupBrushStamp(Graphics2D g, float diameter) {
        Color c = g.getColor();

        if (!c.equals(lastColor)) {
            colorizeBrushImage(c);
            lastColor = c;
            resizeBrushImage(diameter, true);
        } else {
            resizeBrushImage(diameter, false);
        }

    }

    /**
     * This method assumes that the color of coloredBrushImage is OK
     */
    private void resizeBrushImage(float newSize, boolean force) {
        if (!force) {
            if (finalScaledImage != null && finalScaledImage.getWidth() == newSize) {
                return;
            }
        }

        if (finalScaledImage != null) {
            finalScaledImage.flush();
        }

        int newSizeInt = (int) newSize;
        finalScaledImage = new BufferedImage(newSizeInt, newSizeInt, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = finalScaledImage.createGraphics();
        g.drawImage(coloredBrushImage, 0, 0, newSizeInt, newSizeInt, null);
        g.dispose();
    }

    /**
     * Creates a colorized brush image from the template image according to the foreground color
     *
     * @param color
     */
    private void colorizeBrushImage(Color color) {
        coloredBrushImage = new BufferedImage(templateImage.getWidth(), templateImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int[] srcPixels = ImageUtils.getPixelsAsArray(templateImage);
        int[] destPixels = ImageUtils.getPixelsAsArray(coloredBrushImage);

        int destRed = color.getRed();
        int destGreen = color.getGreen();
        int destBlue = color.getBlue();
        for (int i = 0; i < destPixels.length; i++) {
            int srcRGB = srcPixels[i];

            //int a = (srcRGB >>> 24) & 0xFF;
            int srcRed = (srcRGB >>> 16) & 0xFF;
            int srcGreen = (srcRGB >>> 8) & 0xFF;
            int srcBlue = (srcRGB) & 0xFF;
            int srcAverage = (srcRed + srcGreen + srcBlue) / 3;

            destPixels[i] = (0xFF - srcAverage) << 24 | destRed << 16 | destGreen << 8 | destBlue;
        }
    }

    @Override
    public void drawPoint(Graphics2D g, int x, int y, int radius) {
//        System.out.println("UniformImageBrush::drawPoint: x = " + x + ", y = " + y);
        super.drawPoint(g, x, y, radius);

        if(angleAware) {
            // for angle-aware brushes looks better if the first point is not drawn
            // because the correct angle cannot be calculated
            return;
        }

        setupBrushStamp(g, 2*radius);
        g.drawImage(finalScaledImage, x - radius, y - radius, null);
    }

    @Override
    public void drawPointWithAngle(Graphics2D g, int x, int y, int radius, double theta) {
//        double degrees = Math.toDegrees(theta);
//        System.out.println(String.format("UniformImageBrush::drawPointWithAngle: x = %d,y = %d, degrees = %.2f, radians = %.2f", x, y, degrees, theta));

        setupBrushStamp(g, 2 * radius);
        AffineTransform oldTransform = g.getTransform();
        g.rotate(theta, x, y);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(finalScaledImage, x - radius, y - radius, null);
        g.setTransform(oldTransform);
    }
}
