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

package pixelitor.layers;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static java.awt.image.BufferedImage.TYPE_BYTE_GRAY;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

public enum LayerMaskAddType {
    REVEAL_ALL("Reveal All") {
        @Override
        BufferedImage getBWImage(int width, int height) {
//            BufferedImage bwImage = new BufferedImage(width, height, TYPE_INT_ARGB);
            BufferedImage bwImage = new BufferedImage(width, height, TYPE_BYTE_GRAY);
            Graphics2D g = bwImage.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);
            g.dispose();
            return bwImage;
        }
    }, HIDE_ALL("Hide All") {
        @Override
        BufferedImage getBWImage(int width, int height) {
            BufferedImage bwImage = new BufferedImage(width, height, TYPE_INT_ARGB);
//            Graphics2D g = bwImage.createGraphics();
//            g.setColor(Color.BLACK);
//            g.fillRect(0, 0, width, height);
//            g.dispose();
            return bwImage;
        }
//    }, REVEAL_SELECTION("Reveal Selection") {
//        @Override
//        BufferedImage getBWImage(int width, int height) {
//            return null;
//        }
//    }, HIDE_SELECTION("Hide Selection") {
//        @Override
//        BufferedImage getBWImage(int width, int height) {
//            return null;
//        }
    };

    private final String guiName;

    LayerMaskAddType(String guiName) {
        this.guiName = guiName;
    }

    abstract BufferedImage getBWImage(int width, int height);

    @Override
    public String toString() {
        return guiName;
    }
}
