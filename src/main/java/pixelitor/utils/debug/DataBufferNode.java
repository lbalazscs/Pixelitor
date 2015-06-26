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

import java.awt.image.DataBuffer;

/**
 * A debugging node for a DataBuffer
 */
public class DataBufferNode extends DebugNode {

    public DataBufferNode(DataBuffer dataBuffer) {
        super("DataBuffer", dataBuffer);
        addClassChild();

        int numBanks = dataBuffer.getNumBanks();
        addIntChild("numBanks", numBanks);

        int type = dataBuffer.getDataType();
        addStringChild("type", getDataBufferTypeDescription(type));

        int size = dataBuffer.getSize();
        addIntChild("size", size);
    }


    public static String getDataBufferTypeDescription(int type) {
        // The DataBuffer lists the additional types TYPE_SHORT, TYPE_FLOAT, TYPE_DOUBLE, TYPE_UNDEFINED
        // but those are not in use
        return ColorModelNode.getTransferTypeDescription(type);
    }

}