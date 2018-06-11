/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.tools;

import pixelitor.gui.ImageComponent;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

/**
 * When painting the active {@link ImageComponent}, the clip
 * must be set differently for different tools. Each tool
 * has a {@link ClipStrategy} to decide how to set the clip.
 */
public enum ClipStrategy {
    /**
     * Make sure that the painting does not leave the internal frame,
     * even if there are scrollbars. Necessary because we had overridden
     * the clipping previously.
     */
    INTERNAL_FRAME {
        @Override
        public void setClipFor(Graphics2D g, ImageComponent ic) {
            // We have to work in image space because g has the transforms applied.
            // canvas.getBounds() is not reliable because the internal frame might be
            // smaller so we have to use the view rectangle...
            Rectangle componentSpaceVisiblePart = ic.getVisiblePart();
            // ...but first get this to image space...
            Rectangle2D imageSpaceVisiblePart = ic.fromComponentToImageSpace(componentSpaceVisiblePart);
            g.setClip(imageSpaceVisiblePart);
        }
    },
    /**
     * Most tools act on the image, so it is OK
     * if the clip is restricted to the canvas
     */
    CANVAS {
        @Override
        public void setClipFor(Graphics2D g, ImageComponent ic) {
            // empty: the canvas clipping has been already set
        }
    };

    /**
     * Called when the active {@link ImageComponent} is painted
     */
    public abstract void setClipFor(Graphics2D g, ImageComponent ic);
}
