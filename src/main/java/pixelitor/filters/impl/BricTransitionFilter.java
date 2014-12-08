/*
 * Copyright 2009-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.filters.impl;

import com.bric.image.transition.BarsTransition2D;
import com.bric.image.transition.BlendTransition2D;
import com.bric.image.transition.KaleidoscopeTransition2D;
import com.bric.image.transition.Transition;
import com.jhlabs.image.AbstractBufferedImageOp;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * A transition filter based on the com.bric.image.transition classes
 */
public class BricTransitionFilter extends AbstractBufferedImageOp {
    public static final int BARS_HORIZONTAL = 1;
    public static final int BARS_VERTICAL = 2;
    public static final int FADE = 3;

    public static final int KALEIDOSCOPE = 5000;

    private int type;
    private float progress;

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dest) {
        BufferedImage frameA = src;
        BufferedImage frameB = new BufferedImage(src.getWidth(), src.getHeight(), src.getType());

        Graphics2D g = dest.createGraphics();

        Transition transition = null;
        switch (type) {
            case BARS_HORIZONTAL:
                transition = new BarsTransition2D(BarsTransition2D.HORIZONTAL, false);
                break;
            case BARS_VERTICAL:
                transition = new BarsTransition2D(BarsTransition2D.VERTICAL, false);
                break;
            case FADE:
                transition = new BlendTransition2D();
                break;
            case KALEIDOSCOPE:
                transition = new KaleidoscopeTransition2D();
                break;
            default:
                throw new IllegalStateException("Unexpected type = " + type);
        }

        transition.paint(g, frameA, frameB, progress);
        g.dispose();

        return dest;
    }

    public void setProgress(float progress) {
        this.progress = progress;
    }

    public void setType(int type) {
        this.type = type;
    }
}
