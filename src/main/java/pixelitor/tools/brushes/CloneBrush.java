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
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

/**
 * The brush used by the Clone Tool
 */
public class CloneBrush extends DabsBrush {
    private BufferedImage sourceImage;
    private int srcX;
    private int srcY;
    private int destX;
    private int destY;

    private BufferedImage brushImage;
    private Ellipse2D.Double circleClip;

    public CloneBrush(ImageBrushType imageBrushType) {
        super(0.25, false, true);
    }

    @Override
    public void setRadius(int radius) {
        super.setRadius(radius);
        brushImage = new BufferedImage(diameter, diameter, BufferedImage.TYPE_INT_ARGB);
        circleClip = new Ellipse2D.Double(0, 0, diameter, diameter);
    }

    public void setSource(BufferedImage sourceImage, int srcX, int srcY) {
        this.sourceImage = sourceImage;
        this.srcX = srcX;
        this.srcY = srcY;
    }

    public void setDestination(int destX, int destY) {
        this.destX = destX;
        this.destY = destY;
    }


    @Override
    public void putDab(double x, double y, double theta) {
//        targetG.drawImage(brushImage, (int) x - radius, (int) y - radius, null);
        targetG.drawImage(brushImage, AffineTransform.getTranslateInstance(
                x - radius,
                y - radius
        ), null);
    }

    @Override
    void setupBrushStamp(double x, double y) {
        Graphics2D g = brushImage.createGraphics();
        g.setClip(circleClip);
        g.drawImage(sourceImage,
                AffineTransform.getTranslateInstance(
                        -srcX + destX - x + radius,
                        -srcY + destY - y + radius), null);
        g.dispose();
    }
}
