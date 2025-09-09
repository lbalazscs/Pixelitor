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

import pixelitor.AppMode;
import pixelitor.Composition;
import pixelitor.CopyType;
import pixelitor.Views;
import pixelitor.compactions.Crop;
import pixelitor.compactions.FlipDirection;
import pixelitor.compactions.Outsets;
import pixelitor.compactions.QuadrantAngle;
import pixelitor.gui.View;
import pixelitor.history.PixelitorEdit;
import pixelitor.io.ORAImageInfo;
import pixelitor.tools.Tool;
import pixelitor.tools.Tools;
import pixelitor.tools.shapes.StyledShape;
import pixelitor.tools.transform.TransformBox;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Messages;
import pixelitor.utils.debug.DebugNode;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.concurrent.CompletableFuture;

import static pixelitor.Views.thumbSize;

/**
 * A layer that renders a vector-based shape (or in the future: multiple shapes).
 */
public class ShapesLayer extends ContentLayer {
    @Serial
    private static final long serialVersionUID = 1L;

    private static int count;

    private StyledShape styledShape;

    // TODO The box is also stored here because recreating
    //   it from the styled shape is currently not possible.
    private TransformBox transformBox;

    private transient BufferedImage cachedImage;

    public ShapesLayer(Composition comp, String name) {
        super(comp, name);
    }

    /**
     * Creates a new shapes layer and adds it to the composition.
     */
    public static void createNew(Composition comp) {
        var layer = new ShapesLayer(comp, "shape layer " + (++count));
        comp.getHolderForNewLayers()
            .addWithHistory(layer, "Add Shape Layer");
        Tools.SHAPES.activate();
    }

    @Override
    public boolean edit() {
        // shapes layers are edited using the shapes tool
        Tools.SHAPES.activate();
        return true;
    }

    @Override
    protected ShapesLayer createTypeSpecificCopy(CopyType copyType, Composition newComp) {
        String duplicateName = copyType.createLayerCopyName(name);
        var duplicate = new ShapesLayer(comp, duplicateName);

        if (styledShape != null) {
            duplicate.setStyledShape(styledShape.clone());
            if (transformBox != null) {
                View view = comp.getView();
                if (view != null) {
                    // can't be copied without a view
                    transformBox.reInitialize(view, styledShape);
                }

                duplicate.transformBox = transformBox.copy(duplicate.styledShape);
            }
        }
        return duplicate;
    }

    @Override
    public void paint(Graphics2D g, boolean firstVisibleLayer) {
        if (styledShape == null) {
            return;
        }
        // the custom blending modes don't work with gradients
        boolean useCachedImage = g.getComposite().getClass() != AlphaComposite.class
            && styledShape.hasBlendingIssue();
        if (useCachedImage) {
            if (cachedImage == null) {
                int width = comp.getCanvasWidth();
                int height = comp.getCanvasHeight();
                cachedImage = ImageUtils.createSysCompatibleImage(width, height);
                Graphics2D imgG = cachedImage.createGraphics();
                styledShape.paint(imgG);
                imgG.dispose();
            }
            g.drawImage(cachedImage, 0, 0, null);
        } else {
            styledShape.paint(g);
        }
    }

    @Override
    protected BufferedImage transformImage(BufferedImage src) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ORAImageInfo getORAImageInfo() {
        if (!hasShape()) {
            // if no shape, fall back to default behavior (an empty canvas-sized image)
            return super.getORAImageInfo();
        }

        Rectangle shapeBounds = styledShape.getContentBounds();
        if (shapeBounds == null || shapeBounds.isEmpty()) {
            // if bounds are invalid or empty, fall back to default
            return super.getORAImageInfo();
        }

        // create a tightly cropped image for the shape
        BufferedImage tightImage = ImageUtils.createSysCompatibleImage(shapeBounds.width, shapeBounds.height);
        Graphics2D tightG = tightImage.createGraphics();
        try {
            // translate graphics to paint the shape relative to the tight image's origin
            tightG.translate(-shapeBounds.x, -shapeBounds.y);
            styledShape.paint(tightG); // styledShape.paint expects g in image-space
        } finally {
            tightG.dispose();
        }
        return new ORAImageInfo(tightImage, shapeBounds.x, shapeBounds.y);
    }

    @Override
    public BufferedImage createIconThumbnail() {
        BufferedImage img = ImageUtils.createSysCompatibleImage(thumbSize, thumbSize);
        Graphics2D g2 = img.createGraphics();

        if (styledShape == null) {
            thumbCheckerBoardPainter.paint(g2, null, thumbSize, thumbSize);
        } else {
            styledShape.paintIconThumbnail(g2, thumbSize);
        }

        g2.dispose();
        return img;
    }

