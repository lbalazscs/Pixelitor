/*
 * Copyright 2016 Laszlo Balazs-Csiki
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
import pixelitor.ConsistencyChecks;
import pixelitor.filters.comp.Flip;
import pixelitor.filters.comp.Rotate;
import pixelitor.gui.utils.Dialogs;
import pixelitor.history.AddToHistory;
import pixelitor.history.ApplyLayerMaskEdit;
import pixelitor.history.ContentLayerMoveEdit;
import pixelitor.history.History;
import pixelitor.history.ImageEdit;
import pixelitor.history.PixelitorEdit;
import pixelitor.selection.IgnoreSelection;
import pixelitor.selection.Selection;
import pixelitor.tools.Tools;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Messages;
import pixelitor.utils.UpdateGUI;
import pixelitor.utils.Utils;
import pixelitor.utils.VisibleForTesting;

import javax.swing.*;
import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static java.awt.RenderingHints.KEY_INTERPOLATION;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
import static java.util.Objects.requireNonNull;
import static pixelitor.ChangeReason.REPEAT_LAST;
import static pixelitor.Composition.ImageChangeActions.FULL;
import static pixelitor.Composition.ImageChangeActions.INVALIDATE_CACHE;
import static pixelitor.Composition.ImageChangeActions.REPAINT;
import static pixelitor.filters.comp.Flip.Direction.HORIZONTAL;
import static pixelitor.layers.ImageLayer.State.NORMAL;
import static pixelitor.layers.ImageLayer.State.PREVIEW;
import static pixelitor.layers.ImageLayer.State.SHOW_ORIGINAL;

/**
 * An image layer.
 * <p>
 * Filter without a dialog are executed as ChangeReason.OP_WITHOUT_DIALOG on the EDT.
 * The filter asks getFilterSource() in the NORMAL state, and
 * (if there is no selection) the image (not a copy!) is returned as the filter source.
 * The filter transforms the image, and calls filterWithoutDialogFinished
 * with the transformed image.
 * <p>
 * Filters with dialog are executed as ChangeReason.OP_PREVIEW on the EDT.
 * startPreviewing() is called when a new dialog appears,
 * right before creating the adjustment panel.
 * Before executing a filter with dialog, startNewPreviewFromDialog() is called
 * (does nothing in the current implementation), then the filter is executed,
 * and each execution is followed by changePreviewImage().
 * At the end, depending on the user action, okPressedInDialog()
 * or cancelPressedInDialog() is called.
 */
public class ImageLayer extends ContentLayer {
    enum State {
        /**
         * The layer is in normal state when no filter is running on it
         */
        NORMAL {
        },
        /**
         * The layer is in previewing mode when a filter with dialog is opened
         * Filters that work normally without a dialog can still work with a
         * dialog when invoked from places like the the "Random Filter"
         */
        PREVIEW {
        },
        /**
         * The layer is in previewing mode, but "Show Original" is pressed in the dialog
         */
        SHOW_ORIGINAL {
        }
    }

    /**
     * Whether the preview image is different from the normal image
     * It makes sense only in PREVIEW mode
     */
    private transient boolean imageContentChanged = false;

    private static final long serialVersionUID = 2L;

    //
    // transient variables from here!
    //
    private transient State state = NORMAL;

    private transient TmpDrawingLayer tmpDrawingLayer;

    /**
     * The image content of this image layer
     */
    protected transient BufferedImage image = null;

    /**
     * The image shown during previews
     */
    private transient BufferedImage previewImage;

    /**
     * The source image passed to the filters.
     * This is different than image if there is a selection.
     */
    private transient BufferedImage filterSourceImage;

    /**
     * Creates a new layer with the given image
     */
    public ImageLayer(Composition comp, BufferedImage image, String name, Layer parent) {
        super(comp, name == null ? comp.generateNewLayerName() : name, parent);

        requireNonNull(image);

        setImage(image);
        updateIconImage();
        checkConstructorPostConditions();
    }

