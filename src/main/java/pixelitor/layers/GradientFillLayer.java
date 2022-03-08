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

import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.Views;
import pixelitor.compactions.Flip;
import pixelitor.history.GradientFillLayerChangeEdit;
import pixelitor.history.History;
import pixelitor.history.PixelitorEdit;
import pixelitor.tools.Tool;
import pixelitor.tools.Tools;
import pixelitor.tools.gradient.Gradient;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.QuadrantAngle;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.concurrent.CompletableFuture;

import static pixelitor.Composition.LayerAdder.Position.ABOVE_ACTIVE;
import static pixelitor.layers.LayerButtonLayout.thumbSize;

public class GradientFillLayer extends ContentLayer {
    @Serial
    private static final long serialVersionUID = 109261602799761359L;

    private Gradient gradient;
    private transient Gradient backupGradient;
    private transient BufferedImage cachedImage;

    private GradientFillLayer(Composition comp, String name) {
        super(comp, name);
    }

    public static void createNew() {
        var comp = Views.getActiveComp();
        var layer = new GradientFillLayer(comp, "gradient fill");
        new Composition.LayerAdder(comp)
            .atPosition(ABOVE_ACTIVE)
            .withHistory("Add Gradient Fill Layer")
            .add(layer);
        Tools.startAndSelect(Tools.GRADIENT);
    }

    @Override
    public void edit() {
        Tools.startAndSelect(Tools.GRADIENT);
    }

    @Override
    protected Layer createTypeSpecificDuplicate(String duplicateName) {
        var duplicate = new GradientFillLayer(comp, duplicateName);
        if (gradient != null) {
            // could be shared, because it is overwritten
            // when editing, but make a copy for safety
            duplicate.gradient = gradient.copy();
        }
        return duplicate;
    }

    @Override
    public void paintLayerOnGraphics(Graphics2D g, boolean firstVisibleLayer) {
        if (gradient != null) {
            int width = comp.getCanvasWidth();
            int height = comp.getCanvasHeight();
            // the custom blending modes don't work with gradients
            boolean useCachedImage = g.getComposite().getClass() != AlphaComposite.class
                                     // and custom gradients using transparency also have a problem
                                     || gradient.isCustomTransparency();
            if (useCachedImage) {
                if (cachedImage == null) {
                    cachedImage = ImageUtils.createSysCompatibleImage(width, height);
                    Graphics2D imgG = cachedImage.createGraphics();
                    gradient.drawOnGraphics(imgG, comp, width, height);
                    imgG.dispose();
                }
                g.drawImage(cachedImage, 0, 0, null);
            } else {
                gradient.drawOnGraphics(g, comp, width, height);
            }
        }
    }

    @Override
    protected BufferedImage applyOnImage(BufferedImage src) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BufferedImage createIconThumbnail() {
        Canvas canvas = comp.getCanvas();
        Dimension thumbDim = ImageUtils.calcThumbDimensions(
            canvas.getWidth(), canvas.getHeight(), thumbSize);

        BufferedImage img = ImageUtils.createSysCompatibleImage(
            thumbDim.width, thumbDim.height);
        Graphics2D g2 = img.createGraphics();

        if (gradient == null || gradient.hasTransparency()) {
            thumbCheckerBoardPainter.paint(g2, null, thumbDim.width, thumbDim.height);
        }
        if (gradient != null) {
            gradient.paintIconThumbnail(g2, canvas, thumbDim);
        }

        g2.dispose();
        return img;
    }

    @Override
    public CompletableFuture<Void> resize(Dimension newSize) {
        if (gradient != null) {
            AffineTransform at = comp.getCanvas().createImTransformToSize(newSize);
            gradient.imTransform(at);
            cachedImage = null;
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void crop(Rectangle2D cropRect, boolean deleteCropped, boolean allowGrowing) {
        if (gradient != null) {
            gradient.crop(cropRect);
            cachedImage = null;
        }
    }

    @Override
    public void flip(Flip.Direction direction) {
        if (gradient != null) {
            gradient.imTransform(direction.createCanvasTransform(comp.getCanvas()));
            cachedImage = null;
        }
    }

    @Override
    public void rotate(QuadrantAngle angle) {
        if (gradient != null) {
            gradient.imTransform(angle.createCanvasTransform(comp.getCanvas()));
            cachedImage = null;
        }
    }

    @Override
    public void enlargeCanvas(int north, int east, int south, int west) {
        if (gradient != null) {
            gradient.enlargeCanvas(north, west);
            cachedImage = null;
        }
    }

    public Gradient getGradient() {
        return gradient;
    }

    public void setGradient(Gradient gradient, boolean addHistory) {
        // the gradient can be null if this is called while undoing the first gradient
        assert gradient != null || !addHistory;

        Gradient oldGradient = this.gradient;

        this.gradient = gradient;
        cachedImage = null;
        comp.update();
        updateIconImage();

        if (addHistory) {
            History.add(new GradientFillLayerChangeEdit(this, oldGradient, gradient));
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
        if (gradient != null) {
            gradient.startMovement();
            backupGradient = gradient.copy();
        }
    }

    @Override
    public void moveWhileDragging(double relImX, double relImY) {
        super.moveWhileDragging(relImX, relImY);
        if (gradient != null) {
            gradient.moveWhileDragging(relImX, relImY);
        }
    }

    @Override
    public PixelitorEdit endMovement() {
        PixelitorEdit edit = super.endMovement();
        if (gradient != null) {
            gradient.endMovement();
            updateIconImage();
        }
        return edit;
    }

    @Override
    PixelitorEdit createMovementEdit(int oldTx, int oldTy) {
        return new GradientFillLayerChangeEdit(this, backupGradient, gradient);
    }

    @Override
    public Tool getPreferredTool() {
        return Tools.GRADIENT;
    }

    @Override
    public String getTypeString() {
        return "Gradient Fill Layer";
    }
}
