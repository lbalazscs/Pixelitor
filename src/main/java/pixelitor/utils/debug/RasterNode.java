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

import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;

/**
 * A debugging node for a WritableRaster
 */
public class RasterNode extends DebugNode {
    public RasterNode(WritableRaster raster) {
        super("WritableRaster", raster);
        addClassChild();

        SampleModel sampleModel = raster.getSampleModel();
        add(new SampleModelNode(sampleModel));

        DataBuffer dataBuffer = raster.getDataBuffer();
        add(new DataBufferNode(dataBuffer));
    }
}
