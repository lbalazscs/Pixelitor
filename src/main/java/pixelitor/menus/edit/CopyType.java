/*
 * Copyright 2010-2014 Laszlo Balazs-Csiki
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
package pixelitor.menus.edit;

import pixelitor.ImageComponents;

import java.awt.image.BufferedImage;

/**
 *
 */
public enum CopyType {
    COPY_LAYER {
        @Override
        BufferedImage getCopySource() {
//            return AppLogic.getActiveComp().getImageOrSubImageIfSelectedForActiveLayer(false, false);

            // TODO this avoids the raster "has minX or minY not equal to zero" exception,
            // but bow it is copied twice 
            return ImageComponents.getActiveComp().getImageOrSubImageIfSelectedForActiveLayer(false, true);
        }

        @Override
        public String toString() {
            return "Copy Layer";
        }
    },
    COPY_COMPOSITE {
        @Override
        BufferedImage getCopySource() {
            return ImageComponents.getActiveCompositeImage();
        }

        @Override
        public String toString() {
            return "Copy Composite";
        }
    };

    abstract BufferedImage getCopySource();
}
