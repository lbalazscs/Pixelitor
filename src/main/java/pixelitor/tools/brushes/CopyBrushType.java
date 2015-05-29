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

import pixelitor.utils.ImageUtils;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

public enum CopyBrushType {
    SOFT("Soft") {
        private BufferedImage transparencyImage;

        @Override
        public void setSize(int size) {
            super.setSize(size);
            transparencyImage = ImageUtils.createSoftTransparencyImage(size);
        }

        @Override
        public void beforeDrawImage(Graphics2D g) {
            // important in the areas where there is no source defined
            ImageUtils.fillWithTransparentRectangle(g, size);
        }

        @Override
        public void afterDrawImage(Graphics2D g) {
            g.setComposite(AlphaComposite.DstIn);
            g.drawImage(transparencyImage, 0, 0, null);
        }
    }, HARD("Hard") {
        private Ellipse2D.Double circleClip;

        @Override
        public void setSize(int size) {
            super.setSize(size);
            circleClip = new Ellipse2D.Double(0, 0, size, size);
        }

        @Override
        public void beforeDrawImage(Graphics2D g) {
            // important in the areas where there is no source defined
            ImageUtils.fillWithTransparentRectangle(g, size);

            g.setClip(circleClip);
        }

        @Override
        public void afterDrawImage(Graphics2D g) {
            // do nothing
        }
//    }, SQUARE("Square") {
//        @Override
//        public void beforeDrawImage(Graphics2D g) {
//            // important in the areas where there is no source defined
//            ImageUtils.fillWithTransparentRectangle(g, size);
//        }
//
//        @Override
//        public void afterDrawImage(Graphics2D g) {
//            // do nothing
//        }
    };

    private final String guiName;
    protected int size;

    CopyBrushType(String guiName) {
        this.guiName = guiName;
    }

    public abstract void beforeDrawImage(Graphics2D g);

    public abstract void afterDrawImage(Graphics2D g);

    public void setSize(int size) {
        this.size = size;
    }

    @Override
    public String toString() {
        return guiName;
    }
}
