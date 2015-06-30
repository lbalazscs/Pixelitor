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

package pixelitor.layers;

import pixelitor.Composition;
import pixelitor.utils.ImageUtils;

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;

/**
 * A layer mask.
 */
public class LayerMask extends ImageLayer {
    private transient BufferedImage transparencyImage;

    private static final ColorModel transparencyColorModel;

    static {
        byte[] lookupFromIndex = new byte[256];
        for (int i = 0; i < lookupFromIndex.length; i++) {
            lookupFromIndex[i] = (byte) i;
        }
        transparencyColorModel = new IndexColorModel(8, 256, lookupFromIndex, lookupFromIndex, lookupFromIndex, lookupFromIndex);
    }

    public LayerMask(Composition comp, BufferedImage bwImage, String layerName) {
        super(comp, bwImage, layerName + " MASK");
    }

    public BufferedImage getTransparencyImage() {
        return transparencyImage;
    }

    public void updateFromBWImage() {
        System.out.println("LayerMask::updateFromBWImage: CALLED");

        assert image.getType() == BufferedImage.TYPE_BYTE_GRAY;
        assert image.getColorModel() != transparencyColorModel;

        BufferedImage bwImage = image;

        // TODO
        if (bwImage.getType() != BufferedImage.TYPE_BYTE_GRAY) {
            bwImage = ImageUtils.convertToGrayScaleImage(bwImage);
            System.out.println("LayerMask::updateFromBWImage: CONVERTING TO TYPE_BYTE_GRAY");
        }

        WritableRaster raster = bwImage.getRaster();
        this.transparencyImage = new BufferedImage(transparencyColorModel, raster, false, null);
//        this.transparencyImage = new BufferedImage(bwImage.getColorModel(), raster, false, null);
    }

    @Override
    protected BufferedImage createEmptyImageForLayer(int width, int height) {
        ColorSpace graySpace = new ICC_ColorSpace(ICC_Profile.getInstance(ColorSpace.CS_GRAY));
        ColorModel grayModel = new ComponentColorModel(graySpace, false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
        return new BufferedImage(grayModel, grayModel.createCompatibleWritableRaster(width, height), false, null);
    }

    @Override
    protected void imageRefChanged() {
        updateFromBWImage();
        comp.imageChanged(Composition.ImageChangeActions.FULL);
    }
}
