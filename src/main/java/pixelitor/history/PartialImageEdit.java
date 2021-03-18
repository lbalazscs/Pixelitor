/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.history;

import pixelitor.Composition;
import pixelitor.layers.Drawable;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.debug.Debug;
import pixelitor.utils.debug.DebugNode;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.lang.ref.SoftReference;

import static java.lang.String.format;

/**
 * Represents the changes made to a part of an image (for example brush strokes).
 * Only the affected pixels are saved in order to reduce the memory usage
 */
public class PartialImageEdit extends FadeableEdit {
    private final Rectangle saveRect;
    private SoftReference<Raster> backupRasterRef;

    private final Drawable dr;

    public PartialImageEdit(String name, Composition comp, Drawable dr,
                            BufferedImage image, Rectangle saveRect) {
        super(name, comp, dr);

        this.dr = dr;
        this.saveRect = saveRect;

        Raster backupRaster = image.getData(this.saveRect);
        backupRasterRef = new SoftReference<>(backupRaster);
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        if (!swapRasters()) {
            throw new CannotUndoException();
        }
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        if (!swapRasters()) {
            throw new CannotRedoException();
        }
    }

    /**
     * Returns true if successful
     */
    private boolean swapRasters() {
        Raster backupRaster = backupRasterRef.get();
        if (backupRaster == null) {
            return false;
        }

        BufferedImage image = dr.getImage();

        Raster tmpRaster = null;
        try {
            tmpRaster = image.getData(saveRect);
            image.setData(backupRaster);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("PartialImageEdit.swapRasters saveRect = " + saveRect);
            int width = image.getWidth();
            int height = image.getHeight();
            System.out.println("PartialImageEdit.swapRasters width = " + width + ", height = " + height);

            debugRaster("tmpRaster", tmpRaster);
            debugRaster("backupRaster", backupRaster);

            throw e;
        }

        backupRasterRef = new SoftReference<>(tmpRaster);

        comp.update();
        dr.updateIconImage();

        return true;
    }

    private static void debugRaster(String name, Raster raster) {
        if (raster == null) {
            System.err.printf("PartialImageEdit::debugRaster: NULL RASTER, name = '%s'%n", name);
            return;
        }
        Rectangle rasterBounds = raster.getBounds();
        String className = raster.getClass().getSimpleName();
        DataBuffer dataBuffer = raster.getDataBuffer();
        int dataType = dataBuffer.getDataType();
        String typeAsString = Debug.dataBufferTypeAsString(dataType);
        int numBanks = dataBuffer.getNumBanks();
        int numBands = raster.getNumBands();
        int numDataElements = raster.getNumDataElements();

        String msg = format("className = %s, rasterBounds = %s, dataType = %d, " +
                "typeAsString=%s, numBanks = %d, numBands = %d, numDataElements = %d",
            className, rasterBounds, dataType,
            typeAsString, numBanks, numBands, numDataElements);

        System.out.println("PartialImageEdit::debugRaster debugging raster: " + name + ": " + msg);
    }

    @Override
    public void die() {
        super.die();

        backupRasterRef = null;
    }

    @Override
    public BufferedImage getBackupImage() {
        if (backupRasterRef == null) { // died
            return null;
        }
        Raster backupRaster = backupRasterRef.get();
        if (backupRaster == null) { // soft reference lost
            return null;
        }

        // recreate the full image as if it was backed up entirely
        // because Fade expects to fade images of equal size
        // TODO this is not the optimal solution  - Fade should fade only the changed area
        BufferedImage fullImage = dr.getImage();
        BufferedImage previousImage = ImageUtils.copyImage(fullImage);
        previousImage.setData(backupRaster);

        var selection = dr.getComp().getSelection();
        if (selection != null) {
            // backupRaster is relative to the full image, but we need to return a selection-sized image
            previousImage = ImageUtils.getSelectionSizedPartFrom(
                previousImage, selection, dr.getTx(), dr.getTy());
        }

        return previousImage;
    }

    @Override
    public DebugNode createDebugNode() {
        var node = super.createDebugNode();

        int width = -1;
        int height = -1;
        Raster backupRaster = backupRasterRef.get();
        if (backupRaster != null) {
            width = backupRaster.getWidth();
            height = backupRaster.getHeight();
        }

        node.addInt("backup image width", width);
        node.addInt("backup image height", height);

        return node;
    }
}