    /**
     * Creates a new layer with the given image and size.
     * Used when an image is pasted into a layer
     */
    public ImageLayer(Composition comp, BufferedImage pastedImage, String name, int width, int height) {
        super(comp, name, null);

        requireNonNull(pastedImage);

        int pastedWidth = pastedImage.getWidth();
        int pastedHeight = pastedImage.getHeight();

        boolean pastedImageTooSmall = pastedWidth < width || pastedHeight < height;

        BufferedImage newImage = pastedImage;
        if (pastedImageTooSmall) {
            // a new image is created
            int newWidth = Math.max(width, pastedWidth);
            int newHeight = Math.max(height, pastedHeight);
            newImage = createEmptyImageForLayer(newWidth, newHeight);
            Graphics2D g = newImage.createGraphics();

            int drawX = Math.max((width - pastedWidth) / 2, 0);
            int drawY = Math.max((height - pastedHeight) / 2, 0);

            g.drawImage(pastedImage, drawX, drawY, null);
            g.dispose();
        }

        setImage(newImage);

        boolean addXTranslation = pastedWidth > width;
        boolean addYTranslation = pastedHeight > height;
        int newTX = 0;
        int newTY = 0;
        if (addXTranslation) {
            newTX = -(pastedWidth - width) / 2;
        }
        if (addYTranslation) {
            newTY = -(pastedHeight - height) / 2;
        }
        setTranslation(newTX, newTY);

        updateIconImage();
        checkConstructorPostConditions();
    }

    /**
     * Creates a new empty layer
     */
    public ImageLayer(Composition comp, String name) {
        super(comp, name == null ? comp.generateNewLayerName() : name, null);

        BufferedImage emptyImage = createEmptyImageForLayer(canvas.getWidth(), canvas.getHeight());
        setImage(emptyImage);
        checkConstructorPostConditions();
    }

    private void checkConstructorPostConditions() {
        assert canvas != null;
        assert image != null;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        ImageUtils.serializeImage(out, image);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        state = NORMAL;
        in.defaultReadObject();
        setImage(ImageUtils.deserializeImage(in));
        imageContentChanged = false;
    }

    @Override
    public ImageLayer duplicate(boolean sameName) {
        BufferedImage imageCopy = ImageUtils.copyImage(image);
        String duplicateName = sameName ? name : Utils.createCopyName(name);
        ImageLayer d = new ImageLayer(comp, imageCopy, duplicateName, null);
        d.setOpacity(opacity, UpdateGUI.NO, AddToHistory.NO, true);
        d.setTranslation(translationX, translationY);
        d.setBlendingMode(blendingMode, UpdateGUI.NO, AddToHistory.NO, false);

        if (hasMask()) {
            d.addMask(mask.duplicate(d));
        }

        return d;
    }

    public BufferedImage getImage() {
        return image;
    }

    /**
     * If there is no selection, returns the newImage
     * If there is a selection, copies newImage into src according to the selection, and returns src
     */
    private BufferedImage replaceImageWithSelection(BufferedImage src, BufferedImage newImage) {
        assert src != null;
        assert newImage != null;
        assert Utils.checkRasterMinimum(newImage);

        Shape selectionShape = comp.getSelectionShape();
        if (selectionShape == null) {
            return newImage;
        } else {
            // the argument image pixels will replace the old ones only where selected
            Graphics2D g = src.createGraphics();
            g.translate(-getTX(), -getTY());
            g.setComposite(AlphaComposite.Src);
            g.setClip(selectionShape);
            Rectangle bounds = selectionShape.getBounds();
            g.drawImage(newImage, bounds.x, bounds.y, null);
            g.dispose();
            return src;
        }
    }

    private void setPreviewWithSelection(BufferedImage newImage) {
        previewImage = replaceImageWithSelection(previewImage, newImage);
    }

    private void setImageWithSelection(BufferedImage newImage) {
        image = replaceImageWithSelection(image, newImage);
        imageRefChanged();

        comp.imageChanged(INVALIDATE_CACHE);
    }

