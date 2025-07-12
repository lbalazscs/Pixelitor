/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.compactions.FlipDirection;
import pixelitor.compactions.Outsets;
import pixelitor.compactions.QuadrantAngle;
import pixelitor.gui.utils.Dialogs;
import pixelitor.history.*;
import pixelitor.io.ORAImageInfo;
import pixelitor.io.PXCFormat;
import pixelitor.tools.Tools;
import pixelitor.tools.util.PPoint;
import pixelitor.tools.util.PRectangle;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Messages;
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
import static pixelitor.FilterContext.BATCH_AUTOMATE;
import static pixelitor.FilterContext.REPEAT_LAST;
import static pixelitor.Views.thumbSize;
import static pixelitor.compactions.FlipDirection.HORIZONTAL;
import static pixelitor.layers.ImageLayer.State.NORMAL;
import static pixelitor.layers.ImageLayer.State.PREVIEW;
import static pixelitor.layers.ImageLayer.State.SHOW_ORIGINAL;
import static pixelitor.utils.ImageUtils.copyImage;
import static pixelitor.utils.ImageUtils.createThumbnail;
import static pixelitor.utils.ImageUtils.replaceSelectedRegion;
import static pixelitor.utils.Threads.onEDT;

/**
 * A layer that renders a {@link BufferedImage}.
 */
public class ImageLayer extends ContentLayer implements Drawable {
    public enum State {
        NORMAL, // no GUI filter is running on the layer
        PREVIEW, // a filter dialog is shown
        SHOW_ORIGINAL // a filter dialog is shown, and "Show Original" is checked
    }

    @Serial
    private static final long serialVersionUID = 2L;

    //
    // all variables are transient
    //
    private transient State state = NORMAL;

    private transient TmpLayer tmpLayer;

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
     * The source image for filters, which can differ
     * from the layer's image if there is a selection.
     */
    private transient BufferedImage filterSourceImage;

    /**
     * Whether the preview image is different from the normal image.
     * Relevant only in {@link PREVIEW} state.
     */
    private transient boolean imageContentChanged = false;

    private ImageLayer(Composition comp, String name) {
        super(comp, name);
    }

    public ImageLayer(Composition comp, BufferedImage image, String name) {
        this(comp, image, name, 0, 0);
    }

    /**
     * Creates a new layer with the given image and translation.
     */
    public ImageLayer(Composition comp, BufferedImage image,
                      String name, int tx, int ty) {
        super(comp, name);

        setImage(image);

        // has to be set before creating the icon image
        // because it could be nonzero when duplicating
        setTranslation(tx, ty);
    }

    /**
     * Creates a new empty (transparent) layer.
     */
    public static ImageLayer createEmpty(Composition comp, String name) {
        ImageLayer imageLayer = new ImageLayer(comp, name);

        BufferedImage emptyImage = imageLayer.createEmptyLayerImage(
            comp.getCanvasWidth(), comp.getCanvasHeight());
        imageLayer.setImage(emptyImage);

        return imageLayer;
    }

    /**
     * Creates an image layer from an external (pasted or drag-and-dropped)
     * image, which can have a different size than the canvas. The new layer
     * is sized and positioned to center the external image on the canvas.
     */
    public static ImageLayer fromExternalImage(BufferedImage externalImg,
                                               Composition comp,
                                               String layerName) {
        ImageLayer layer = new ImageLayer(comp, layerName);
        requireNonNull(externalImg);

        BufferedImage newImage = layer.calcNewImageFromExternal(externalImg);
        layer.setImage(newImage);

        // set the translation to center the new layer image on the canvas
        layer.centerExternalImage(externalImg);

        return layer;
    }

