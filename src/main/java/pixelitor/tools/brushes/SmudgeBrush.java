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
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

/**
 * The brush used by the Smudge Tool
 */
public class SmudgeBrush extends DabsBrush {
    private BufferedImage sourceImage;

    private BufferedImage brushImage;
    private Ellipse2D.Double circleClip;

    public SmudgeBrush(ImageBrushType imageBrushType) {
        super(0.25, false, true);
    }

    @Override
    public void setRadius(int radius) {
        super.setRadius(radius);
        brushImage = new BufferedImage(diameter, diameter, BufferedImage.TYPE_INT_ARGB);
        circleClip = new Ellipse2D.Double(0, 0, diameter, diameter);
    }

    @Override
    public void putDab(double x, double y, double theta) {
        // TODO
        updateComp((int) x, (int) y);
    }

    @Override
    void setupBrushStamp(double x, double y) {
        Graphics2D g = brushImage.createGraphics();
        g.setClip(circleClip);
        // TODO
//        g.drawImage(sourceImage,
//                AffineTransform.getTranslateInstance(
//                        dx - x,
//                        dy - y), null);
        g.dispose();
    }
}
