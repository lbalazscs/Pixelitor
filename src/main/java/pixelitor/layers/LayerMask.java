/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.colors.Colors;
import pixelitor.history.History;
import pixelitor.history.ImageEdit;
import pixelitor.history.LinkLayerMaskEdit;
import pixelitor.history.PixelitorEdit;
import pixelitor.tools.Tools;
import pixelitor.tools.util.PPoint;
import pixelitor.tools.util.PRectangle;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.debug.DebugNode;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.Serial;

import static java.awt.AlphaComposite.DstIn;
import static java.awt.image.BufferedImage.TYPE_BYTE_GRAY;
import static pixelitor.utils.ImageUtils.isGrayscale;
import static pixelitor.utils.Thumbnails.createThumbnail;

/**
 * A grayscale mask that controls the owner layer's visibility.
 */
public class LayerMask extends ImageLayer {
    @Serial
    private static final long serialVersionUID = 1L;

    // a color model that interprets the grayscale pixel data as transparency
    public static final ColorModel TRANSPARENCY_COLOR_MODEL;

    // a color model that interprets the grayscale pixel data as a rubylith overlay
    public static final ColorModel RUBYLITH_COLOR_MODEL;

    private transient BufferedImage transparencyImage;

    // the owner (parent) layer of this mask
    private Layer owner;

    // whether the mask is linked to its owner layer (moves together).
    private boolean linked = true;

    static {
        // initialize the transparency color model
        byte[] identityLookup = new byte[256];
        for (int i = 0; i < 256; i++) {
            identityLookup[i] = (byte) i;
        }
        TRANSPARENCY_COLOR_MODEL = new IndexColorModel(8, 256,
            identityLookup,  // red
            identityLookup,  // green
            identityLookup,  // blue
            identityLookup); // alpha

        // initialize the rubylith color model
        byte[] invertedLookup = new byte[256];
        for (int i = 0; i < 256; i++) {
            invertedLookup[i] = (byte) (255 - i);
        }
        byte[] zeroLookup = new byte[256]; // all zeros for green and blue

        RUBYLITH_COLOR_MODEL = new IndexColorModel(8, 256,
            invertedLookup,  // red
            zeroLookup,   // green
            zeroLookup,   // blue
            invertedLookup); // alpha
    }

    public static final Composite RUBYLITH_COMPOSITE = AlphaComposite.SrcOver.derive(0.5f);

    public LayerMask(Composition comp, BufferedImage bwImage,
                     Layer owner, int tx, int ty) {
        super(comp, bwImage, owner.getName() + " MASK", tx, ty);

        assert isGrayscale(bwImage);

        this.owner = owner;

        // this field should never be accessed, because it is not updated
        // when the layer and its mask are moved into a layer group
        this.holder = null;
    }

    /**
     * Applies this mask to the given image, modifying its alpha channel.
     * Expects canvas-sized images.
     */
    public void applyTo(BufferedImage in) {
        applyTo(in, getTx(), getTy());
    }

    /**
     * Applies this mask to the given image, modifying its alpha channel.
     * (dx, dy) is the position of the mask image's top-left corner
     * in the pixel coordinates of the given image.
     */
    public void applyTo(BufferedImage in, int dx, int dy) {
        Graphics2D g = in.createGraphics();
        g.setComposite(DstIn);
        g.drawImage(getTransparencyImage(), dx, dy, null);
        g.dispose();
    }

    /**
     * Updates the cached transparency image to reflect changes in the mask.
     */
    private void updateTransparencyImage() {
        assert isGrayscale(image);
        assert image.getColorModel() != TRANSPARENCY_COLOR_MODEL;

        // The transparency image shares the raster data with the BW image,
        // but interprets the bytes differently.
        // Therefore, this method needs to be called only when
        // the visible image reference changes.
        WritableRaster raster = getVisibleImage().getRaster();
        transparencyImage = new BufferedImage(TRANSPARENCY_COLOR_MODEL,
            raster, false, null);
    }

