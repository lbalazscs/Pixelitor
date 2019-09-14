/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.gui.OpenComps;
import pixelitor.gui.View;
import pixelitor.gui.utils.Dialogs;
import pixelitor.history.History;
import pixelitor.layers.Drawable;
import pixelitor.tools.ClipStrategy;
import pixelitor.tools.DragTool;
import pixelitor.tools.gradient.history.GradientChangeEdit;
import pixelitor.tools.gradient.history.GradientHandlesHiddenEdit;
import pixelitor.tools.gradient.history.NewGradientEdit;
import pixelitor.tools.util.DragDisplayType;
import pixelitor.tools.util.DraggablePoint;
import pixelitor.tools.util.ImDrag;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.Cursors;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;

import static java.awt.MultipleGradientPaint.CycleMethod.NO_CYCLE;
import static java.awt.MultipleGradientPaint.CycleMethod.REFLECT;
import static java.awt.MultipleGradientPaint.CycleMethod.REPEAT;
import static pixelitor.tools.util.DraggablePoint.activePoint;

/**
 * The gradient tool
 */
public class GradientTool extends DragTool {
    private GradientHandles handles;

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
    private JCheckBox revertCheckBox;
    private BlendingModePanel blendingModePanel;

    private Gradient lastGradient;
    private boolean ignoreRegenerate = false;

    public GradientTool() {
        super("Gradient", 'G', "gradient_tool_icon.png",
                "<b>click</b> and <b>drag</b> to draw a gradient, " +
                        "<b>Shift-drag</b> to constrain the direction. " +
                        "Press <b>Esc</b> to hide the handles and the arrow.",
                Cursors.DEFAULT, true, true,
                true, ClipStrategy.FULL);
        spaceDragStartPoint = true;
    }

    @Override
    public void initSettingsPanel() {
        addTypeSelector();
        addCycleMethodSelector();

        settingsPanel.addSeparator();

        addColorTypeSelector();
        addRevertCheckBox();

        settingsPanel.addSeparator();

        addBlendingModePanel();
    }

    private void addTypeSelector() {
        typeSelector = new JComboBox<>(GradientType.values());
        typeSelector.addActionListener(e -> regenerateGradient(true));
        settingsPanel.addWithLabel("Type: ",
                typeSelector, "gradientTypeSelector");
    }

    private void addCycleMethodSelector() {
        // cycle methods cannot be put directly in the JComboBox,
        // because they would be all uppercase
        cycleMethodSelector = new JComboBox<>(CYCLE_METHODS);
        cycleMethodSelector.addActionListener(e -> regenerateGradient(true));
        settingsPanel.addWithLabel("Cycling: ",
                cycleMethodSelector, "gradientCycleMethodSelector");
    }

    private void addColorTypeSelector() {
        colorTypeSelector = new JComboBox<>(GradientColorType.values());
        colorTypeSelector.addActionListener(e -> regenerateGradient(true));
        settingsPanel.addWithLabel("Color: ",
                colorTypeSelector, "gradientColorTypeSelector");
    }

    private void addRevertCheckBox() {
        revertCheckBox = new JCheckBox();
        revertCheckBox.addActionListener(e -> regenerateGradient(true));
        settingsPanel.addWithLabel("Revert: ", revertCheckBox, "gradientRevert");
    }

    private void addBlendingModePanel() {
        blendingModePanel = new BlendingModePanel(true);
        blendingModePanel.addActionListener(e -> regenerateGradient(true));
        settingsPanel.add(blendingModePanel);
    }

    private void regenerateGradient(boolean addToHistory) {
        if (ignoreRegenerate) {
            return;
        }

        Drawable dr = OpenComps.getActiveDrawableOrNull();
        if (dr == null) {
            Dialogs.showNotDrawableDialog();
            return;
        }

        // regenerate the gradient if a tool setting
        // was changed while handles are present
        if (handles != null) {
            View view = OpenComps.getActiveView();
            if (view != null) {
                ImDrag imDrag = handles.toImDrag(view);
                if (!imDrag.isClick()) {
                    drawGradient(dr, imDrag, addToHistory);
                }
            }
        }
    }

    @Override
    public void dragStarted(PMouseEvent e) {
        double x = e.getCoX();
        double y = e.getCoY();
        if (handles != null) {
            DraggablePoint hit = handles.handleWasHit(x, y);
            if (hit != null) {
                hit.setActive(true);
                hit.mousePressed(x, y);
            }
            e.repaint();
        }
    }

    @Override
    public void ongoingDrag(PMouseEvent e) {
        // the gradient will be drawn only when the mouse is released

        if (activePoint != null) {
            // draw the handles
            double x = e.getCoX();
            double y = e.getCoY();
            activePoint.mouseDragged(x, y, e.isShiftDown());
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
        ImDrag imDrag;
        if (activePoint != null) { // a handle was dragged
            assert handles != null;
            
            double x = e.getCoX();
            double y = e.getCoY();
            activePoint.mouseReleased(x, y, e.isShiftDown());
            if (!activePoint.handleContains(x, y)) {
                // we can get here if the handle has a
                // constrained position
                activePoint = null;
            }

            imDrag = handles.toImDrag(e.getView());
            if (imDrag.isClick()) {
                return;
            }
        } else { // a gradient was dragged
            imDrag = userDrag.toImDrag();
            handles = new GradientHandles(
                    userDrag.getCoStartX(), userDrag.getCoStartY(),
                    userDrag.getCoEndX(), userDrag.getCoEndY(), e.getView());
        }

        Composition comp = e.getComp();
        Drawable dr = comp.getActiveDrawableOrThrow();
        drawGradient(dr, imDrag, true);
    }

    @Override
    public void mouseMoved(MouseEvent e, View view) {
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
            view.repaint();
        } else {
            if (activePoint != null) {
                activePoint = null;
                view.repaint();
            }
        }
    }

