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
import static pixelitor.Views.thumbSize;
import static pixelitor.utils.ImageUtils.createThumbnail;
import static pixelitor.utils.ImageUtils.isGrayscale;

/**
 * A layer mask that applies a transparency mask to an associated layer, controlling its visibility.
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

        // layer masks use the UI of their owner
        ui = owner.getUI();
    }

    /**
     * Applies this mask to the given image, modifying its alpha channel.
     */
    public void applyTo(BufferedImage in) {
        Graphics2D g = in.createGraphics();
        g.setComposite(DstIn);
        g.drawImage(getTransparencyImage(), 0, 0, null);
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
        g.drawImage(rubylithImage, 0, 0, null);
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
        if (owner instanceof SmartFilter sf) {
            sf.layerLevelPropertyChanged(false);
        }
    }

    /**
     * Duplicates this layer mask, and attaches the duplicated mask
     * to the given layer
     */
    public LayerMask duplicate(Layer owner, Composition newComp) {
        BufferedImage maskImageCopy = ImageUtils.copyImage(image);
        return new LayerMask(newComp, maskImageCopy, owner, getTx(), getTy());
    }

    public boolean isLinked() {
        return linked;
    }

    public void setLinked(boolean linked, boolean addToHistory) {
        this.linked = linked;
        notifyListeners();
        if (addToHistory) {
            History.add(new LinkLayerMaskEdit(comp, this));
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
        if (Tools.isShapesDrawing()) {
            paintLayerWithShapes(g, visibleImage, firstVisibleLayer);
        } else { // the simple case
            g.drawImage(visibleImage, getTx(), getTy(), null);
        }
    }

    @Override
    protected void paintLayerWithShapes(Graphics2D g,
                                        BufferedImage visibleImage,
                                        boolean firstVisibleLayer) {
        g.drawImage(visibleImage, getTx(), getTy(), null);
        Tools.SHAPES.paintOverActiveLayer(g);
    }

    /**
     * Returns an image representing the mask's transparency,
     * potentially including a preview of active shape tool drawing.
     */
    public BufferedImage getTransparencyImage() {
        if (!owner.isMaskEditing() || !Tools.isShapesDrawing()) {
            // simple case
            return transparencyImage;
        } else {
            // drawing with the shapes tool while editing the mask:
            // create a temporary image that shows how the image would look like
            // if the shapes tool would draw directly into the mask image

            // we can use `image` instead of `getVisibleImage()` because shapes
            // tool drawing and filter preview can't happen at the same time
            var tmpImg = new BufferedImage(
                image.getWidth(), image.getHeight(), TYPE_BYTE_GRAY);
            Graphics2D tmpG = tmpImg.createGraphics();
            tmpG.drawImage(image, 0, 0, null);
            Tools.SHAPES.paintOverActiveLayer(tmpG);
            tmpG.dispose();

            // ... and return a transparency image based on it
            return new BufferedImage(TRANSPARENCY_COLOR_MODEL,
                tmpImg.getRaster(), false, null);
        }
    }

    /**
     * Modifies this mask to hide areas outside the given shape.
     */
    public PixelitorEdit modifyToHide(Shape shape, boolean createEdit) {
        BufferedImage maskImageBackup = null;
        if (createEdit) {
            maskImageBackup = ImageUtils.copyImage(image);
        }
        Graphics2D g = image.createGraphics();

        // fill the unselected part with black to hide it
        Shape unselectedPart = comp.getCanvas().invertShape(shape);
        g.setColor(Color.BLACK);
        g.fill(unselectedPart);
        g.dispose();

        updateTransparencyImage();
        updateIconImage();

        if (createEdit) {
            return new ImageEdit("Modify Mask",
                comp, this, maskImageBackup, true);
        } else {
            return null;
        }
    }

    @Override
    public Layer getLayer() {
        return owner;
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
        this.ui = owner.ui;
    }

    public Layer getOwner() {
        return owner;
    }

    @Override
    public String getName() {
        return "mask of " + owner.getName();
    }

    @Override
    public BufferedImage createIconThumbnail() {
        // same as for the image layer, but without checkerboard painter
        BufferedImage bigImg = getCanvasSizedSubImage();
        return createThumbnail(bigImg, thumbSize, null);
    }

    @Override
    public void update(boolean updateHistogram) {
        if (owner instanceof SmartFilter sf) {
            sf.layerLevelPropertyChanged(false);
        }
        holder.update(updateHistogram);
    }

    @Override
    public void repaintRegion(PPoint start, PPoint end, double thickness) {
        if (owner instanceof SmartFilter sf) {
            sf.layerLevelPropertyChanged(false);
        }
        comp.repaintRegion(start, end, thickness);
    }

    @Override
    public void repaintRegion(PRectangle area) {
        if (owner instanceof SmartFilter sf) {
            sf.layerLevelPropertyChanged(false);
        }
        comp.repaintRegion(area);
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
