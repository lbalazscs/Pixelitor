/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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
 * The brush used by the Clone Tool.
 */
public class CloneBrush extends CopyBrush {
    // Original source coordinates relative to the source image.
    // The actually used source coordinates change during a cloning stroke.
    private double origSrcX;
    private double origSrcY;

    // The offset between the source and destination points.
    // These do not change during a cloning stroke.
    private double offsetX;
    private double offsetY;

    // In aligned mode the offsets don't change for new
    // cloning strokes, only after a new source point is set.
    private boolean aligned = true;

    // Whether a new source point has been set,
    // requiring offset recalculation.
    private boolean newSourcePoint = true;

    private double scaleX;
    private double scaleY;
    private double rotationAngle;

    public CloneBrush(double radius, CopyBrushType type) {
        super(radius, type, new RadiusRatioSpacing(0.25));
    }

    /**
     * Sets the source image and the initial source coordinates.
     */
    public void setSource(BufferedImage image, double x, double y) {
        sourceImage = image;
        origSrcX = x;
        origSrcY = y;
        newSourcePoint = true;
    }

    /**
     * Marks the point where the cloning was started.
     */
    public void setCloningDestPoint(PPoint dest) {
        // aligned = forces the source point to follow the mouse,
        // even after a stroke is completed
        // unaligned = the cloning distance is reinitialized for each stroke
        if (!aligned || newSourcePoint) {
            // recalculate the offsets
            offsetX = dest.getImX() - origSrcX;
            offsetY = dest.getImY() - origSrcY;
        }
        newSourcePoint = false;
    }

    /**
     * Recalculates the brush stamp image before each dab.
     */
    @Override
    void initBrushStamp(PPoint p) {
        Graphics2D g = brushImage.createGraphics();

        type.beforeDrawImage(g);

        // the current sampling coordinates relative to the source image
        double currSrcX = offsetX - p.getImX();
        double currSrcY = offsetY - p.getImY();

        // create the transformation from the source image to the brush image
        var transform = AffineTransform.getTranslateInstance(
            currSrcX + radius, currSrcY + radius);
        if (scaleX != 1.0 || scaleY != 1.0 || rotationAngle != 0.0) {
            g.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR);
            // apply scaling and rotation around the original source point
            transform.translate(origSrcX, origSrcY);
            transform.scale(scaleX, scaleY);
            transform.rotate(rotationAngle);
            transform.translate(-origSrcX, -origSrcY);
        }

        g.drawImage(sourceImage, transform, null);
        type.afterDrawImage(g);

        g.dispose();

        debugImage();
    }

    @Override
    public void putDab(PPoint currentPoint, double angle) {
        var transform = AffineTransform.getTranslateInstance(
            currentPoint.getImX() - radius,
            currentPoint.getImY() - radius
        );
        targetG.drawImage(brushImage, transform, null);
        repaintComp(currentPoint);
    }

    public void setAligned(boolean aligned) {
        this.aligned = aligned;
    }

    public void setScale(double scaleX, double scaleY) {
        this.scaleX = scaleX;
        this.scaleY = scaleY;
    }

    public void setRotationAngle(double rotationAngle) {
        this.rotationAngle = rotationAngle;
    }

    public boolean isAligned() {
        return aligned;
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = super.createDebugNode(key);

        node.addDouble("orig src x", origSrcX);
        node.addDouble("orig src y", origSrcY);
        node.addDouble("dx", offsetX);
        node.addDouble("dy", offsetY);
        node.addDouble("scale x", scaleX);
        node.addDouble("scale y", scaleY);
        node.addDouble("rotate", rotationAngle);
        node.addBoolean("aligned", aligned);
        node.addBoolean("new source point", newSourcePoint);

        return node;
    }
}
