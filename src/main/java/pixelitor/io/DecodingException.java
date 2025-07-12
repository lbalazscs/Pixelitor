/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

import java.io.File;

public class DecodingException extends RuntimeException {
    private final File file;
    private final boolean magick;

    // private constructor to enforce factory method usage
    private DecodingException(File file, boolean magick, Exception cause) {
        super(generateErrorMessage(file, magick, cause), cause);
        this.file = file;
        this.magick = magick;
    }

    /**
     * Create a DecodingException for ImageIO read errors.
     */
    public static DecodingException forImageIORead(File file, Exception cause) {
        return new DecodingException(file, false, cause);
    }

    /**
     * Create a DecodingException for ImageMagick import errors.
     */
    public static DecodingException forMagickImport(File file, Exception cause) {
        return new DecodingException(file, true, cause);
    }

    // create the message shown to the user
    private static String generateErrorMessage(File file, boolean magick, Exception cause) {
        String operation = magick ? "import" : "read";
        String origMessage = (cause == null)
            ? ""
            : "<br>Error details: " + cause.getMessage();
        return String.format("<html>Failed to %s <b>%s</b> as an image file.%s",
            operation, file.getName(), origMessage);
    }

    public File getFile() {
        return file;
    }

    /**
     * Whether this error occured during an ImageMagick import.
     */
    public boolean wasMagick() {
        return magick;
    }
}
