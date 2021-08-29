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
import pixelitor.utils.ImageUtils;

import java.awt.*;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.Serializable;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static pixelitor.colors.FgBgColors.getBGColor;
import static pixelitor.colors.FgBgColors.getFGColor;
import static pixelitor.layers.LayerButtonLayout.thumbSize;

/**
 * This class describes a gradient with all the information
 * necessary to recreate it.
 * Note that no pixel values are stored here, this is
 * all vector graphics.
 */
public class Gradient implements Serializable {
    private Drag drag;
    private final GradientType type;
    private final CycleMethod cycleMethod;
    private final GradientColorType colorType;
    private final boolean reverted;
    private final BlendingMode blendingMode;
    private final float opacity;
    private final Color[] colors;

    private final Color fgColor;
    private final Color bgColor;

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

        Color startColor = colorType.getStartColor(reverted);
        Color endColor = colorType.getEndColor(reverted);
        assert startColor != null;
        assert endColor != null;
        colors = new Color[]{startColor, endColor};

        fgColor = getFGColor();
        bgColor = getBGColor();
    }

    public void drawOn(Drawable dr) {
        Graphics2D g;
        var comp = dr.getComp();
        Canvas canvas = comp.getCanvas();
        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();
        boolean smallImage; // the temporary image might be smaller than the canvas, if there is selection
        if (dr instanceof LayerMask) {
            BufferedImage subImage = dr.getCanvasSizedSubImage();
            g = subImage.createGraphics();
            assert canvasWidth == subImage.getWidth();
            assert canvasHeight == subImage.getHeight();
            smallImage = false;
        } else {
            var composite = blendingMode.getComposite(opacity);
            var tmpDrawingLayer = dr.createTmpDrawingLayer(composite, true);
            g = tmpDrawingLayer.getGraphics();
            smallImage = tmpDrawingLayer.hasSmallImage();
            drag = tmpDrawingLayer.translateDrag(drag);
        }

        drawOnGraphics(g, comp, canvasWidth, canvasHeight, smallImage);

        g.dispose();
        dr.mergeTmpDrawingLayerDown();
        dr.updateIconImage();
    }

    public void drawOnGraphics(Graphics2D g, Composition comp, int canvasWidth, int canvasHeight, boolean smallImage) {
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        Paint paint = type.createPaint(drag, colors, cycleMethod);
        g.setPaint(paint);
        if (smallImage) {
            Rectangle bounds = comp.getSelection().getShapeBounds();
            g.fillRect(0, 0, bounds.width, bounds.height);
        } else {
            g.fillRect(0, 0, canvasWidth, canvasHeight);
        }
    }

    public BufferedImage createIconThumbnail(Canvas canvas) {
        Dimension thumbDim = ImageUtils.calcThumbDimensions(
            canvas.getWidth(), canvas.getHeight(), thumbSize);

        double scaling;
        if (thumbDim.width > thumbDim.height) {
            scaling = thumbDim.width / (double) canvas.getWidth();
        } else {
            scaling = thumbDim.height / (double) canvas.getHeight();
        }

        BufferedImage img = ImageUtils.createSysCompatibleImage(
            thumbDim.width, thumbDim.height);
        Graphics2D g2 = img.createGraphics();
        g2.scale(scaling, scaling);

        Paint paint = type.createPaint(drag, colors, cycleMethod);
        g2.setPaint(paint);
        g2.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        g2.dispose();
        return img;
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
}