    /**
     * Sets the image ignoring the selection
     */
    public void setImage(BufferedImage newImage) {
        BufferedImage oldRef = image;
        image = requireNonNull(newImage);
        imageRefChanged();

        assert Utils.checkRasterMinimum(newImage);

        comp.imageChanged(INVALIDATE_CACHE);

        if (oldRef != null && oldRef != image) {
            oldRef.flush();
        }
    }

    /**
     * Replaces the image with history and icon update
     */
    public void replaceImage(BufferedImage newImage, String editName) {
        BufferedImage oldImage = image;
        setImage(newImage);
        ImageEdit edit = new ImageEdit(comp, editName, this, oldImage, IgnoreSelection.YES, false);
        History.addEdit(edit);

        updateIconImage();
    }

    /**
     * Initializes a preview session
     */
    public void startPreviewing() {
        assert state == NORMAL : "state was " + state;

        if (comp.hasSelection()) {
            // if we have a selection, then the preview image reference cannot be simply
            // the image reference, because when we draw into the preview image, we would
            // also draw on the real image, and after cancel we would still have the changed version.
            previewImage = ImageUtils.copyImage(image);
        } else {
            // if there is no selection, then there is no problem, because
            // the previewImage reference will be overwritten
            previewImage = image;
        }
        setState(PREVIEW);
    }

    public void okPressedInDialog(String filterName) {
        assert (state == PREVIEW) || (state == SHOW_ORIGINAL);
        assert previewImage != null;

        if (imageContentChanged) {
            ImageEdit edit = new ImageEdit(comp, filterName, this, getImageOrSubImageIfSelected(true, true),
                    IgnoreSelection.NO, true);
            History.addEdit(edit);
        }

        image = previewImage;
        imageRefChanged();

        if (imageContentChanged) {
            updateIconImage();
        }

        previewImage = null;

        boolean wasShowOriginal = (state == SHOW_ORIGINAL);
        setState(NORMAL);

        if (wasShowOriginal) {
            comp.imageChanged(FULL);
//        } else {
//            comp.imageChanged(INVALIDATE_CACHE);
        }
    }

    public void cancelPressedInDialog() {
        stopPreviewing();
    }

    public void stopPreviewing() {
        assert state == PREVIEW || state == SHOW_ORIGINAL;
        assert previewImage != null;

        setState(NORMAL);

        // so that layer mask transparency image is regenerated
        // from the real image after the previews
        imageRefChanged();

        previewImage = null;
        comp.imageChanged(FULL);
    }

    public void tweenCalculatingStarted() {
        assert state == NORMAL;
        startPreviewing();
    }

    public void tweenCalculatingEnded() {
        assert state == PREVIEW;
        stopPreviewing();
    }

    /**
     * @return true if the image has to be repainted
     */
    public void changePreviewImage(BufferedImage img, String filterName, ChangeReason changeReason) {
        // typically we should be in PREVIEW mode
        if (state == SHOW_ORIGINAL) {
            // this is OK, something was adjusted while in show original mode
        } else if (state == NORMAL) {
            throw new IllegalStateException(String.format(
                    "change preview in normal state, filter = %s, changeReason = %s, class = %s)",
                    filterName, changeReason, this.getClass().getSimpleName()));
        }

        assert previewImage != null :
                String.format("previewImage was null with %s, changeReason = %s, class = %s",
                        filterName, changeReason, this.getClass().getSimpleName());
        assert img != null;

        if (img == image) {
            // this can happen if a filter with preview decides that no
            // change is necessary and returns the src

            imageContentChanged = false; // no history will be necessary

            // it still can happen that the image needs to be repainted
            // because the preview image can be different from the image
            // (the user does something, but then resets the params to a do-nothing state)
            boolean shouldRefresh = image != previewImage;
            previewImage = image;

            if (shouldRefresh) {
                imageRefChanged();
                comp.imageChanged(FULL);
            }
        } else {
            imageContentChanged = true; // history will be necessary

            setPreviewWithSelection(img);
            setState(PREVIEW);
            imageRefChanged();
            comp.imageChanged(FULL);
        }
    }

