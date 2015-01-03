/*
 * Copyright (c) 2015 Laszlo Balazs-Csiki
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
import pixelitor.history.History;
import pixelitor.history.ImageEdit;
import pixelitor.history.TranslateEdit;
import pixelitor.selection.Selection;
import pixelitor.tools.Tools;
import pixelitor.utils.Dialogs;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Optional;
import pixelitor.utils.Utils;
import pixelitor.utils.debug.ImageLayerNode;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * An image layer.
 *
 * Filter without a dialog are executed as ChangeReason.OP_WITHOUT_DIALOG on the EDT.
 * The filter asks getFilterSource() in the NORMAL state, and
 * (if there is no selection) the image (not a copy!) is returned as the filter source.
 * The filter transforms the image, and calls filterWithoutDialogFinished
 * with the transformed image.
 *
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
    //    enum State {NORMAL, EDITING_PREVIEW, EDITING_SHOW_ORIGINAL}  // TODO
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
        };
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
    private transient State state = State.NORMAL;

    private transient TmpDrawingLayer tmpDrawingLayer;

    // During dialog previews the image displayed by this layer will be replaced
    // and the original image (or a subimage if there is selection) is stored here
//    private transient BufferedImage backupForPreviewBufferedImage = null;

    /**
     * The image content of this image layer
     */
    private transient BufferedImage image = null;

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
    public ImageLayer(Composition comp, BufferedImage image, String name) {
        super(comp, name == null ? comp.generateNewLayerName() : name);
        canvas = comp.getCanvas();
        if (image == null) {
            throw new IllegalArgumentException("image is null");
        }

        setImage(image);
        checkConstructorPostConditions();
    }

    /**
     * Creates a new layer with the given image and size. Used when an image is pasted into a layer
     */
    public ImageLayer(Composition comp, BufferedImage pastedImage, String name, int width, int height) {
        super(comp, name);
        canvas = comp.getCanvas();

        if (pastedImage == null) {
            throw new IllegalArgumentException("image is null");
        }

        int pastedWidth = pastedImage.getWidth();
        int pastedHeight = pastedImage.getHeight();

        boolean createNewImage = pastedWidth < width || pastedHeight < height;
        boolean addXTranslation = pastedWidth > width;
        boolean addYTranslation = pastedHeight > height;

        BufferedImage newImage = pastedImage;
        if (createNewImage) { // if the pasted image is too small, a new image is created
            int newWidth = Math.max(width, pastedWidth);
            int newHeight = Math.max(height, pastedHeight);
            newImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB_PRE);
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

        if (addXTranslation) {
            int newXTrans = -(pastedWidth - width) / 2;
            setTranslationX(newXTrans);
        }
        if (addYTranslation) {
            int newYTrans = -(pastedHeight - height) / 2;
            setTranslationY(newYTrans);
        }

        checkConstructorPostConditions();
    }

    /**
     * Creates a new empty layer
     */
    public ImageLayer(Composition comp, String name) {
        super(comp, name == null ? comp.generateNewLayerName() : name);
        canvas = comp.getCanvas();

        setImage(new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE));
        checkConstructorPostConditions();
    }

    @Override
    public ImageLayer duplicate() {
        BufferedImage imageCopy = ImageUtils.copyImage(image);
        ImageLayer d = new ImageLayer(comp, imageCopy, getDuplicateLayerName());
        d.setOpacity(opacity, false, true, true);
        d.setTranslationX(translationX);
        d.setTranslationY(translationY);
        d.setBlendingMode(blendingMode, false, true, true);

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
        if (!selection.isPresent()) {
            return newImage;
        } else {
            Shape selectionShape = selection.get().getShape();
            if (selectionShape != null) {
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
        comp.imageChanged(false, false);
    }

    // sets the image object ignoring the selection
    public void setImage(BufferedImage newImage) {
        if (newImage == null) {
            throw new IllegalArgumentException("newImage is null");
        }

        assert Utils.checkRasterMinimum(newImage);

        image = newImage;

        comp.imageChanged(false, false);
    }

    /**
     * This method is called when a new dialog appears,
     * right before creating the adjustment panel
     */
    public void startPreviewing() {
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
        setState(State.PREVIEW);
    }

    /**
     * This method is called when a new adjustment is made in the dialog, before running the filter
     */
    public void startNewPreviewFromDialog() {
//        restoreOriginalFromPreviewBackup();
    }

    public void okPressedInDialog(String filterName) {
        assert state == State.PREVIEW;
        assert previewImage != null;

        if (isImageContentChanged()) {
            ImageEdit edit = new ImageEdit(filterName, comp, this, getImageOrSubImageIfSelected(true, true), true);
            History.addEdit(edit);
        }

        image = previewImage;
        previewImage = null;

        setState(State.NORMAL);
    }

    public void cancelPressedInDialog() {
        assert state == State.PREVIEW;
        assert previewImage != null;

        setState(State.NORMAL);
        previewImage = null;
        comp.imageChanged(true, true);
    }

    public void tweenCalculatingStarted() {
        assert state == State.NORMAL;
//        setState(State.PREVIEW);
        startPreviewing();
    }

    // currently the same as cancelPressedInDialog
    public void tweenCalculatingEnded() {
        assert state == State.PREVIEW;
        setState(State.NORMAL);

        getComposition().repaint(); // TODO necessary?
    }

    /**
     * @return true if the image has to be repainted
     */
    public boolean changePreviewImage(BufferedImage img, String filterName) {
//        System.out.println(String.format("ImageLayer::changePreviewImage: filterName = '%s'", filterName));

        assert state == State.PREVIEW : "state was " + state +
                ", with the filter " + filterName +
                ", in the composition " + comp.getName();
        assert previewImage != null : "previewImage was null with " + filterName;

        if (img == null) {
            throw new IllegalArgumentException("img == null");
        }
        if (img == image) {
            // this can happen if a filter with preview decides that no
            // change is necessary and returns the src

            imageContentChanged = false; // no history will be necessary

            // it still can happen that the image needs to be repainted
            // because the preview image can be different from the image
            // (the user does something, but then resets the params to a do-nothing state)
            boolean shouldRefresh = image != previewImage;
            previewImage = image;

            return shouldRefresh;
        }
        imageContentChanged = true; // history will be necessary

        setPreviewWithSelection(img);
        setState(State.PREVIEW);
        return true;
    }

    public void filterWithoutDialogFinished(BufferedImage transformedImage, ChangeReason changeReason, String opName) {
        if (transformedImage == null) {
            throw new IllegalArgumentException("transformedImage == null");
        }
        // A filter without dialog should never return the original image
        if (transformedImage == image) { // the filter returned the original
            throw new IllegalStateException(opName + " returned the original image");
        }

        // filters without dialog run in the normal state
        assert state == State.NORMAL;

        BufferedImage imageForUndo = getFilterSourceImage();
        setImageWithSelection(transformedImage);

        if (changeReason == ChangeReason.PERFORMANCE_TEST) {
            // no undo is necessary for performance tests
            return;
        }

        // at this point we are sure that the image changed,
        // considering that a filter without dialog was running
        if (imageForUndo == image) {
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
        if (img == null) {
            throw new IllegalArgumentException("img == null");
        }
        assert img != image; // simple filters always change something
        assert state == State.NORMAL;
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

    public boolean checkForLayerEnlargement() {
        Rectangle canvasBounds = comp.getCanvasBounds();
        Rectangle layerBounds = getBounds();
        boolean needsEnlarging = !(layerBounds.contains(canvasBounds));
        return needsEnlarging;
    }

    public void enlargeLayer() {
        try {
            if (translationX >= 0) {
                if (translationY >= 0) {
                    enlargeNW();
                } else {
                    enlargeSW();
                }
            } else {
                if (translationY >= 0) {
                    enlargeNE();
                } else {
                    enlargeSE();
                }
            }
        } catch (OutOfMemoryError e) {
            Dialogs.showOutOfMemoryDialog(e);
        }
    }

    private void enlargeSE() {
        BufferedImage bi = new BufferedImage(
                image.getWidth() - translationX,
                image.getHeight() - translationY,
                image.getType());
        Graphics2D g = bi.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();

        setImage(bi);
    }

    private void enlargeNE() {
        BufferedImage bi = new BufferedImage(
                image.getWidth() - translationX,
                image.getHeight() + translationY,
                image.getType());
        Graphics2D g = bi.createGraphics();
        g.drawImage(image, 0, translationY, null);
        g.dispose();
        setImage(bi);
        translationY = 0;
    }

    private void enlargeSW() {
        BufferedImage bi = new BufferedImage(
                image.getWidth() + translationX,
                image.getHeight() - translationY,
                image.getType());
        Graphics2D g = bi.createGraphics();
        g.drawImage(image, translationX, 0, null);
        g.dispose();
        setImage(bi);
        translationX = 0;
    }

    private void enlargeNW() {
        BufferedImage bi = new BufferedImage(
                image.getWidth() + translationX,
                image.getHeight() + translationY,
                image.getType());
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
        in.defaultReadObject();
        setImage(ImageUtils.deserializeImage(in));
        state = State.NORMAL;
        imageContentChanged = false;
    }

    /**
     * Returns the image shown in the image selector in filter dialogs
     */
    public BufferedImage getImageForFilterDialogs() {
        Optional<Selection> selection = comp.getSelection();
        if (!selection.isPresent()) {
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

        BufferedImage dest = new BufferedImage(imageWidth, imageHeight, src.getType());
        Graphics2D g2 = dest.createGraphics();

        if (direction == Flip.Direction.HORIZONTAL) {
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

        if (!comp.hasSelection()) {
            setTranslationX(-newTranslationXAbs);
            setTranslationY(-newTranslationYAbs);
        }

        setImageWithSelection(dest);
    }

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

        if (angleDegree == 90) {
            newTranslationXAbs = imageHeight - translationYAbs - canvasHeight;
            newTranslationYAbs = translationXAbs;
        } else if (angleDegree == 270) {
            newTranslationXAbs = translationYAbs;
            newTranslationYAbs = imageWidth - translationXAbs - canvasWidth;
        } else if (angleDegree == 180) {
            newTranslationXAbs = imageWidth - canvasWidth - translationXAbs;
            newTranslationYAbs = imageHeight - canvasHeight - translationYAbs;
        }

        int newImageWidth;
        int newImageHeight;

        if (angleDegree == 90 || angleDegree == 270) {
            newImageWidth = imageHeight;
            newImageHeight = imageWidth;
        } else {
            newImageWidth = imageWidth;
            newImageHeight = imageHeight;
        }

        // TODO for arbitrary  rotation create a rectangle, then rotate it with the same AffineTransform
        // something like this: http://forums.sun.com/thread.jspa?threadID=5362614

        BufferedImage dest = new BufferedImage(newImageWidth, newImageHeight, img.getType());

        Graphics2D g2 = dest.createGraphics();
        // TODO we should not need bicubic here as long as we have only 90, 180, 270 degrees
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        if (angleDegree == 90) {
            g2.translate(imageHeight, 0);
        } else if (angleDegree == 180) {
            g2.translate(imageWidth, imageHeight);
        } else if (angleDegree == 270) {
            g2.translate(0, imageWidth);
        }

        g2.rotate(Math.toRadians(angleDegree));
        g2.drawImage(img, 0, 0, imageWidth, imageHeight, null);
        g2.dispose();

        if (!comp.hasSelection()) {
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
        if (tmpDrawingLayer == null) {
            return;
        }
        Graphics2D g = image.createGraphics();

        tmpDrawingLayer.paintLayer(g, -getTranslationX(), -getTranslationY());
        g.dispose();

        tmpDrawingLayer.dispose();
        tmpDrawingLayer = null;
    }

    public BufferedImage createCompositionSizedTmpImage() {
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        int type = image.getType();
        return new BufferedImage(width, height, type);
    }

    public BufferedImage createCompositionSizedSubImage() {
        int x = -getTranslationX();
        int y = -getTranslationY();
        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();

        assert ConsistencyChecks.translationCheck(comp);

        BufferedImage subImage;
        try {
            subImage = image.getSubimage(x, y, canvasWidth, canvasHeight);
        } catch (RasterFormatException e) {
            System.out.println("ImageLayer.createCompositionSizedSubImage x = " + x + ", y = " + y + ", canvasWidth = " + canvasWidth + ", canvasHeight = " + canvasHeight);
            WritableRaster raster = image.getRaster();
            int minX = raster.getMinX();
            int minY = raster.getMinY();
            System.out.println("ImageLayer.createCompositionSizedSubImage minX = " + minX + ", minY = " + minY);

            throw e;
        }

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
//        new Exception("copyIfNoSelection = " + copyIfNoSelection +
//                ", copyAndTranslateIfSelected = " + copyAndTranslateIfSelected)
//                .printStackTrace();
        Optional<Selection> selection = comp.getSelection();
        if (!selection.isPresent()) {
            if (copyIfNoSelection) {
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
        Rectangle imageBounds = new Rectangle(0, 0, src.getWidth(), src.getHeight());
        // TODO SwingUtilities.computeIntersection can do this without allocating a rectangle
        bounds = bounds.intersection(imageBounds);

        if (bounds.isEmpty()) { // TODO if the selection is outside the image?
            if (copyAndTranslateIfSelected) {
                return ImageUtils.copyImage(src);
            } else {
                return src;
            }
        }

        if (copyAndTranslateIfSelected) {
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

        if ((imageWidth > canvasWidth) || (imageHeight > canvasHeight)) {
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
        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();

        // TODO the layer can already be larger than the canvas

        int newImageWidth = width + east + west;
        int newImageHeight = height + north + south;
        int newImagePaintX = west;
        int newImagePaintY = north;
        int newTranslationX = getTranslationX();
        int newTranslationY = getTranslationY();

        BufferedImage newImage = new BufferedImage(newImageWidth, newImageHeight, image.getType());
        Graphics2D g = newImage.createGraphics();
        g.drawImage(image, newImagePaintX, newImagePaintY, null);
        g.dispose();

        image.flush();
        image = newImage;

        setTranslationX(newTranslationX);
        setTranslationY(newTranslationY);
    }

    @Override
    TranslateEdit createTranslateEdit(int oldTranslationX, int oldTranslationY) {
        TranslateEdit edit = null;
        boolean needsEnlarging = checkForLayerEnlargement();
        if (needsEnlarging) {
            edit = new TranslateEdit(this, getImage(), oldTranslationX, oldTranslationY);
        } else {
            edit = new TranslateEdit(this, null, oldTranslationX, oldTranslationY);
        }

        if (needsEnlarging) {
            enlargeLayer();
        }

        return edit;
    }

    @Override
    public void resize(int targetWidth, int targetHeight, boolean progressiveBilinear) {
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
        if (bigLayer) {
            horizontalResizeRatio = ((double) targetWidth) / canvas.getWidth();
            verticalResizeRatio = ((double) targetHeight) / canvas.getHeight();
            resizeWidth = (int) (img.getWidth() * horizontalResizeRatio);
            resizeHeight = (int) (img.getHeight() * verticalResizeRatio);
        }

        BufferedImage resizedImg = ImageUtils.getFasterScaledInstance(img, resizeWidth, resizeHeight, RenderingHints.VALUE_INTERPOLATION_BICUBIC, progressiveBilinear);
        setImage(resizedImg);

        if (bigLayer) {
            setTranslationX((int) (getTranslationX() * horizontalResizeRatio));
            setTranslationY((int) (getTranslationY() * verticalResizeRatio));
        }
    }

    @Override
    public void crop(Rectangle selectionBounds) {
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

        BufferedImage visibleImage = null;
        if (state == State.NORMAL) {
            visibleImage = image;
        } else {
            assert previewImage != null : "no preview image in state " + state;
            visibleImage = previewImage;
        }

        if (tmpDrawingLayer == null) {
            if (Tools.isShapesDrawing() && isActiveLayer()) {
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

            if (blendingMode == BlendingMode.NORMAL && opacity > 0.999f) {  // layer in normal mode, opacity  = 100%
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

    private void setState(State newState) {
        state = newState;
        if (newState == State.NORMAL) {
            previewImage = null;
            filterSourceImage = null;
        }
        comp.imageChanged(false, false);
    }

    public void debugImages() {
        Utils.debugImage(image, "image");
        if (previewImage != null) {
            Utils.debugImage(previewImage, "previewImage");
        } else {
            Dialogs.showInfoDialog("null", "previewImage is null");
        }
    }

    boolean isImageContentChanged() {
        assert state == State.PREVIEW;
        return imageContentChanged;
    }

    State getState() {
        return state;
    }
}
