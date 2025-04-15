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

package pixelitor.tools.gradient;

import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.compactions.Outsets;
import pixelitor.gui.View;
import pixelitor.layers.BlendingMode;
import pixelitor.layers.Drawable;
import pixelitor.layers.LayerMask;
import pixelitor.tools.util.Drag;
import pixelitor.utils.debug.DebugNode;
import pixelitor.utils.debug.Debuggable;

import java.awt.*;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.geom.AffineTransform;
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
 * A vector graphics gradient with all necessary information for rendering.
 */
public class Gradient implements Serializable, Debuggable {
    @Serial
    private static final long serialVersionUID = -6574312118763734469L;

    // the drag representing the start and end points
    private Drag drag;

    private final GradientType type;
    private final CycleMethod cycleMethod;
    private final GradientColorType colorType;
    private final boolean reversed;
    private final BlendingMode blendingMode;
    private final float opacity;

    // the actual painting colors
    private final Color[] colors;

    // stored here so that they can be restored
    private final Color fgColor;
    private final Color bgColor;

    // helper variable, used when a gradient
    // fill layer is moved using the move tool
    private transient Drag origDrag;

    public Gradient(Drag drag, GradientType type,
                    CycleMethod cycleMethod, GradientColorType colorType,
                    boolean reversed, BlendingMode blendingMode, float opacity) {
        assert !drag.isImClick();

        this.drag = drag;
        this.type = type;
        this.cycleMethod = cycleMethod;
        this.colorType = colorType;
        this.reversed = reversed;
        this.blendingMode = blendingMode;
        this.opacity = opacity;

        fgColor = getFGColor();
        bgColor = getBGColor();
        colors = initColors(colorType, reversed);
    }

    private Gradient(Gradient source) {
        this.drag = source.drag;
        this.type = source.type;
        this.cycleMethod = source.cycleMethod;
        this.colorType = source.colorType;
        this.reversed = source.reversed;
        this.blendingMode = source.blendingMode;
        this.opacity = source.opacity;
        this.fgColor = source.fgColor;
        this.bgColor = source.bgColor;
        this.colors = source.colors;
    }

    public Gradient copy() {
        return new Gradient(this);
    }

    private static Color[] initColors(GradientColorType colorType, boolean reversed) {
        Color startColor = colorType.getStartColor(reversed);
        Color endColor = colorType.getEndColor(reversed);

        return new Color[]{startColor, endColor};
    }

    public void paintOn(Drawable dr) {
        Graphics2D g;
        Composition comp = dr.getComp();
        Canvas canvas = comp.getCanvas();
        int width, height;
        if (dr instanceof LayerMask) {
            // paint directly, without a temporary layer
            BufferedImage subImage = dr.getCanvasSizedSubImage();
            g = subImage.createGraphics();
            width = canvas.getWidth();
            height = canvas.getHeight();
        } else {
            // use a temporary layer when painting on an image
            // layer, in order to have soft selection
            var composite = blendingMode.getComposite(opacity);
            var tmpDrawingLayer = dr.createTmpLayer(composite, true);
            g = tmpDrawingLayer.getGraphics();

            if (tmpDrawingLayer.hasSmallImage()) {
                Rectangle bounds = comp.getSelection().getShapeBounds();
                width = bounds.width;
                height = bounds.height;
            } else {
                width = canvas.getWidth();
                height = canvas.getHeight();
            }

            drag = tmpDrawingLayer.translate(drag);
        }

        paintOnGraphics(g, width, height);

        g.dispose();
        dr.mergeTmpDrawingLayerDown();
        dr.updateIconImage();
    }

    public void paintOnGraphics(Graphics2D g, int width, int height) {
        // No composite is set in this method, because
        // it's not needed for gradient fill layers.
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        Paint paint = type.createPaint(drag, colors, cycleMethod);
        g.setPaint(paint);
        g.fillRect(0, 0, width, height);
    }

    /**
     * Paints a thumbnail preview of the gradient.
     */
    public void paintThumbnail(Graphics2D g2, Canvas canvas, Dimension thumbSize) {
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
    public boolean isFullyCovering() {
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

    public boolean isReversed() {
        return reversed;
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
        drag.calcCoCoords(view);
        return new GradientHandles(drag.getStart(view), drag.getEnd(view), view);
    }

    public void crop(Rectangle2D cropRect) {
        drag = drag.imTranslatedCopy(-cropRect.getX(), -cropRect.getY());
    }

    public void enlargeCanvas(Outsets enlargement) {
        drag = drag.imTranslatedCopy(enlargement.left, enlargement.top);
    }

    public void imTransform(AffineTransform at) {
        drag = drag.imTransformedCopy(at);
    }

    public void prepareMovement() {
        origDrag = drag.copy();
    }

    public void moveWhileDragging(double x, double y) {
        drag = origDrag.imTranslatedCopy(x, y);
    }

    public void finalizeMovement() {
        // nothing to do
    }

    public boolean hasTransparency() {
        return colorType.hasTransparency();
    }

    public boolean hasCustomTransparency() {
        return colorType.hasTransparency() && isCustom();
    }

    private boolean isCustom() {
        return switch (type) {
            case LINEAR, RADIAL -> false;
            case ANGLE, SPIRAL_CW, SPIRAL_CCW, DIAMOND -> true;
        };
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = new DebugNode(key, this);

        node.add(drag.createDebugNode("drag"));
        node.addAsString("type", type);
        node.addAsString("cycle method", cycleMethod);
        node.addAsString("color type", colorType);
        node.addBoolean("reversed", reversed);
        node.addAsString("blending mode", blendingMode);
        node.addFloat("opacity", opacity);

        return node;
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