    public void filterWithoutDialogFinished(BufferedImage transformedImage, ChangeReason changeReason, String opName) {
        requireNonNull(transformedImage);

        comp.setDirty(true);

        // A filter without dialog should never return the original image...
        if (transformedImage == image) {
            // ...unless Repeat Last starts a filter that normally has a dialog without one
            if (changeReason != REPEAT_LAST) {
                throw new IllegalStateException(opName + " returned the original image, changeReason = " + changeReason);
            } else {
                return;
            }
        }

        // filters without dialog run in the normal state
        assert state == NORMAL;

        BufferedImage imageForUndo = getFilterSourceImage();
        setImageWithSelection(transformedImage);

        if (!changeReason.needsUndo()) {
            return;
        }

        // at this point we are sure that the image changed,
        // considering that a filter without dialog was running
        if (imageForUndo == image) {
            throw new IllegalStateException("imageForUndo == image");
        }
        assert imageForUndo != null;
        ImageEdit edit = new ImageEdit(comp, opName, this,
                imageForUndo, IgnoreSelection.NO, true);
        History.addEdit(edit);

        // otherwise the next filter run will take the old image source,
        // not the actual one
        filterSourceImage = null;
        updateIconImage();
        comp.imageChanged(FULL);
    }

    public void changeImageUndoRedo(BufferedImage img, IgnoreSelection ignoreSelection) {
        requireNonNull(img);
        assert img != image; // simple filters always change something
        assert state == NORMAL;

        if (ignoreSelection.isYes()) {
            setImage(img);
        } else {
            setImageWithSelection(img);
        }
    }

    // returns the image bounds relative to the canvas
    public Rectangle getImageBounds() {
        return new Rectangle(translationX, translationY, image.getWidth(), image.getHeight());
    }

    public boolean checkImageDoesNotCoverCanvas() {
        Rectangle canvasBounds = comp.getCanvasBounds();
        Rectangle imageBounds = getImageBounds();
        boolean needsEnlarging = !(imageBounds.contains(canvasBounds));
        return needsEnlarging;
    }

    /**
     * Enlarges the image so that it covers the canvas completely.
     */
    public void enlargeImage(Rectangle canvasBounds) {
        try {
            Rectangle imageBounds = getImageBounds();
            Rectangle targetImageBounds = imageBounds.union(canvasBounds);

            int newWidth = targetImageBounds.width;
            int newHeight = targetImageBounds.height;

            BufferedImage bi = createEmptyImageForLayer(newWidth, newHeight);
            Graphics2D g = bi.createGraphics();
            int drawX = imageBounds.x - targetImageBounds.x;
            int drawY = imageBounds.y - targetImageBounds.y;
            g.drawImage(image, drawX, drawY, null);
            g.dispose();

            translationX = targetImageBounds.x - canvasBounds.x;
            translationY = targetImageBounds.y - canvasBounds.y;

            setImage(bi);
        } catch (OutOfMemoryError e) {
            Dialogs.showOutOfMemoryDialog(e);
        }
    }

    /**
     * Returns the image shown in the image selector in filter dialogs.
     * The canvas size is not considered, only the selection.
     */
    public BufferedImage getImageForFilterDialogs() {
        Selection selection = comp.getSelection();
        if (selection == null) {
            return image;
        }

        Rectangle selectionBounds = selection.getShapeBounds();
        return image.getSubimage(selectionBounds.x, selectionBounds.y, selectionBounds.width, selectionBounds.height);
    }

    @Override
    public void flip(Flip.Direction direction) {
        AffineTransform imageTx = direction.getImageTX(this);
        int tXAbs = -getTX();
        int tYAbs = -getTY();
        int newTXAbs;
        int newTYAbs;

        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();

        BufferedImage dest = ImageUtils.createImageWithSameColorModel(image);
        Graphics2D g2 = dest.createGraphics();

        if (direction == HORIZONTAL) {
            newTXAbs = imageWidth - canvasWidth - tXAbs;
            newTYAbs = tYAbs;
        } else {
            newTXAbs = tXAbs;
            newTYAbs = imageHeight - canvasHeight - tYAbs;
        }

        g2.setTransform(imageTx);
        g2.drawImage(image, 0, 0, imageWidth, imageHeight, null);
        g2.dispose();

        setTranslation(-newTXAbs, -newTYAbs);

        setImage(dest);
    }

