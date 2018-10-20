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
 * How the clipping shape of {@link Graphics2D} is set when painting the
 * active {@link ImageComponent}.
 * Each tool has its own {@link ClipStrategy}.
 */
public enum ClipStrategy {
    /**
     * The painting is allowed to leave the canvas,
     * but not the internal frame.
     * Necessary because the clipping was overridden previously
     * in {@link ImageComponent}.
     *
     * TODO probably restoring Swing's original clip shape
     * would have the same effect
     */
    FULL {
        @Override
        public void setClipFor(Graphics2D g, ImageComponent ic) {
            // Note that the internal frame might be
            // smaller than the ImageComponent when there are scrollbars,
            // so we have to use the view rectangle:
            Rectangle componentSpaceVisiblePart = ic.getVisiblePart();

            // We are in image space because g has the transforms applied.
            Rectangle2D imageSpaceVisiblePart = ic.componentToImageSpace(componentSpaceVisiblePart);
            g.setClip(imageSpaceVisiblePart);
        }
    },
    /**
     * The painting is not allowed to leave the canvas.
     * This should be used if a tool only acts on the image,
     * without helper handles that can be outside the canvas.
     */
    CANVAS {
        @Override
        public void setClipFor(Graphics2D g, ImageComponent ic) {
            // empty: the canvas clipping has been already set
        }
    },
    /**
     * The tool itself will set its own custom clipping.
     */
    CUSTOM {
        @Override
        public void setClipFor(Graphics2D g, ImageComponent ic) {
            // empty: it will be set later in the tool
        }
    };

    /**
     * Called when the active {@link ImageComponent} is painted
     */
    public abstract void setClipFor(Graphics2D g, ImageComponent ic);
}
