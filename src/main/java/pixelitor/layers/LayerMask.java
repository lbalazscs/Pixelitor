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

import pixelitor.Composition;
import pixelitor.history.AddToHistory;
import pixelitor.history.History;
import pixelitor.history.LinkLayerMaskEdit;
import pixelitor.tools.Tools;
import pixelitor.utils.ImageUtils;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;

import static java.awt.AlphaComposite.DstIn;

/**
 * A layer mask.
 */
public class LayerMask extends ImageLayer {
    private static final long serialVersionUID = 1L;

    private transient BufferedImage transparencyImage;
    public static final ColorModel TRANSPARENCY_COLOR_MODEL;
    public static final ColorModel RUBYLITH_COLOR_MODEL;
    private boolean linked = true; // whether it moves together with its parent layer

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
        byte[] allZeroLookup = new byte[256];

        for (int i = 0; i < 256; i++) {
            invertedLookup[i] = (byte) (255 - i);
        }

        RUBYLITH_COLOR_MODEL = new IndexColorModel(8, 256,
                invertedLookup,  // red
                allZeroLookup,   // green
                allZeroLookup,   // blue
                invertedLookup); // alpha
    }

    public static final Composite RUBYLITH_COMPOSITE = AlphaComposite.SrcOver.derive(0.5f);

    public LayerMask(Composition comp, BufferedImage bwImage, Layer layer, boolean inheritTranslation) {
        super(comp, bwImage, layer.getName() + " MASK", layer);

        if (inheritTranslation && layer instanceof ContentLayer) {
            ContentLayer contentLayer = (ContentLayer) layer;
            translationX = contentLayer.getTX();
            translationY = contentLayer.getTY();
        }

        assert bwImage.getType() == BufferedImage.TYPE_BYTE_GRAY;
    }

    public void applyToImage(BufferedImage in) {
        Graphics2D g = in.createGraphics();
        g.setComposite(DstIn);
        g.drawImage(getTransparencyImage(), 0, 0, null);
        g.dispose();
    }

    public void updateFromBWImage() {
        assert image.getType() == BufferedImage.TYPE_BYTE_GRAY;
        assert image.getColorModel() != TRANSPARENCY_COLOR_MODEL;

        // The transparency image shares the raster data with the BW image,
        // but interprets the bytes differently.
        // Therefore this method needs to be called only when
        // the visible image reference changes.
        WritableRaster raster = getVisibleImage().getRaster();
        this.transparencyImage = new BufferedImage(TRANSPARENCY_COLOR_MODEL, raster, false, null);
    }

    public void paintAsRubylith(Graphics2D g) {
        Composite oldComposite = g.getComposite();
        WritableRaster raster = getVisibleImage().getRaster();
        BufferedImage rubylithImage = new BufferedImage(RUBYLITH_COLOR_MODEL, raster, false, null);
        g.setComposite(RUBYLITH_COMPOSITE);
        g.drawImage(rubylithImage, 0, 0, null);
        g.setComposite(oldComposite);
    }

    @Override
    protected BufferedImage createEmptyImageForLayer(int width, int height) {
//        BufferedImage empty = new BufferedImage(GRAY_MODEL, GRAY_MODEL.createCompatibleWritableRaster(width, height), false, null);
        BufferedImage empty = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        // when enlarging a layer mask, the new areas need to be white
        Graphics2D g = empty.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.dispose();

        return empty;
    }

    @Override
    protected void imageRefChanged() {
        updateFromBWImage();
    }

    @Override
    public void updateIconImage() {
        LayerGUI button = getUI();
        if(button != null) { // can be null while deserializing
            button.updateLayerIconImage(this);
        }
    }

    /**
     * Duplicates this layer mask, and attaches the duplicated mask
     * to the given layer
     */
    public LayerMask duplicate(Layer master) {
        BufferedImage maskImageCopy = ImageUtils.copyImage(image);

        LayerMask d = new LayerMask(comp, maskImageCopy, master, false);
        d.setTranslation(getTX(), getTY());

        return d;
    }

    public boolean isLinked() {
        return linked;
    }

    public void setLinked(boolean linked, AddToHistory addToHistory) {
        this.linked = linked;
        notifyLayerChangeObservers();
        History.addEdit(addToHistory, () -> new LinkLayerMaskEdit(comp, this));
    }

    @Override
    public TmpDrawingLayer createTmpDrawingLayer(Composite c) {
        throw new IllegalStateException("tmp layer with masks");
    }

//    @Override
//    public void mergeTmpDrawingLayerDown() {
//        updateIconImage();
//    }

    @Override
    protected void paintLayerOnGraphicsWOTmpLayer(Graphics2D g, boolean firstVisibleLayer, BufferedImage visibleImage) {
        if (Tools.isShapesDrawing()) {
            paintDraggedShapesIntoActiveLayer(g, visibleImage, firstVisibleLayer);
        } else { // the simple case
            g.drawImage(visibleImage, getTX(), getTY(), null);
        }
    }

    @Override
    protected void paintDraggedShapesIntoActiveLayer(Graphics2D g, BufferedImage visibleImage, boolean firstVisibleLayer) {
        g.drawImage(visibleImage, getTX(), getTY(), null);
        Tools.SHAPES.paintOverLayer(g, comp);
    }

    public BufferedImage getTransparencyImage() {
        if(!parent.isMaskEditing() || !Tools.isShapesDrawing()) {
            // simple case
            return transparencyImage;
        } else { // drawing with the shapes tool while in Ctrl-3 mode

            // Create a temporary image that shows how the image would look like
            // if the shapes tool would draw directly into the mask image
            BufferedImage tmp = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
            Graphics2D tmpG = tmp.createGraphics();
            tmpG.drawImage(image, 0, 0, null);
            Tools.SHAPES.paintOverLayer(tmpG, comp);
            tmpG.dispose();

            // ... and return a transparency image based on it
            WritableRaster raster = tmp.getRaster();
            BufferedImage tmpTransparency = new BufferedImage(TRANSPARENCY_COLOR_MODEL, raster, false, null);
            return tmpTransparency;
        }
    }
}
