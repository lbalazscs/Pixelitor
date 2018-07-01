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

import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.gui.BlendingModePanel;
import pixelitor.gui.ImageComponent;
import pixelitor.gui.ImageComponents;
import pixelitor.history.History;
import pixelitor.layers.Drawable;
import pixelitor.layers.LayerMask;
import pixelitor.layers.TmpDrawingLayer;
import pixelitor.tools.ClipStrategy;
import pixelitor.tools.DragTool;
import pixelitor.tools.DraggablePoint;
import pixelitor.tools.ImDrag;
import pixelitor.tools.PMouseEvent;
import pixelitor.utils.Cursors;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint.CycleMethod;
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
public class GradientTool extends DragTool {
    private GradientPoints handles;
    private DraggablePoint activeHandle;

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

    public GradientTool() {
        super('g', "Gradient", "gradient_tool_icon.png",
                "<b>click</b> and <b>drag</b> to draw a gradient, <b>Shift-drag</b> to constrain the direction. Press <b>Esc</b> to hide the handles and the arrow.",
                Cursors.DEFAULT, true, true, true, ClipStrategy.CANVAS);
    }

    @Override
    public void initSettingsPanel() {
        typeSelector = new JComboBox<>(GradientType.values());
        typeSelector.addActionListener(e -> regenerateGradient());
        settingsPanel.addWithLabel("Type: ", typeSelector, "gradientTypeSelector");

        // cycle methods cannot be put directly in the JComboBox, because they would be all uppercase
        cycleMethodSelector = new JComboBox<>(CYCLE_METHODS);
        cycleMethodSelector.addActionListener(e -> regenerateGradient());
        settingsPanel.addWithLabel("Cycling: ", cycleMethodSelector, "gradientCycleMethodSelector");

        settingsPanel.addSeparator();

        colorTypeSelector = new JComboBox<>(GradientColorType.values());
        colorTypeSelector.addActionListener(e -> regenerateGradient());
        settingsPanel.addWithLabel("Color: ", colorTypeSelector, "gradientColorTypeSelector");

        invertCheckBox = new JCheckBox();
        invertCheckBox.addActionListener(e -> regenerateGradient());
        settingsPanel.addWithLabel("Invert: ", invertCheckBox, "gradientInvert");

        settingsPanel.addSeparator();

        blendingModePanel = new BlendingModePanel(true);
        blendingModePanel.addActionListener(e -> regenerateGradient());
        settingsPanel.add(blendingModePanel);
    }

    public void regenerateGradient() {
        // regenerate the gradient if a tool setting
        // was changed while handles are present
        if (handles != null) {
            ImageComponent ic = ImageComponents.getActiveIC();
            if (ic != null) {
                ImDrag imDrag = handles.toImDrag(ic);
                if (!imDrag.isClick()) {
                    drawGradient(ic.getComp(), imDrag);
                }
            }
        }
    }

    @Override
    public void dragStarted(PMouseEvent e) {
        int x = e.getCoX();
        int y = e.getCoY();
        if (handles != null) {
            DraggablePoint handle = handles.handleWasHit(x, y);
            if (handle != null) {
                activeHandle = handle;
                handle.setActive(true);
                handle.mousePressed(x, y);
            }
            e.repaint();
        }
    }

    @Override
    public void ongoingDrag(PMouseEvent e) {
        // the gradient will be drawn only when the mouse is released

        if (activeHandle != null) {
            // draw the handles
            int x = e.getCoX();
            int y = e.getCoY();
            activeHandle.mouseDragged(x, y, e.isShiftDown());
        } else {
            // if we are dragging a new gradient from scratch,
            // we don't want to show the old handles
            handles = null;
        }

        e.repaint();
    }

    @Override
    public void dragFinished(PMouseEvent e) {
        if (userDrag.isClick()) {
            return;
        }
        Composition comp = e.getComp();
        ImDrag imDrag;
        if (activeHandle != null) { // a handle was dragged
            int x = e.getCoX();
            int y = e.getCoY();
            activeHandle.mouseReleased(x, y, e.isShiftDown());
            if (!activeHandle.handleContains(x, y)) {
                // we can get here if the handle has a
                // constrained position
                activeHandle.setActive(false);
                activeHandle = null;
            }

            imDrag = handles.toImDrag(e.getIC());
        } else { // a gradient was dragged
            imDrag = userDrag.toImDrag();
            handles = new GradientPoints(
                    userDrag.getCoStartX(), userDrag.getCoStartY(),
                    userDrag.getCoEndX(), userDrag.getCoEndY(), e.getIC());
        }
        drawGradient(e.getComp(), imDrag);
    }

    @Override
    public void mouseMoved(MouseEvent e, ImageComponent ic) {
        if (handles == null) {
            // in this method we only want to highlight the
            // handle under the mouse
            return;
        }
        int x = e.getX();
        int y = e.getY();
        DraggablePoint handle = handles.handleWasHit(x, y);
        if (handle != null) {
            handle.setActive(true);
            activeHandle = handle;
            ic.repaint();
        } else {
            if (activeHandle != null) {
                activeHandle.setActive(false);
                ic.repaint();
            }
            activeHandle = null;
        }
    }

    @Override
    protected void toolEnded() {
        super.toolEnded();

        resetStateToInitial();
    }

    @Override
    public void fgBgColorsChanged() {
        regenerateGradient();
    }

    @Override
    public void icSizeChanged(ImageComponent ic) {
        if (handles != null) {
            handles.viewSizeChanged(ic);
        }
    }

    @Override
    public void resetStateToInitial() {
        handles = null;
        activeHandle = null;
        ImageComponents.repaintActive();
    }

    @Override
    public void escPressed() {
        // hide the handles
        if (handles != null) {
            handles = null;
            activeHandle = null;
            ImageComponents.repaintActive();
        }
    }

    private void drawGradient(Composition comp, ImDrag imDrag) {
        History.addImageEdit(getName(), comp);
        drawGradient(comp.getActiveDrawable(),
                getType(),
                getGradientColorType(),
                getCycleType(),
                blendingModePanel.getComposite(),
                imDrag,
                invertCheckBox.isSelected()
        );
        comp.imageChanged(FULL);
    }

    private CycleMethod getCycleType() {
        return getCycleMethodFromString((String) cycleMethodSelector.getSelectedItem());
    }

    private GradientColorType getGradientColorType() {
        return (GradientColorType) colorTypeSelector.getSelectedItem();
    }

    private GradientType getType() {
        return (GradientType) typeSelector.getSelectedItem();
    }

    public static void drawGradient(Drawable dr, GradientType gradientType,
                                    GradientColorType colorType,
                                    CycleMethod cycleMethod,
                                    Composite composite,
                                    ImDrag imDrag, boolean invert) {
        if (imDrag.isClick()) {
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

        Paint gradient = gradientType.getGradient(imDrag, colors, cycleMethod);

        g.setPaint(gradient);

        g.fillRect(0, 0, width, height);
        g.dispose();
        dr.mergeTmpDrawingLayerDown();
        dr.updateIconImage();
    }

    @Override
    public void paintOverImage(Graphics2D g2, Canvas canvas, ImageComponent ic, AffineTransform componentTransform, AffineTransform imageTransform) {
        if (handles != null) {
            handles.paint(g2);
        } else {
            if (userDrag != null && !userDrag.isFinished()) {
                userDrag.drawGradientArrow(g2);
            }
        }
    }

    private static CycleMethod getCycleMethodFromString(String s) {
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
