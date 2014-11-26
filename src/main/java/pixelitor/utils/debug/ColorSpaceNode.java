/*
 * Copyright 2009-2014 Laszlo Balazs-Csiki
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
package pixelitor.utils.debug;

import java.awt.color.ColorSpace;

/**
 * A debugging node for a ColorSpace
 */
public class ColorSpaceNode extends DebugNode {
    public ColorSpaceNode(ColorSpace colorSpace) {
        super("ColorSpace", colorSpace);

        addClassChild();

        int numComponents = colorSpace.getNumComponents();
        addIntChild("numComponents", numComponents);

        int type = colorSpace.getType();
        addStringChild("type", getColorSpaceTypeDescription(type));

        boolean is_sRGB = colorSpace.isCS_sRGB();
        addBooleanChild("is_sRGB", is_sRGB);
    }

    private static String getColorSpaceTypeDescription(int type) {
        switch (type) {
            case ColorSpace.TYPE_2CLR:
                return "TYPE_2CLR";
            case ColorSpace.TYPE_3CLR:
                return "TYPE_3CLR";
            case ColorSpace.TYPE_4CLR:
                return "TYPE_4CLR";
            case ColorSpace.TYPE_5CLR:
                return "TYPE_5CLR";
            case ColorSpace.TYPE_6CLR:
                return "TYPE_6CLR";
            case ColorSpace.TYPE_7CLR:
                return "TYPE_7CLR";
            case ColorSpace.TYPE_8CLR:
                return "TYPE_8CLR";
            case ColorSpace.TYPE_9CLR:
                return "TYPE_9CLR";
            case ColorSpace.TYPE_ACLR:
                return "TYPE_ACLR";
            case ColorSpace.TYPE_BCLR:
                return "TYPE_BCLR";
            case ColorSpace.TYPE_CCLR:
                return "TYPE_CCLR";
            case ColorSpace.TYPE_CMY:
                return "TYPE_CMY";
            case ColorSpace.TYPE_CMYK:
                return "TYPE_CMYK";
            case ColorSpace.TYPE_DCLR:
                return "TYPE_DCLR";
            case ColorSpace.TYPE_ECLR:
                return "TYPE_ECLR";
            case ColorSpace.TYPE_FCLR:
                return "TYPE_FCLR";
            case ColorSpace.TYPE_GRAY:
                return "TYPE_GRAY";
            case ColorSpace.TYPE_HLS:
                return "TYPE_HLS";
            case ColorSpace.TYPE_HSV:
                return "TYPE_HSV";
            case ColorSpace.TYPE_Lab:
                return "TYPE_Lab";
            case ColorSpace.TYPE_Luv:
                return "TYPE_Luv";
            case ColorSpace.TYPE_RGB:
                return "TYPE_RGB";
            case ColorSpace.TYPE_XYZ:
                return "TYPE_XYZ";
            case ColorSpace.TYPE_YCbCr:
                return "TYPE_YCbCr";
            case ColorSpace.TYPE_Yxy:
                return "TYPE_Yxy";
            default:
                return "UNKNOWN";
        }
    }

}
