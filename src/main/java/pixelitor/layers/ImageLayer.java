/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.history.ApplyLayerMaskEdit;
import pixelitor.history.ContentLayerMoveEdit;
import pixelitor.history.History;
import pixelitor.history.ImageEdit;
import pixelitor.history.PixelitorEdit;
import pixelitor.io.PXCFormat;
import pixelitor.selection.Selection;
import pixelitor.tools.Tools;
import pixelitor.utils.ImageTrimUtil;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Messages;
import pixelitor.utils.Utils;
import pixelitor.utils.VisibleForTesting;
import pixelitor.utils.test.Assertions;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Point;
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
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static pixelitor.ChangeReason.REPEAT_LAST;
import static pixelitor.Composition.ImageChangeActions.INVALIDATE_CACHE;
import static pixelitor.Composition.ImageChangeActions.REPAINT;
import static pixelitor.filters.comp.Flip.Direction.HORIZONTAL;
import static pixelitor.layers.ImageLayer.State.NORMAL;
import static pixelitor.layers.ImageLayer.State.PREVIEW;
import static pixelitor.layers.ImageLayer.State.SHOW_ORIGINAL;
import static pixelitor.utils.ImageUtils.copyImage;

/**
 * An image layer.
 */
public class ImageLayer extends ContentLayer implements Drawable {
    public enum State {
        /**
         * The layer is in normal state when no filter is running on it
         */
        NORMAL {
        },
        /**
         * The layer is in previewing mode when a filter with dialog is opened.
         */
        PREVIEW {
        },
        /**
         * The layer is in previewing mode, but "Show Original"
         * is checked in the dialog
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
     * The regular image content of this image layer
     */
    protected transient BufferedImage image = null;

    /**
     * The image shown during filter previews.
     */
    private transient BufferedImage previewImage;

    /**
     * The source image passed to the filters.
     * This is different from the image if there is a selection.
     */
    private transient BufferedImage filterSourceImage;

    /**
     * The image bounding box trimmed from transparent pixels
     */
    private transient Rectangle trimmedBoundingBox;

    private ImageLayer(Composition comp, String name, Layer parent) {
        super(comp, name, parent);
    }

    /**
     * Creates a new layer with the given image
     */
    public ImageLayer(Composition comp, BufferedImage image, String name, Layer parent) {
        this(comp, name, parent);

        requireNonNull(image);

        setImage(image);
        updateIconImage();
        checkConstructorPostConditions();
    }

    /**
     * Creates a new empty layer
     */
    public static ImageLayer createEmpty(Composition comp, String name) {
        ImageLayer imageLayer = new ImageLayer(comp, name, null);

        BufferedImage emptyImage = imageLayer.createEmptyImageForLayer(
                comp.getCanvasImWidth(), comp.getCanvasImHeight());
        imageLayer.setImage(emptyImage);
        imageLayer.checkConstructorPostConditions();

        return imageLayer;
    }

    /**
     * Creates an image layer from an external (pasted or drag-and-dropped)
     * image, which can have a different size than the canvas.
     */
    public static ImageLayer createFromExternalImage(BufferedImage pastedImage,
                                                     Composition comp,
                                                     String layerName) {
        ImageLayer layer = new ImageLayer(comp, layerName, null);
        requireNonNull(pastedImage);

        int canvasWidth = comp.getCanvasImWidth();
        int canvasHeight = comp.getCanvasImHeight();

        int pastedWidth = pastedImage.getWidth();
        int pastedHeight = pastedImage.getHeight();

        BufferedImage newImage = layer.calcNewImageFromPasted(pastedImage,
                canvasWidth, canvasHeight, pastedWidth, pastedHeight);
        layer.setImage(newImage);

        layer.setTranslationForPasted(canvasWidth, canvasHeight,
                pastedWidth, pastedHeight);

        layer.updateIconImage();
        layer.checkConstructorPostConditions();

        return layer;
    }

