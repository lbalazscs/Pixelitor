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

import pixelitor.Composition;
import pixelitor.OpenImages;
import pixelitor.gui.BlendingModePanel;
import pixelitor.gui.GUIText;
import pixelitor.gui.View;
import pixelitor.history.History;
import pixelitor.layers.Drawable;
import pixelitor.layers.Layer;
import pixelitor.menus.DrawableAction;
import pixelitor.tools.DragTool;
import pixelitor.tools.gradient.history.GradientChangeEdit;
import pixelitor.tools.gradient.history.GradientHandlesHiddenEdit;
import pixelitor.tools.gradient.history.NewGradientEdit;
import pixelitor.tools.util.*;
import pixelitor.utils.Cursors;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;

import static java.awt.MultipleGradientPaint.CycleMethod.*;
import static pixelitor.colors.FgBgColors.setBGColor;
import static pixelitor.colors.FgBgColors.setFGColor;
import static pixelitor.tools.util.DraggablePoint.activePoint;

/**
 * The gradient tool
 */
public class GradientTool extends DragTool {
    private GradientHandles handles;

    private static final String CYCLE_NONE = "No Cycle";
    private static final String CYCLE_REFLECT = "Reflect";
    private static final String CYCLE_REPEAT = "Repeat";
    public static final String[] CYCLE_METHODS = {
        CYCLE_NONE, CYCLE_REFLECT, CYCLE_REPEAT};

    private JComboBox<GradientColorType> colorTypeCB;
    private JComboBox<GradientType> typeCB;
    private JComboBox<String> cycleMethodCB;
    private JCheckBox revertCB;
    private BlendingModePanel blendingModePanel;

    private Gradient lastGradient;
    private boolean ignoreRegenerate = false;

    public GradientTool() {
        super("Gradient", 'G', "gradient_tool.png",
            "<b>click</b> and <b>drag</b> to draw a gradient, " +
                "<b>Shift-drag</b> to constrain the direction. " +
                "Press <b>Esc</b> or <b>click</b> outside to hide the handles.",
            Cursors.DEFAULT, true);
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
        typeCB = new JComboBox<>(GradientType.values());
        typeCB.addActionListener(e ->
            regenerateGradient("Change Gradient Type"));
        settingsPanel.addComboBox(GUIText.TYPE + ": ",
            typeCB, "typeCB");
    }

    private void addCycleMethodSelector() {
        // cycle methods can't be put directly in the JComboBox,
        // because they would be all uppercase
        cycleMethodCB = new JComboBox<>(CYCLE_METHODS);
        cycleMethodCB.addActionListener(e ->
            regenerateGradient("Change Gradient Cycling"));
        settingsPanel.addComboBox("Cycling: ", cycleMethodCB, "cycleMethodCB");
    }

    private void addColorTypeSelector() {
        colorTypeCB = new JComboBox<>(GradientColorType.values());
        colorTypeCB.addActionListener(e ->
            regenerateGradient("Change Gradient Colors"));
        settingsPanel.addComboBox("Color: ", colorTypeCB, "colorTypeCB");
    }

    private void addRevertCheckBox() {
        revertCB = new JCheckBox();
        revertCB.addActionListener(e ->
            regenerateGradient("Change Gradient Direction"));
        settingsPanel.addWithLabel("Revert: ", revertCB, "revertCB");
    }

    private void addBlendingModePanel() {
        blendingModePanel = new BlendingModePanel(true);
        blendingModePanel.addActionListener(this::blendingModePanelChanged);
        settingsPanel.add(blendingModePanel);
    }

    private void blendingModePanelChanged(ActionEvent e) {
        String editName;
        if (e.getSource() instanceof JComboBox) {
            editName = "Change Gradient Blending Mode";
        } else {
            editName = "Change Gradient Opacity";
        }
        regenerateGradient(editName);
    }

    private void regenerateGradient(String editName) {
        if (ignoreRegenerate) {
            return;
        }
        if (handles == null) {
            return;
        }

        DrawableAction.run(editName,
            dr -> regenerateOnDrawable(dr, true, editName));
    }

