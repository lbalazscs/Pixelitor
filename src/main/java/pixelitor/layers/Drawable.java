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

package pixelitor.layers;

import pixelitor.Composition;
import pixelitor.FilterContext;

import java.awt.Composite;
import java.awt.image.BufferedImage;

/**
 * Something that (unlike text and adjustment layers)
 * consists of a bunch of pixels: an image layer or a layer mask.
 * Can be used with brush tools and filters.
 */
public interface Drawable {
    BufferedImage getImage();

    /**
     * Sets the image ignoring the selection
     */
    void setImage(BufferedImage newImage);

    /**
     * Initializes a filter previewing session.
     */
    void startPreviewing();

    void stopPreviewing();

    void changePreviewImage(BufferedImage img, String filterName, FilterContext context);

    void onFilterDialogAccepted(String filterName);

    void onFilterDialogCanceled();

    void tweenCalculatingStarted();

    void tweenCalculatingEnded();

    void filterWithoutDialogFinished(BufferedImage filteredImage, FilterContext context, String filterName);

    void changeImageForUndoRedo(BufferedImage img, boolean ignoreSelection);

    /**
     * Returns the image from which the "image position" thumbnail is calculated.
     * The canvas size is not considered, only the selection.
     */
    BufferedImage getImageForFilterDialogs();

    TmpDrawingLayer createTmpDrawingLayer(Composite c, boolean softSelection);

    void mergeTmpDrawingLayerDown();

    BufferedImage getCanvasSizedSubImage();

    BufferedImage getFilterSourceImage();

    /**
     * Returns the subimage determined by the selection bounds,
     * or the image if there is no selection.
     */
    BufferedImage getSelectedSubImage(boolean copyIfNoSelection);

    void setShowOriginal(boolean b);

    void debugImages();

    void updateIconImage();

    Composition getComp();

    int getTx();

    int getTy();

    boolean isMaskEditing();

    Layer getLayer();

    String getName();
}
