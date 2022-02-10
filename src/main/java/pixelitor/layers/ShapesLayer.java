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
import pixelitor.Views;
import pixelitor.compactions.Crop;
import pixelitor.compactions.Flip;
import pixelitor.history.History;
import pixelitor.history.PixelitorEdit;
import pixelitor.history.ShapesLayerChangeEdit;
import pixelitor.tools.Tool;
import pixelitor.tools.Tools;
import pixelitor.tools.shapes.StyledShape;
import pixelitor.tools.transform.TransformBox;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.QuadrantAngle;
import pixelitor.utils.debug.DebugNode;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.concurrent.CompletableFuture;

import static pixelitor.Composition.LayerAdder.Position.ABOVE_ACTIVE;
import static pixelitor.layers.LayerButtonLayout.thumbSize;

public class ShapesLayer extends ContentLayer {
    @Serial
    private static final long serialVersionUID = 1L;

    private static int count;

    private StyledShape styledShape;

    // The box is also stored here because recreating
    // it from the styled shape is currently not possible.
    private TransformBox transformBox;

    ShapesLayer(Composition comp, String name) {
        super(comp, name);
    }

    public static void createNew() {
        var comp = Views.getActiveComp();
        var layer = new ShapesLayer(comp, "shape layer " + (++count));
        new Composition.LayerAdder(comp)
            .atPosition(ABOVE_ACTIVE)
            .withHistory("Add Shape Layer")
            .add(layer);
        Tools.startAndSelect(Tools.SHAPES);
    }

    @Override
    public void edit() {
        Tools.startAndSelect(Tools.SHAPES);
    }

    @Override
    protected Layer createTypeSpecificDuplicate(String duplicateName) {
        var duplicate = new ShapesLayer(comp, duplicateName);
        if (styledShape != null) {
            duplicate.styledShape = styledShape.clone();
            duplicate.transformBox = transformBox.copy(duplicate.styledShape, comp.getView());
        }
        return duplicate;
    }

    @Override
    public void paintLayerOnGraphics(Graphics2D g, boolean firstVisibleLayer) {
        if (styledShape != null) {
            styledShape.paint(g);
        }
    }

    @Override
    protected BufferedImage applyOnImage(BufferedImage src) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BufferedImage createIconThumbnail() {
        BufferedImage img = ImageUtils.createSysCompatibleImage(
            thumbSize, thumbSize);
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
        if (styledShape != null) {
            double sx = newSize.getWidth() / comp.getCanvasWidth();
            double sy = newSize.getHeight() / comp.getCanvasHeight();
            transformBox.imTransform(AffineTransform.getScaleInstance(sx, sy));
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void crop(Rectangle2D cropRect, boolean deleteCroppedPixels, boolean allowGrowing) {
        if (styledShape != null) {
            AffineTransform at = Crop.createCanvasTransform(cropRect);
            transformBox.imTransform(at);
        }
    }

    @Override
    public void flip(Flip.Direction direction) {
        if (styledShape != null) {
            transformBox.imCoordsChanged(direction.createCanvasTransform(comp.getCanvas()), comp);
        }
    }

    @Override
    public void rotate(QuadrantAngle angle) {
        if (styledShape != null) {
            transformBox.imCoordsChanged(angle.createCanvasTransform(comp.getCanvas()), comp);
        }
    }

    @Override
    public void enlargeCanvas(int north, int east, int south, int west) {
        if (styledShape != null) {
            transformBox.imTransform(AffineTransform.getTranslateInstance(west, north));
        }
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

    // Only sets the reference when the mouse is pressed.
    // Later, when the mouse is released, the history and
    // the icon image will also be handled
    public void setStyledShape(StyledShape styledShape) {
        this.styledShape = styledShape;
    }

    public void setStyledShape(StyledShape shape, boolean addHistory) {
        // the styled shape can be null if this is called while undoing the first shape
        assert shape != null || !addHistory;

        StyledShape oldShape = this.styledShape;

        this.styledShape = shape;
        comp.update();
        updateIconImage();

        if (addHistory) {
            History.add(new ShapesLayerChangeEdit(this, oldShape, shape));
//        } else {
//            // called from the undo/redo
//            Tools.editingTargetChanged(this);
        }
    }

    @Override
    public Rectangle getEffectiveBoundingBox() {
        return comp.getCanvasBounds();
    }

    @Override
    public Rectangle getContentBounds() {
        // by returning null, the move tool shows no outline
        return null;
    }

    @Override
    public int getPixelAtPoint(Point p) {
        return 0;
    }

    @Override
    public void startMovement() {
        super.startMovement();
        if (transformBox != null) {
            transformBox.startMovement();
        }
    }

    @Override
    public void moveWhileDragging(double relImX, double relImY) {
        super.moveWhileDragging(relImX, relImY);
        if (transformBox != null) {
            transformBox.moveWhileDragging(relImX, relImY);
        }
    }

    @Override
    public PixelitorEdit endMovement() {
        PixelitorEdit edit = super.endMovement();
        if (transformBox != null) {
            transformBox.endMovement();
            updateIconImage();
        }
        return edit;
    }

    @Override
    PixelitorEdit createMovementEdit(int oldTx, int oldTy) {
        if (transformBox != null) {
            return transformBox.createMovementEdit(comp, "Move Shape Layer");
        }
        return null;
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
    public DebugNode createDebugNode(String descr) {
        DebugNode node = super.createDebugNode(descr);

        if (styledShape == null) {
            node.addString("shape", "NONE");
        } else {
            node.add(styledShape.createDebugNode());
        }

        if (transformBox == null) {
            node.addString("box", "null");
        } else {
            node.add(transformBox.createDebugNode());
        }

        return node;
    }
}
