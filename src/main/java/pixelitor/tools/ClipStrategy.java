/*
 * Copyright 2016 Laszlo Balazs-Csiki
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
 * Different tools can have different clipping requirements, which are
 * represented here
 */
public enum ClipStrategy {
    INTERNAL_FRAME {
        @Override
        public void setClip(Graphics2D g, ImageComponent ic) {
            // We are here in image space because g2 has the transforms applied.
            // We are overriding the clip of g2, therefore we must manually
            // make sure that we don't paint anything outside the internal frame.
            // canvas.getBounds() is not reliable because the internal frame might be smaller
            // so we have to use the view rectangle...
            Rectangle componentSpaceViewRect = ic.getViewRect();
            // ...but first get this to image space...
            Rectangle2D imageSpaceViewRect = ic.fromComponentToImageSpace(componentSpaceViewRect);
            g.setClip(imageSpaceViewRect);
        }
    }, IMAGE_ONLY {
        @Override
        public void setClip(Graphics2D g, ImageComponent ic) {
            // empty: the image clipping has been already set
        }
    };

    public abstract void setClip(Graphics2D g, ImageComponent ic);
}