    @Override
    public void rotate(Rotate.SpecialAngle angle) {
        int tx = getTX();
        int ty = getTY();
        int tXAbs = -tx;
        int tYAbs = -ty;
        int newTXAbs = 0;
        int newTYAbs = 0;

        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();

        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();

        int angleDegree = angle.getAngleDegree();
        if (angleDegree == 90) {
            newTXAbs = imageHeight - tYAbs - canvasHeight;
            newTYAbs = tXAbs;
        } else if (angleDegree == 270) {
            newTXAbs = tYAbs;
            newTYAbs = imageWidth - tXAbs - canvasWidth;
        } else if (angleDegree == 180) {
            newTXAbs = imageWidth - canvasWidth - tXAbs;
            newTYAbs = imageHeight - canvasHeight - tYAbs;
        }

        BufferedImage dest = angle.createDestImage(image);

        Graphics2D g2 = dest.createGraphics();
        // nearest neighbor should be ok for 90, 180, 270 degrees
        g2.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        g2.setTransform(angle.getImageTX(this));

        g2.drawImage(image, 0, 0, imageWidth, imageHeight, null);
        g2.dispose();

        setTranslation(-newTXAbs, -newTYAbs);

//        setImageWithSelection(dest);
        setImage(dest);
    }

    private BufferedImage getMaskedImage() {
        if(mask == null || !isMaskEnabled()) {
            return image;
        } else {
            BufferedImage copy = ImageUtils.copyImage(image);
            mask.applyToImage(copy);
            return copy;
        }
    }

    @Override
    public void mergeDownOn(ImageLayer bellowImageLayer) {
        int aX = getTX();
        int aY = getTY();
        BufferedImage bellowImage = bellowImageLayer.getImage();
        int bX = bellowImageLayer.getTX();
        int bY = bellowImageLayer.getTY();
        BufferedImage ourImage = getMaskedImage();
        Graphics2D g = bellowImage.createGraphics();
        int x = aX - bX;
        int y = aY - bY;
        Composite composite = blendingMode.getComposite(opacity);
        g.setComposite(composite);
        g.drawImage(ourImage, x, y, null);
        g.dispose();
    }

    public TmpDrawingLayer createTmpDrawingLayer(Composite c) {
        tmpDrawingLayer = new TmpDrawingLayer(this, c);
        return tmpDrawingLayer;
    }

    public void mergeTmpDrawingLayerDown() {
        if (tmpDrawingLayer == null) {
            return;
        }
        Graphics2D g = image.createGraphics();

        tmpDrawingLayer.paintLayer(g, -getTX(), -getTY());
        g.dispose();

        tmpDrawingLayer.dispose();
        tmpDrawingLayer = null;
    }

    public BufferedImage createCompositionSizedTmpImage() {
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        // it is important that the tmp image has transparency
        // even for layer masks, otherwise drawing is not possible
        return ImageUtils.createSysCompatibleImage(width, height);
    }

    public BufferedImage getCanvasSizedSubImage() {
        if (!isBigLayer()) {
            return image;
        }

        int x = -getTX();
        int y = -getTY();

        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();

        assert ConsistencyChecks.imageCoversCanvasCheck(this);

        BufferedImage subImage;
        try {
            subImage = image.getSubimage(x, y, canvasWidth, canvasHeight);
        } catch (RasterFormatException e) {
            System.out.printf("ImageLayer.getCanvasSizedSubImage x = %d, y = %d, " +
                            "canvasWidth = %d, canvasHeight = %d, " +
                            "imageWidth = %d, imageHeight = %d%n",
                    x, y, canvasWidth, canvasHeight, image.getWidth(), image.getHeight());
            WritableRaster raster = image.getRaster();

            System.out.printf("ImageLayer.getCanvasSizedSubImage " +
                            "minX = %d, minY = %d, width = %d, height=%d %n",
                    raster.getMinX(), raster.getMinY(), raster.getWidth(), raster.getHeight());

            throw e;
        }

        assert subImage.getWidth() == canvasWidth;
        assert subImage.getHeight() == canvasHeight;
        return subImage;
    }

