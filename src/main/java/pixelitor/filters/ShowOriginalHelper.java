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
package pixelitor.filters;

import java.awt.image.BufferedImage;

/**
 * Caches the last transformed image to support "show original" functionality of the filters
 * After the checkbox is deselected, the last transformed does not have to be recalculated
 */
public class ShowOriginalHelper {
    private boolean previousShowOriginal;
    private boolean showOriginal;
    private BufferedImage lastTransformed;

    public ShowOriginalHelper() {
    }

    public BufferedImage getLastTransformed() {
        return lastTransformed;
    }

    public void setLastTransformed(BufferedImage lastTransformed) {
        this.lastTransformed = lastTransformed;
    }

    public boolean showCached() {
        boolean retVal = (!showOriginal) && previousShowOriginal && (lastTransformed != null);

        return retVal;
    }

    public void setShowOriginal(boolean newShowOriginal) {
        this.previousShowOriginal = this.showOriginal;
        this.showOriginal = newShowOriginal;
    }

    public void setPreviousShowOriginal(boolean previousShowOriginal) {
        this.previousShowOriginal = previousShowOriginal;
    }

    public void releaseCachedImage() {
        lastTransformed = null;
    }
}
