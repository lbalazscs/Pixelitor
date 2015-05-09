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

import java.awt.image.SampleModel;

/**
 * A debugging node for a SampleModel
 */
public class SampleModelNode extends DebugNode {

    public SampleModelNode(SampleModel sampleModel) {
        super("SampleModel", sampleModel);
        addClassChild();

        int width = sampleModel.getWidth();
        addIntChild("width", width);

        int height = sampleModel.getHeight();
        addIntChild("height", height);

        int dataType = sampleModel.getDataType();
        addStringChild("dataType", dataAndTransferTypeToString(dataType));

        int numBands = sampleModel.getNumBands();
        addIntChild("numBands", numBands);

        int transferType = sampleModel.getTransferType();
        addStringChild("transferType", dataAndTransferTypeToString(transferType));

        int numDataElements = sampleModel.getNumDataElements();
        addIntChild("numDataElements", numDataElements);
    }

    // strings based on the constants defined in java.awt.image.DataBuffer
    private static String dataAndTransferTypeToString(int type) {
        switch (type) {
            case 0:
                return "TYPE_BYTE";
            case 1:
                return "TYPE_USHORT";
            case 2:
                return "TYPE_SHORT";
            case 3:
                return "TYPE_INT";
            case 4:
                return "TYPE_FLOAT";
            case 5:
                return "TYPE_DOUBLE";
            case 32:
                return "TYPE_UNDEFINED";
            default:
                return "UNKNOWN";
        }
    }
}
