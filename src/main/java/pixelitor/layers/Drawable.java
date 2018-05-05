/*
 * Copyright 2018 Laszlo Balazs-Csiki
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

import pixelitor.ChangeReason;
import pixelitor.Composition;
import pixelitor.filters.comp.Flip;
import pixelitor.filters.comp.Rotate;
import pixelitor.selection.Selection;
import pixelitor.utils.VisibleForTesting;

import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 * A bunch of pixels.
 * Practically always an image layer or a layer mask.
 */
public interface Drawable {
    ImageLayer duplicate(boolean sameName);

    BufferedImage getImage();

    void setImage(BufferedImage newImage);

    void replaceImage(BufferedImage newImage, String editName);

    void startPreviewing();

    void onDialogAccepted(String filterName);

    void onDialogCanceled();

    void stopPreviewing();

    void tweenCalculatingStarted();

    void tweenCalculatingEnded();

    void changePreviewImage(BufferedImage img, String filterName, ChangeReason cr);

    void filterWithoutDialogFinished(BufferedImage transformedImage, ChangeReason cr, String filterName);

    void changeImageUndoRedo(BufferedImage img, boolean ignoreSelection);

    // returns the image bounds relative to the canvas
    Rectangle getImageBounds();

    boolean checkImageDoesNotCoverCanvas();

    void enlargeImage(Rectangle canvasBounds);

    BufferedImage getImageForFilterDialogs();

    void flip(Flip.Direction direction);

    void rotate(Rotate.SpecialAngle angle);

    TmpDrawingLayer createTmpDrawingLayer(Composite c);

    void mergeTmpDrawingLayerDown();

    BufferedImage createCompositionSizedTmpImage();

    BufferedImage getCanvasSizedSubImage();

    BufferedImage getFilterSourceImage();

    BufferedImage getImageOrSubImageIfSelected(boolean copyIfNoSelection, boolean copyAndTranslateIfSelected);

    BufferedImage getSelectionSizedPartFrom(BufferedImage src, Selection selection, boolean copyAndTranslateIfSelected);

    boolean cropToCanvasSize();

    void enlargeCanvas(int north, int east, int south, int west);

    void resize(int canvasTargetWidth, int canvasTargetHeight, boolean progressiveBilinear);

    boolean isBigLayer();

    void crop(Rectangle2D cropRect);

    void paintLayerOnGraphics(Graphics2D g, boolean firstVisibleLayer);

    void setShowOriginal(boolean b);

    void debugImages();

    void updateIconImage();

    BufferedImage applyLayerMask(boolean addToHistory);

    @VisibleForTesting
    BufferedImage getPreviewImage();

    Composition getComp();

    int getTX();

    int getTY();

    boolean isMaskEditing();
}
