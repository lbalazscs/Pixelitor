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

package pixelitor.io;

import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * Information about a layer's image, as required for OpenRaster export.
 * OpenRaster allows layers to be smaller than the canvas.
 * For example, a text layer can export a small image with an offset.
 *
 * @param exportedImage the layer image to be saved as a PNG
 * @param tx            horizontal offset of exportedImage relative to the canvas
 * @param ty            vertical offset of exportedImage relative to the canvas
 */
public record ORAImageInfo(BufferedImage exportedImage, int tx, int ty) {
    public ORAImageInfo {
        Objects.requireNonNull(exportedImage);
    }

    @Override
    public String toString() {
        return "ORAImageInfo[size=%dx%d, tx=%d, ty=%d]".formatted(
            exportedImage.getWidth(), exportedImage.getHeight(), tx, ty);
    }
}
