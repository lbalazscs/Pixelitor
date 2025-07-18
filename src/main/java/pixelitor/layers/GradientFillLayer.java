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
import pixelitor.CopyType;
import pixelitor.compactions.FlipDirection;
import pixelitor.compactions.Outsets;
import pixelitor.compactions.QuadrantAngle;
import pixelitor.history.GradientFillLayerChangeEdit;
import pixelitor.history.History;
import pixelitor.history.PixelitorEdit;
import pixelitor.tools.Tool;
import pixelitor.tools.Tools;
import pixelitor.tools.gradient.Gradient;
import pixelitor.tools.util.Drag;
import pixelitor.utils.ImageUtils;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.concurrent.CompletableFuture;

/**
 * A gradient fill layer that fills the entire canvas with a given gradient.
 */
public class GradientFillLayer extends ContentLayer {
    @Serial
    private static final long serialVersionUID = 109261602799761359L;

    private Gradient gradient;

    // a snapshot of the gradient before a Move Tool operation
    private transient Gradient backupGradient;

    private transient BufferedImage cachedImage;

    private static int count;

    // helper for Move Tool support
    private transient Drag origDrag;

    public GradientFillLayer(Composition comp, String name) {
        super(comp, name);
    }

    public static void createNew(Composition comp) {
        var layer = new GradientFillLayer(comp, generateName());
        comp.getHolderForNewLayers()
            .addWithHistory(layer, "Add Gradient Fill Layer");
        Tools.GRADIENT.activate();
    }

    private static String generateName() {
        return "gradient fill " + (++count);
    }

    @Override
    public boolean edit() {
        Tools.GRADIENT.activate();
        return true;
    }

    @Override
    protected GradientFillLayer createTypeSpecificCopy(CopyType copyType, Composition newComp) {
        String copyName = copyType.createLayerCopyName(name);
        var copy = new GradientFillLayer(newComp, copyName);
        if (gradient != null) {
            // could be shared, because it is overwritten
            // when editing, but make a copy for safety
            copy.gradient = gradient.copy();
        }
        return copy;
    }

    @Override
    public void paint(Graphics2D g, boolean firstVisibleLayer) {
        if (gradient == null) {
            return;
        }
        int width = comp.getCanvasWidth();
        int height = comp.getCanvasHeight();

        // the custom blending modes don't work with gradients
        boolean needsCache = g.getComposite().getClass() != AlphaComposite.class
            // and custom gradients using transparency also have a problem
            || gradient.hasCustomTransparency();

        if (needsCache) {
            if (cachedImage == null) {
                cachedImage = ImageUtils.createSysCompatibleImage(width, height);
                Graphics2D imgG = cachedImage.createGraphics();
                gradient.paintOnGraphics(imgG, width, height);
                imgG.dispose();
            }
            g.drawImage(cachedImage, 0, 0, null);
        } else {
            gradient.paintOnGraphics(g, width, height);
        }
    }

    @Override
    protected BufferedImage transformImage(BufferedImage src) {
        // gradient fill layers are not image adjustments
        throw new UnsupportedOperationException();
    }

    @Override
    public BufferedImage createIconThumbnail() {
        Dimension thumbDim = comp.getCanvas().getThumbSize();

        BufferedImage img = ImageUtils.createSysCompatibleImage(
            thumbDim.width, thumbDim.height);
        Graphics2D g2 = img.createGraphics();

        if (gradient == null || gradient.hasTransparency()) {
            thumbCheckerBoardPainter.paint(g2, null, thumbDim.width, thumbDim.height);
        }
        if (gradient != null) {
            gradient.paintThumbnail(g2, comp.getCanvas(), thumbDim);
        }

        g2.dispose();
        return img;
    }

    @Override
    public CompletableFuture<Void> resize(Dimension newSize) {
        if (gradient != null) {
            AffineTransform at = comp.getCanvas().createImTransformToFit(newSize);
            gradient.imTransform(at);
            invalidateGradientCache();
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void crop(Rectangle2D cropRect, boolean deleteCropped, boolean allowGrowing) {
        if (gradient != null) {
            gradient.crop(cropRect);
            invalidateGradientCache();
        }
    }

    @Override
    public void flip(FlipDirection direction) {
        if (gradient != null) {
            gradient.imTransform(direction.createCanvasTransform(comp.getCanvas()));
            invalidateGradientCache();
        }
    }

    @Override
    public void rotate(QuadrantAngle angle) {
        if (gradient != null) {
            gradient.imTransform(angle.createCanvasTransform(comp.getCanvas()));
            invalidateGradientCache();
        }
    }

    @Override
    public void enlargeCanvas(Outsets out) {
        if (gradient != null) {
            gradient.enlargeCanvas(out);
            invalidateGradientCache();
        }
    }

    private void invalidateGradientCache() {
        cachedImage = null;
    }

    public Gradient getGradient() {
        return gradient;
    }

    public void setGradient(Gradient newGradient, boolean addHistory) {
        // the new gradient can be null if this is called while undoing the first gradient
        assert newGradient != null || !addHistory;

        Gradient prevGradient = this.gradient;

        this.gradient = newGradient;
        invalidateGradientCache();
        holder.update();
        updateIconImage();

        if (addHistory) {
            History.add(new GradientFillLayerChangeEdit(this, prevGradient, newGradient));
        } else { // called from the undo/redo
            if (Tools.GRADIENT.isActive()) {
                // the handles have to be updated by the tool
                Tools.GRADIENT.updateFrom(this);
            }
        }
    }

    @Override
    public Rectangle getContentBounds(boolean includeTransparent) {
        // by returning null, the move tool shows no outline for the layer content
        return null;
    }

    @Override
    public int getPixelAtPoint(Point p) {
        // a crude implementation that ensures that gradient fill
        // layers are always found by the Move Tool/Auto Select Layer
        return 0xFF_FF_FF_FF;
    }

    @Override
    public void prepareMovement() {
        super.prepareMovement();
        if (gradient != null) {
            origDrag = gradient.getDrag().copy();
            backupGradient = gradient.copy();
        }
    }

    @Override
    public void moveWhileDragging(double imDx, double imDy) {
        super.moveWhileDragging(imDx, imDy);
        if (gradient != null) {
            Drag newDrag = origDrag.imTranslatedCopy(imDx, imDy);
            gradient.setDrag(newDrag);
        }
    }

    @Override
    public PixelitorEdit finalizeMovement() {
        PixelitorEdit edit = super.finalizeMovement();
        if (gradient != null) {
            updateIconImage();
        }
        return edit;
    }

    @Override
    PixelitorEdit createMovementEdit(int prevTx, int prevTy) {
        // prevTx and prevTy are for the ContentLayer's translation, not directly
        // used here as gradient movement is handled via gradient state
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
