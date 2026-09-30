/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.colors.Colors;
import pixelitor.utils.ImageUtils;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

/**
 * Shapes the edge of a {@link CopyBrush} stamp of one specific size.
 */
sealed interface EdgeStyle {
    /**
     * Prepares the graphics context before the source image is drawn.
     */
    void beforeDrawImage(Graphics2D g);

    /**
     * Applies final effects after the source image is drawn.
     */
    void afterDrawImage(Graphics2D g);

    final class SoftEdge implements EdgeStyle {
        private final int diameter;
        private final BufferedImage transparencyImage;

        SoftEdge(int diameter) {
            this.diameter = diameter;
            this.transparencyImage = ImageUtils.createSoftTransparencyImage(diameter);
        }

        @Override
        public void beforeDrawImage(Graphics2D g) {
            // important in the areas where there is no source defined
            Colors.fillWithTransparent(g, diameter);
        }

        @Override
        public void afterDrawImage(Graphics2D g) {
            g.setComposite(AlphaComposite.DstIn);
            g.drawImage(transparencyImage, 0, 0, null);
        }
    }

    final class HardEdge implements EdgeStyle {
        private final int diameter;
        private final Ellipse2D.Double circleClip;

        HardEdge(int diameter) {
            this.diameter = diameter;
            this.circleClip = new Ellipse2D.Double(0, 0, diameter, diameter);
        }

        @Override
        public void beforeDrawImage(Graphics2D g) {
            // important in the areas where there is no source defined
            Colors.fillWithTransparent(g, diameter);
            g.setClip(circleClip);
        }

        @Override
        public void afterDrawImage(Graphics2D g) {
            // do nothing
        }
    }
}
