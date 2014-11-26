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

        // TODO: as string
        int dataType = sampleModel.getDataType();
        addIntChild("dataType", dataType);

        int numBands = sampleModel.getNumBands();
        addIntChild("numBands", numBands);

        // TODO: as string
        int transferType = sampleModel.getTransferType();
        addIntChild("transferType", transferType);

        int numDataElements = sampleModel.getNumDataElements();
        addIntChild("numDataElements", numDataElements);
    }
}
