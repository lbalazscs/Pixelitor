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

import pixelitor.selection.Selection;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.Shape;
import java.awt.image.BufferedImage;

import static java.awt.image.BufferedImage.TYPE_BYTE_GRAY;

public enum LayerMaskAddType {
    REVEAL_ALL("Reveal All", false) {
        @Override
        BufferedImage getBWImage(int width, int height, Selection selection) {
            return createFullImage(width, height, Color.WHITE, null, null);
        }
    }, HIDE_ALL("Hide All", false) {
        @Override
        BufferedImage getBWImage(int width, int height, Selection selection) {
            return createFullImage(width, height, Color.BLACK, null, null);
        }
    }, REVEAL_SELECTION("Reveal Selection", true) {
        @Override
        BufferedImage getBWImage(int width, int height, Selection selection) {
            return createFullImage(width, height, Color.BLACK, Color.WHITE, selection.getShape());
        }
    }, HIDE_SELECTION("Hide Selection", true) {
        @Override
        BufferedImage getBWImage(int width, int height, Selection selection) {
            return createFullImage(width, height, Color.WHITE, Color.BLACK, selection.getShape());
        }
    }, PATTERN ("Pattern", false) { // only for debugging
        @Override
        BufferedImage getBWImage(int width, int height, Selection selection) {
            BufferedImage bi = createFullImage(width, height, Color.WHITE, null, null);
            Graphics2D g = bi.createGraphics();
            float cx = width / 2.0f;
            float cy = height / 2.0f;
            float radius = Math.min(cx, cy);
            float[] fractions = {0.5f, 1.0f};
            Paint gradient = new RadialGradientPaint(cx, cy, radius, fractions, new Color[]{
                    Color.WHITE, Color.BLACK
            });
            g.setPaint(gradient);
            g.fillRect(0, 0, width, height);
            g.dispose();
            return bi;
        }
    };

    private static BufferedImage createFullImage(int width, int height, Color bg, Color fg, Shape shape) {
        BufferedImage bwImage = new BufferedImage(width, height, TYPE_BYTE_GRAY);
        Graphics2D g = bwImage.createGraphics();
        g.setColor(bg);
        g.fillRect(0, 0, width, height);
        if(fg != null) {
            g.setClip(shape);
            g.setColor(fg);
            g.fillRect(0, 0, width, height);
        }
        g.dispose();
        return bwImage;
    }

    private final String guiName;
    private final boolean needsSelection;

    LayerMaskAddType(String guiName, boolean needsSelection) {
        this.guiName = guiName;
        this.needsSelection = needsSelection;
    }

    abstract BufferedImage getBWImage(int width, int height, Selection selection);

    @Override
    public String toString() {
        return guiName;
    }

    /**
     * Returns true if the action needs selection and there is no selection.
     */
    public boolean missingSelection(Selection selection) {
        if(needsSelection) {
            return selection == null;
        } else {
            return false;
        }
    }

    public boolean needsSelection() {
        return needsSelection;
    }
}