    private BufferedImage calcNewImageFromExternal(BufferedImage img) {
        Canvas canvas = comp.getCanvas();

        // if the external image is already large enough to
        // cover the canvas, then it doesn't have to be enlarged
        if (canvas.isFullyCoveredBy(img)) {
            return ImageUtils.toSysCompatibleImage(img);
        }

        // the external image is smaller than the canvas in at least one
        // dimension, so create a new image that is at least canvas-sized
        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();
        int imgWidth = img.getWidth();
        int imgHeight = img.getHeight();

        int newWidth = Math.max(canvasWidth, imgWidth);
        int newHeight = Math.max(canvasHeight, imgHeight);
        BufferedImage newImage = createEmptyLayerImage(newWidth, newHeight);

        // draw the external image centered within the new image buffer
        Graphics2D g = newImage.createGraphics();
        int drawX = Math.max((canvasWidth - imgWidth) / 2, 0);
        int drawY = Math.max((canvasHeight - imgHeight) / 2, 0);
        g.drawImage(img, drawX, drawY, null);
        g.dispose();

        return newImage;
    }

    // if the external image is bigger than the canvas, then add a
    // translation to it in order to make it centered on the canvas
    private void centerExternalImage(BufferedImage img) {
        int canvasWidth = comp.getCanvasWidth();
        int canvasHeight = comp.getCanvasHeight();
        int imgWidth = img.getWidth();
        int imgHeight = img.getHeight();

        int newTx = 0;
        if (imgWidth > canvasWidth) {
            newTx = -(imgWidth - canvasWidth) / 2;
        }

        int newTy = 0;
        if (imgHeight > canvasHeight) {
            newTy = -(imgHeight - canvasHeight) / 2;
        }

        setTranslation(newTx, newTy);
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
        tmpLayer = null;
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
            if (previewImage != null) {
                previewImage.flush();
                previewImage = null;
            }
            if (filterSourceImage != null) {
                filterSourceImage.flush();
                filterSourceImage = null;
            }
        }
    }

    @Override
    public void setShowOriginal(boolean b) {
        State targetState = b ? SHOW_ORIGINAL : PREVIEW;
        if (state == targetState) {
            return;
        }

        setState(targetState);
        imageRefChanged();
        holder.update(false);
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
        return ImageUtils.extractSelectedRegion(image,
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

    private BufferedImage getCanvasSizedVisibleImage() {
        if (!isBigLayer()) {
            return getVisibleImage();
        }

        return getVisibleImage().getSubimage(-getTx(), -getTy(),
            comp.getCanvasWidth(), comp.getCanvasHeight());
    }

    /**
     * Returns the image that should be shown by this layer,
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

    /**
     * Returns a transparent image, but it's overridden in the
     * {@link LayerMask} subclass to return a white image.
     */
    protected BufferedImage createEmptyLayerImage(int width, int height) {
        return ImageUtils.createSysCompatibleImage(width, height);
    }

    @Override
    public BufferedImage toImage(boolean applyMask, boolean applyOpacity) {
        if (!usesMask() && getOpacity() == 1.0f) {
            return getCanvasSizedVisibleImage();
        }
        return super.toImage(applyMask, applyOpacity);
    }

    @Override
    public ORAImageInfo getORAImageInfo() {
        return new ORAImageInfo(image, getTx(), getTy());
    }

    @Override
    public boolean isRasterizable() {
        return false;
    }

    private void setPreviewWithSelection(BufferedImage newPreview) {
        previewImage = replaceSelectedRegion(previewImage, newPreview, false, this);

        setState(PREVIEW);
        imageRefChanged();
        holder.update();
    }

    private void setImageWithSelection(BufferedImage newImage, boolean isUndoRedo) {
        image = replaceSelectedRegion(image, newImage, isUndoRedo, this);
        imageRefChanged();

        comp.invalidateImageCache();
    }

    @Override
    public void setImage(BufferedImage newImage) {
        BufferedImage prevRef = image;
        image = requireNonNull(newImage);

        imageRefChanged();

        assert Assertions.rasterStartsAtOrigin(newImage);

        comp.invalidateImageCache();

        if (prevRef != null && prevRef != image) {
            prevRef.flush();
        }
    }

    /**
     * Replaces the image with history and icon update
     */
    public void replaceImage(BufferedImage newImage, String editName) {
        BufferedImage prevImage = image;
        setImage(newImage);

        History.add(new ImageEdit(editName, comp, this, prevImage, true));
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
    public void changePreviewImage(BufferedImage newPreview, String filterName, FilterContext context) {
        if (state == NORMAL) {
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

        // a filter without dialog should never return the original image...
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
            Rectangle rect = ImageUtils.calcOpaqueBounds(image);
            rect.translate(getTx(), getTy());
            return rect;
        }
    }

    @Override
    public int getPixelAtPoint(Point p) {
        return ImageUtils.getPixelAt(this, image, p);
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

            BufferedImage bi = createEmptyLayerImage(target.width, target.height);
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
    public void flip(FlipDirection direction) {
        int newTx;
        int newTy;
        if (direction == HORIZONTAL) {
            newTx = comp.getCanvasWidth() - image.getWidth() - getTx();
            newTy = getTy();
        } else {
            newTx = getTx();
            newTy = comp.getCanvasHeight() - image.getHeight() - getTy();
        }

        BufferedImage dest = ImageUtils.createImageWithSameCM(image);
        Graphics2D g2 = dest.createGraphics();

        g2.setTransform(direction.createImageTransform(image));
        g2.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);
        g2.dispose();

        setTranslation(newTx, newTy);

        setImage(dest);
    }

    @Override
    public void rotate(QuadrantAngle angle) {
        int newTx;
        int newTy;
        switch (angle.getAngleDegree()) {
            case 90 -> {
                newTx = comp.getCanvasHeight() - image.getHeight() - getTy();
                newTy = getTx();
            }
            case 270 -> {
                newTx = getTy();
                newTy = comp.getCanvasWidth() - image.getWidth() - getTx();
            }
            case 180 -> {
                newTx = comp.getCanvasWidth() - image.getWidth() - getTx();
                newTy = comp.getCanvasHeight() - image.getHeight() - getTy();
            }
            default -> throw new IllegalStateException("angleDegree = " + angle.getAngleDegree());
        }

        BufferedImage dest = angle.createDestImage(image);

        Graphics2D g2 = dest.createGraphics();
        // nearest neighbor should be ok for 90, 180, 270 degrees
        g2.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.setTransform(angle.createImageTransform(image));
        g2.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);
        g2.dispose();

        setTranslation(newTx, newTy);
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
            // the simple case: it's guaranteed that the image will
            // cover the new canvas, so just set the new translation
            super.crop(cropRect, false, allowGrowing);
            return;
        }

        int cropWidth = (int) cropRect.getWidth();
        int cropHeight = (int) cropRect.getHeight();
        assert cropWidth > 0 : "cropRect = " + cropRect;
        assert cropHeight > 0 : "cropRect = " + cropRect;

        // the cropRect is in image space, but relative to the canvas,
        // so it's translated to get the correct image coordinates
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
        if (!changed) {
            Messages.showStatusMessage("The layer <b>\"%s\"</b> was already the same size (%dx%d) as the canvas."
                .formatted(getName(), comp.getCanvasWidth(), comp.getCanvasHeight()));
            return;
        }

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

        Messages.showStatusMessage("The layer <b>\"%s\"</b> was cropped to %dx%d from %dx%d."
            .formatted(getName(), comp.getCanvasWidth(), comp.getCanvasHeight(), backupImage.getWidth(), backupImage.getHeight()));
    }

    /**
     * Crops to the canvas size, without managing history.
     * Returns true if something was changed.
     */
    public boolean toCanvasSize() {
        if (isBigLayer()) {
            BufferedImage newImage = ImageUtils.crop(image,
                -getTx(), -getTy(), comp.getCanvasWidth(), comp.getCanvasHeight());

            BufferedImage tmp = image;
            setImage(newImage);
            tmp.flush();

            setTranslation(0, 0);
            return true;
        }
        return false;
    }

    @Override
    public void enlargeCanvas(Outsets out) {
        // all coordinates in this method are
        // relative to the previous state of the canvas
        Rectangle imageBounds = getContentBounds();
        Rectangle canvasBounds = comp.getCanvasBounds();

        int newX = canvasBounds.x - out.left;
        int newY = canvasBounds.y - out.top;
        int newWidth = canvasBounds.width + out.left + out.right;
        int newHeight = canvasBounds.height + out.top + out.bottom;
        var newCanvasBounds = new Rectangle(newX, newY, newWidth, newHeight);

        if (imageBounds.contains(newCanvasBounds)) {
            // even after the canvas enlargement, the image does not need to be enlarged
            setTranslation(getTx() + out.left, getTy() + out.top);
        } else {
            enlargeImage(newCanvasBounds);
        }
    }

    @Override
    public TmpLayer createTmpLayer(Composite c, boolean softSelection) {
        tmpLayer = new TmpLayer(this, c, softSelection);
        return tmpLayer;
    }

    @Override
    public void mergeTmpDrawingLayerDown() {
        if (tmpLayer == null) {
            return;
        }

        Graphics2D g = image.createGraphics();
        tmpLayer.paintOn(g, -getTx(), -getTy());
        g.dispose();

        tmpLayer.dispose();
        tmpLayer = null;
    }

    @Override
    ContentLayerMoveEdit createMovementEdit(int prevTx, int prevTy) {
        ContentLayerMoveEdit edit;
        boolean needsEnlarging = imageDoesNotCoverCanvas();
        if (needsEnlarging) {
            BufferedImage backupImage = getImage();
            enlargeImage(comp.getCanvasBounds());
            edit = new ContentLayerMoveEdit(this, backupImage, prevTx, prevTy);
        } else {
            edit = new ContentLayerMoveEdit(this, null, prevTx, prevTy);
        }

        return edit;
    }

    @Override
    public PixelitorEdit finalizeMovement() {
        PixelitorEdit edit = super.finalizeMovement();
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
    private boolean isBigLayer() {
        return image.getWidth() > comp.getCanvasWidth()
            || image.getHeight() > comp.getCanvasHeight();
    }

    @Override
    public void paint(Graphics2D g, boolean firstVisibleLayer) {
        BufferedImage visibleImage = getVisibleImage();

        if (tmpLayer == null) {
            paintWithoutTmpLayer(g, visibleImage, firstVisibleLayer);
        } else { // we are in the middle of a brush draw
            paintWithTmpLayer(g, visibleImage);
        }
    }

    protected void paintWithoutTmpLayer(Graphics2D g,
                                        BufferedImage visibleImage,
                                        boolean firstVisibleLayer) {
        if (Tools.isShapesDrawing() && isActive() && !isMaskEditing()) {
            paintLayerWithShapes(g, visibleImage, firstVisibleLayer);
        } else { // the simple case
            g.drawImage(visibleImage, getTx(), getTy(), null);
        }
    }

    private void paintWithTmpLayer(Graphics2D g, BufferedImage visibleImage) {
        if (isNormalAndOpaque()) {
            g.drawImage(visibleImage, getTx(), getTy(), null);
            tmpLayer.paintOn(g, 0, 0);
        } else {
            // the composite of the graphics is already set up, but
            // the drawing layer still has to be considered

            // first create a merged layer-brush image
            BufferedImage mergedLayerBrushImg = copyImage(visibleImage);
            // TODO a canvas-sized image would be enough?
            Graphics2D mergedLayerBrushG = mergedLayerBrushImg.createGraphics();

            // draw the drawing layer on the layer
            tmpLayer.paintOn(mergedLayerBrushG, -getTx(), -getTy());
            mergedLayerBrushG.dispose();

            // now draw the merged layer-brush on the target Graphics
            // with the layer composite
            g.drawImage(mergedLayerBrushImg, getTx(), getTy(), null);
        }
    }

    /**
     * Paints the current layer and the unrasterized shapes from the
     * shapes tool on top of it, onto the given Graphics context.
     */
    protected void paintLayerWithShapes(Graphics2D g,
                                        BufferedImage visibleImage,
                                        boolean firstVisibleLayer) {
        if (firstVisibleLayer) {
            // create a copy of the graphics, because we don't want to
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
            Messages.showInfo("No Preview", "previewImage is null");
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
    public BufferedImage transformImage(BufferedImage src) {
        throw new UnsupportedOperationException();
    }

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
