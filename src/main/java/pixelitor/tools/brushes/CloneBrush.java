/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.utils.debug.DebugNode;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import static java.awt.RenderingHints.KEY_INTERPOLATION;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR;

/**
 * The brush used by the Clone Tool
 */
public class CloneBrush extends CopyBrush {
    // The coordinates of the original source relative to the source image.
    // The actually used source coordinates change during a cloning stroke.
    private double origSrcX;
    private double origSrcY;

    // The relative distances between the source and destination coordinates.
    // These do not change during a cloning stroke.
    private double dx;
    private double dy;

    // In aligned mode dx and dy don't change for new cloning strokes,
    // only after a new source point is set
    private boolean aligned = true;

    // if a new source point was just set, the distances
    // have to be recalculated when the painting begins
    private boolean newSourcePointWasJustSet = true;

    private double scaleX;
    private double scaleY;
    private double rotate;

    public CloneBrush(double radius, CopyBrushType type) {
        super(radius, type, new RadiusRatioSpacing(0.25));
    }

    public void setSource(BufferedImage image, double x, double y) {
        this.sourceImage = image;
        this.origSrcX = x;
        this.origSrcY = y;
        newSourcePointWasJustSet = true;
    }

    /**
     * Marks the point where the cloning was started.
     * Called at the beginning of a new cloning stroke
     */
    public void setCloningDestPoint(PPoint p) {
        double destX = p.getImX();
        double destY = p.getImY();
        boolean reinitializeDistance = false;
        // aligned = forces the source point to follow the mouse,
        // even after a stroke is completed
        // unaligned = the cloning distance is reinitialized for each stroke
        if (!aligned || newSourcePointWasJustSet) {
            reinitializeDistance = true;
        }
        newSourcePointWasJustSet = false;

        if (reinitializeDistance) {
            this.dx = destX - origSrcX;
            this.dy = destY - origSrcY;
        }
    }

    /**
     * Recalculates the brush stamp image before each dab
     */
    @Override
    void setupBrushStamp(PPoint p) {
        Graphics2D g = brushImage.createGraphics();

        type.beforeDrawImage(g);

        // the current sampling coordinates relative to the source image
        double currSrcX = dx - p.getImX();
        double currSrcY = dy - p.getImY();

        // Now calculate the transformation from the source to the brush image.
        // Concatenated transformations have a last-specified-first-applied
        // order, so start with the last transformation
        // that works when there is no scaling/rotating
        AffineTransform transform = AffineTransform.getTranslateInstance(
            currSrcX + radius, currSrcY + radius);

        if (scaleX != 1.0 || scaleY != 1.0 || rotate != 0.0) {
            g.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR);
            // we need to scale/rotate the image
            // around the source point, so translate first
            transform.translate(origSrcX, origSrcY);
            transform.scale(scaleX, scaleY);
            transform.rotate(rotate);
            transform.translate(-origSrcX, -origSrcY);
        }

        g.drawImage(sourceImage, transform, null);

        type.afterDrawImage(g);

        g.dispose();
        super.debugImage();
    }

    @Override
    public void putDab(PPoint p, double theta) {
        AffineTransform transform = AffineTransform.getTranslateInstance(
                p.getImX() - radius,
                p.getImY() - radius
        );
        targetG.drawImage(brushImage, transform, null);
        updateComp(p);
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

        node.addDouble("origSrcX", origSrcX);
        node.addDouble("origSrcY", origSrcY);
        node.addDouble("dx", dx);
        node.addDouble("dy", dy);
        node.addDouble("scaleX", scaleX);
        node.addDouble("scaleY", scaleY);
        node.addDouble("rotate", rotate);
        node.addBoolean("aligned", aligned);
        node.addBoolean("newSourcePointWasJustSet", newSourcePointWasJustSet);

        return node;
    }
}
