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

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

/**
 * The brush used by the Clone Tool
 */
public class CloneBrush extends DabsBrush {
    private BufferedImage sourceImage;
    private int srcX;
    private int srcY;
    private int dx;
    private int dy;
    private boolean aligned = true;

    private BufferedImage brushImage;
    private boolean firstCloningStart = true;
    private CloneBrushType type;

    public CloneBrush(CloneBrushType type) {
        super(new RadiusRatioSpacingStrategy(0.25), false, true);
        this.type = type;
    }

    @Override
    public void setRadius(int radius) {
        super.setRadius(radius);
        brushImage = new BufferedImage(diameter, diameter, TYPE_INT_ARGB);
        type.setSize(diameter);
    }

    public void setSource(BufferedImage sourceImage, int srcX, int srcY) {
        this.sourceImage = sourceImage;
        this.srcX = srcX;
        this.srcY = srcY;
        firstCloningStart = true;
    }

    // marks the point where the cloning was started
    public void setCloningStartPoint(int destX, int destY) {
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

//        double scale = 0.5;

        AffineTransform transform = AffineTransform.getTranslateInstance(
                (dx - x),
                (dy - y));
//        transform.scale(scale, scale);

        g.drawImage(sourceImage,
                transform, null);

        type.afterDrawImage(g);

        g.dispose();
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

    public void typeChanged(CloneBrushType type) {
        this.type = type;
    }
}
