/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.utils.debug;

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DirectColorModel;

public class DebugUtils {
    private DebugUtils() {
        // shouldn't be instantiated
    }

    public static String dateBufferTypeAsString(int type) {
        switch (type) {
            case DataBuffer.TYPE_BYTE:
                return "BYTE";
            case DataBuffer.TYPE_USHORT:
                return "USHORT";
            case DataBuffer.TYPE_SHORT:
                return "SHORT";
            case DataBuffer.TYPE_INT:
                return "INT";
            case DataBuffer.TYPE_FLOAT:
                return "FLOAT";
            case DataBuffer.TYPE_DOUBLE:
                return "DOUBLE";
            case DataBuffer.TYPE_UNDEFINED:
                return "UNDEFINED";
            default:
                return "unrecognized (" + type + ")";
        }
    }

    static String transparencyAsString(int transparency) {
        switch (transparency) {
            case Transparency.OPAQUE:
                return "OPAQUE";
            case Transparency.BITMASK:
                return "BITMASK";
            case Transparency.TRANSLUCENT:
                return "TRANSLUCENT";
            default:
                return "unrecognized (" + transparency + ")";
        }
    }

    public static String bufferedImageTypeAsString(int type) {
        switch (type) {
            case BufferedImage.TYPE_3BYTE_BGR:
                return "3BYTE_BGR";
            case BufferedImage.TYPE_4BYTE_ABGR:
                return "4BYTE_ABGR";
            case BufferedImage.TYPE_4BYTE_ABGR_PRE:
                return "4BYTE_ABGR_PRE";
            case BufferedImage.TYPE_BYTE_BINARY:
                return "BYTE_BINARY";
            case BufferedImage.TYPE_BYTE_GRAY:
                return "BYTE_GRAY";
            case BufferedImage.TYPE_BYTE_INDEXED:
                return "BYTE_INDEXED";
            case BufferedImage.TYPE_CUSTOM:
                return "CUSTOM";
            case BufferedImage.TYPE_INT_ARGB:
                return "INT_ARGB";
            case BufferedImage.TYPE_INT_ARGB_PRE:
                return "INT_ARGB_PRE";
            case BufferedImage.TYPE_INT_BGR:
                return "INT_BGR";
            case BufferedImage.TYPE_INT_RGB:
                return "INT_RGB";
            case BufferedImage.TYPE_USHORT_555_RGB:
                return "USHORT_555_RGB";
            case BufferedImage.TYPE_USHORT_565_RGB:
                return "USHORT_565_RGB";
            case BufferedImage.TYPE_USHORT_GRAY:
                return "USHORT_GRAY";
            default:
                return "unrecognized (" + type + ")";
        }
    }

    static String colorSpaceTypeAsString(int type) {
        switch (type) {
            case ColorSpace.TYPE_2CLR:
                return "2CLR";
            case ColorSpace.TYPE_3CLR:
                return "3CLR";
            case ColorSpace.TYPE_4CLR:
                return "4CLR";
            case ColorSpace.TYPE_5CLR:
                return "5CLR";
            case ColorSpace.TYPE_6CLR:
                return "6CLR";
            case ColorSpace.TYPE_7CLR:
                return "7CLR";
            case ColorSpace.TYPE_8CLR:
                return "8CLR";
            case ColorSpace.TYPE_9CLR:
                return "9CLR";
            case ColorSpace.TYPE_ACLR:
                return "ACLR";
            case ColorSpace.TYPE_BCLR:
                return "BCLR";
            case ColorSpace.TYPE_CCLR:
                return "CCLR";
            case ColorSpace.TYPE_CMY:
                return "CMY";
            case ColorSpace.TYPE_CMYK:
                return "CMYK";
            case ColorSpace.TYPE_DCLR:
                return "DCLR";
            case ColorSpace.TYPE_ECLR:
                return "ECLR";
            case ColorSpace.TYPE_FCLR:
                return "FCLR";
            case ColorSpace.TYPE_GRAY:
                return "GRAY";
            case ColorSpace.TYPE_HLS:
                return "HLS";
            case ColorSpace.TYPE_HSV:
                return "HSV";
            case ColorSpace.TYPE_Lab:
                return "Lab";
            case ColorSpace.TYPE_Luv:
                return "Luv";
            case ColorSpace.TYPE_RGB:
                return "RGB";
            case ColorSpace.TYPE_XYZ:
                return "XYZ";
            case ColorSpace.TYPE_YCbCr:
                return "YCbCr";
            case ColorSpace.TYPE_Yxy:
                return "Yxy";
            default:
                return "unrecognized (" + type + ")";
        }
    }

    public static boolean isRgbColorModel(ColorModel cm) {
        if (cm instanceof DirectColorModel &&
                cm.getTransferType() == DataBuffer.TYPE_INT) {
            var directCM = (DirectColorModel) cm;

            return directCM.getRedMask() == 0x00FF0000 &&
                    directCM.getGreenMask() == 0x0000FF00 &&
                    directCM.getBlueMask() == 0x000000FF &&
                    (directCM.getNumComponents() == 3 ||
                            directCM.getAlphaMask() == 0xFF000000);
        }

        return false;
    }

    public static boolean isBgrColorModel(ColorModel cm) {
        if (cm instanceof DirectColorModel &&
                cm.getTransferType() == DataBuffer.TYPE_INT) {
            var directCM = (DirectColorModel) cm;

            return directCM.getRedMask() == 0x000000FF &&
                    directCM.getGreenMask() == 0x0000FF00 &&
                    directCM.getBlueMask() == 0x00FF0000 &&
                    (directCM.getNumComponents() == 3 ||
                            directCM.getAlphaMask() == 0xFF000000);
        }

        return false;
    }
}