    public BufferedImage getFilterSourceImage() {
        if (filterSourceImage == null) {
            filterSourceImage = getImageOrSubImageIfSelected(false, true);
        }
        return filterSourceImage;
    }

    /**
     * If there is a selection, then the filters work on a subimage determined by the selection bounds.
     */
    public BufferedImage getImageOrSubImageIfSelected(boolean copyIfNoSelection, boolean copyAndTranslateIfSelected) {
        Selection selection = comp.getSelection();
        if (selection == null) {
            if (copyIfNoSelection) {
                return ImageUtils.copyImage(image);
            }
            return image;
        }

        return getSelectionSizedPartFrom(image, selection, copyAndTranslateIfSelected);
    }

    public BufferedImage getSelectionSizedPartFrom(BufferedImage src, Selection selection, boolean copyAndTranslateIfSelected) {
        assert selection != null;

        Rectangle bounds = selection.getShapeBounds(); // relative to the composition

        bounds.translate(-getTX(), -getTY()); // relative to the image

        bounds = SwingUtilities.computeIntersection(
                0, 0, src.getWidth(), src.getHeight(), // image bounds
                bounds);

        if (bounds.isEmpty()) { // TODO if the selection is outside the image?
            if (copyAndTranslateIfSelected) {
                return ImageUtils.copyImage(src);
            } else {
                return src;
            }
        }

        if (copyAndTranslateIfSelected) {
            return ImageUtils.getCopiedSubimage(src, bounds);
        } else {
            BufferedImage retVal = src.getSubimage(bounds.x, bounds.y, bounds.width, bounds.height);
            return retVal;
        }
    }

    /**
     * Returns true if something was changed
     */
    public boolean cropToCanvasSize() {
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();

        if ((imageWidth > canvasWidth) || (imageHeight > canvasHeight)) {
            BufferedImage newImage = ImageUtils.crop(image, -getTX(), -getTY(), canvasWidth, canvasHeight);

            BufferedImage tmp = image;
            setImage(newImage);
            tmp.flush();

            setTranslation(0, 0);
            return true;
        }
        return false;
    }

    @Override
    public void enlargeCanvas(int north, int east, int south, int west) {
        // all coordinates in this method are
        // relative to the previous state of the canvas
        Rectangle imageBounds = getImageBounds();
        Rectangle canvasBounds = comp.getCanvasBounds();

        Rectangle transformedCanvasBounds = new Rectangle(
                canvasBounds.x - west,
                canvasBounds.y - north,
                canvasBounds.width + west + east,
                canvasBounds.height + north + south);

        if (imageBounds.contains(transformedCanvasBounds)) {
            // even after the canvas enlargement, the image does not need to be enlarged
            translationX += west;
            translationY += north;
        } else {
            enlargeImage(transformedCanvasBounds);
        }
    }

    @Override
    ContentLayerMoveEdit createMovementEdit(int oldTX, int oldTY) {
        ContentLayerMoveEdit edit;
        boolean needsEnlarging = checkImageDoesNotCoverCanvas();
        if (needsEnlarging) {
            BufferedImage backupImage = getImage();
            enlargeImage(comp.getCanvasBounds());
            edit = new ContentLayerMoveEdit(this, backupImage, oldTX, oldTY);
        } else {
            edit = new ContentLayerMoveEdit(this, null, oldTX, oldTY);
        }

        return edit;
    }