    private void regenerateOnDrawable(Drawable dr,
                                      boolean addToHistory, String editName) {
        // regenerate the gradient if a tool setting
        // was changed while handles are present
        if (handles != null) {
            View view = OpenImages.getActiveView();
            if (view != null) {
                ImDrag imDrag = handles.toImDrag(view);
                if (!imDrag.isClick()) {
                    drawGradient(dr, imDrag, addToHistory, editName);
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
            if (activePoint == null) {
                // clicked outside the handles
                hideHandles(e.getComp(), true);
            }
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
        } else { // the initial drag just ended
            imDrag = userDrag.toImDrag();
            handles = new GradientHandles(
                userDrag.getCoStartX(), userDrag.getCoStartY(),
                userDrag.getCoEndX(), userDrag.getCoEndY(), e.getView());
        }

        var comp = e.getComp();
        Drawable dr = comp.getActiveDrawableOrThrow();
        drawGradient(dr, imDrag, true, null);
    }

    @Override
    public void mouseMoved(MouseEvent e, View view) {
        if (handles == null) {
            // in this method we only want to highlight the
            // handle under the mouse
            return;
        }

        DraggablePoint handle = handles.handleWasHit(e.getX(), e.getY());
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
        regenerateGradient("Change Gradient Colors");
    }

    @Override
    public void coCoordsChanged(View view) {
        if (handles != null) {
            handles.coCoordsChanged(view);
        }
    }

    @Override
    public void imCoordsChanged(AffineTransform at, Composition comp) {
        if (handles != null) {
            handles.imCoordsChanged(at, comp);
        }
    }

    @Override
    public void resetInitialState() {
        handles = null;
        activePoint = null;
        OpenImages.repaintActive();
    }

    @Override
    public void compReplaced(Composition newComp, boolean reloaded) {
        if (reloaded && handles != null) {
            hideHandles(newComp, false);
        }
    }

    @Override
    public void editedObjectChanged(Layer layer) {
        if (handles != null) {
            hideHandles(layer.getComp(), false);
        }
    }

    @Override
    public void forceFinish() {
        hideHandles(false);
    }

    @Override
    public void escPressed() {
        hideHandles(true);
    }

    private void hideHandles(boolean addHistory) {
        if (handles != null) {
            var comp = OpenImages.getActiveComp();
            hideHandles(comp, addHistory);
        }
    }

    private void hideHandles(Composition comp, boolean addHistory) {
        if (addHistory) {
            History.add(new GradientHandlesHiddenEdit(comp, lastGradient));
        }

        handles = null;
        activePoint = null;
        lastGradient = null;
        comp.repaint();
    }

    @Override
    public boolean arrowKeyPressed(ArrowKey key) {
        if (handles != null) {
            View view = OpenImages.getActiveView();
            assert view != null;

            handles.arrowKeyPressed(key, view);

            var comp = view.getComp();
            Drawable dr = comp.getActiveDrawable();
            if (dr == null) {
                // It shouldn't be possible to have handles without drawable,
                // but if somehow it does happen, then just move the handles.
                comp.repaint(); // make the arrow movement visible
                return false;
            }

            ImDrag imDrag = handles.toImDrag(view);
            String editName = key.isShiftDown()
                ? "Shift-nudge Gradient"
                : "Nudge Gradient";
            drawGradient(dr, imDrag, true, editName);

            return true;
        }
        return false;
    }

    private CycleMethod getCycleType() {
        String typeString = (String) cycleMethodCB.getSelectedItem();
        return getCycleMethodFromString(typeString);
    }

    private GradientColorType getGradientColorType() {
        return (GradientColorType) colorTypeCB.getSelectedItem();
    }

    private GradientType getType() {
        return (GradientType) typeCB.getSelectedItem();
    }

    private void drawGradient(Drawable dr, ImDrag imDrag,
                              boolean addToHistory, String editName) {
        Gradient gradient = new Gradient(imDrag,
            getType(), getCycleType(), getGradientColorType(),
            revertCB.isSelected(),
            blendingModePanel.getBlendingMode(),
            blendingModePanel.getOpacity());

        if (addToHistory) {
            boolean isFirst = lastGradient == null;
            if (isFirst) {
                History.add(new NewGradientEdit(dr, gradient));
            } else {
                History.add(new GradientChangeEdit(editName, dr, lastGradient, gradient));
            }
        }

        gradient.drawOn(dr);
        dr.getComp().imageChanged();
        lastGradient = gradient;
    }

    @Override
    public void paintOverImage(Graphics2D g2, Composition comp) {
        // the superclass draws the drag display
        super.paintOverImage(g2, comp);

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
        // The handles have to paint their drag display separately,
        // based on the handle positions, and not on the user drag
        return DragDisplayType.NONE;
    }

    private static CycleMethod getCycleMethodFromString(String s) {
        return switch (s) {
            case CYCLE_NONE -> NO_CYCLE;
            case CYCLE_REFLECT -> REFLECT;
            case CYCLE_REPEAT -> REPEAT;
            default -> throw new IllegalStateException("should not get here");
        };
    }

    private static String cycleMethodAsString(CycleMethod cm) {
        return switch (cm) {
            case NO_CYCLE -> CYCLE_NONE;
            case REFLECT -> CYCLE_REFLECT;
            case REPEAT -> CYCLE_REPEAT;
        };
    }

    @Override
    protected void toolStarted() {
        super.toolStarted();

        Layer activeLayer = OpenImages.getActiveLayer();
        if (activeLayer != null) {
            setupMaskEditing(activeLayer.isMaskEditing());
        }
    }

    @Override
    public void setupMaskEditing(boolean editMask) {
        blendingModePanel.setEnabled(!editMask);
    }

    // called only by history edits
    public void setGradient(Gradient gradient, boolean regenerate, Drawable dr) {
        var comp = dr.getComp();
        if (gradient == null) {
            hideHandles(comp, false);
            return;
        }

        assert gradient != null;

        View view = comp.getView();
        handles = gradient.createHandles(view);

        // set the settings
        ignoreRegenerate = true;

        colorTypeCB.setSelectedItem(gradient.getColorType());
        typeCB.setSelectedItem(gradient.getType());
        cycleMethodCB.setSelectedItem(cycleMethodAsString(gradient.getCycleMethod()));
        revertCB.setSelected(gradient.isReverted());
        blendingModePanel.setBlendingMode(gradient.getBlendingMode());
        blendingModePanel.setOpacity(gradient.getOpacity());

        setFGColor(gradient.getFgColor(), false);
        setBGColor(gradient.getBgColor(), false);

        ignoreRegenerate = false;
        if (regenerate) {
            regenerateOnDrawable(dr, false, null);
        }

        lastGradient = gradient;
        view.repaint();
    }

    @Override
    public boolean allowOnlyDrawables() {
        return true;
    }

    @Override
    public DebugNode createDebugNode() {
        var node = super.createDebugNode();

        node.addString("type", getType().toString());
        node.addString("cycling", getCycleType().toString());
        node.addQuotedString("color", getGradientColorType().toString());
        node.addBoolean("revert", revertCB.isSelected());
        node.addFloat("opacity", blendingModePanel.getOpacity());
        node.addQuotedString("blending mode",
            blendingModePanel.getBlendingMode().toString());

        return node;
    }
}
