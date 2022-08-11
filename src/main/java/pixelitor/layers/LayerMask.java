/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.history.LinkLayerMaskEdit;
import pixelitor.tools.Tools;
import pixelitor.tools.util.PPoint;
import pixelitor.tools.util.PRectangle;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.debug.DebugNode;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.Serial;

import static java.awt.AlphaComposite.DstIn;
import static java.awt.image.BufferedImage.TYPE_BYTE_GRAY;
import static pixelitor.layers.LayerGUILayout.thumbSize;
import static pixelitor.utils.ImageUtils.createThumbnail;

/**
 * A layer mask.
 */
public class LayerMask extends ImageLayer {
    @Serial
    private static final long serialVersionUID = 1L;

    private transient BufferedImage transparencyImage;
    public static final ColorModel TRANSPARENCY_COLOR_MODEL;
    public static final ColorModel RUBYLITH_COLOR_MODEL;
    private boolean linked = true; // whether it moves together with its parent layer

    // the real layer for this mask
    private Layer owner;

    static {
        byte[] lookup = new byte[256];
        for (int i = 0; i < 256; i++) {
            lookup[i] = (byte) i;
        }
        TRANSPARENCY_COLOR_MODEL = new IndexColorModel(8, 256,
            lookup,  // red
            lookup,  // green
            lookup,  // blue
            lookup); // alpha

        byte[] invertedLookup = new byte[256];
        for (int i = 0; i < 256; i++) {
            invertedLookup[i] = (byte) (255 - i);
        }
        byte[] allZeroLookup = new byte[256];

        RUBYLITH_COLOR_MODEL = new IndexColorModel(8, 256,
            invertedLookup,  // red
            allZeroLookup,   // green
            allZeroLookup,   // blue
            invertedLookup); // alpha
    }

    public static final Composite RUBYLITH_COMPOSITE = AlphaComposite.SrcOver.derive(0.5f);

    public LayerMask(Composition comp, BufferedImage bwImage,
                     Layer owner, int tx, int ty) {
        super(comp, bwImage, owner.getName() + " MASK", tx, ty);

        assert bwImage.getType() == TYPE_BYTE_GRAY;

        this.owner = owner;

        // layer masks use the button of their owner
        ui = owner.getUI();
    }

    public void applyTo(BufferedImage in) {
        Graphics2D g = in.createGraphics();
        g.setComposite(DstIn);
        g.drawImage(getTransparencyImage(), 0, 0, null);
        g.dispose();
    }

    public void updateTransparencyImage() {
        assert image.getType() == TYPE_BYTE_GRAY;
        assert image.getColorModel() != TRANSPARENCY_COLOR_MODEL;

        // The transparency image shares the raster data with the BW image,
        // but interprets the bytes differently.
        // Therefore, this method needs to be called only when
        // the visible image reference changes.
        WritableRaster raster = getVisibleImage().getRaster();
        transparencyImage = new BufferedImage(TRANSPARENCY_COLOR_MODEL,
            raster, false, null);
    }

    public void paintAsRubylith(Graphics2D g) {
        Composite oldComposite = g.getComposite();
        WritableRaster raster = getVisibleImage().getRaster();
        var rubylithImage = new BufferedImage(RUBYLITH_COLOR_MODEL,
            raster, false, null);
        g.setComposite(RUBYLITH_COMPOSITE);
        g.drawImage(rubylithImage, 0, 0, null);
        g.setComposite(oldComposite);
    }

    @Override
    protected BufferedImage createEmptyImageForLayer(int width, int height) {
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
            sf.maskChanged();
        }
    }

    /**
     * Duplicates this layer mask, and attaches the duplicated mask
     * to the given layer
     */
    public LayerMask duplicate(Layer owner) {
        BufferedImage maskImageCopy = ImageUtils.copyImage(image);
        return new LayerMask(comp, maskImageCopy, owner, getTx(), getTy());
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
    public TmpDrawingLayer createTmpDrawingLayer(Composite c, boolean softSelection) {
        throw new IllegalStateException("tmp layer with masks");
    }

    @Override
    protected void paintLayerOnGraphicsWOTmpLayer(Graphics2D g,
                                                  BufferedImage visibleImage,
                                                  boolean firstVisibleLayer) {
        if (Tools.isShapesDrawing()) {
            paintDraggedShapesIntoActiveLayer(g, visibleImage, firstVisibleLayer);
        } else { // the simple case
            g.drawImage(visibleImage, getTx(), getTy(), null);
        }
    }

    @Override
    protected void paintDraggedShapesIntoActiveLayer(Graphics2D g,
                                                     BufferedImage visibleImage,
                                                     boolean firstVisibleLayer) {
        g.drawImage(visibleImage, getTx(), getTy(), null);
        Tools.SHAPES.paintOverActiveLayer(g);
    }

    public BufferedImage getTransparencyImage() {
        if (!owner.isMaskEditing() || !Tools.isShapesDrawing()) {
            // simple case
            return transparencyImage;
        } else { // drawing with the shapes tool while in Ctrl-3 mode

            // Create a temporary image that shows how the image would look like
            // if the shapes tool would draw directly into the mask image
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

    @Override
    protected Layer getLinked() {
        if (owner.isMaskEditing()) {
            if (isLinked()) {
                return owner;
            }
        }
        return null;
    }

    public void changeOwner(Layer owner) {
        this.owner = owner;
        this.ui = owner.ui;
    }

    @Override
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
    public void update() {
        if (owner instanceof SmartFilter sf) {
            sf.maskChanged();
        }
        comp.update();
    }

    @Override
    public void repaintRegion(PPoint start, PPoint end, double thickness) {
        if (owner instanceof SmartFilter sf) {
            sf.maskChanged();
        }
        comp.repaintRegion(start, end, thickness);
    }

    @Override
    public void repaintRegion(PRectangle area) {
        if (owner instanceof SmartFilter sf) {
            sf.maskChanged();
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