    private BufferedImage calcNewImageFromPasted(BufferedImage pastedImage,
                                                 int canvasWidth, int canvasHeight,
                                                 int pastedWidth, int pastedHeight) {
        boolean pastedImageTooSmall = pastedWidth < canvasWidth
                || pastedHeight < canvasHeight;

        BufferedImage newImage = pastedImage;
        if (pastedImageTooSmall) {
            // a new image is created
            int newWidth = Math.max(canvasWidth, pastedWidth);
            int newHeight = Math.max(canvasHeight, pastedHeight);
            newImage = createEmptyImageForLayer(newWidth, newHeight);
            Graphics2D g = newImage.createGraphics();

            int drawX = Math.max((canvasWidth - pastedWidth) / 2, 0);
            int drawY = Math.max((canvasHeight - pastedHeight) / 2, 0);

            g.drawImage(pastedImage, drawX, drawY, null);
            g.dispose();
        }
        return newImage;
    }

    private void setTranslationForPasted(int canvasWidth, int canvasHeight,
                                         int pastedWidth, int pastedHeight) {
        boolean addXTranslation = pastedWidth > canvasWidth;
        boolean addYTranslation = pastedHeight > canvasHeight;
        int newTX = 0;
        if (addXTranslation) {
            newTX = -(pastedWidth - canvasWidth) / 2;
        }
        int newTY = 0;
        if (addYTranslation) {
            newTY = -(pastedHeight - canvasHeight) / 2;
        }
        setTranslation(newTX, newTY);
    }

    private void checkConstructorPostConditions() {
        assert canvas != null;
        assert image != null;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        PXCFormat.serializeImage(out, image);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        // init transient fields
        state = NORMAL;
        tmpDrawingLayer = null;
        previewImage = null;
        filterSourceImage = null;
        image = null;

        in.defaultReadObject();
        setImage(PXCFormat.deserializeImage(in));
        imageContentChanged = false;
    }

    @Override
    public ImageLayer duplicate(boolean sameName) {
        BufferedImage imageCopy = copyImage(image);
        String duplicateName = sameName ? name : Utils.createCopyName(name);
        ImageLayer d = new ImageLayer(comp, imageCopy, duplicateName, null);
        d.setOpacity(opacity, false, false, true);
        d.setTranslation(translationX, translationY);
        d.setBlendingMode(blendingMode, false, false, false);

        if (hasMask()) {
            d.addConfiguredMask(mask.duplicate(d));
        }

        return d;
    }

    @Override
    public BufferedImage getImage() {
        return image;
    }

    private void setPreviewWithSelection(BufferedImage newImage) {
        previewImage = replaceSelectedPart(previewImage, newImage);
    }

    private void setImageWithSelection(BufferedImage newImage) {
        image = replaceSelectedPart(image, newImage);
        imageRefChanged();

        comp.imageChanged(INVALIDATE_CACHE);
    }

    /**
     * If there is no selection, returns the newImg
     * If there is a selection, copies newImg into src
     * according to the selection, and returns src
     */
    private BufferedImage replaceSelectedPart(BufferedImage src, BufferedImage newImg) {
        assert src != null;
        assert newImg != null;
        assert Assertions.checkRasterMinimum(newImg);

        Shape selectionShape = comp.getSelectionShape();
        if (selectionShape == null) {
            return newImg;
        } else {
            // the argument image pixels will replace the old ones only where selected
            Graphics2D g = src.createGraphics();
            g.translate(-getTX(), -getTY());
            g.setComposite(AlphaComposite.Src);
            g.setClip(selectionShape);
            Rectangle bounds = selectionShape.getBounds();
            g.drawImage(newImg, bounds.x, bounds.y, null);
            g.dispose();
            return src;
        }
    }

    /**
     * Sets the image ignoring the selection
     */
    @Override
    public void setImage(BufferedImage newImage) {
        BufferedImage oldRef = image;
        image = requireNonNull(newImage);
        imageRefChanged();

        assert Assertions.checkRasterMinimum(newImage);

        comp.imageChanged(INVALIDATE_CACHE);
        imageChanged();

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
        ImageEdit edit = new ImageEdit(editName, comp, this, oldImage, true, false);
        History.addEdit(edit);

        updateIconImage();
    }

    /**
     * Initializes a preview session. Called when
     * a new dialog appears, right before creating the adjustment panel.
     */
    @Override
    public void startPreviewing() {
        assert state == NORMAL : "state was " + state;

        if (comp.hasSelection()) {
            // if we have a selection, then the preview image reference cannot be simply
            // the image reference, because when we draw into the preview image, we would
            // also draw on the real image, and after cancel we would still have the
            // changed version.
            previewImage = copyImage(image);
        } else {
            // if there is no selection, then there is no problem, because
            // the previewImage reference will be overwritten
            previewImage = image;
        }
        setState(PREVIEW);
    }

