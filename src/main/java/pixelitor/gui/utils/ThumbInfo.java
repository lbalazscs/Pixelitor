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

package pixelitor.gui.utils;

import java.awt.image.BufferedImage;

/**
 * Information associated with a thumbnail image
 */
public class ThumbInfo {
    private final BufferedImage thumb;

    // these sizes refer to the original, not to the thumb!
    private final int origWidth;
    private final int origHeight;

    public ThumbInfo(BufferedImage thumb, int origWidth, int origHeight) {
        this.thumb = thumb;
        this.origWidth = origWidth;
        this.origHeight = origHeight;
    }

    public BufferedImage getThumb() {
        return thumb;
    }

    public int getOrigWidth() {
        return origWidth;
    }

    public int getOrigHeight() {
        return origHeight;
    }
}
