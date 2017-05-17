/*
 * Copyright 2017 Laszlo Balazs-Csiki
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

package pixelitor.io;

public class JpegSettings {
    private final float quality;
    private final boolean progressive;
    private static final float DEFAULT_JPEG_QUALITY = 0.87f;

    private static final JpegSettings defaults = new JpegSettings(DEFAULT_JPEG_QUALITY, false);

    public JpegSettings(float quality, boolean progressive) {
        this.quality = quality;
        this.progressive = progressive;
    }

    public float getQuality() {
        return quality;
    }

    public boolean isProgressive() {
        return progressive;
    }

    public static JpegSettings getDefaults() {
        return defaults;
    }
}