    @Override
    public void stopPreviewing() {
        assert state == PREVIEW || state == SHOW_ORIGINAL;
        assert previewImage != null;

        setState(NORMAL);

        // so that layer mask transparency image is regenerated
        // from the real image after the previews
        imageRefChanged();

        previewImage = null;
        comp.imageChanged();
    }

    @Override
    public void onDialogAccepted(String filterName) {
        assert (state == PREVIEW) || (state == SHOW_ORIGINAL);
        assert previewImage != null;

        if (imageContentChanged) {
            ImageEdit edit = new ImageEdit(filterName, comp, this,
                    getSelectedSubImage(true),
                    false, true);
            History.addEdit(edit);
        }

        image = previewImage;
        imageRefChanged();

        if (imageContentChanged) {
            updateIconImage();
            imageChanged();
        }

        previewImage = null;

        boolean wasShowOriginal = (state == SHOW_ORIGINAL);
        setState(NORMAL);

        if (wasShowOriginal) {
            comp.imageChanged();
        }
    }

    @Override
    public void onDialogCanceled() {
        stopPreviewing();
    }

    @Override
    public void tweenCalculatingStarted() {
        assert state == NORMAL;
        startPreviewing();
    }

    @Override
    public void tweenCalculatingEnded() {
        assert state == PREVIEW;
        stopPreviewing();
    }

