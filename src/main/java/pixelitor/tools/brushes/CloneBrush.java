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

import pixelitor.utils.debug.DebugNode;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * The brush used by the Clone Tool
 */
public class CloneBrush extends CopyBrush {
    private double srcX;
    private double srcY;
    private double dx;
    private double dy;
    private boolean aligned = true;

    private boolean firstCloningStart = true;
    private double scaleX;
    private double scaleY;
    private double rotate;

    public CloneBrush(int radius, CopyBrushType type) {
        super(radius, type, new RadiusRatioSpacing(0.25));
    }

    public void setSource(BufferedImage sourceImage, double srcX, double srcY) {
        this.sourceImage = sourceImage;
        this.srcX = srcX;
        this.srcY = srcY;
        firstCloningStart = true;
    }

    // marks the point where the cloning was started
    public void setCloningDestPoint(double destX, double destY) {
        boolean reinitializeDistance = false;
        // aligned = forces the source point to follow the mouse, even after a stroke is completed
        // unaligned =  the cloning distance is reinitialized for each stroke
        if ((aligned && firstCloningStart) || (!aligned)) {
            reinitializeDistance = true;
        }
        firstCloningStart = false;

        if (reinitializeDistance) {
            this.dx = -srcX + destX + radius;
            this.dy = -srcY + destY + radius;
        }
    }

    @Override
    void setupBrushStamp(double x, double y) {
        Graphics2D g = brushImage.createGraphics();

        type.beforeDrawImage(g);

        // Concatenated transformations have a last-specified-first-applied order,
        // so start with the last transformation, that works when there is no scaling/rotating
        AffineTransform transform = AffineTransform.getTranslateInstance(
                (dx - x),
                (dy - y));

        if (scaleX != 1.0 || scaleY != 1.0 || rotate != 0.0) {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            // first we need to scale/rotate the image around the source point
            transform.translate(srcX, srcY);
            transform.scale(scaleX, scaleY);
            transform.rotate(rotate);
            transform.translate(-srcX, -srcY);
        }

        g.drawImage(sourceImage,
                transform, null);

        type.afterDrawImage(g);

        g.dispose();
        super.debugImage();
    }

    @Override
    public void putDab(double x, double y, double theta) {
        AffineTransform transform = AffineTransform.getTranslateInstance(
                x - radius,
                y - radius
        );
        targetG.drawImage(brushImage, transform, null);
        updateComp((int) x, (int) y);
    }

    public void setAligned(boolean aligned) {
        this.aligned = aligned;
    }

    public void setScale(double scaleX, double scaleY) {
        this.scaleX = scaleX;
        this.scaleY = scaleY;
    }

    public void setRotate(double rotate) {
        this.rotate = rotate;
    }

    public boolean isAligned() {
        return aligned;
    }

    @Override
    public DebugNode getDebugNode() {
        DebugNode node = super.getDebugNode();

        node.addDoubleChild("srcX", srcX);
        node.addDoubleChild("srcY", srcY);
        node.addDoubleChild("dx", dx);
        node.addDoubleChild("dy", dy);
        node.addDoubleChild("scaleX", scaleX);
        node.addDoubleChild("scaleY", scaleY);
        node.addDoubleChild("rotate", rotate);

        node.addBooleanChild("aligned", aligned);
        node.addBooleanChild("firstCloningStart", firstCloningStart);

        return node;
    }
}
