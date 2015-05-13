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

package pixelitor.tools.brushes;

import pixelitor.tools.FgBgColorSelector;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

/**
 * The brush used by the Smudge Tool
 */
public class SmudgeBrush extends DabsBrush {
    private BufferedImage sourceImage;

    private BufferedImage brushImage;
    private Ellipse2D.Double circleClip;
    double lastX;
    double lastY;
    private float strength;
    private boolean firstUsageInStroke = true;
    private boolean fingerPainting = false;

    public SmudgeBrush() {
        super(new FixedDistanceSpacingStrategy(1.0), false, true);
    }

    @Override
    public void setRadius(int radius) {
        super.setRadius(radius);
        brushImage = new BufferedImage(diameter, diameter, TYPE_INT_ARGB);
        circleClip = new Ellipse2D.Double(0, 0, diameter, diameter);
    }

    public void setSource(BufferedImage sourceImage, int srcX, int srcY, float strength) {
        this.strength = strength;
//        System.out.println("SmudgeBrush::setSource: srcX = " + srcX + ", srcY = " + srcY);
        this.sourceImage = sourceImage;
        lastX = srcX;
        lastY = srcY;
        firstUsageInStroke = true;
    }

    @Override
    void setupBrushStamp(double x, double y) {
//        System.out.println(String.format("SmudgeBrush::setupBrushStamp: x = %.2f, y = %.2f", x, y));

        // here we sample the source image at lastX, lastY into the brush image
        Graphics2D g = brushImage.createGraphics();
        g.setClip(circleClip);

        if (firstUsageInStroke && fingerPainting) {
            g.setColor(FgBgColorSelector.getFG());
            g.fillRect(0, 0, diameter, diameter);
        } else {
            g.drawImage(sourceImage,
                    AffineTransform.getTranslateInstance(
                            -lastX + radius,
                            -lastY + radius), null);
        }
        g.dispose();

        firstUsageInStroke = false;
//        Utils.debugImage(brushImage, "BrushImage");
    }

    @Override
    public void putDab(double x, double y, double theta) {
//        System.out.println(String.format("SmudgeBrush::putDab: x = %.2f, y = %.2f", x, y));

        AffineTransform transform = AffineTransform.getTranslateInstance(
                x - radius,
                y - radius
        );

        // does not handle transparency
        targetG.setComposite(AlphaComposite.SrcAtop.derive(strength));
//        targetG.setComposite(BlendComposite.CrossFade.derive(strength));

        targetG.drawImage(brushImage, transform, null);


        lastX = x;
        lastY = y;

        updateComp((int) x, (int) y);
    }

    public void setFingerPainting(boolean fingerPainting) {
        this.fingerPainting = fingerPainting;
    }
}