    @Override
    public void changePreviewImage(BufferedImage img, String filterName, ChangeReason cr) {
        // typically we should be in PREVIEW mode
        if (state == SHOW_ORIGINAL) {
            // this is OK, something was adjusted while in show original mode
        } else if (state == NORMAL) {
            throw new IllegalStateException(format(
                    "change preview in normal state, filter = %s, changeReason = %s, class = %s)",
                    filterName, cr, this.getClass().getSimpleName()));
        }

        assert previewImage != null :
                format("previewImage was null with %s, changeReason = %s, class = %s",
                        filterName, cr, this.getClass().getSimpleName());
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
                comp.imageChanged();
            }
        } else {
            imageContentChanged = true; // history will be necessary

            setPreviewWithSelection(img);
            setState(PREVIEW);
            imageRefChanged();
            comp.imageChanged();
        }
    }

    @Override
    public void filterWithoutDialogFinished(BufferedImage transformedImage, ChangeReason cr, String filterName) {
        requireNonNull(transformedImage);

        comp.setDirty(true);

        // A filter without dialog should never return the original image...
        if (transformedImage == image) {
            // ...unless "Repeat Last" starts a filter with settings
            // without a dialog
            if (cr != REPEAT_LAST) {
                throw new IllegalStateException(filterName
                        + " returned the original image, changeReason = " + cr);
            } else {
                return;
            }
        }

        // filters without dialog run in the normal state
        assert state == NORMAL;

        BufferedImage imageForUndo = getFilterSourceImage();
        setImageWithSelection(transformedImage);

        if (!cr.needsUndo()) {
            return;
        }

        // at this point we are sure that the image changed,
        // considering that a filter without dialog was running
        if (imageForUndo == image) {
            throw new IllegalStateException("imageForUndo == image");
        }
        assert imageForUndo != null;
        ImageEdit edit = new ImageEdit(filterName, comp, this,
                imageForUndo, false, true);
        History.addEdit(edit);

        // otherwise the next filter run will take the old image source,
        // not the actual one
        filterSourceImage = null;
        updateIconImage();
        comp.imageChanged();
        imageChanged();
    }

    @Override
    public void changeImageForUndoRedo(BufferedImage img, boolean ignoreSelection) {
        requireNonNull(img);
        assert img != image; // simple filters always change something
        assert state == NORMAL;

        if (ignoreSelection) {
            setImage(img);
        } else {
            setImageWithSelection(img);
        }
    }

    /**
     * Returns the image bounds relative to the canvas
     */
    public Rectangle getImageBounds() {
        return new Rectangle(
                translationX, translationY,
                image.getWidth(), image.getHeight());
    }

    public void imageChanged() {
        invalidateCache();
    }

    public void invalidateCache() {
        trimmedBoundingBox = null;
    }

    @Override
    public Rectangle getEffectiveBoundingBox() {
        // cache trimmed rect until better solution is found
        if (trimmedBoundingBox == null) {
            trimmedBoundingBox = ImageTrimUtil.getTrimRect(getImage());
        }

        return new Rectangle(
                translationX + trimmedBoundingBox.x,
                translationY + trimmedBoundingBox.y,
                trimmedBoundingBox.width,
                trimmedBoundingBox.height
        );
    }

    @Override
    public Rectangle getSnappingBoundingBox() {
        return getEffectiveBoundingBox();
    }

    @Override
    public int getMouseHitPixelAtPoint(Point p) {
        BufferedImage image = getImage();
        int x = p.x - translationX;
        int y = p.y - translationY;
        if (x >= 0 && y >= 0 && x < image.getWidth() && y < image.getHeight()) {
            if (hasMask() && getMask().isMaskEnabled()) {
                int maskPixel = getMask().getMouseHitPixelAtPoint(p);
                if (maskPixel != 0) {
                    int imagePixel = image.getRGB(x, y);
                    float maskAlpha = (maskPixel & 0xff) / 255.0f;
                    int imageAlpha = (imagePixel >> 24) & 0xff;
                    int layerAlpha = (int) (imageAlpha * maskAlpha);
                    return imagePixel & 0x00ffffff | (layerAlpha << 24);
                }
            }

            return image.getRGB(x, y);
        }

        return 0x00000000;
    }

    public boolean checkImageDoesNotCoverCanvas() {
        Rectangle canvasBounds = comp.getCanvasImBounds();
        Rectangle imageBounds = getImageBounds();
        boolean needsEnlarging = !(imageBounds.contains(canvasBounds));
        return needsEnlarging;
    }

    /**
     * Enlarges the image so that it covers the canvas completely.
     */
    public void enlargeImage(Rectangle canvasBounds) {
        try {
            Rectangle current = getImageBounds();
            Rectangle target = current.union(canvasBounds);

            BufferedImage bi = createEmptyImageForLayer(target.width, target.height);
            Graphics2D g = bi.createGraphics();
            int drawX = current.x - target.x;
            int drawY = current.y - target.y;
            g.drawImage(image, drawX, drawY, null);
            g.dispose();

            translationX = target.x - canvasBounds.x;
            translationY = target.y - canvasBounds.y;

            setImage(bi);
        } catch (OutOfMemoryError e) {
            Dialogs.showOutOfMemoryDialog(e);
        }
    }

    /**
     * Returns the image shown in the image selector in filter dialogs.
     * The canvas size is not considered, only the selection.
     */
    @Override
    public BufferedImage getImageForFilterDialogs() {
        Selection selection = comp.getSelection();
        if (selection == null) {
            return image;
        }

        Rectangle selBounds = selection.getShapeBounds();
        return image.getSubimage(
                selBounds.x, selBounds.y,
                selBounds.width, selBounds.height);
    }

    @Override
    public void flip(Flip.Direction direction) {
        AffineTransform imageTx = direction.getImageTX(this);
        int tXAbs = -getTX();
        int tYAbs = -getTY();
        int newTXAbs;
        int newTYAbs;

        int canvasWidth = canvas.getImWidth();
        int canvasHeight = canvas.getImHeight();
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();

        BufferedImage dest = ImageUtils.createImageWithSameCM(image);
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

        int canvasWidth = canvas.getImWidth();
        int canvasHeight = canvas.getImHeight();

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

        g2.setTransform(angle.createImageTX(this));

        g2.drawImage(image, 0, 0, imageWidth, imageHeight, null);
        g2.dispose();

        setTranslation(-newTXAbs, -newTYAbs);

        setImage(dest);
    }

    private BufferedImage getMaskedImage() {
        if (mask == null || !isMaskEnabled()) {
            return image;
        } else {
            BufferedImage copy = copyImage(image);
            mask.applyToImage(copy);
            return copy;
        }
    }

    @Override
    public TmpDrawingLayer createTmpDrawingLayer(Composite c) {
        tmpDrawingLayer = new TmpDrawingLayer(this, c);
        return tmpDrawingLayer;
    }

    @Override
    public void mergeTmpDrawingLayerDown() {
        if (tmpDrawingLayer == null) {
            return;
        }
        Graphics2D g = image.createGraphics();

        tmpDrawingLayer.paintOn(g, -getTX(), -getTY());
        g.dispose();

        tmpDrawingLayer.dispose();
        tmpDrawingLayer = null;
    }

    public BufferedImage createCanvasSizedTmpImage() {
        int width = canvas.getImWidth();
        int height = canvas.getImHeight();
        // it is important that the tmp image has transparency
        // even for layer masks, otherwise drawing is not possible
        return ImageUtils.createSysCompatibleImage(width, height);
    }

    @Override
    public BufferedImage getCanvasSizedSubImage() {
        if (!isBigLayer()) {
            return image;
        }

        int x = -getTX();
        int y = -getTY();

        assert x >= 0 : "x = " + x;
        assert y >= 0 : "y = " + y;

        int canvasWidth = canvas.getImWidth();
        int canvasHeight = canvas.getImHeight();

        assert ConsistencyChecks.imageCoversCanvas(this);

        BufferedImage subImage;
        try {
            subImage = image.getSubimage(x, y, canvasWidth, canvasHeight);
        } catch (RasterFormatException e) {
            System.out.printf("ImageLayer.getCanvasSizedSubImage x = %d, y = %d, " +
                            "canvasWidth = %d, canvasHeight = %d, " +
                            "imageWidth = %d, imageHeight = %d%n",
                    x, y, canvasWidth, canvasHeight,
                    image.getWidth(), image.getHeight());
            WritableRaster raster = image.getRaster();

            System.out.printf("ImageLayer.getCanvasSizedSubImage " +
                            "minX = %d, minY = %d, width = %d, height=%d %n",
                    raster.getMinX(), raster.getMinY(),
                    raster.getWidth(), raster.getHeight());

            throw e;
        }

        assert subImage.getWidth() == canvasWidth;
        assert subImage.getHeight() == canvasHeight;
        return subImage;
    }

    @Override
    public BufferedImage getFilterSourceImage() {
        if (filterSourceImage == null) {
            filterSourceImage = getSelectedSubImage(false);
        }
        return filterSourceImage;
    }

    /**
     * Returns the subimage determined by the selection bounds,
     * or the image if there is no selection.
     */
    @Override
    public BufferedImage getSelectedSubImage(boolean copyIfNoSelection) {
        Selection selection = comp.getSelection();
        if (selection == null) { // no selection => return full image
            if (copyIfNoSelection) {
                return copyImage(image);
            }
            return image;
        }

        // there is selection
        return ImageUtils.getSelectionSizedPartFrom(image,
                selection,
                getTX(), getTY());
    }

    @Override
    public void setTranslation(int x, int y) {
        // don't allow positive translations for for image layers
        if (x > 0 || y > 0) {
            throw new IllegalArgumentException("x = " + x + ", y = " + y);
        }
        super.setTranslation(x, y);
    }

    @Override
    public void crop(Rectangle2D cropRect,
                     boolean deleteCroppedPixels,
                     boolean allowGrowing) {
        if (!deleteCroppedPixels && !allowGrowing) {
            // the simple case: it is guaranteed that the image will
            // cover the new canvas, so just set the new translation
            super.crop(cropRect, false, allowGrowing);
            return;
        }

        int cropWidth = (int) cropRect.getWidth();
        int cropHeight = (int) cropRect.getHeight();

        // the cropRect is in image space, but relative to the canvas,
        // so it is translated to get the correct image coordinates
        int cropX = (int) (cropRect.getX() - getTX());
        int cropY = (int) (cropRect.getY() - getTY());

        if (!deleteCroppedPixels) {
            assert allowGrowing;

            boolean imageCoversNewCanvas =
                    cropX >= 0
                            && cropY >= 0
                            && cropX + cropWidth <= image.getWidth()
                            && cropY + cropHeight <= image.getHeight();
            if (imageCoversNewCanvas) {
                // no need to change the image, just set the translation
                super.crop(cropRect, false, allowGrowing);
            } else {
                // the image still has to be enlarged, but the translation will not be zero
                int westEnlargement = Math.max(0, -cropX);
                int newWidth = westEnlargement + Math.max(image.getWidth(), cropX + cropWidth);
                int northEnlargement = Math.max(0, -cropY);
                int newHeight = northEnlargement + Math.max(image.getHeight(), cropY + cropHeight);

                BufferedImage newImage = ImageUtils
                        .crop(image, -westEnlargement, -northEnlargement, newWidth, newHeight);
                setImage(newImage);
                setTranslation(
                        Math.min(-cropX, 0),
                        Math.min(-cropY, 0)
                );
            }
            return;
        }

        // if we get here, we know that the pixels have to be deleted,
        // that is, the new image dimensions must be cropWidth, cropHeight
        // and the translation must be 0, 0
        assert deleteCroppedPixels;

        // this method call can also grow the image
        BufferedImage newImage = ImageUtils.crop(image, cropX, cropY, cropWidth, cropHeight);
        setImage(newImage);
        setTranslation(0, 0);
    }

    /**
     * Returns true if something was changed
     */
    public boolean cropToCanvasSize() {
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        int canvasWidth = canvas.getImWidth();
        int canvasHeight = canvas.getImHeight();

        if ((imageWidth > canvasWidth) || (imageHeight > canvasHeight)) {
            BufferedImage newImage = ImageUtils.crop(image,
                    -getTX(), -getTY(), canvasWidth, canvasHeight);

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
        Rectangle canvasBounds = comp.getCanvasImBounds();

        int newX = canvasBounds.x - west;
        int newY = canvasBounds.y - north;
        int newWidth = canvasBounds.width + west + east;
        int newHeight = canvasBounds.height + north + south;
        Rectangle newCanvasBounds = new Rectangle(newX, newY, newWidth, newHeight);

        if (imageBounds.contains(newCanvasBounds)) {
            // even after the canvas enlargement, the image does not need to be enlarged
            translationX += west;
            translationY += north;
        } else {
            enlargeImage(newCanvasBounds);
        }
    }

    @Override
    ContentLayerMoveEdit createMovementEdit(int oldTX, int oldTY) {
        ContentLayerMoveEdit edit;
        boolean needsEnlarging = checkImageDoesNotCoverCanvas();
        if (needsEnlarging) {
            BufferedImage backupImage = getImage();
            enlargeImage(comp.getCanvasImBounds());
            edit = new ContentLayerMoveEdit(this, backupImage, oldTX, oldTY);
        } else {
            edit = new ContentLayerMoveEdit(this, null, oldTX, oldTY);
        }

        return edit;
    }

    @Override
    public void resize(int canvasTargetWidth, int canvasTargetHeight) {
        boolean bigLayer = isBigLayer();

        int imgTargetWidth = canvasTargetWidth;
        int imgTargetHeight = canvasTargetHeight;

        int newTx = 0, newTy = 0; // used only for big layers

        if (bigLayer) {
            double horRatio = ((double) canvasTargetWidth) / canvas.getImWidth();
            double verRatio = ((double) canvasTargetHeight) / canvas.getImHeight();
            imgTargetWidth = (int) (image.getWidth() * horRatio);
            imgTargetHeight = (int) (image.getHeight() * verRatio);

            newTx = (int) (getTX() * horRatio);
            newTy = (int) (getTY() * verRatio);

            // correct rounding problems that can cause
            // "image does dot cover canvas" errors
            if (imgTargetWidth + newTx < canvasTargetWidth) {
                imgTargetWidth++;
            }
            if (imgTargetHeight + newTy < canvasTargetHeight) {
                imgTargetHeight++;
            }
        }

        BufferedImage resizedImg = ImageUtils.getFasterScaledInstance(
                image, imgTargetWidth, imgTargetHeight,
                VALUE_INTERPOLATION_BICUBIC);
        setImage(resizedImg);

        if (bigLayer) {
            setTranslation(newTx, newTy);
        }
    }

    /**
     * Returns true if the layer image is bigger than the canvas
     */
    public boolean isBigLayer() {
        Rectangle canvasBounds = canvas.getImBounds();
        Rectangle layerBounds = getImageBounds();
        return !canvasBounds.contains(layerBounds);
    }

    @Override
    public void paintLayerOnGraphics(Graphics2D g, boolean firstVisibleLayer) {
        BufferedImage visibleImage = getVisibleImage();

        if (tmpDrawingLayer == null) {
            paintLayerOnGraphicsWOTmpLayer(g, visibleImage, firstVisibleLayer);
        } else { // we are in the middle of a brush draw
            if (isNormalAndOpaque()) {
                g.drawImage(visibleImage, getTX(), getTY(), null);
                tmpDrawingLayer.paintOn(g, 0, 0);
            } else { // layer is not in normal mode
                // first create a merged layer-brush image
                BufferedImage mergedLayerBrushImg = copyImage(visibleImage);
                // TODO a canvas-sized image would be enough?
                Graphics2D mergedLayerBrushG = mergedLayerBrushImg.createGraphics();

                // draw the brush on the layer
                tmpDrawingLayer.paintOn(mergedLayerBrushG, -getTX(), -getTY());
                mergedLayerBrushG.dispose();

                // now draw the merged layer-brush on the target Graphics
                // with the layer composite
                g.drawImage(mergedLayerBrushImg, getTX(), getTY(), null);
            }
        }
    }

    protected void paintLayerOnGraphicsWOTmpLayer(Graphics2D g,
                                                  BufferedImage visibleImage,
                                                  boolean firstVisibleLayer) {
        if (Tools.isShapesDrawing() && isActive() && !isMaskEditing()) {
            paintDraggedShapesIntoActiveLayer(g, visibleImage, firstVisibleLayer);
        } else { // the simple case
            g.drawImage(visibleImage, getTX(), getTY(), null);
        }
    }

    protected void paintDraggedShapesIntoActiveLayer(Graphics2D g,
                                                     BufferedImage visibleImage,
                                                     boolean firstVisibleLayer) {
        if (firstVisibleLayer) {
            // Create a copy of the graphics, because we don't want to
            // mess with the clipping of the original
            Graphics2D gCopy = (Graphics2D) g.create();
            gCopy.drawImage(visibleImage, getTX(), getTY(), null);
            comp.applySelectionClipping(gCopy);
            Tools.SHAPES.paintOverLayer(gCopy, comp);
            gCopy.dispose();
        } else {
            // We need to draw inside the layer, but only temporarily.
            // When the mouse is released, then the shape will become part of
            // the image pixels.
            // But, until then, the image and the shape have to be mixed first
            // and then the result must be composited into the main Graphics,
            // otherwise we don't get the correct result if this layer is not the
            // first visible layer and has a blending mode different from normal
            BufferedImage tmp = createCanvasSizedTmpImage();
            Graphics2D tmpG = tmp.createGraphics();
            tmpG.drawImage(visibleImage, getTX(), getTY(), null);

            comp.applySelectionClipping(tmpG);
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

    @Override
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

    @Override
    public void debugImages() {
        Utils.debugImage(image, "image");
        if (previewImage != null) {
            Utils.debugImage(previewImage, "previewImage");
        } else {
            Messages.showInfo("null", "previewImage is null");
        }
    }

    public State getState() {
        return state;
    }

    // every image creation in this class should use this method
    // which is overridden by the LayerMask subclass
    // because normal image layers are enlarged with transparent pixels
    // and layer masks are enlarged with white pixels
    protected BufferedImage createEmptyImageForLayer(int width, int height) {
        return ImageUtils.createSysCompatibleImage(width, height);
    }

    // called when the image variable points to a new reference
    protected void imageRefChanged() {
        // empty here, but overridden in LayerMask
        // to update the transparency image
    }

    @Override
    public void updateIconImage() {
        getUI().updateLayerIconImage(this);
    }

    /**
     * Deletes the layer mask, but its effect is transferred
     * to the transparency of the layer
     */
    public BufferedImage applyLayerMask(boolean addToHistory) {
        // the image reference will not be replaced
        BufferedImage oldImage = copyImage(image);

        LayerMask oldMask = mask;
        MaskViewMode oldMode = comp.getIC().getMaskViewMode();

        mask.applyToImage(image);
        deleteMask(false);

        if (addToHistory) {
            History.addEdit(new ApplyLayerMaskEdit(
                    comp, this, oldMask, oldImage, oldMode));
        }

        updateIconImage();
        return oldImage;
    }

    @Override
    public BufferedImage actOnImageFromLayerBellow(BufferedImage src) {
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
        return "{canvasWidth=" + canvas.getImWidth()
                + ", canvasHeight=" + canvas.getImHeight()
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
