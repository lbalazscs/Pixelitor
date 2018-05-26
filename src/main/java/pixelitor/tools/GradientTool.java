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

package pixelitor.tools;

import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.gui.BlendingModePanel;
import pixelitor.gui.ImageComponent;
import pixelitor.layers.Drawable;
import pixelitor.layers.LayerMask;
import pixelitor.layers.TmpDrawingLayer;
import pixelitor.menus.view.ZoomLevel;
import pixelitor.utils.Cursors;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.Paint;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import static java.awt.MultipleGradientPaint.CycleMethod.NO_CYCLE;
import static java.awt.MultipleGradientPaint.CycleMethod.REFLECT;
import static java.awt.MultipleGradientPaint.CycleMethod.REPEAT;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static pixelitor.Composition.ImageChangeActions.FULL;

/**
 * The gradient tool
 */
public class GradientTool extends Tool {
    private boolean thereWasDragging = false;

    private static final String NO_CYCLE_AS_STRING = "No Cycle";
    private static final String REFLECT_AS_STRING = "Reflect";
    private static final String REPEAT_AS_STRING = "Repeat";
    public static final String[] CYCLE_METHODS = {
            NO_CYCLE_AS_STRING,
            REFLECT_AS_STRING,
            REPEAT_AS_STRING};

    private JComboBox<GradientColorType> colorTypeSelector;
    private JComboBox<GradientType> typeSelector;
    private JComboBox<String> cycleMethodSelector;
    private JCheckBox invertCheckBox;
    private BlendingModePanel blendingModePanel;

    GradientTool() {
        super('g', "Gradient", "gradient_tool_icon.png", "click and drag to draw a gradient, Shift-drag to constrain the direction.",
                Cursors.DEFAULT, true, true, true, ClipStrategy.CANVAS);
    }

    @Override
    public void initSettingsPanel() {
        typeSelector = new JComboBox<>(GradientType.values());
        settingsPanel.addWithLabel("Type: ", typeSelector, "gradientTypeSelector");

        // cycle methods cannot be put directly in the JComboBox, because they would be all uppercase
        cycleMethodSelector = new JComboBox<>(CYCLE_METHODS);
        settingsPanel.addWithLabel("Cycling: ", cycleMethodSelector, "gradientCycleMethodSelector");

        settingsPanel.addSeparator();

        colorTypeSelector = new JComboBox<>(GradientColorType.values());
        settingsPanel.addWithLabel("Color: ", colorTypeSelector, "gradientColorTypeSelector");

        invertCheckBox = new JCheckBox();
        settingsPanel.addWithLabel("Invert: ", invertCheckBox, "gradientInvert");

        settingsPanel.addSeparator();

        blendingModePanel = new BlendingModePanel(true);
        settingsPanel.add(blendingModePanel);
    }

    @Override
    public void mousePressed(MouseEvent e, ImageComponent ic) {

    }

    @Override
    public void mouseDragged(MouseEvent e, ImageComponent ic) {
        thereWasDragging = true;  // the gradient will be drawn only when the mouse is released
        ic.repaint();
    }

    @Override
    public void mouseReleased(MouseEvent e, ImageComponent ic) {
        if (thereWasDragging) {
            Composition comp = ic.getComp();

            saveFullImageForUndo(comp);
            drawGradient(comp.getActiveDrawable(),
                    getType(),
                    getGradientColorType(),
                    getCycleType(),
                    blendingModePanel.getComposite(),
                    userDrag,
                    invertCheckBox.isSelected()
            );

            thereWasDragging = false;
            comp.imageChanged(FULL);
        }
    }

    private MultipleGradientPaint.CycleMethod getCycleType() {
        return getCycleMethodFromString((String) cycleMethodSelector.getSelectedItem());
    }

    private GradientColorType getGradientColorType() {
        return (GradientColorType) colorTypeSelector.getSelectedItem();
    }

    private GradientType getType() {
        return (GradientType) typeSelector.getSelectedItem();
    }

    @Override
    public boolean dispatchMouseClicked(MouseEvent e, ImageComponent ic) {
        if (super.dispatchMouseClicked(e, ic)) {
            return true;
        }
        thereWasDragging = false;
        return false;
    }

    public static void drawGradient(Drawable dr, GradientType gradientType, GradientColorType colorType, MultipleGradientPaint.CycleMethod cycleMethod, Composite composite, UserDrag userDrag, boolean invert) {
        if (userDrag.isClick()) {
            return;
        }

        Graphics2D g;
        int width;
        int height;
        if (dr instanceof LayerMask) {
            BufferedImage subImage = dr.getCanvasSizedSubImage();
            g = subImage.createGraphics();
            width = subImage.getWidth();
            height = subImage.getHeight();
        } else {
            TmpDrawingLayer tmpDrawingLayer = dr.createTmpDrawingLayer(composite);
            g = tmpDrawingLayer.getGraphics();
            width = tmpDrawingLayer.getWidth();
            height = tmpDrawingLayer.getHeight();
        }
        dr.getComp().applySelectionClipping(g, null);
        // repeated gradients are still jaggy
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

        Color startColor = colorType.getStartColor(invert);
        Color endColor = colorType.getEndColor(invert);
        assert startColor != null;
        assert endColor != null;
        Color[] colors = {startColor, endColor};

        Paint gradient = gradientType.getGradient(userDrag, colors, cycleMethod);

        g.setPaint(gradient);

        g.fillRect(0, 0, width, height);
        g.dispose();
        dr.mergeTmpDrawingLayerDown();
        dr.updateIconImage();
    }

    @Override
    public void paintOverImage(Graphics2D g2, Canvas canvas, ImageComponent ic, AffineTransform unscaledTransform) {
        if (thereWasDragging) {
            g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

            ZoomLevel zoomLevel = ic.getZoomLevel();

            g2.setColor(Color.BLACK);
            g2.setStroke(zoomLevel.getOuterStroke());
            userDrag.drawLine(g2);

            g2.setColor(Color.WHITE);
            g2.setStroke(zoomLevel.getInnerStroke());
            userDrag.drawLine(g2);
        }
    }

    private static MultipleGradientPaint.CycleMethod getCycleMethodFromString(String s) {
        switch (s) {
            case NO_CYCLE_AS_STRING:
                return NO_CYCLE;
            case REFLECT_AS_STRING:
                return REFLECT;
            case REPEAT_AS_STRING:
                return REPEAT;
        }
        throw new IllegalStateException("should not get here");
    }

    public void setupMaskDrawing(boolean editMask) {
        if (editMask) {
            blendingModePanel.setEnabled(false);
        } else {
            blendingModePanel.setEnabled(true);
        }
    }

    @Override
    public DebugNode getDebugNode() {
        DebugNode node = super.getDebugNode();

        node.addString("Type", getType().toString());
        node.addString("Cycling", getCycleType().toString());
        node.addQuotedString("Color", getGradientColorType().toString());
        node.addBoolean("Invert", invertCheckBox.isSelected());
        node.addFloat("Opacity", blendingModePanel.getOpacity());
        node.addQuotedString("Blending Mode", blendingModePanel.getBlendingMode().toString());

        return node;
    }
}
