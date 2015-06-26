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
package pixelitor.utils.debug;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;

/**
 * A debugging node for a BufferedImage
 */
public class BufferedImageNode extends DebugNode {
    public BufferedImageNode(BufferedImage image) {
        this("BufferedImage", image);
    }

    public BufferedImageNode(String name, BufferedImage image) {
        super(name, image);
        ColorModel colorModel = image.getColorModel();
        add(new ColorModelNode(colorModel));

        WritableRaster raster = image.getRaster();
        add(new RasterNode(raster));

        String typeDescription = getTypeDescription(image.getType());
        addStringChild("type", typeDescription);

        int width = image.getWidth();
        addIntChild("width", width);

        int height = image.getHeight();
        addIntChild("height", height);

        boolean alphaPremultiplied = image.isAlphaPremultiplied();
        addBooleanChild("alphaPremultiplied", alphaPremultiplied);
    }


    public static String getTypeDescription(int type) {
        String retVal;
        switch (type) {
            case BufferedImage.TYPE_3BYTE_BGR:
                retVal = "TYPE_3BYTE_BGR";
                break;
            case BufferedImage.TYPE_4BYTE_ABGR:
                retVal = "TYPE_4BYTE_ABGR";
                break;
            case BufferedImage.TYPE_4BYTE_ABGR_PRE:
                retVal = "TYPE_4BYTE_ABGR_PRE";
                break;
            case BufferedImage.TYPE_BYTE_BINARY:
                retVal = "TYPE_BYTE_BINARY";
                break;
            case BufferedImage.TYPE_BYTE_GRAY:
                retVal = "TYPE_BYTE_GRAY";
                break;
            case BufferedImage.TYPE_BYTE_INDEXED:
                retVal = "TYPE_BYTE_INDEXED";
                break;
            case BufferedImage.TYPE_CUSTOM:
                retVal = "TYPE_CUSTOM";
                break;
            case BufferedImage.TYPE_INT_ARGB:
                retVal = "TYPE_INT_ARGB";
                break;
            case BufferedImage.TYPE_INT_ARGB_PRE:
                retVal = "TYPE_INT_ARGB_PRE";
                break;
            case BufferedImage.TYPE_INT_BGR:
                retVal = "TYPE_INT_BGR";
                break;
            case BufferedImage.TYPE_INT_RGB:
                retVal = "TYPE_INT_RGB";
                break;
            case BufferedImage.TYPE_USHORT_555_RGB:
                retVal = "TYPE_USHORT_555_RGB";
                break;
            case BufferedImage.TYPE_USHORT_565_RGB:
                retVal = "TYPE_USHORT_565_RGB";
                break;
            case BufferedImage.TYPE_USHORT_GRAY:
                retVal = "TYPE_USHORT_GRAY";
                break;
            default:
                retVal = "unrecognized (program error)";
                break;
        }
        return retVal;
    }
}