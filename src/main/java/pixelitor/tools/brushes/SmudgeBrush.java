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

package pixelitor.tools.brushes;

import pixelitor.colors.FgBgColors;
import pixelitor.utils.debug.DebugNode;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * The brush used by the Smudge Tool
 */
public class SmudgeBrush extends CopyBrush {
    /**
     * The smudge brush samples the source image at
     * the last mouse coordinates and puts the pixels to
     * the current coordinates.
     */
    private double lastX;
    private double lastY;

    private float strength;
    private boolean firstUsageInStroke = true;

    /**
     * If true, we start with the foreground color
     */
    private boolean fingerPainting = false;

    public SmudgeBrush(int radius, CopyBrushType type) {
        super(radius, type, new FixedDistanceSpacing(1.0));
    }

    public void setSource(BufferedImage sourceImage, double srcX, double srcY, float strength) {
        this.sourceImage = sourceImage;
        lastX = srcX;
        lastY = srcY;
        this.strength = strength;
        firstUsageInStroke = true;
    }

    @Override
    void setupBrushStamp(double x, double y) {
        Graphics2D g = brushImage.createGraphics();
        type.beforeDrawImage(g);

        if (firstUsageInStroke && fingerPainting) {
            g.setColor(FgBgColors.getFG());
            g.fillRect(0, 0, diameter, diameter);
        } else {
            // samples the source image at lastX, lastY into the brush image
            g.drawImage(sourceImage,
                    AffineTransform.getTranslateInstance(
                            -lastX + radius,
                            -lastY + radius), null);
        }

        type.afterDrawImage(g);
        g.dispose();

        firstUsageInStroke = false;
        super.debugImage();
    }

    @Override
    public void putDab(double x, double y, double theta) {
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

    @Override
    public DebugNode getDebugNode() {
        DebugNode node = super.getDebugNode();

        node.addDoubleChild("lastX", lastX);
        node.addDoubleChild("lastY", lastY);
        node.addFloatChild("strength", strength);
        node.addBooleanChild("firstUsageInStroke", firstUsageInStroke);

        return node;
    }
}