    @Override
    public CompletableFuture<Void> resize(Dimension newSize) {
        transform(comp.getCanvas().createImTransformToFit(newSize));
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void crop(Rectangle2D cropRect, boolean deleteCropped, boolean allowGrowing) {
        transform(Crop.createCropTransform(cropRect));
    }

    @Override
    public void flip(FlipDirection direction, boolean layerTransform) {
        transform(direction.createCanvasTransform(comp.getCanvas()));
    }

    @Override
    public void rotate(QuadrantAngle angle, boolean layerTransform) {
        transform(angle.createCanvasTransform(comp.getCanvas()));
    }

    @Override
    public void enlargeCanvas(Outsets out) {
        transform(AffineTransform.getTranslateInstance(out.left, out.top));
    }

    /**
     * Applies an affine transform to the layer's shape and/or transform box.
     */
    private void transform(AffineTransform at) {
        if (hasShape()) {
            if (transformBox != null) {
                // the box will also transform the shape
                transformBox.imCoordsChanged(at, comp.getView());
            } else {
                // This case should never happen, because an
                // initialized shape should always have a box.
                // Implemented here as a fallback, but not for the Move Tool support.
                styledShape.captureOrigState();
                styledShape.imTransform(at);

                if (AppMode.isDevelopment()) {
                    Messages.showError("Error", "No box in ShapesLayer.transform");
                }
            }
        }
    }

    /**
     * Returns true if this layer has a styled shape with actual geometry.
     */
    private boolean hasShape() {
        return styledShape != null && styledShape.hasShape();
    }

    public StyledShape getStyledShape() {
        return styledShape;
    }

    public TransformBox getTransformBox() {
        return transformBox;
    }

    public void setTransformBox(TransformBox transformBox) {
        this.transformBox = transformBox;
    }

    public void setStyledShape(StyledShape styledShape) {
        assert styledShape != null;
        this.styledShape = styledShape;

        // register a listener to invalidate the layer's
        // image cache when the styled shape changes
        styledShape.setChangeListener(() -> cachedImage = null);
    }

    @Override
    public Rectangle getContentBounds(boolean includeTransparent) {
        if (hasShape()) {
            return styledShape.getContentBounds();
        }
        return null;
    }

    @Override
    public int getPixelAtPoint(Point p) {
        if (hasShape() && styledShape.containsPoint(p)) {
            return 0xFF_FF_FF_FF; // opaque white for points inside the shape
        }
        return 0; // transparent black otherwise
    }

    @Override
    public void prepareMovement() {
        super.prepareMovement();
        if (hasShape() && transformBox != null) {
            View view = Views.getActive();
            transformBox.reInitialize(view, styledShape);
            transformBox.prepareMovement();
        }
    }

    @Override
    public void moveWhileDragging(double imDx, double imDy) {
        super.moveWhileDragging(imDx, imDy);
        if (hasShape() && transformBox != null) {
            transformBox.moveWhileDragging(imDx, imDy);
        }
    }

    @Override
    public PixelitorEdit finalizeMovement() {
        PixelitorEdit edit = super.finalizeMovement();
        if (hasShape() && transformBox != null) {
            transformBox.finalizeMovement();
        }
        return edit;
    }

    @Override
    PixelitorEdit createMovementEdit(int prevTx, int prevTy) {
        if (hasShape() && transformBox != null) {
//            return transformBox.createMovementEdit(comp, "Move Layer");
            return null; // TODO
        }
        return null;
    }

    @Override
    public void update(boolean updateHistogram) {
        super.update(updateHistogram);

        // TODO currently updateIconImage() of ShapesLayer is
        //  called only when the icon really changes.
        //  This must change when multiple shapes are allowed.
        if (holder != comp) {
            ((CompositeLayer) holder).updateIconImage();
        }
    }

    @Override
    public Tool getPreferredTool() {
        return Tools.SHAPES;
    }

    @Override
    public String getTypeString() {
        return "Shape Layer";
    }

    @Override
    public boolean checkInvariants() {
        if (!super.checkInvariants()) {
            return false;
        }
        if (styledShape != null) {
            return styledShape.checkInvariants();
        }
        return true; // no styledShape is a valid state (e.g., layer just created)
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = super.createDebugNode(key);

        node.addNullableDebuggable("styledShape", styledShape);
        node.addNullableDebuggable("transformBox", transformBox);

        return node;
    }
}