    @Override
    protected void toolEnded() {
        super.toolEnded();

        resetInitialState();
    }

    @Override
    public void fgBgColorsChanged() {
        regenerateGradient(true);
    }

    @Override
    public void coCoordsChanged(View view) {
        if (handles != null) {
            handles.coCoordsChanged(view);
        }
    }

    @Override
    public void imCoordsChanged(Composition comp, AffineTransform at) {
        if (handles != null) {
            handles.imCoordsChanged(at);
        }
    }

    @Override
    public void resetInitialState() {
        handles = null;
        activePoint = null;
        OpenComps.repaintActive();
    }

    @Override
    public void compReplaced(Composition oldComp, Composition newComp) {
        hideHandles(newComp.getView());
    }

    @Override
    public void escPressed() {
        if (handles != null) {
            Composition comp = OpenComps.getActiveCompOrNull();
            History.addEdit(new GradientHandlesHiddenEdit(comp, lastGradient));

            hideHandles(comp.getView());
        }
    }

    private void hideHandles(View view) {
        handles = null;
        activePoint = null;
        lastGradient = null;
        view.repaint();
    }

    private CycleMethod getCycleType() {
        String typeString = (String) cycleMethodSelector.getSelectedItem();
        return getCycleMethodFromString(typeString);
    }

    private GradientColorType getGradientColorType() {
        return (GradientColorType) colorTypeSelector.getSelectedItem();
    }

    private GradientType getType() {
        return (GradientType) typeSelector.getSelectedItem();
    }

    private void drawGradient(Drawable dr, ImDrag imDrag, boolean addToHistory) {
        Gradient gradient = new Gradient(imDrag,
                getType(), getCycleType(), getGradientColorType(),
                revertCheckBox.isSelected(),
                blendingModePanel.getBlendingMode(),
                blendingModePanel.getOpacity());


        if (addToHistory) {
            boolean isFirst = lastGradient == null;
            if (isFirst) {
                History.addEdit(new NewGradientEdit(dr, gradient));
            } else {
                History.addEdit(new GradientChangeEdit(dr, lastGradient, gradient));
            }
        }

        gradient.drawOn(dr);
        dr.getComp().imageChanged();
        lastGradient = gradient;
    }

    @Override
    public void paintOverImage(Graphics2D g2, Canvas canvas, View view,
                               AffineTransform componentTransform,
                               AffineTransform imageTransform) {
        // the superclass draws the drag display
        super.paintOverImage(g2, canvas, view, componentTransform, imageTransform);

        if (handles != null) {
            handles.paint(g2);
        } else {
            if (userDrag != null && userDrag.isDragging()) {
                // during the first drag, when there are no handles yet,
                // paint only the arrow
                userDrag.drawGradientArrow(g2);
            }
        }
    }

    @Override
    public DragDisplayType getDragDisplayType() {
        if (handles == null) {
            return DragDisplayType.ANGLE_DIST;
        }
        // TODO has to be implemented for the handles separately,
        // it cannot rely on the user drag
        return DragDisplayType.NONE;
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

    private static String cycleMethodAsString(CycleMethod cm) {
        switch (cm) {
            case NO_CYCLE:
                return NO_CYCLE_AS_STRING;
            case REFLECT:
                return REFLECT_AS_STRING;
            case REPEAT:
                return REPEAT_AS_STRING;
        }
        throw new IllegalStateException("should not get here");
    }

    public void setupMaskEditing(boolean editMask) {
        if (editMask) {
            blendingModePanel.setEnabled(false);
        } else {
            blendingModePanel.setEnabled(true);
        }
    }

    // called only by history edits
    public void setGradient(Gradient gradient, boolean regenerate, View view) {
        if (gradient == null) {
            hideHandles(view);
            return;
        }

        assert gradient != null;
        handles = gradient.createHandles(view);

        // set the settings
        ignoreRegenerate = true;

        colorTypeSelector.setSelectedItem(gradient.getColorType());
        typeSelector.setSelectedItem(gradient.getType());
        cycleMethodSelector.setSelectedItem(
                cycleMethodAsString(gradient.getCycleMethod()));
        revertCheckBox.setSelected(gradient.isReverted());
        blendingModePanel.setBlendingMode(gradient.getBlendingMode());
        blendingModePanel.setOpacity(gradient.getOpacity());

        ignoreRegenerate = false;
        if (regenerate) {
            regenerateGradient(false);
        }

        lastGradient = gradient;
        view.repaint();
    }

    @Override
    public DebugNode getDebugNode() {
        DebugNode node = super.getDebugNode();

        node.addString("Type", getType().toString());
        node.addString("Cycling", getCycleType().toString());
        node.addQuotedString("Color", getGradientColorType().toString());
        node.addBoolean("Revert", revertCheckBox.isSelected());
        node.addFloat("Opacity", blendingModePanel.getOpacity());
        node.addQuotedString("Blending Mode",
                blendingModePanel.getBlendingMode().toString());

        return node;
    }
}
