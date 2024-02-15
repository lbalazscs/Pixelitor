/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

import javax.imageio.ImageWriteParam;
import java.io.File;
import java.util.function.Consumer;

import static javax.imageio.ImageWriteParam.MODE_DEFAULT;
import static javax.imageio.ImageWriteParam.MODE_DISABLED;
import static javax.imageio.ImageWriteParam.MODE_EXPLICIT;

/**
 * Settings for writing JPEG images
 */
public record JpegSettings(File file, FileFormat format, float quality, boolean progressive) implements SaveSettings {
    private static final float DEFAULT_QUALITY = 0.87f;

    public JpegSettings(float quality, boolean progressive, File outputFile) {
        this(outputFile, FileFormat.JPG, quality, progressive);
    }

    /**
     * Converts a generic {@link SaveSettings} to a {@link JpegSettings}.
     * Needed because a {@link JpegSettings} must be used for saving
     * a JPEG file, even if it wasn't customized by the user.
     */
    public static JpegSettings from(SaveSettings settings) {
        assert settings.format() == FileFormat.JPG;
        if (settings instanceof JpegSettings) {
            return (JpegSettings) settings;
        }
        return new JpegSettings(DEFAULT_QUALITY, false, settings.file());
    }

    public Consumer<ImageWriteParam> toCustomizer() {
        return createJpegCustomizer(quality, progressive);
    }

    public static Consumer<ImageWriteParam> createJpegCustomizer(float quality, boolean progressive) {
        return imageWriteParam -> {
            if (progressive) {
                imageWriteParam.setProgressiveMode(MODE_DEFAULT);
            } else {
                imageWriteParam.setProgressiveMode(MODE_DISABLED);
            }

            imageWriteParam.setCompressionMode(MODE_EXPLICIT);
            imageWriteParam.setCompressionQuality(quality);
        };
    }
}
