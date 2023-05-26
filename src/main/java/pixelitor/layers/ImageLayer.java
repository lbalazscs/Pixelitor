/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.Canvas;
import pixelitor.*;
import pixelitor.colors.Colors;
import pixelitor.compactions.Flip;
import pixelitor.gui.utils.Dialogs;
import pixelitor.history.*;
import pixelitor.io.PXCFormat;
import pixelitor.io.TranslatedImage;
import pixelitor.tools.Tools;
import pixelitor.tools.util.PPoint;
import pixelitor.tools.util.PRectangle;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Messages;
import pixelitor.utils.QuadrantAngle;
import pixelitor.utils.VisibleForTesting;
import pixelitor.utils.debug.Debug;
import pixelitor.utils.debug.DebugNode;
import pixelitor.utils.debug.DebugNodes;
import pixelitor.utils.test.Assertions;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.concurrent.CompletableFuture;

import static java.awt.RenderingHints.KEY_INTERPOLATION;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static pixelitor.Composition.UpdateActions.REPAINT;
import static pixelitor.FilterContext.BATCH_AUTOMATE;
import static pixelitor.FilterContext.REPEAT_LAST;
import static pixelitor.Views.thumbSize;
import static pixelitor.compactions.Flip.Direction.HORIZONTAL;
import static pixelitor.layers.ImageLayer.State.*;
import static pixelitor.utils.ImageUtils.copyImage;
import static pixelitor.utils.ImageUtils.createThumbnail;
import static pixelitor.utils.Threads.onEDT;

/**
 * An image layer.
 */
public class ImageLayer extends ContentLayer implements Drawable {
    public enum State {
        NORMAL, // no filter is running on the layer
        PREVIEW, // a filter dialog is shown
        SHOW_ORIGINAL // a filter dialog is shown + "Show Original" is checked
    }

    @Serial
    private static final long serialVersionUID = 2L;

    //
    // all variables are transient
    //
    private transient State state = NORMAL;

    private transient TmpDrawingLayer tmpDrawingLayer;

    /**
     * The regular image content of this image layer.
     * Transient because BufferedImage can't be directly serialized.
     */
    protected transient BufferedImage image = null;

    /**
     * The image shown during filter previews.
     */
    private transient BufferedImage previewImage;

    /**
     * The source image passed to the filters.
     * It's different from the layer's image if there is a selection.
     */
    private transient BufferedImage filterSourceImage;

    /**
     * Whether the preview image is different from the normal image
     * It makes sense only in PREVIEW mode
     */
    private transient boolean imageContentChanged = false;

    ImageLayer(Composition comp, String name) {
        super(comp, name);
    }

    public ImageLayer(Composition comp, BufferedImage image, String name) {
        this(comp, image, name, 0, 0);
    }

    /**
     * Creates a new layer with the given image
     */
    public ImageLayer(Composition comp, BufferedImage image,
                      String name, int tx, int ty) {
        this(comp, name);

        requireNonNull(image);

        setImage(image);

        // has to be set before creating the icon image
        // because it could be nonzero when duplicating
        setTranslation(tx, ty);

        checkConstructorPostConditions();
    }

    /**
     * Creates a new empty layer
     */
    public static ImageLayer createEmpty(Composition comp, String name) {
        ImageLayer imageLayer = new ImageLayer(comp, name);

        BufferedImage emptyImage = imageLayer.createEmptyImageForLayer(
            comp.getCanvasWidth(), comp.getCanvasHeight());
        imageLayer.setImage(emptyImage);
        imageLayer.checkConstructorPostConditions();

        return imageLayer;
    }

    /**
     * Creates an image layer from an external (pasted or drag-and-dropped)
     * image, which can have a different size than the canvas.
     */
    public static ImageLayer fromExternalImage(BufferedImage pastedImage,
                                               Composition comp,
                                               String layerName) {
        ImageLayer layer = new ImageLayer(comp, layerName);
        requireNonNull(pastedImage);

        BufferedImage newImage = layer.calcNewImageFromPasted(pastedImage);
        layer.setImage(newImage);

        layer.setTranslationForPasted(pastedImage);

//        layer.updateIconImage();
        layer.checkConstructorPostConditions();

        return layer;
    }

