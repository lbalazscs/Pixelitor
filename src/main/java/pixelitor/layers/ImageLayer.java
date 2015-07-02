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

import pixelitor.ChangeReason;
import pixelitor.Composition;
import pixelitor.ConsistencyChecks;
import pixelitor.filters.comp.Flip;
import pixelitor.history.AddToHistory;
import pixelitor.history.ApplyLayerMaskEdit;
import pixelitor.history.ContentLayerMoveEdit;
import pixelitor.history.History;
import pixelitor.history.ImageEdit;
import pixelitor.selection.Selection;
import pixelitor.tools.Tools;
import pixelitor.utils.Dialogs;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Utils;
import pixelitor.utils.debug.ImageLayerNode;

import javax.swing.*;
import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Optional;

import static java.awt.RenderingHints.KEY_INTERPOLATION;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC;
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
        checkConstructorPostConditions();
    }

    /**
     * Creates a new layer with the given image and size. Used when an image is pasted into a layer
     */
    public ImageLayer(Composition comp, BufferedImage pastedImage, String name, int width, int height) {
        super(comp, name, null);

        requireNonNull(pastedImage);

        int pastedWidth = pastedImage.getWidth();
        int pastedHeight = pastedImage.getHeight();

        boolean createNewImage = pastedWidth < width || pastedHeight < height;
        boolean addXTranslation = pastedWidth > width;
        boolean addYTranslation = pastedHeight > height;

        BufferedImage newImage = pastedImage;
        if(createNewImage) { // if the pasted image is too small, a new image is created
            int newWidth = Math.max(width, pastedWidth);
            int newHeight = Math.max(height, pastedHeight);
            newImage = createEmptyImageForLayer(newWidth, newHeight);
            Graphics2D g = newImage.createGraphics();

            int drawX = Math.max((width - pastedWidth) / 2, 0);
            int drawY = Math.max((height - pastedHeight) / 2, 0);

            g.drawImage(pastedImage, drawX, drawY, null);
            g.dispose();
        }

        // TODO called with the ignoreSelection=true, because otherwise setImage assumes that there is already
        // a bufferedImage for this layer
        // the right thing to do would be respecting the selection
        setImage(newImage);

        if(addXTranslation) {
            int newXTrans = -(pastedWidth - width) / 2;
            setTranslationX(newXTrans);
        }
        if(addYTranslation) {
            int newYTrans = -(pastedHeight - height) / 2;
            setTranslationY(newYTrans);
        }

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

    @Override
    public ImageLayer duplicate() {
        BufferedImage imageCopy = ImageUtils.copyImage(image);
        ImageLayer d = new ImageLayer(comp, imageCopy, getDuplicateLayerName(), null);
        d.setOpacity(opacity, false, AddToHistory.NO, true);
        d.setTranslationX(translationX);
        d.setTranslationY(translationY);
        d.setBlendingMode(blendingMode, false, AddToHistory.NO, true);

        if (hasMask()) {
            d.addMaskBack(mask.duplicate(d));
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

        Optional<Selection> selection = comp.getSelection();
        if(!selection.isPresent()) {
            return newImage;
        } else {
            Shape selectionShape = selection.get().getShape();
            if(selectionShape != null) {
                // the argument image pixels will replace the old ones only where selected
                Graphics2D g = src.createGraphics();
                g.translate(-getTranslationX(), -getTranslationY());
                g.setComposite(AlphaComposite.Src);
                g.setClip(selectionShape);
                Rectangle bounds = selectionShape.getBounds();
                g.drawImage(newImage, bounds.x, bounds.y, null);
                g.dispose();
                return src;
            } else {
                return newImage;
            }
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

    // sets the image object ignoring the selection
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
     * Initializes a preview session
     */
    public void startPreviewing() {
//        System.out.println("ImageLayer::startPreviewing: this class = " + this.getClass().getName());
//        if(!(this instanceof LayerMask)) {
//            Thread.dumpStack();
//        }

        if(comp.hasSelection()) {
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

        if(isImageContentChanged()) {
            ImageEdit edit = new ImageEdit(filterName, comp, this, getImageOrSubImageIfSelected(true, true), true);
            History.addEdit(edit);
        }

        image = previewImage;
        imageRefChanged();

        previewImage = null;

        boolean wasShowOriginal = (state == SHOW_ORIGINAL);
        setState(NORMAL);

        if(wasShowOriginal) {
            comp.imageChanged(FULL);
//        } else {
//            comp.imageChanged(INVALIDATE_CACHE);
        }
    }

    public void cancelPressedInDialog() {
        stopPreviewing();
    }

    public void stopPreviewing() {
//        System.out.println("ImageLayer::stopPreviewing: this class = " + this.getClass().getName());

        assert state == PREVIEW || state == SHOW_ORIGINAL;
        assert previewImage != null;

        setState(NORMAL);
        previewImage = null;
        comp.imageChanged(FULL);
    }

    public void tweenCalculatingStarted() {
        assert state == NORMAL;
        startPreviewing();
    }

    // currently the same as cancelPressedInDialog
    public void tweenCalculatingEnded() {
        assert state == PREVIEW;
        setState(NORMAL);

        comp.imageChanged(REPAINT); // TODO necessary?
    }

    /**
     * @return true if the image has to be repainted
     */
    public void changePreviewImage(BufferedImage img, String filterName, ChangeReason changeReason) {
//        System.out.println(String.format("ImageLayer::changePreviewImage: filterName = '%s'", filterName));

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

        if(img == image) {
            // this can happen if a filter with preview decides that no
            // change is necessary and returns the src

            imageContentChanged = false; // no history will be necessary

            // it still can happen that the image needs to be repainted
            // because the preview image can be different from the image
            // (the user does something, but then resets the params to a do-nothing state)
            boolean shouldRefresh = image != previewImage;
            previewImage = image;

            if (shouldRefresh) {
                comp.imageChanged(FULL);
            }
        } else {
            imageContentChanged = true; // history will be necessary

            setPreviewWithSelection(img);
            setState(PREVIEW);
            comp.imageChanged(FULL);
        }
    }

    public void filterWithoutDialogFinished(BufferedImage transformedImage, ChangeReason changeReason, String opName) {
        requireNonNull(transformedImage);

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

        if(!changeReason.needsUndo()) {
            return;
        }

        // at this point we are sure that the image changed,
        // considering that a filter without dialog was running
        if(imageForUndo == image) {
            throw new IllegalStateException("imageForUndo == image");
        }
        assert imageForUndo != null;
        ImageEdit edit = new ImageEdit(opName, comp, this, imageForUndo, true);
        History.addEdit(edit);

        // otherwise the next filter run will take the old image source,
        // not the actual one
        filterSourceImage = null;
    }

    public void changeImageUndoRedo(BufferedImage img) {
        requireNonNull(img);
        assert img != image; // simple filters always change something
        assert state == NORMAL;
        setImageWithSelection(img);
    }

    @Override
    public String toString() {
        ImageLayerNode node = new ImageLayerNode(this);
        return node.toDetailedString();
    }

    public Rectangle getBounds() {
        return new Rectangle(translationX, translationY, image.getWidth(), image.getHeight());
    }

    public boolean checkImageDoesNotCoverCanvas() {
        Rectangle canvasBounds = comp.getCanvasBounds();
        Rectangle layerBounds = getBounds();
        boolean needsEnlarging = !(layerBounds.contains(canvasBounds));
        return needsEnlarging;
    }

    public void enlargeLayer() {
        try {
            if(translationX >= 0) {
                if(translationY >= 0) {
                    enlargeNW();
                } else {
                    enlargeSW();
                }
            } else {
                if(translationY >= 0) {
                    enlargeNE();
                } else {
                    enlargeSE();
                }
            }
        } catch(OutOfMemoryError e) {
            Dialogs.showOutOfMemoryDialog(e);
        }
    }

    private void enlargeSE() {
        int newWidth = image.getWidth() - translationX;
        int newHeight = image.getHeight() - translationY;
        BufferedImage bi = createEmptyImageForLayer(newWidth, newHeight);
        Graphics2D g = bi.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();

        setImage(bi);
    }

    private void enlargeNE() {
        int newWidth = image.getWidth() - translationX;
        int newHeight = image.getHeight() + translationY;
        BufferedImage bi = createEmptyImageForLayer(newWidth, newHeight);
        Graphics2D g = bi.createGraphics();
        g.drawImage(image, 0, translationY, null);
        g.dispose();
        setImage(bi);
        translationY = 0;
    }

    private void enlargeSW() {
        int newWidth = image.getWidth() + translationX;
        int newHeight = image.getHeight() - translationY;
        BufferedImage bi = createEmptyImageForLayer(newWidth, newHeight);
        Graphics2D g = bi.createGraphics();
        g.drawImage(image, translationX, 0, null);
        g.dispose();
        setImage(bi);
        translationX = 0;
    }

    private void enlargeNW() {
        int newWidth = image.getWidth() + translationX;
        int newHeight = image.getHeight() + translationY;
        BufferedImage bi = createEmptyImageForLayer(newWidth, newHeight);
        Graphics2D g = bi.createGraphics();
        g.drawImage(image, translationX, translationY, null);
        g.dispose();
        setImage(bi);
        translationX = 0;
        translationY = 0;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        ImageUtils.serializeImage(out, image);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
//        System.out.println("ImageLayer::readObject: CALLED , class = " + getClass().getSimpleName());
        in.defaultReadObject();
        setImage(ImageUtils.deserializeImage(in));
        state = NORMAL;
        imageContentChanged = false;
//        System.out.println("ImageLayer::readObject: ENDED for " + getName() + ", class = " + getClass().getSimpleName());
    }

    /**
     * Returns the image shown in the image selector in filter dialogs
     */
    public BufferedImage getImageForFilterDialogs() {
        Optional<Selection> selection = comp.getSelection();
        if(!selection.isPresent()) {
            return image;
        }

        Rectangle selectionBounds = selection.get().getShapeBounds();
        return image.getSubimage(selectionBounds.x, selectionBounds.y, selectionBounds.width, selectionBounds.height);
    }

    @Override
    public void flip(Flip.Direction direction) {
        int translationXAbs = -getTranslationX();
        int translationYAbs = -getTranslationY();
        int newTranslationXAbs;
        int newTranslationYAbs;

        BufferedImage src = getImageOrSubImageIfSelected(false, false);

        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();
        int imageWidth = src.getWidth();
        int imageHeight = src.getHeight();

        BufferedImage dest = ImageUtils.createCompatibleDest(src);
        Graphics2D g2 = dest.createGraphics();

        if(direction == HORIZONTAL) {
            g2.translate(imageWidth, 0);
            g2.scale(-1, 1);

            newTranslationXAbs = imageWidth - canvasWidth - translationXAbs;
            newTranslationYAbs = translationYAbs;
        } else {
            g2.translate(0, imageHeight);
            g2.scale(1, -1);

            newTranslationXAbs = translationXAbs;
            newTranslationYAbs = imageHeight - canvasHeight - translationYAbs;
        }

        g2.drawImage(src, 0, 0, imageWidth, imageHeight, null);
        g2.dispose();

        if(!comp.hasSelection()) {
            setTranslationX(-newTranslationXAbs);
            setTranslationY(-newTranslationYAbs);
        }

        setImageWithSelection(dest);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    @Override
    public void rotate(int angleDegree) {
        int translationXAbs = -getTranslationX();
        int translationYAbs = -getTranslationY();
        int newTranslationXAbs = 0;
        int newTranslationYAbs = 0;

        BufferedImage img = getImageOrSubImageIfSelected(false, false);

        int imageWidth = img.getWidth();
        int imageHeight = img.getHeight();

        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();

        if(angleDegree == 90) {
            newTranslationXAbs = imageHeight - translationYAbs - canvasHeight;
            newTranslationYAbs = translationXAbs;
        } else if(angleDegree == 270) {
            newTranslationXAbs = translationYAbs;
            newTranslationYAbs = imageWidth - translationXAbs - canvasWidth;
        } else if(angleDegree == 180) {
            newTranslationXAbs = imageWidth - canvasWidth - translationXAbs;
            newTranslationYAbs = imageHeight - canvasHeight - translationYAbs;
        }

        int newImageWidth;
        int newImageHeight;

        if(angleDegree == 90 || angleDegree == 270) {
            newImageWidth = imageHeight;
            newImageHeight = imageWidth;
        } else {
            newImageWidth = imageWidth;
            newImageHeight = imageHeight;
        }

        // TODO implement arbitrary rotation:
        // create a rectangle, then rotate it with the same AffineTransform

        BufferedImage dest = ImageUtils.createCompatibleDest(img, newImageWidth, newImageHeight);

        Graphics2D g2 = dest.createGraphics();
        // TODO we should not need bicubic here as long as we have only 90, 180, 270 degrees
        g2.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BICUBIC);

        if(angleDegree == 90) {
            g2.translate(imageHeight, 0);
        } else if(angleDegree == 180) {
            g2.translate(imageWidth, imageHeight);
        } else if(angleDegree == 270) {
            g2.translate(0, imageWidth);
        }

        // TODO rotate with exact transform
        g2.rotate(Math.toRadians(angleDegree));
        g2.drawImage(img, 0, 0, imageWidth, imageHeight, null);
        g2.dispose();

        if(!comp.hasSelection()) {
            setTranslationX(-newTranslationXAbs);
            setTranslationY(-newTranslationYAbs);
        }
        setImageWithSelection(dest);
    }

    @Override
    public void mergeDownOn(ImageLayer bellowImageLayer) {
        int aX = getTranslationX();
        int aY = getTranslationY();
        BufferedImage bellowImage = bellowImageLayer.getImage();
        int bX = bellowImageLayer.getTranslationX();
        int bY = bellowImageLayer.getTranslationY();
        BufferedImage activeImage = getImage();
        Graphics2D g = bellowImage.createGraphics();
        int x = aX - bX;
        int y = aY - bY;
        Composite composite = blendingMode.getComposite(opacity);
        g.setComposite(composite);
        g.drawImage(activeImage, x, y, null);
        g.dispose();
    }

    public TmpDrawingLayer createTmpDrawingLayer(Composite c, boolean respectSelection) {
        tmpDrawingLayer = new TmpDrawingLayer(this, c, respectSelection);
        return tmpDrawingLayer;
    }

    public void mergeTmpDrawingImageDown() {
        if(tmpDrawingLayer == null) {
            return;
        }
        Graphics2D g = image.createGraphics();

        tmpDrawingLayer.paintLayer(g, -getTranslationX(), -getTranslationY());
        g.dispose();

        tmpDrawingLayer.dispose();
        tmpDrawingLayer = null;

        updateIconImage();
    }

    public BufferedImage createCompositionSizedTmpImage() {
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        // it is important that the tmp image has transparency
        // even for layer masks, otherwise drawing is not possible
        return ImageUtils.createCompatibleImage(width, height);
    }

    public BufferedImage getCompositionSizedSubImage() {
        int x = -getTranslationX();
        int y = -getTranslationY();

        if (x == 0 && y == 0) {
            return image;
        }

        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();

        assert ConsistencyChecks.translationCheck(comp);

        BufferedImage subImage;
        try {
            subImage = image.getSubimage(x, y, canvasWidth, canvasHeight);
        } catch(RasterFormatException e) {
            System.out.println("ImageLayer.getCompositionSizedSubImage x = " + x + ", y = " + y + ", canvasWidth = " + canvasWidth + ", canvasHeight = " + canvasHeight);
            WritableRaster raster = image.getRaster();
            int minX = raster.getMinX();
            int minY = raster.getMinY();
            System.out.println("ImageLayer.getCompositionSizedSubImage minX = " + minX + ", minY = " + minY);

            throw e;
        }

        return subImage;
    }

    public BufferedImage getFilterSourceImage() {
        if(filterSourceImage == null) {
            filterSourceImage = getImageOrSubImageIfSelected(false, true);
        }
        return filterSourceImage;
    }

    /**
     * If there is a selection, then the filters work on a subimage determined by the selection bounds.
     */
    public BufferedImage getImageOrSubImageIfSelected(boolean copyIfNoSelection, boolean copyAndTranslateIfSelected) {
//        new Exception("copyIfNoSelection = " + copyIfNoSelection +
//                ", copyAndTranslateIfSelected = " + copyAndTranslateIfSelected)
//                .printStackTrace();
        Optional<Selection> selection = comp.getSelection();
        if(!selection.isPresent()) {
            if(copyIfNoSelection) {
                return ImageUtils.copyImage(image);
            }
            return image;
        }

        return getSelectionSizedPartFrom(image, selection.get(), copyAndTranslateIfSelected);
    }

    public BufferedImage getSelectionSizedPartFrom(BufferedImage src, Selection selection, boolean copyAndTranslateIfSelected) {
        assert selection != null;

        Rectangle bounds = selection.getShapeBounds(); // relative to the composition

        bounds.translate(-getTranslationX(), -getTranslationY()); // relative to the image

//        Rectangle imageBounds = new Rectangle(0, 0, src.getWidth(), src.getHeight());
//        bounds = bounds.intersection(imageBounds);

        bounds = SwingUtilities.computeIntersection(
                0, 0, src.getWidth(), src.getHeight(), // image bounds
                bounds);

        if(bounds.isEmpty()) { // TODO if the selection is outside the image?
            if(copyAndTranslateIfSelected) {
                return ImageUtils.copyImage(src);
            } else {
                return src;
            }
        }

        if(copyAndTranslateIfSelected) {
            return ImageUtils.copyAndTranslateSubimage(src, bounds);
        } else {
            BufferedImage retVal = src.getSubimage(bounds.x, bounds.y, bounds.width, bounds.height);
            return retVal;
        }
    }

    public void cropToCanvasSize() {
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();

        if((imageWidth > canvasWidth) || (imageHeight > canvasHeight)) {
            BufferedImage newImage = ImageUtils.crop(image, -getTranslationX(), -getTranslationY(), canvasWidth, canvasHeight);

            BufferedImage tmp = image;
            setImage(newImage);
            tmp.flush();

            setTranslationX(0);
            setTranslationY(0);
        }
    }

    @Override
    public void enlargeCanvas(int north, int east, int south, int west) {
        int width = image.getWidth();
        int height = image.getHeight();
//        int canvasWidth = canvas.getWidth();
//        int canvasHeight = canvas.getHeight();

        // TODO the layer can already be larger than the canvas

        int newImageWidth = width + east + west;
        int newImageHeight = height + north + south;
        int newImagePaintX = west;
        int newImagePaintY = north;
        int newTranslationX = getTranslationX();
        int newTranslationY = getTranslationY();

        BufferedImage newImage = ImageUtils.createCompatibleDest(image, newImageWidth, newImageHeight);

        Graphics2D g = newImage.createGraphics();
        g.drawImage(image, newImagePaintX, newImagePaintY, null);
        g.dispose();

        image.flush();
        image = newImage;
        imageRefChanged();

        setTranslationX(newTranslationX);
        setTranslationY(newTranslationY);
    }

    @Override
    ContentLayerMoveEdit createMovementEdit(int oldTranslationX, int oldTranslationY) {
        ContentLayerMoveEdit edit;
        boolean needsEnlarging = checkImageDoesNotCoverCanvas();
        boolean moveMask = hasMask() && mask.isLinked();
        if(needsEnlarging) {
            BufferedImage oldMask = null;
            if (moveMask) {
                oldMask = mask.getImage();
            }
            edit = new ContentLayerMoveEdit(this, getImage(), oldMask, oldTranslationX, oldTranslationY);
        } else {
            edit = new ContentLayerMoveEdit(this, null, null, oldTranslationX, oldTranslationY);
        }

        if(needsEnlarging) {
            enlargeLayer();
        }

        return edit;
    }

    @Override
    public void resize(int targetWidth, int targetHeight, boolean progressiveBilinear) {
        resizeMask(targetWidth, targetHeight, progressiveBilinear);

        Rectangle canvasBounds = comp.getCanvasBounds();

        BufferedImage img = getImage();

        Rectangle layerBounds = getBounds();
        // the layer size can be bigger than the canvas size, and it can have a negative
        // translation value
        boolean bigLayer = !canvasBounds.contains(layerBounds);
        int resizeWidth = targetWidth;
        int resizeHeight = targetHeight;

        double horizontalResizeRatio = 1.0;
        double verticalResizeRatio = 1.0;
        if(bigLayer) {
            horizontalResizeRatio = ((double) targetWidth) / canvas.getWidth();
            verticalResizeRatio = ((double) targetHeight) / canvas.getHeight();
            resizeWidth = (int) (img.getWidth() * horizontalResizeRatio);
            resizeHeight = (int) (img.getHeight() * verticalResizeRatio);
        }

        BufferedImage resizedImg = ImageUtils.getFasterScaledInstance(img, resizeWidth, resizeHeight, VALUE_INTERPOLATION_BICUBIC, progressiveBilinear);
        setImage(resizedImg);

        if(bigLayer) {
            setTranslationX((int) (getTranslationX() * horizontalResizeRatio));
            setTranslationY((int) (getTranslationY() * verticalResizeRatio));
        }
    }

    @Override
    public void crop(Rectangle selectionBounds) {
        cropMask(selectionBounds);

        int cropWidth = selectionBounds.width;
        int cropHeight = selectionBounds.height;

        BufferedImage img = getImage();

        // the selectionBounds is in image space except for the translation
        int transX = getTranslationX();
        int transY = getTranslationY();

        int cropX = selectionBounds.x - transX;
        int cropY = selectionBounds.y - transY;

        BufferedImage dest = ImageUtils.crop(img, cropX, cropY, cropWidth, cropHeight);
        setImage(dest);
        setTranslationX(0);
        setTranslationY(0);
    }

    private void checkConstructorPostConditions() {
        assert canvas != null;
        assert image != null;
    }

    @Override
    public void paintLayerOnGraphics(Graphics2D g, boolean isFirstVisibleLayer) {
        setupDrawingComposite(g, isFirstVisibleLayer);

        BufferedImage visibleImage;

        switch(state) {
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

        if(tmpDrawingLayer == null) {
            if(Tools.isShapesDrawing() && isActive()) {
                // we need to draw inside the layer, but only temporarily
                BufferedImage tmp = createCompositionSizedTmpImage();
                Graphics2D tmpG = tmp.createGraphics();
                tmpG.drawImage(visibleImage, getTranslationX(), getTranslationY(), null);

                // brush and shapes cannot be active together, therefore if is enough to call this only here!
                comp.setSelectionClipping(tmpG, null);
                Tools.SHAPES.paintOverLayer(tmpG, comp);
                tmpG.dispose();

                g.drawImage(tmp, 0, 0, null);
                tmp.flush();
            } else { // the simple case
                g.drawImage(visibleImage, getTranslationX(), getTranslationY(), null);
            }
        } else { // we are in the middle of a brush draw

            if(isNormalAndOpaque()) {
                g.drawImage(visibleImage, getTranslationX(), getTranslationY(), null);
                tmpDrawingLayer.paintLayer(g, 0, 0);
            } else { // layer is not in normal mode
                // first create a merged layer-brush image
                BufferedImage mergedLayerBrushImg = ImageUtils.copyImage(visibleImage); // TODO a canvas-sized image is enough and then less translating is necessary
                Graphics2D mergedLayerBrushG = mergedLayerBrushImg.createGraphics();

                tmpDrawingLayer.paintLayer(mergedLayerBrushG, -getTranslationX(), -getTranslationY()); // draw the brush on the layer
                mergedLayerBrushG.dispose();

                // now draw the merged layer-brush on the target Graphics with the layer composite
                g.drawImage(mergedLayerBrushImg, getTranslationX(), getTranslationY(), null);
            }
        }
    }

    public void setShowOriginal(boolean b) {
        if(b) {
            if(state == SHOW_ORIGINAL) {
                return;
            }
            setState(SHOW_ORIGINAL);
        } else {
            if(state == PREVIEW) {
                return;
            }
            setState(PREVIEW);
        }
        comp.imageChanged(REPAINT);
    }

    private void setState(State newState) {
        state = newState;
        if(newState == NORMAL) { // back to normal: cleanup
            previewImage = null;
            filterSourceImage = null;
        }
    }

    public void debugImages() {
        Utils.debugImage(image, "image");
        if(previewImage != null) {
            Utils.debugImage(previewImage, "previewImage");
        } else {
            Dialogs.showInfoDialog("null", "previewImage is null");
        }
    }

    private boolean isImageContentChanged() {
        assert state == PREVIEW || state == SHOW_ORIGINAL;
        return imageContentChanged;
    }

    State getState() {
        return state;
    }

    // every image creation in thus class should use this method
    // which can be overridden by the LayerMask subclass
    protected BufferedImage createEmptyImageForLayer(int width, int height) {
        return ImageUtils.createCompatibleImage(width, height);
    }

    protected void imageRefChanged() {
        updateIconImage();
    }

    public void updateIconImage() {
//        System.out.println("ImageLayer::updateIconImage: CALLED, class = " + getClass().getSimpleName());
        getLayerButton().updateLayerIconImage(image, false);
    }

    public void applyLayerMask(AddToHistory addToHistory) {
        BufferedImage backupImage = ImageUtils.copyImage(image);
        LayerMask oldMask = mask;

        mask.applyToImage(image);
        deleteMask(AddToHistory.NO);

        if (addToHistory == AddToHistory.YES) {
            ApplyLayerMaskEdit edit = new ApplyLayerMaskEdit(comp, this, oldMask, backupImage);
            History.addEdit(edit);
        }

        updateIconImage();
    }

}
