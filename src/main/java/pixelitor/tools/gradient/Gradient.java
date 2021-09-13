/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.tools.gradient;

import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.gui.View;
import pixelitor.layers.BlendingMode;
import pixelitor.layers.Drawable;
import pixelitor.layers.LayerMask;
import pixelitor.tools.util.Drag;

import java.awt.*;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.io.Serializable;
import java.util.StringJoiner;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static pixelitor.colors.FgBgColors.getBGColor;
import static pixelitor.colors.FgBgColors.getFGColor;

/**
 * This class describes a gradient with all the information
 * necessary to recreate it.
 * Note that no pixel values are stored here, this is
 * all vector graphics.
 */
public class Gradient implements Serializable {
    @Serial
    private static final long serialVersionUID = -6574312118763734469L;

    private Drag drag;
    private final GradientType type;
    private final CycleMethod cycleMethod;
    private final GradientColorType colorType;
    private final boolean reverted;
    private final BlendingMode blendingMode;
    private final float opacity;

    // the actual drawing colors
    private final Color[] colors;

    // saved so that they can be restored
    private final Color fgColor;
    private final Color bgColor;

    private transient Drag moveStartDrag;

    public Gradient(Drag drag, GradientType type,
                    CycleMethod cycleMethod, GradientColorType colorType,
                    boolean reverted, BlendingMode blendingMode, float opacity) {
        assert !drag.isImClick();
        this.drag = drag;
        this.type = type;
        this.cycleMethod = cycleMethod;
        this.colorType = colorType;
        this.reverted = reverted;
        this.blendingMode = blendingMode;
        this.opacity = opacity;

        fgColor = getFGColor();
        bgColor = getBGColor();
        colors = initColors(colorType, reverted);
    }

    private static Color[] initColors(GradientColorType colorType, boolean reverted) {
        Color startColor = colorType.getStartColor(reverted);
        Color endColor = colorType.getEndColor(reverted);
        assert startColor != null;
        assert endColor != null;
        return new Color[]{startColor, endColor};
    }

    public void drawOn(Drawable dr) {
        Graphics2D g;
        var comp = dr.getComp();
        Canvas canvas = comp.getCanvas();
        int width, height;
        if (dr instanceof LayerMask) {
            BufferedImage subImage = dr.getCanvasSizedSubImage();
            g = subImage.createGraphics();
            width = canvas.getWidth();
            height = canvas.getHeight();
        } else {
            var composite = blendingMode.getComposite(opacity);
            var tmpDrawingLayer = dr.createTmpDrawingLayer(composite, true);
            g = tmpDrawingLayer.getGraphics();

            // the temporary image might be smaller than the canvas, if there is selection

            boolean smallImage = tmpDrawingLayer.hasSmallImage();
            if (smallImage) {
                Rectangle bounds = comp.getSelection().getShapeBounds();
                width = bounds.width;
                height = bounds.height;
            } else {
                width = canvas.getWidth();
                height = canvas.getHeight();
            }

            drag = tmpDrawingLayer.translateDrag(drag);
        }

        drawOnGraphics(g, comp, width, height);

        g.dispose();
        dr.mergeTmpDrawingLayerDown();
        dr.updateIconImage();
    }

    public void drawOnGraphics(Graphics2D g, Composition comp, int width, int height) {
        // No composite is set in this method, because
        // it's not needed for gradient fill layers.
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        Paint paint = type.createPaint(drag, colors, cycleMethod);
        g.setPaint(paint);
        g.fillRect(0, 0, width, height);
    }

    public void paintIconThumbnail(Graphics2D g2, Canvas canvas,
                                   Dimension thumbSize) {
        double scaling;
        if (thumbSize.width > thumbSize.height) {
            scaling = thumbSize.width / (double) canvas.getWidth();
        } else {
            scaling = thumbSize.height / (double) canvas.getHeight();
        }
        g2.scale(scaling, scaling);

        Paint paint = type.createPaint(drag, colors, cycleMethod);
        g2.setPaint(paint);
        g2.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    /**
     * Returns whether the gradient pixels fully cover the originals.
     * If true, then it should not be necessary to save the images for undo.
     */
    public boolean fullyCovers() {
        return colorType != GradientColorType.FG_TO_TRANSPARENT
               && blendingMode == BlendingMode.NORMAL
               && opacity == 1.0f;
    }

    public GradientType getType() {
        return type;
    }

    public CycleMethod getCycleMethod() {
        return cycleMethod;
    }

    public GradientColorType getColorType() {
        return colorType;
    }

    public boolean isReverted() {
        return reverted;
    }

    public BlendingMode getBlendingMode() {
        return blendingMode;
    }

    public float getOpacity() {
        return opacity;
    }

    public Color getFgColor() {
        return fgColor;
    }

    public Color getBgColor() {
        return bgColor;
    }

    public GradientHandles createHandles(View view) {
        Point2D handleStart = view.imageToComponentSpace(drag.getStartPoint());
        Point2D handleEnd = view.imageToComponentSpace(drag.getEndPoint());

        return new GradientHandles(handleStart, handleEnd, view);
    }

    public void crop(Rectangle2D cropRect) {
        drag = drag.translatedCopy(-cropRect.getX(), -cropRect.getY());
    }

    public void enlargeCanvas(int north, int west) {
        drag = drag.translatedCopy(west, north);
    }

    public void transform(AffineTransform at) {
        drag = drag.transformedCopy(at);
    }

    public Gradient copy() {
        return new Gradient(drag, type, cycleMethod, colorType, reverted, blendingMode, opacity);
    }

    public void startMovement() {
        moveStartDrag = drag.copy();
    }

    public void moveWhileDragging(double x, double y) {
        drag = moveStartDrag.translatedCopy(x, y);
    }

    public void endMovement() {
    }

    public boolean hasTransparency() {
        return colorType.hasTransparency();
    }

    public boolean isCustomTransparency() {
        return colorType.hasTransparency() && isCustom();
    }

    public boolean isCustom() {
        return switch (type) {
            case LINEAR, RADIAL -> false;
            default -> true;
        };
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Gradient.class.getSimpleName() + "[", "]")
            .add("fgColor=" + fgColor)
            .add("bgColor=" + bgColor)
            .add("startColor=" + colors[0])
            .add("endColor=" + colors[1])
            .toString();
    }
}
