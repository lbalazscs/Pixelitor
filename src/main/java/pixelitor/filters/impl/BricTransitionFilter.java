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

import com.bric.image.transition.KaleidoscopeTransition2D;
import com.jhlabs.image.AbstractBufferedImageOp;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * A transition filter based on the com.bric.image.transition classes
 */
public class BricTransitionFilter extends AbstractBufferedImageOp {
    private float progress;

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dest) {
        BufferedImage frameA = src;
        BufferedImage frameB = new BufferedImage(src.getWidth(), src.getHeight(), src.getType());

        Graphics2D g = dest.createGraphics();

        KaleidoscopeTransition2D transition = new KaleidoscopeTransition2D();

        transition.paint(g, frameA, frameB, progress);
        g.dispose();

        return dest;
    }

    public void setProgress(float progress) {
        this.progress = progress;
    }
}
