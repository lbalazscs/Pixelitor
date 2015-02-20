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

public class CloneBrush extends DabsBrush {
    private BufferedImage sourceImage;
    private int srcX;
    private int srcY;
    private int destX;
    private int destY;

    private BufferedImage brushImage;

    public CloneBrush(ImageBrushType imageBrushType) {
        super(0.25, false, true);
    }

    @Override
    public void setRadius(int radius) {
        super.setRadius(radius);
        brushImage = new BufferedImage(diameter, diameter, BufferedImage.TYPE_INT_ARGB);
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
        System.out.println("CloneBrush::putDab: CALLED");
        targetG.drawImage(brushImage, (int) x, (int) y, null);
    }

    @Override
    void setupBrushStamp(double x, double y) {
        System.out.println("CloneBrush::setupBrushStamp: CALLED");

        Graphics2D g = brushImage.createGraphics();
        g.drawImage(sourceImage,
                AffineTransform.getTranslateInstance(
                        -srcX + destX - x,
                        -srcY + destY - y), null);
        g.dispose();
    }
}