    /**
     * Paints the mask as a rubylith overlay.
     */
    public void paintAsRubylith(Graphics2D g) {
        Composite origComposite = g.getComposite();
        WritableRaster raster = getVisibleImage().getRaster();
        var rubylithImage = new BufferedImage(RUBYLITH_COLOR_MODEL,
            raster, false, null);
        g.setComposite(RUBYLITH_COMPOSITE);
//        g.drawImage(rubylithImage, getTx(), getTy(), null);
        ImageUtils.drawImageVisiblePart(g, rubylithImage, getTx(), getTy());
        g.setComposite(origComposite);
    }

    @Override
    protected BufferedImage createEmptyLayerImage(int width, int height) {
        var empty = new BufferedImage(width, height, TYPE_BYTE_GRAY);

        // when enlarging a layer mask, the new areas need to be white
        Graphics2D g = empty.createGraphics();
        Colors.fillWith(Color.WHITE, g, width, height);
        g.dispose();

        return empty;
    }

    @Override
    protected void imageRefChanged() {
        updateTransparencyImage();
        if (owner != null) {
            owner.maskChanged();
        }
    }

    /**
     * Duplicates this layer mask, and attaches the duplicated mask
     * to the given layer.
     */
    public LayerMask duplicate(Layer owner, Composition newComp) {
        BufferedImage maskImageCopy = ImageUtils.copyImage(image);
        LayerMask copy = new LayerMask(newComp, maskImageCopy, owner, getTx(), getTy());
        copy.linked = linked;
        return copy;
    }

    public boolean isLinked() {
        return linked;
    }

    public void setLinked(boolean linked, boolean addToHistory) {
        if (this.linked == linked) {
            return;
        }

        this.linked = linked;
        notifyListeners();
        if (addToHistory) {
            History.add(new LinkLayerMaskEdit(comp, this, linked));
        }
    }

    @Override
    public TmpLayer createTmpLayer(Composite c, boolean softSelection) {
        // masks don't use temporary drawing layers
        throw new IllegalStateException("tmp layer with masks");
    }

    @Override
    protected void paintWithoutTmpLayer(Graphics2D g,
                                        BufferedImage visibleImage,
                                        boolean firstVisibleLayer) {
        if (Tools.isDrawingShapes()) {
            paintLayerWithShapes(g, visibleImage, firstVisibleLayer);
        } else { // the simple case
            //g.drawImage(visibleImage, getTx(), getTy(), null);
            ImageUtils.drawImageVisiblePart(g, visibleImage, getTx(), getTy());
        }
    }

    @Override
    protected void paintLayerWithShapes(Graphics2D g,
                                        BufferedImage visibleImage,
                                        boolean firstVisibleLayer) {
        //g.drawImage(visibleImage, getTx(), getTy(), null);
        ImageUtils.drawImageVisiblePart(g, visibleImage, getTx(), getTy());

        // use a copy, because we don't want to
        // mess with the clipping of the original
        Graphics2D gCopy = (Graphics2D) g.create();
        comp.applySelectionClipping(gCopy);
        Tools.SHAPES.paintOverActiveLayer(gCopy);
        gCopy.dispose();
    }

    /**
     * Returns an image representing the mask's transparency,
     * potentially including a preview of active shape tool drawing.
     */
    public BufferedImage getTransparencyImage() {
        if (!owner.isMaskEditing() || !Tools.isDrawingShapes()) {
            // simple case
            return transparencyImage;
        }

        // if drawing with the shapes tool while editing the mask:
        // create a temporary image that shows how the image would look like
        // if the shapes tool would draw directly into the mask image

        // we can use `image` instead of `getVisibleImage()` because shapes
        // tool drawing and filter preview can't happen at the same time
        var tmpImg = new BufferedImage(
            image.getWidth(), image.getHeight(), TYPE_BYTE_GRAY);
        Graphics2D tmpG = tmpImg.createGraphics();
        tmpG.drawImage(image, 0, 0, null);

        // The shapes tool paints in canvas-relative coordinates, but this
        // buffer starts at the mask image's origin. The translation must
        // be set before applying the selection clipping, because the
        // clip shape is interpreted in the current user space.
        tmpG.translate(-getTx(), -getTy());
        comp.applySelectionClipping(tmpG);
        Tools.SHAPES.paintOverActiveLayer(tmpG);
        tmpG.dispose();

        // ... and return a transparency image based on it
        return new BufferedImage(TRANSPARENCY_COLOR_MODEL,
            tmpImg.getRaster(), false, null);
    }