    @Override
    public void resize(int canvasTargetWidth, int canvasTargetHeight, boolean progressiveBilinear) {
        boolean bigLayer = isBigLayer();

        int imgTargetWidth = canvasTargetWidth;
        int imgTargetHeight = canvasTargetHeight;

        double horizontalResizeRatio = 1.0;
        double verticalResizeRatio = 1.0;

        int newTx = 0, newTy = 0; // used only for big layers

        if (bigLayer) {
            horizontalResizeRatio = ((double) canvasTargetWidth) / canvas.getWidth();
            verticalResizeRatio = ((double) canvasTargetHeight) / canvas.getHeight();
            imgTargetWidth = (int) (image.getWidth() * horizontalResizeRatio);
            imgTargetHeight = (int) (image.getHeight() * verticalResizeRatio);

            newTx = (int) (getTX() * horizontalResizeRatio);
            newTy = (int) (getTY() * verticalResizeRatio);

            // correct rounding problems that can cause
            // "image does dot cover canvas" errors
            if (imgTargetWidth + newTx < canvasTargetWidth) {
                imgTargetWidth++;
            }
            if (imgTargetHeight + newTy < canvasTargetHeight) {
                imgTargetHeight++;
            }
        }

        BufferedImage resizedImg = ImageUtils.getFasterScaledInstance(image, imgTargetWidth, imgTargetHeight, VALUE_INTERPOLATION_BICUBIC, progressiveBilinear);
        setImage(resizedImg);

        if (bigLayer) {
            setTranslation(newTx, newTy);
        }
    }

    public boolean isBigLayer() {
        Rectangle canvasBounds = canvas.getBounds();
        Rectangle layerBounds = getImageBounds();
        return !canvasBounds.contains(layerBounds);
    }

    @Override
    public void crop(Rectangle2D cropRect) {
        int cropWidth = (int) cropRect.getWidth();
        int cropHeight = (int) cropRect.getHeight();

        BufferedImage img = getImage();

        // the selectionBounds is in image space except for the translation
        int transX = getTX();
        int transY = getTY();

        int cropX = (int) (cropRect.getX() - transX);
        int cropY = (int) (cropRect.getY() - transY);

        BufferedImage dest = ImageUtils.crop(img, cropX, cropY, cropWidth, cropHeight);
        setImage(dest);
        setTranslation(0, 0);
    }

    @Override
    public void paintLayerOnGraphics(Graphics2D g, boolean firstVisibleLayer) {
        BufferedImage visibleImage = getVisibleImage();

        if (tmpDrawingLayer == null) {
            paintLayerOnGraphicsWOTmpLayer(g, firstVisibleLayer, visibleImage);
        } else { // we are in the middle of a brush draw
            if (isNormalAndOpaque()) {
                g.drawImage(visibleImage, getTX(), getTY(), null);
                tmpDrawingLayer.paintLayer(g, 0, 0);
            } else { // layer is not in normal mode
                // first create a merged layer-brush image
                BufferedImage mergedLayerBrushImg = ImageUtils.copyImage(visibleImage); // TODO a canvas-sized image is enough and then less translating is necessary
                Graphics2D mergedLayerBrushG = mergedLayerBrushImg.createGraphics();

                tmpDrawingLayer.paintLayer(mergedLayerBrushG, -getTX(), -getTY()); // draw the brush on the layer
                mergedLayerBrushG.dispose();

                // now draw the merged layer-brush on the target Graphics with the layer composite
                g.drawImage(mergedLayerBrushImg, getTX(), getTY(), null);
            }
        }
    }

    protected void paintLayerOnGraphicsWOTmpLayer(Graphics2D g, boolean firstVisibleLayer, BufferedImage visibleImage) {
        if (Tools.isShapesDrawing() && isActive() && !isMaskEditing()) {
            paintDraggedShapesIntoActiveLayer(g, visibleImage, firstVisibleLayer);
        } else { // the simple case
            g.drawImage(visibleImage, getTX(), getTY(), null);
        }
    }