    private BufferedImage calcNewImageFromPasted(BufferedImage pastedImage) {
        Canvas canvas = comp.getCanvas();
        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();

        int pastedWidth = pastedImage.getWidth();
        int pastedHeight = pastedImage.getHeight();

        if (canvas.isFullyCoveredBy(pastedImage)) {
            return ImageUtils.toSysCompatibleImage(pastedImage);
        }

        // the pasted image is too small: a new image is created,
        // and the pasted image is centered within it
        int newWidth = Math.max(canvasWidth, pastedWidth);
        int newHeight = Math.max(canvasHeight, pastedHeight);
        BufferedImage newImage = createEmptyImageForLayer(newWidth, newHeight);
        Graphics2D g = newImage.createGraphics();

        // center the pasted image within the new image
        int drawX = Math.max((canvasWidth - pastedWidth) / 2, 0);
        int drawY = Math.max((canvasHeight - pastedHeight) / 2, 0);

        g.drawImage(pastedImage, drawX, drawY, null);
        g.dispose();

        return newImage;
    }

    private void setTranslationForPasted(BufferedImage pastedImage) {
        int canvasWidth = comp.getCanvasWidth();
        int canvasHeight = comp.getCanvasHeight();

        int pastedWidth = pastedImage.getWidth();
        int pastedHeight = pastedImage.getHeight();

        // if the pasted image is bigger than the canvas, then add a
        // translation to it in order to make it centered within the canvas
        boolean addXTranslation = pastedWidth > canvasWidth;
        boolean addYTranslation = pastedHeight > canvasHeight;

        int newTx = 0;
        if (addXTranslation) {
            newTx = -(pastedWidth - canvasWidth) / 2;
        }

        int newTy = 0;
        if (addYTranslation) {
            newTy = -(pastedHeight - canvasHeight) / 2;
        }

        setTranslation(newTx, newTy);
    }

    private void checkConstructorPostConditions() {
        assert image != null;
    }

    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        PXCFormat.serializeImage(out, image);
    }

    @Serial
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

    public State getState() {
        return state;
    }

    private void setState(State newState) {
        state = newState;
        if (newState == NORMAL) { // back to normal: cleanup
            previewImage = null;
            filterSourceImage = null;
        }
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
        holder.update(REPAINT);
    }

    @Override
    protected ImageLayer createTypeSpecificCopy(CopyType copyType, Composition newComp) {
        BufferedImage imageCopy = copyImage(image);
        if (imageCopy == null) {
            // there was an out of memory error
            return null;
        }

        String copyName = copyType.createLayerCopyName(name);
        return new ImageLayer(comp, imageCopy, copyName, getTx(), getTy());
    }

    @Override
    public BufferedImage getImage() {
        return image;
    }

    @Override
    public BufferedImage getFilterSourceImage() {
        if (filterSourceImage == null) {
            filterSourceImage = getSelectedSubImage(false);
        }
        return filterSourceImage;
    }

    @Override
    public BufferedImage getSelectedSubImage(boolean copyIfNoSelection) {
        var selection = comp.getSelection();
        if (selection == null) { // no selection => return full image
            if (copyIfNoSelection) {
                return copyImage(image);
            }
            return image;
        }

        // there is selection
        return ImageUtils.getSelectionSizedPartFrom(image,
            selection, getTx(), getTy());
    }

    @Override
    public BufferedImage getImageForFilterDialogs() {
        var selection = comp.getSelection();
        if (selection == null) {
            return image;
        }

        Rectangle selBounds = selection.getShapeBounds();

        assert image.getRaster().getBounds().contains(selBounds) :
            "image bounds = " + image.getRaster().getBounds()
            + ", selection bounds = " + selBounds;

        return image.getSubimage(
            selBounds.x, selBounds.y,
            selBounds.width, selBounds.height);
    }

    @Override
    public BufferedImage getCanvasSizedSubImage() {
        if (!isBigLayer()) {
            return image;
        }

        return image.getSubimage(-getTx(), -getTy(),
            comp.getCanvasWidth(), comp.getCanvasHeight());
    }

    public BufferedImage getCanvasSizedVisibleImage() {
        if (!isBigLayer()) {
            return getVisibleImage();
        }

        return getVisibleImage().getSubimage(-getTx(), -getTy(),
            comp.getCanvasWidth(), comp.getCanvasHeight());
    }