    /**
     * Modifies this mask to hide areas outside the given shape.
     */
    public PixelitorEdit hideOutsideShape(Shape shape, boolean createEdit) {
        BufferedImage maskImageBackup = null;
        if (createEdit) {
            maskImageBackup = ImageUtils.copyImage(image);
        }
        Graphics2D g = image.createGraphics();

        // the shape is relative to the canvas, but the mask image
        // can be translated relative to the canvas
        g.translate(-getTx(), -getTy());

        // fills the unselected part with black to hide it
        // TODO the off-canvas part of a big mask is not touched,
        //  because the inverted shape only covers the canvas
        Shape unselectedPart = comp.getCanvas().invertShape(shape);
        g.setColor(Color.BLACK);
        g.fill(unselectedPart);
        g.dispose();

        updateTransparencyImage();
        // the pixels were modified in place, so the owner's cached
        // masked image is stale
        invalidateMaskedImageCache();
        updateIconImage();

        if (createEdit) {
            return new ImageEdit("Modify Mask",
                comp, this, maskImageBackup, true);
        } else {
            return null;
        }
    }

    @Override
    protected Layer getLinked() {
        // returns the parent layer if this mask is linked
        if (owner.isMaskEditing() && isLinked()) {
            return owner;
        }
        return null;
    }

    public void changeOwner(Layer owner) {
        this.owner = owner;
    }

    public Layer getOwner() {
        return owner;
    }

    @Override
    public LayerUI getUI() {
        // layer masks use the UI of their owner
        return owner.getUI();
    }

    @Override
    public boolean hasUI() {
        // layer masks use the UI of their owner
        return owner.hasUI();
    }

    @Override
    public LayerHolder getHolder() {
        // layer masks use the holder of their owner
        return owner.getHolder();
    }

    @Override
    public Layer getTopLevelLayer() {
        return owner.getTopLevelLayer();
    }

    public LayerUI createUI() {
        // should only be called on the owner
        throw new UnsupportedOperationException();
    }

    public void updateUI() {
        // should only be called on the owner
        throw new UnsupportedOperationException();
    }

    public void setVisible(boolean newVisibility, boolean addToHistory, boolean update) {
        // layer masks have no independent visibility
        throw new UnsupportedOperationException();
    }

    public void setOpacity(float newOpacity, boolean addToHistory, boolean update) {
        // layer masks have no independent opacity
        throw new UnsupportedOperationException();
    }

    public void setBlendingMode(BlendingMode newMode, boolean addToHistory, boolean update) {
        // layer masks have no independent blending mode
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName() {
        return "mask of " + owner.getName();
    }

    @Override
    public BufferedImage createIconThumbnail() {
        // same as for the image layer, but without checkerboard painter
        BufferedImage bigImg = getCanvasSizedSubImage();
        return createThumbnail(bigImg, null);
    }

    @Override
    public void update() {
        owner.maskChanged();
        getHolder().update();
    }

    @Override
    public void repaintRegion(PPoint start, PPoint end, double thickness) {
        owner.maskChanged();
        comp.repaintRegion(start, end, thickness);
    }

    @Override
    public void repaintRegion(PRectangle area) {
        owner.maskChanged();
        comp.repaintRegion(area);
    }

    @Override
    public void invalidateMaskedImageCache() {
        // a mask can't have its own mask, so calling super is not necessary
        if (owner != null) {
            owner.invalidateMaskedImageCache();
        }
    }

    @Override
    public String getTypeString() {
        return "Layer Mask";
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = super.createDebugNode(key);

        node.addBoolean("enabled", getOwner().isMaskEnabled());
        node.addBoolean("linked", isLinked());

        return node;
    }
}