    protected void paintDraggedShapesIntoActiveLayer(Graphics2D g, BufferedImage visibleImage, boolean firstVisibleLayer) {
        if (firstVisibleLayer) {
            // Create a copy of the graphics, because we don't want to
            // mess with the clipping of the original
            Graphics2D gCopy = (Graphics2D) g.create();
            gCopy.drawImage(visibleImage, getTX(), getTY(), null);
            comp.applySelectionClipping(gCopy, null);
            Tools.SHAPES.paintOverLayer(gCopy, comp);
            gCopy.dispose();
        } else {
            // We need to draw inside the layer, but only temporarily.
            // When the mouse is released, then the shape will become part of
            // the image pixels.
            // But, until then, the image and the shape have to be mixed first
            // and then the result must be composited into the main Graphics,
            // otherwise we don't get the correct result if this layer not the
            // first visible layer and has a blending mode different from normal
            BufferedImage tmp = createCompositionSizedTmpImage();
            Graphics2D tmpG = tmp.createGraphics();
            tmpG.drawImage(visibleImage, getTX(), getTY(), null);

            comp.applySelectionClipping(tmpG, null);
            Tools.SHAPES.paintOverLayer(tmpG, comp);
            tmpG.dispose();

            g.drawImage(tmp, 0, 0, null);
            tmp.flush();
        }
    }

    /**
     * Returns the image that should be shown by this layer.
     */
    protected BufferedImage getVisibleImage() {
        BufferedImage visibleImage;

        switch (state) {
            case NORMAL:
                visibleImage = image;
                break;
            case PREVIEW:
                assert previewImage != null : "no preview image in state " + state;
                visibleImage = previewImage;
                break;
            case SHOW_ORIGINAL:
                assert previewImage != null : "no preview image in state " + state;
                visibleImage = image;
                break;
            default:
                throw new IllegalStateException("state = " + state);
        }
        return visibleImage;
    }

    public void setShowOriginal(boolean b) {
        if (b) {
            if (state == SHOW_ORIGINAL) {
                return;
            }
            setState(SHOW_ORIGINAL);
        } else {
            if (state == PREVIEW) {
                return;
            }
            setState(PREVIEW);
        }
        imageRefChanged();
        comp.imageChanged(REPAINT);
    }

    private void setState(State newState) {
        state = newState;
        if (newState == NORMAL) { // back to normal: cleanup
            previewImage = null;
            filterSourceImage = null;
        }
    }

    public void debugImages() {
        Utils.debugImage(image, "image");
        if (previewImage != null) {
            Utils.debugImage(previewImage, "previewImage");
        } else {
            Messages.showInfo("null", "previewImage is null");
        }
    }

    State getState() {
        return state;
    }

    // every image creation in this class should use this method
    // which is overridden by the LayerMask subclass
    // because normal image layers are enlarged with transparent pixels
    // and layer masks are enlarged with white pixels
    protected BufferedImage createEmptyImageForLayer(int width, int height) {
        return ImageUtils.createSysCompatibleImage(width, height);
    }

    protected void imageRefChanged() {
        // overridden in LayerMask
    }

    public void updateIconImage() {
//        Thread.dumpStack();

        getUI().updateLayerIconImage(this);
    }

    public BufferedImage applyLayerMask(AddToHistory addToHistory) {
        // the image reference will not be replaced
        BufferedImage oldImage = ImageUtils.copyImage(image);

        LayerMask oldMask = mask;
        MaskViewMode oldMode = comp.getIC().getMaskViewMode();

        mask.applyToImage(image);
        deleteMask(AddToHistory.NO);

        History.addEdit(addToHistory, () ->
                new ApplyLayerMaskEdit(comp, this, oldMask, oldImage, oldMode));

        updateIconImage();
        return oldImage;
    }

    @Override
    public BufferedImage adjustImage(BufferedImage src) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PixelitorEdit endMovement() {
        PixelitorEdit edit = super.endMovement();
        updateIconImage();
        return edit;
    }

    @VisibleForTesting
    public BufferedImage getPreviewImage() {
        return previewImage;
    }

    public String toDebugCanvasString() {
        return "{canvasWidth=" + canvas.getWidth()
                + ", canvasHeight=" + canvas.getHeight()
                + ", tx=" + translationX
                + ", ty=" + translationY
                + ", imgWidth=" + image.getWidth()
                + ", imgHeight=" + image.getHeight()
                + '}';
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
                + "{" + "state=" + state
                + ", super=" + super.toString()
                + '}';
    }
}
