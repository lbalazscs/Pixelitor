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

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DirectColorModel;

/**
 * A A debugging node for a ColorModel
 */
public class ColorModelNode extends DebugNode {
    public ColorModelNode(ColorModel colorModel) {
        this("ColorModel", colorModel);
    }

    public ColorModelNode(String name, ColorModel colorModel) {
        super(name, colorModel);
        addClassChild();

        ColorSpace colorSpace = colorModel.getColorSpace();
        add(new ColorSpaceNode(colorSpace));

        int numColorComponents = colorModel.getNumColorComponents();
        addIntChild("numColorComponents", numColorComponents);

        int numComponents = colorModel.getNumComponents();
        addIntChild("numComponents", numComponents);

        boolean hasAlpha = colorModel.hasAlpha();
        addBooleanChild("hasAlpha", hasAlpha);

        int pixelSize = colorModel.getPixelSize();
        addIntChild("pixelSize", pixelSize);

        int transferType = colorModel.getTransferType();
        String transferTypeDescription = getTransferTypeDescription(transferType);
        addStringChild("transferType", transferTypeDescription);

        int transparency = colorModel.getTransparency();
        addStringChild("transparency", getTransparencyDescription(transparency));

        boolean isRGB = isRgbColorModel(colorModel);
        addBooleanChild("isRGB", isRGB);

        boolean isBGR = isBgrColorModel(colorModel);
        addBooleanChild("isBGR", isBGR);
    }

    private static boolean isRgbColorModel(ColorModel cm) {
        if (cm instanceof DirectColorModel &&
                cm.getTransferType() == DataBuffer.TYPE_INT) {
            DirectColorModel directCM = (DirectColorModel) cm;

            return directCM.getRedMask() == 0x00FF0000 &&
                    directCM.getGreenMask() == 0x0000FF00 &&
                    directCM.getBlueMask() == 0x000000FF &&
                    (directCM.getNumComponents() == 3 ||
                            directCM.getAlphaMask() == 0xFF000000);
        }

        return false;
    }

    private static boolean isBgrColorModel(ColorModel cm) {
        if (cm instanceof DirectColorModel &&
                cm.getTransferType() == DataBuffer.TYPE_INT) {
            DirectColorModel directCM = (DirectColorModel) cm;

            return directCM.getRedMask() == 0x000000FF &&
                    directCM.getGreenMask() == 0x0000FF00 &&
                    directCM.getBlueMask() == 0x00FF0000 &&
                    (directCM.getNumComponents() == 3 ||
                            directCM.getAlphaMask() == 0xFF000000);
        }

        return false;
    }

    public static String getTransferTypeDescription(int transferType) {
        switch (transferType) {
            case DataBuffer.TYPE_BYTE:
                return "TYPE_BYTE";
            case DataBuffer.TYPE_USHORT:
                return "TYPE_USHORT";
            case DataBuffer.TYPE_INT:
                return "TYPE_INT";
            default:
                return "UNKNOWN";
        }
    }

    private static String getTransparencyDescription(int transparency) {
        if (transparency == Transparency.OPAQUE) {
            return "OPAQUE";
        }
        if (transparency == Transparency.BITMASK) {
            return "BITMASK";
        } else if (transparency == Transparency.TRANSLUCENT) {
            return "TRANSLUCENT";
        }
        return "UNKNOWN";
    }

}