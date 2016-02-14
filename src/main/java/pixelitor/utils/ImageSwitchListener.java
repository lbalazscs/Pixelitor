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

package pixelitor.utils;

import pixelitor.Composition;
import pixelitor.gui.ImageComponent;

public interface ImageSwitchListener {
    /**
     * Called when the user has closed all the images
     */
    void noOpenImageAnymore();

    /**
     * Called when the user has opened a new image,
     * and also when the composition was reloaded.
     */
    void newImageOpened(Composition comp);

    /**
     * Called when the used switches to another image
     */
    void activeImageHasChanged(ImageComponent oldIC, ImageComponent newIC);
}
