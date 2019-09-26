/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.gui.View;
import pixelitor.layers.BlendingMode;
import pixelitor.layers.Drawable;
import pixelitor.layers.LayerMask;
import pixelitor.layers.TmpDrawingLayer;
import pixelitor.tools.util.ImDrag;

import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.Paint;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

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
public class Gradient {
    private final ImDrag imDrag;
    private final GradientType type;
    private final CycleMethod cycleMethod;
    private final GradientColorType colorType;
    private final boolean reverted;
    private final BlendingMode blendingMode;
    private final float opacity;
    private final Color[] colors;

    private final Color fgColor;
    private final Color bgColor;

    public Gradient(ImDrag imDrag, GradientType type,
                    CycleMethod cycleMethod, GradientColorType colorType,
                    boolean reverted, BlendingMode blendingMode, float opacity) {
        assert !imDrag.isClick();
        this.imDrag = imDrag;
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
        int width;
        int height;
        if (dr instanceof LayerMask) {
            BufferedImage subImage = dr.getCanvasSizedSubImage();
            g = subImage.createGraphics();
            width = subImage.getWidth();
            height = subImage.getHeight();
        } else {
            Composite composite = blendingMode.getComposite(opacity);
            TmpDrawingLayer tmpDrawingLayer = dr.createTmpDrawingLayer(composite);
            g = tmpDrawingLayer.getGraphics();
            width = tmpDrawingLayer.getWidth();
            height = tmpDrawingLayer.getHeight();
        }
        dr.getComp().applySelectionClipping(g);

        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

        Paint paint = type.createPaint(imDrag, colors, cycleMethod);

        g.setPaint(paint);

        g.fillRect(0, 0, width, height);

        g.dispose();
        dr.mergeTmpDrawingLayerDown();
        dr.updateIconImage();
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
        Point2D handleStart = view.imageToComponentSpace(imDrag.getStartPoint());
        Point2D handleEnd = view.imageToComponentSpace(imDrag.getEndPoint());

        return new GradientHandles(handleStart, handleEnd, view);
    }
}
