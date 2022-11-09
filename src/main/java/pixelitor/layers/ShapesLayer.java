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
import pixelitor.CopyType;
import pixelitor.Views;
import pixelitor.compactions.Crop;
import pixelitor.compactions.Flip;
import pixelitor.gui.View;
import pixelitor.history.PixelitorEdit;
import pixelitor.io.TranslatedImage;
import pixelitor.tools.Tool;
import pixelitor.tools.Tools;
import pixelitor.tools.shapes.StyledShape;
import pixelitor.tools.transform.TransformBox;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.QuadrantAngle;
import pixelitor.utils.debug.DebugNode;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.concurrent.CompletableFuture;

import static pixelitor.Composition.LayerAdder.Position.ABOVE_ACTIVE;
import static pixelitor.layers.LayerGUILayout.thumbSize;

public class ShapesLayer extends ContentLayer {
    @Serial
    private static final long serialVersionUID = 1L;

    private static int count;

    private StyledShape styledShape;

    // The box is also stored here because recreating
    // it from the styled shape is currently not possible.
    private TransformBox transformBox;

    private transient BufferedImage cachedImage;

    public ShapesLayer(Composition comp, String name) {
        super(comp, name);
    }

    public static void createNew(Composition comp) {
        var layer = new ShapesLayer(comp, "shape layer " + (++count));
        new Composition.LayerAdder(comp.getHolderForNewLayers())
            .atPosition(ABOVE_ACTIVE)
            .withHistory("Add Shape Layer")
            .add(layer);
        Tools.SHAPES.activate();
    }

    @Override
    public boolean edit() {
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
                if (view != null && transformBox.needsInitialization(view)) {
                    // can't be copied without a view
                    transformBox.reInitialize(view, styledShape);
                }

                duplicate.transformBox = transformBox.copy(duplicate.styledShape);
            }
        }
        return duplicate;
    }

    @Override
    public void paintLayerOnGraphics(Graphics2D g, boolean firstVisibleLayer) {
        if (styledShape != null) {
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
    }

    @Override
    protected BufferedImage applyOnImage(BufferedImage src) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TranslatedImage getTranslatedImage() {
        // TODO the default implementation works, but it's suboptimal.
        //   This is related to the fact that the cached image is also unnecessarily big.
        return super.getTranslatedImage();
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
        transform(comp.getCanvas().createImTransformToSize(newSize));
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void crop(Rectangle2D cropRect, boolean deleteCropped, boolean allowGrowing) {
        transform(Crop.createCanvasTransform(cropRect));
    }

    @Override
    public void flip(Flip.Direction direction) {
        transform(direction.createCanvasTransform(comp.getCanvas()));
    }

    @Override
    public void rotate(QuadrantAngle angle) {
        transform(angle.createCanvasTransform(comp.getCanvas()));
    }

    @Override
    public void enlargeCanvas(int north, int east, int south, int west) {
        transform(AffineTransform.getTranslateInstance(west, north));
    }

    private void transform(AffineTransform at) {
        if (hasShape()) {
            if (transformBox != null) {
                // the box will also transform the shape
                transformBox.imCoordsChanged(at, comp.getView());
            } else {
                // This case should never happen, because an
                // initialized shape should always have a box.
                // Implemented here, but not for the Move Tool support.
                styledShape.resetTransform();
                styledShape.imTransform(at);
                System.out.println("ShapesLayer::transform: transforming only the shape");
            }
        }
    }

    private boolean hasShape() {
        return styledShape != null && styledShape.isInitialized();
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

    // When the mouse is pressed, only the reference is set.
    // Later, when the mouse is released, the history and
    // the icon image will also be handled
    public void setStyledShape(StyledShape styledShape) {
        assert styledShape != null;
        this.styledShape = styledShape;
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
        if (hasShape()) {
            if (styledShape.containsPoint(p)) {
                return 0xFF_FF_FF_FF;
            }
        }
        return 0;
    }

    @Override
    public void startMovement() {
        super.startMovement();
        if (hasShape() && transformBox != null) {
            View view = Views.getActive();
            if (transformBox.needsInitialization(view)) {
                transformBox.reInitialize(view, styledShape);
            }
            transformBox.startMovement();
        }
    }

    @Override
    public void moveWhileDragging(double relImX, double relImY) {
        super.moveWhileDragging(relImX, relImY);
        if (hasShape() && transformBox != null) {
            transformBox.moveWhileDragging(relImX, relImY);
        }
    }

    @Override
    public PixelitorEdit endMovement() {
        PixelitorEdit edit = super.endMovement();
        if (hasShape() && transformBox != null) {
            transformBox.endMovement();
//            updateIconImage();
        }
        return edit;
    }

    @Override
    PixelitorEdit createMovementEdit(int oldTx, int oldTy) {
        if (hasShape() && transformBox != null) {
            return transformBox.createMovementEdit(comp, "Move Layer");
        }
        return null;
    }

    @Override
    public void update(Composition.UpdateActions actions) {
        super.update(actions);

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
        return true;
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = super.createDebugNode(key);

        if (styledShape == null) {
            node.addString("styledShape", "null");
        } else {
            node.add(styledShape.createDebugNode());
        }

        if (transformBox == null) {
            node.addString("transformBox", "null");
        } else {
            node.add(transformBox.createDebugNode());
        }

        return node;
    }
}