//    private BufferedImage getMaskedImage() {
//        if (mask == null || !isMaskEnabled()) {
//            return image;
//        } else {
//            BufferedImage copy = copyImage(image);
//            mask.applyToImage(copy);
//            return copy;
//        }
//    }

    /**
     * Returns the image that should be shown by this layer
     * without considering the canvas or the translation.
     */
    public BufferedImage getVisibleImage() {
        BufferedImage visibleImage = switch (state) {
            case NORMAL, SHOW_ORIGINAL -> image;
            case PREVIEW -> previewImage;
        };

        assert visibleImage != null : "state = " + state + " in " + getName();
        return visibleImage;
    }

    // every image creation in this class should use this method
    // which is overridden by the LayerMask subclass
    // because normal image layers are enlarged with transparent pixels
    // and layer masks are enlarged with white pixels
    protected BufferedImage createEmptyImageForLayer(int width, int height) {
        return ImageUtils.createSysCompatibleImage(width, height);
    }

    @Override
    public BufferedImage asImage(boolean applyMask, boolean applyOpacity) {
        if (!usesMask() && getOpacity() == 1.0f) {
            return getCanvasSizedVisibleImage();
        }
        return super.asImage(applyMask, applyOpacity);
    }

    @Override
    public TranslatedImage getTranslatedImage() {
        return new TranslatedImage(image, getTx(), getTy());
    }

    @Override
    public boolean isRasterizable() {
        return false;
    }

    private void setPreviewWithSelection(BufferedImage newPreview) {
        previewImage = replaceSelectedRegion(previewImage, newPreview, false);

        setState(PREVIEW);
        imageRefChanged();
        holder.update();
    }

    private void setImageWithSelection(BufferedImage newImage, boolean isUndoRedo) {
        image = replaceSelectedRegion(image, newImage, isUndoRedo);
        imageRefChanged();

        comp.invalidateImageCache();
    }

    /**
     * If there is no selection, returns the newImg
     * If there is a selection, copies newImg into src
     * according to the selection, and returns src
     */
    private BufferedImage replaceSelectedRegion(BufferedImage src,
                                                BufferedImage newImg,
                                                boolean isUndoRedo) {
        assert src != null;
        assert newImg != null;
        assert Assertions.checkRasterMinimum(newImg);

        var selection = comp.getSelection();
        if (selection == null) {
            return newImg;
        } else if (isUndoRedo) {
            // when undoing something and there is a selection, the whole
            // new image should be copied onto the old one, and the exact
            // selection shape should not be considered because of AA effects
            Graphics2D g = src.createGraphics();
            Rectangle selBounds = selection.getShapeBounds();
            g.drawImage(newImg, selBounds.x - getTx(), selBounds.y - getTy(), null);
            g.dispose();
            return src;
        } else if (selection.isRectangular()) {
            // rectangular selection, simple selection shape clipping
            // is enough, because there are no aliasing problems
            Graphics2D g = src.createGraphics();
            g.translate(-getTx(), -getTy());

            // transparency comes from the new image
            g.setComposite(AlphaComposite.Src);
            Shape shape = selection.getShape();
            g.setClip(shape);
            Rectangle bounds = selection.getShapeBounds();
            g.drawImage(newImg, bounds.x, bounds.y, null);
            g.dispose();
            return src;
        } else {
            Rectangle bounds = selection.getShapeBounds();
            BufferedImage tmpImg = ImageUtils.createSysCompatibleImage(bounds.width, bounds.height);
            Graphics2D g2 = ImageUtils.setupForSoftSelection(tmpImg, selection.getShape(), bounds.x, bounds.y);

            g2.drawImage(newImg, 0, 0, null);
            g2.dispose();

            Graphics2D srcG = src.createGraphics();
//            srcG.setComposite(AlphaComposite.Src);
            srcG.drawImage(tmpImg, bounds.x - getTx(), bounds.y - getTy(), null);
            srcG.dispose();

            return src;
        }
    }

    @Override
    public void setImage(BufferedImage newImage) {
        BufferedImage oldRef = image;
        image = requireNonNull(newImage);
        imageRefChanged();

        assert Assertions.checkRasterMinimum(newImage);

        comp.invalidateImageCache();

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

        History.add(new ImageEdit(editName, comp, this, oldImage, true));
        holder.update();
        updateIconImage();
    }

    @Override
    public void startPreviewing() {
        assert state == NORMAL : "state was " + state;

        if (comp.hasSelection()) {
            // if we have a selection, then the preview image reference can't be simply
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

        holder.update();
    }

    @Override
    public void onFilterDialogAccepted(String filterName) {
        assert state == PREVIEW || state == SHOW_ORIGINAL;
        assert previewImage != null;

        if (imageContentChanged) {
            History.add(new ImageEdit(filterName, comp, this,
                getSelectedSubImage(true), false));
        }

        image = previewImage;
        imageRefChanged();

        if (imageContentChanged) {
            updateIconImage();
        }

        boolean wasShowOriginal = state == SHOW_ORIGINAL;
        setState(NORMAL);

        if (wasShowOriginal) {
            holder.update();
        }
    }

    @Override
    public void onFilterDialogCanceled() {
        stopPreviewing();
    }

    @Override
    public void startTweening() {
        assert state == NORMAL;
        startPreviewing();
    }

    @Override
    public void endTweening() {
        assert state == PREVIEW;
        stopPreviewing();
    }

    @Override
    public void changePreviewImage(BufferedImage newPreview, String filterName, FilterContext context) {
        // typically we should be in PREVIEW mode
        if (state == SHOW_ORIGINAL) {
            // this is OK, something was adjusted while in show original mode
        } else if (state == NORMAL) {
            throw new IllegalStateException(format(
                "change preview in normal state, filter = %s, context = %s, class = %s)",
                filterName, context, getClass().getSimpleName()));
        }

        assert previewImage != null :
            format("previewImage was null with %s, context = %s, class = %s",
                filterName, context, getClass().getSimpleName());
        assert newPreview != null;

        if (newPreview == image) {
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
                holder.update();
            }
        } else {
            imageContentChanged = true; // history will be necessary

            setPreviewWithSelection(newPreview);
        }
    }

    @Override
    public void filterWithoutDialogFinished(BufferedImage filteredImage, FilterContext context, String filterName) {
        requireNonNull(filteredImage);

        comp.setDirty(true);

        // A filter without dialog should never return the original image...
        if (filteredImage == image) {
            // ...unless "Repeat Last" or "Batch Filter" starts a filter
            // with settings without its dialog
            if (context != REPEAT_LAST && context != BATCH_AUTOMATE) {
                throw new IllegalStateException(filterName
                                                + " returned the original image, context = " + context);
            } else {
                return;
            }
        }

        // filters without dialog run in the normal state
        assert state == NORMAL;

        BufferedImage imageForUndo = getFilterSourceImage();
        setImageWithSelection(filteredImage, false);

        if (!context.needsUndo()) {
            return;
        }

        // at this point we are sure that the image changed,
        // considering that a filter without dialog was running
        if (imageForUndo == image) {
            throw new IllegalStateException("imageForUndo == image");
        }
        assert imageForUndo != null;

        var edit = new ImageEdit(filterName, comp, this,
            imageForUndo, false);
        History.add(edit);

        // otherwise the next filter run will take the old image source,
        // not the actual one
        filterSourceImage = null;
        holder.update();
        updateIconImage();
    }

    @Override
    public void changeImageForUndoRedo(BufferedImage img, boolean ignoreSelection) {
        requireNonNull(img);
        assert img != image;
        assert state == NORMAL;

        if (ignoreSelection) {
            setImage(img);
        } else {
            setImageWithSelection(img, true);
        }
    }

    /**
     * Returns the image bounds relative to the canvas
     */
    @Override
    public Rectangle getContentBounds(boolean includeTransparent) {
        if (includeTransparent) {
            return new Rectangle(getTx(), getTy(), image.getWidth(), image.getHeight());
        } else {
            Rectangle rect = ImageUtils.getNonTransparentBounds(image);
            rect.translate(getTx(), getTy());
            return rect;
        }
    }

    @Override
    public int getPixelAtPoint(Point p) {
        int x = p.x - getTx();
        int y = p.y - getTy();
        if (x >= 0 && y >= 0 && x < image.getWidth() && y < image.getHeight()) {
            if (hasMask() && isMaskEnabled()) {
                int maskPixel = getMask().getPixelAtPoint(p);
                if (maskPixel != 0) {
                    int imagePixel = image.getRGB(x, y);
                    float maskAlpha = (maskPixel & 0xFF) / 255.0f;
                    int imageAlpha = (imagePixel >> 24) & 0xFF;
                    int effectiveAlpha = (int) (imageAlpha * maskAlpha);
                    return Colors.setAlpha(imagePixel, effectiveAlpha);
                }
            }

            return image.getRGB(x, y);
        }

        return 0x00_00_00_00;
    }

    private boolean imageDoesNotCoverCanvas() {
        Rectangle canvasBounds = comp.getCanvasBounds();
        Rectangle imageBounds = getContentBounds();
        return !imageBounds.contains(canvasBounds);
    }

    /**
     * Enlarges the image so that it covers the canvas completely.
     */
    private void enlargeImage(Rectangle canvasBounds) {
        try {
            Rectangle current = getContentBounds();
            Rectangle target = current.union(canvasBounds);

            BufferedImage bi = createEmptyImageForLayer(target.width, target.height);
            Graphics2D g = bi.createGraphics();
            int drawX = current.x - target.x;
            int drawY = current.y - target.y;
            g.drawImage(image, drawX, drawY, null);
            g.dispose();

            setTranslation(target.x - canvasBounds.x, target.y - canvasBounds.y);

            setImage(bi);
        } catch (OutOfMemoryError e) {
            Dialogs.showOutOfMemoryDialog(e);
        }
    }

    @Override
    public void flip(Flip.Direction direction) {
        var imageTransform = direction.createImageTransform(image);
        int txAbs = -getTx();
        int tyAbs = -getTy();
        int newTxAbs;
        int newTyAbs;

        int canvasWidth = comp.getCanvasWidth();
        int canvasHeight = comp.getCanvasHeight();
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();

        BufferedImage dest = ImageUtils.createImageWithSameCM(image);
        Graphics2D g2 = dest.createGraphics();

        if (direction == HORIZONTAL) {
            newTxAbs = imageWidth - canvasWidth - txAbs;
            newTyAbs = tyAbs;
        } else {
            newTxAbs = txAbs;
            newTyAbs = imageHeight - canvasHeight - tyAbs;
        }

        g2.setTransform(imageTransform);
        g2.drawImage(image, 0, 0, imageWidth, imageHeight, null);
        g2.dispose();

        setTranslation(-newTxAbs, -newTyAbs);

        setImage(dest);
    }

    @Override
    public void rotate(QuadrantAngle angle) {
        int tx = getTx();
        int ty = getTy();
        int txAbs = -tx;
        int tyAbs = -ty;
        int newTxAbs = 0;
        int newTyAbs = 0;

        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();

        int canvasWidth = comp.getCanvasWidth();
        int canvasHeight = comp.getCanvasHeight();

        int angleDegree = angle.getAngleDegree();
        switch (angleDegree) {
            case 90 -> {
                newTxAbs = imageHeight - tyAbs - canvasHeight;
                newTyAbs = txAbs;
            }
            case 270 -> {
                newTxAbs = tyAbs;
                newTyAbs = imageWidth - txAbs - canvasWidth;
            }
            case 180 -> {
                newTxAbs = imageWidth - canvasWidth - txAbs;
                newTyAbs = imageHeight - canvasHeight - tyAbs;
            }
            default -> throw new IllegalStateException("angleDegree = " + angleDegree);
        }

        BufferedImage dest = angle.createDestImage(image);

        Graphics2D g2 = dest.createGraphics();
        // nearest neighbor should be ok for 90, 180, 270 degrees
        g2.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.setTransform(angle.createImageTransform(image));
        g2.drawImage(image, 0, 0, imageWidth, imageHeight, null);
        g2.dispose();

        setTranslation(-newTxAbs, -newTyAbs);
        setImage(dest);
    }

    @Override
    public void setTranslation(int x, int y) {
        // don't allow positive translations for image layers
        if (x > 0 || y > 0) {
            throw new IllegalArgumentException("x = " + x + ", y = " + y + ", this = " + this);
        }
        super.setTranslation(x, y);
    }

    public void forceTranslation(int x, int y) {
        // skips the range check on the overridden setTranslation
        super.setTranslation(x, y);
    }

    @Override
    public void crop(Rectangle2D cropRect,
                     boolean deleteCropped,
                     boolean allowGrowing) {
        assert !cropRect.isEmpty() : "empty crop rectangle";

        if (!deleteCropped && !allowGrowing) {
            // the simple case: it is guaranteed that the image will
            // cover the new canvas, so just set the new translation
            super.crop(cropRect, false, allowGrowing);
            return;
        }

        int cropWidth = (int) cropRect.getWidth();
        int cropHeight = (int) cropRect.getHeight();
        assert cropWidth > 0 : "cropRect = " + cropRect;
        assert cropHeight > 0 : "cropRect = " + cropRect;

        // the cropRect is in image space, but relative to the canvas,
        // so it is translated to get the correct image coordinates
        int cropX = (int) (cropRect.getX() - getTx());
        int cropY = (int) (cropRect.getY() - getTy());

        if (!deleteCropped) {
            assert allowGrowing;

            boolean imageCoversNewCanvas = cropX >= 0 && cropY >= 0
                                           && cropX + cropWidth <= image.getWidth()
                                           && cropY + cropHeight <= image.getHeight();
            if (imageCoversNewCanvas) {
                // no need to change the image, just set the translation
                super.crop(cropRect, false, allowGrowing);
            } else {
                // the image still has to be enlarged, but the translation will not be zero
                int westEnlargement = Math.max(0, -cropX);
                int newWidth = westEnlargement + Math.max(
                    image.getWidth(), cropX + cropWidth);
                int northEnlargement = Math.max(0, -cropY);
                int newHeight = northEnlargement + Math.max(
                    image.getHeight(), cropY + cropHeight);

                BufferedImage newImage = ImageUtils.crop(image,
                    -westEnlargement, -northEnlargement, newWidth, newHeight);
                setImage(newImage);
                setTranslation(Math.min(-cropX, 0), Math.min(-cropY, 0));
            }
            return;
        }

        // if we get here, we know that the pixels have to be deleted,
        // that is, the new image dimensions must be cropWidth, cropHeight
        // and the translation must be 0, 0

        // this method call can also grow the image
        BufferedImage newImage = ImageUtils.crop(image, cropX, cropY, cropWidth, cropHeight);
        setImage(newImage);
        setTranslation(0, 0);
    }

    public void toCanvasSizeWithHistory() {
        BufferedImage backupImage = getImage();
        // must be created before the change
        var translationEdit = new TranslationEdit(comp, this, true);

        boolean changed = toCanvasSize();
        if (changed) {
            addToCanvasSizeToHistory(backupImage, translationEdit);
        }
    }

    /**
     * Crops to the canvas size, without managing history.
     * Returns true if something was changed.
     */
    public boolean toCanvasSize() {
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        int canvasWidth = comp.getCanvasWidth();
        int canvasHeight = comp.getCanvasHeight();

        if (imageWidth > canvasWidth || imageHeight > canvasHeight) {
            BufferedImage newImage = ImageUtils.crop(image,
                -getTx(), -getTy(), canvasWidth, canvasHeight);

            BufferedImage tmp = image;
            setImage(newImage);
            tmp.flush();

            setTranslation(0, 0);
            return true;
        }
        return false;
    }

    private void addToCanvasSizeToHistory(BufferedImage backupImage,
                                          TranslationEdit translationEdit) {
        boolean maskChanged = false;
        BufferedImage maskBackupImage = null;
        if (hasMask()) {
            maskBackupImage = mask.getImage();
            maskChanged = mask.toCanvasSize();
        }

        ImageEdit imageEdit;
        String editName = "Layer to Canvas Size";
        if (maskChanged) {
            imageEdit = new ImageAndMaskEdit(editName, comp, this, backupImage, maskBackupImage);
        } else {
            // no mask or no mask change, a simple ImageEdit will do
            imageEdit = new ImageEdit(editName, comp, this, backupImage, true);
            imageEdit.setFadeable(false);
        }
        History.add(new MultiEdit(editName, comp, translationEdit, imageEdit));
    }

    @Override
    public void enlargeCanvas(int north, int east, int south, int west) {
        // all coordinates in this method are
        // relative to the previous state of the canvas
        Rectangle imageBounds = getContentBounds();
        Rectangle canvasBounds = comp.getCanvasBounds();

        int newX = canvasBounds.x - west;
        int newY = canvasBounds.y - north;
        int newWidth = canvasBounds.width + west + east;
        int newHeight = canvasBounds.height + north + south;
        var newCanvasBounds = new Rectangle(newX, newY, newWidth, newHeight);

        if (imageBounds.contains(newCanvasBounds)) {
            // even after the canvas enlargement, the image does not need to be enlarged
            setTranslation(getTx() + west, getTy() + north);
        } else {
            enlargeImage(newCanvasBounds);
        }
    }

    @Override
    public TmpDrawingLayer createTmpDrawingLayer(Composite c, boolean softSelection) {
        tmpDrawingLayer = new TmpDrawingLayer(this, c, softSelection);
        return tmpDrawingLayer;
    }

    @Override
    public void mergeTmpDrawingLayerDown() {
        if (tmpDrawingLayer == null) {
            return;
        }

        Graphics2D g = image.createGraphics();
        tmpDrawingLayer.paintOn(g, -getTx(), -getTy());
        g.dispose();

        tmpDrawingLayer.dispose();
        tmpDrawingLayer = null;
    }

    @Override
    ContentLayerMoveEdit createMovementEdit(int oldTx, int oldTy) {
        ContentLayerMoveEdit edit;
        boolean needsEnlarging = imageDoesNotCoverCanvas();
        if (needsEnlarging) {
            BufferedImage backupImage = getImage();
            enlargeImage(comp.getCanvasBounds());
            edit = new ContentLayerMoveEdit(this, backupImage, oldTx, oldTy);
        } else {
            edit = new ContentLayerMoveEdit(this, null, oldTx, oldTy);
        }

        return edit;
    }

    @Override
    public PixelitorEdit endMovement() {
        PixelitorEdit edit = super.endMovement();
        updateIconImage();
        return edit;
    }

    @Override
    public CompletableFuture<Void> resize(Dimension newSize) {
        boolean bigLayer = isBigLayer();

        int imgTargetWidth = newSize.width;
        int imgTargetHeight = newSize.height;

        int newTx = 0, newTy = 0; // used only for big layers

        if (bigLayer) {
            double horRatio = newSize.getWidth() / comp.getCanvasWidth();
            double verRatio = newSize.getHeight() / comp.getCanvasHeight();
            imgTargetWidth = (int) (image.getWidth() * horRatio);
            imgTargetHeight = (int) (image.getHeight() * verRatio);

            newTx = (int) (getTx() * horRatio);
            newTy = (int) (getTy() * verRatio);

            // correct rounding problems that can cause
            // "image does dot cover canvas" errors
            if (imgTargetWidth + newTx < newSize.width) {
                imgTargetWidth++;
            }
            if (imgTargetHeight + newTy < newSize.height) {
                imgTargetHeight++;
            }

            assert (long) imgTargetWidth * imgTargetHeight < Integer.MAX_VALUE :
                ", tx = " + getTx() + ", ty = " + getTy()
                + ", imgTargetWidth = " + imgTargetWidth + ", imgTargetHeight = " + imgTargetHeight
                + ", newWidth = " + newSize.getWidth() + ", newHeight() = " + newSize.getHeight()
                + ", imgWidth = " + image.getWidth() + ", imgHeight = " + image.getHeight()
                + ", canvasWidth = " + comp.getCanvasWidth() + ", canvasHeight = " + comp.getCanvasHeight()
                + ", horRatio = " + horRatio + ", verRatio = " + verRatio;
        }

        int finalTx = newTx;
        int finalTy = newTy;
        return ImageUtils
            .resizeAsync(image, imgTargetWidth, imgTargetHeight)
            .thenAcceptAsync(resizedImg -> {
                setImage(resizedImg);
                if (bigLayer) {
                    setTranslation(finalTx, finalTy);
                }
            }, onEDT);
    }

    /**
     * Returns true if the layer image is bigger than the canvas
     */
    public boolean isBigLayer() {
        Rectangle canvasBounds = comp.getCanvasBounds();
        Rectangle layerBounds = getContentBounds();
        return !canvasBounds.contains(layerBounds);
    }

    @Override
    public void paintLayerOnGraphics(Graphics2D g, boolean firstVisibleLayer) {
        BufferedImage visibleImage = getVisibleImage();

        if (tmpDrawingLayer == null) {
            paintLayerOnGraphicsWOTmpLayer(g, visibleImage, firstVisibleLayer);
        } else { // we are in the middle of a brush draw
            if (isNormalAndOpaque()) {
                g.drawImage(visibleImage, getTx(), getTy(), null);
                tmpDrawingLayer.paintOn(g, 0, 0);
            } else { // layer is not in normal mode
                // the composite of the graphics is already set up, but
                // the drawing layer still has to be considered

                // first create a merged layer-brush image
                BufferedImage mergedLayerBrushImg = copyImage(visibleImage);
                // TODO a canvas-sized image would be enough?
                Graphics2D mergedLayerBrushG = mergedLayerBrushImg.createGraphics();

                // draw the drawing layer on the layer
                tmpDrawingLayer.paintOn(mergedLayerBrushG, -getTx(), -getTy());
                mergedLayerBrushG.dispose();

                // now draw the merged layer-brush on the target Graphics
                // with the layer composite
                g.drawImage(mergedLayerBrushImg, getTx(), getTy(), null);
            }
        }
    }

    protected void paintLayerOnGraphicsWOTmpLayer(Graphics2D g,
                                                  BufferedImage visibleImage,
                                                  boolean firstVisibleLayer) {
        if (Tools.isShapesDrawing() && isActive() && !isMaskEditing()) {
            paintDraggedShapesIntoActiveLayer(g, visibleImage, firstVisibleLayer);
        } else { // the simple case
            g.drawImage(visibleImage, getTx(), getTy(), null);
        }
    }

    protected void paintDraggedShapesIntoActiveLayer(Graphics2D g,
                                                     BufferedImage visibleImage,
                                                     boolean firstVisibleLayer) {
        if (firstVisibleLayer) {
            // Create a copy of the graphics, because we don't want to
            // mess with the clipping of the original
            Graphics2D gCopy = (Graphics2D) g.create();
            gCopy.drawImage(visibleImage, getTx(), getTy(), null);
            comp.applySelectionClipping(gCopy);
            Tools.SHAPES.paintOverActiveLayer(gCopy);
            gCopy.dispose();
        } else {
            // We need to draw inside the layer, but only temporarily.
            // When the mouse is released, then the shape will become part of
            // the image pixels.
            // But, until then, the image and the shape have to be mixed first
            // and then the result must be composited into the main Graphics,
            // otherwise we don't get the correct result if this layer is not the
            // first visible layer and has a blending mode different from normal
            BufferedImage tmp = comp.getCanvas().createTmpImage();
            Graphics2D tmpG = tmp.createGraphics();
            tmpG.drawImage(visibleImage, getTx(), getTy(), null);

            comp.applySelectionClipping(tmpG);
            Tools.SHAPES.paintOverActiveLayer(tmpG);
            tmpG.dispose();

            g.drawImage(tmp, 0, 0, null);
            tmp.flush();
        }
    }

    @Override
    public void debugImages() {
        Debug.debugImage(image, "image");
        if (previewImage != null) {
            Debug.debugImage(previewImage, "previewImage");
        } else {
            Messages.showInfo("null", "previewImage is null");
        }
    }

    // called when the visible image's variable
    // points to a new reference
    protected void imageRefChanged() {
        // empty here, but overridden in LayerMask
        // to update the transparency image
    }

    @Override
    public BufferedImage createIconThumbnail() {
        BufferedImage bigImg = getCanvasSizedSubImage();
        return createThumbnail(bigImg, thumbSize, thumbCheckerBoardPainter);
    }

    /**
     * Deletes the layer mask, but its effect is transferred
     * to the transparency of the layer
     */
    public BufferedImage applyLayerMask(boolean addToHistory) {
        BufferedImage previousLayerImage = copyImage(image);
        LayerMask previousMask = mask;
        MaskViewMode previousMaskViewMode = comp.getView().getMaskViewMode();

        mask.applyTo(image);
        deleteMask(false);

        if (addToHistory) {
            History.add(new ApplyLayerMaskEdit(this, previousMask,
                previousLayerImage, previousMaskViewMode));
        }

        updateIconImage();
        return previousLayerImage;
    }

    @Override
    public BufferedImage applyOnImage(BufferedImage src) {
        throw new UnsupportedOperationException();
    }

    @VisibleForTesting
    public BufferedImage getPreviewImage() {
        return previewImage;
    }

    public void convertMode(ImageMode mode) {
        image = mode.convert(image);
    }

    @Override
    public String getTypeString() {
        return "Image Layer";
    }

    @Override
    public void repaintRegion(PPoint start, PPoint end, double thickness) {
        comp.repaintRegion(start, end, thickness);
    }

    @Override
    public void repaintRegion(PRectangle area) {
        comp.repaintRegion(area);
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = super.createDebugNode(key);

        node.addAsString("state", state);
        node.add(DebugNodes.createBufferedImageNode("image", image));

        return node;
    }
}
