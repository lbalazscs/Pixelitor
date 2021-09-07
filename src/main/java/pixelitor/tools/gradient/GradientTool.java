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
import pixelitor.layers.GradientFillLayer;
import pixelitor.layers.Layer;
import pixelitor.menus.DrawableAction;
import pixelitor.tools.DragTool;
import pixelitor.tools.Tool;
import pixelitor.tools.gradient.history.GradientChangeEdit;
import pixelitor.tools.gradient.history.GradientHandlesHiddenEdit;
import pixelitor.tools.gradient.history.NewGradientEdit;
import pixelitor.tools.gui.ToolButton;
import pixelitor.tools.util.*;
import pixelitor.utils.Cursors;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.Paint;
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

    private GradientHandles handles;
    private Gradient lastGradient;
    private boolean ignoreRegenerate = false;

    private GradientFillLayer gradientLayer;

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
        settingsPanel.addComboBox(GUIText.TYPE + ": ", typeCB, "typeCB");
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

        if (editingGradientLayer()) {
            View view = OpenImages.getActiveView();
            if (view != null) {
                Drag renderedDrag = handles.toDrag(view);
                if (!renderedDrag.isImClick()) {
                    gradientLayer.setGradient(createGradient(renderedDrag));
                }
            }
        } else {
            DrawableAction.run(editName,
                dr -> regenerateOnDrawable(dr, true, editName));
        }
    }

    private boolean editingGradientLayer() {
        return gradientLayer != null;
    }

    private void regenerateOnDrawable(Drawable dr,
                                      boolean addToHistory, String editName) {
        // regenerate the gradient if a tool setting
        // was changed while handles are present
        if (handles == null) {
            return;
        }
        View view = OpenImages.getActiveView();
        if (view != null) {
            Drag renderedDrag = handles.toDrag(view);
            if (!renderedDrag.isImClick()) {
                drawGradient(dr, renderedDrag, addToHistory, editName);
            }
        }
    }

    @Override
    public void dragStarted(PMouseEvent e) {
        if (handles == null) {
            return;
        }
        double x = e.getCoX();
        double y = e.getCoY();
        DraggablePoint hit = handles.handleWasHit(x, y);
        if (hit != null) {
            hit.setActive(true);
            hit.mousePressed(x, y);
        }
        e.repaint();
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
            // if a new gradient is created from scratch, hide the old handles
            handles = null;
        }

        e.repaint();
    }

    @Override
    public void dragFinished(PMouseEvent e) {
        if (drag.isClick()) {
            if (activePoint == null) {
                // clicked outside the handles
                hideHandles(e.getComp(), true);
            }
            return;
        }
        Drag renderedDrag;
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

            renderedDrag = handles.toDrag(e.getView());
            if (renderedDrag.isImClick()) {
                return;
            }
        } else { // the initial drag just ended
            renderedDrag = drag;
            handles = new GradientHandles(
                drag.getCoStartX(), drag.getCoStartY(),
                drag.getCoEndX(), drag.getCoEndY(), e.getView());
        }

        if (editingGradientLayer()) {
            gradientLayer.setGradient(createGradient(renderedDrag));
        } else {
            Drawable dr = e.getComp().getActiveDrawableOrThrow();
            drawGradient(dr, renderedDrag, true, null);
        }
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
        lastGradient = null;
        OpenImages.repaintActive();
    }

    @Override
    public void compReplaced(Composition newComp, boolean reloaded) {
        if (reloaded && handles != null) {
            hideHandles(newComp, false);
        }
        handleNewActiveLayer(newComp.getActiveLayer());
    }

    @Override
    public void editedObjectChanged(Layer layer) {
        handleNewActiveLayer(layer);
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
        if (editingGradientLayer()) {
            return;
        }

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
        if (handles == null) {
            return false;
        }
        View view = OpenImages.getActiveView();
        assert view != null;

        handles.arrowKeyPressed(key, view);
        Drag handleDrag = handles.toDrag(view);

        if (editingGradientLayer()) {
            gradientLayer.setGradient(createGradient(handleDrag));
        } else {
            var comp = view.getComp();
            Drawable dr = comp.getActiveDrawable();
            if (dr == null) {
                // It shouldn't be possible to have handles without drawable,
                // but if somehow it does happen, then just move the handles.
                comp.repaint(); // make the arrow movement visible
                return false;
            }

            String editName = key.isShiftDown() ? "Shift-nudge Gradient" : "Nudge Gradient";
            drawGradient(dr, handleDrag, true, editName);
        }

        return true;
    }

    private CycleMethod getCycleType() {
        return cycleMethodFromString((String) cycleMethodCB.getSelectedItem());
    }

    private GradientColorType getGradientColorType() {
        return (GradientColorType) colorTypeCB.getSelectedItem();
    }

    private GradientType getType() {
        return (GradientType) typeCB.getSelectedItem();
    }

    private void drawGradient(Drawable dr, Drag drag,
                              boolean addToHistory, String editName) {
        Gradient gradient = createGradient(drag);

        if (addToHistory) {
            boolean isFirst = lastGradient == null;
            if (isFirst) {
                History.add(new NewGradientEdit(dr, gradient));
            } else {
                History.add(new GradientChangeEdit(editName, dr, lastGradient, gradient));
            }
        }

        gradient.drawOn(dr);
        dr.getComp().update();
        lastGradient = gradient;
    }

    private Gradient createGradient(Drag drag) {
        return new Gradient(drag,
            getType(), getCycleType(), getGradientColorType(),
            revertCB.isSelected(),
            blendingModePanel.getBlendingMode(),
            blendingModePanel.getOpacity());
    }

    @Override
    public void paintOverImage(Graphics2D g2, Composition comp) {
        // the superclass draws the drag display
        super.paintOverImage(g2, comp);

        if (handles != null) {
            handles.paint(g2);
        } else {
            if (drag != null && drag.isDragging()) {
                // during the first drag, when there are no handles yet,
                // paint only the arrow
                drag.drawGradientArrow(g2);
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

    private static CycleMethod cycleMethodFromString(String s) {
        return switch (s) {
            case CYCLE_NONE -> NO_CYCLE;
            case CYCLE_REFLECT -> REFLECT;
            case CYCLE_REPEAT -> REPEAT;
            default -> throw new IllegalStateException("should not get here");
        };
    }

    private static String cycleMethodToString(CycleMethod cm) {
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
            handleNewActiveLayer(activeLayer);
        }
    }

    private void handleNewActiveLayer(Layer layer) {
        if (layer.isMaskEditing()) {
            setupMaskEditing(true);
            gradientLayer = null;
        } else {
            if (layer instanceof GradientFillLayer gfl) {
                blendingModePanel.setEnabled(false);
                gradientLayer = gfl;
                Gradient gradient = gfl.getGradient();
                if (gradient != null) {
                    loadSettingsFromGradient(gradient, gfl.getComp().getView());

                    // make the loaded handles visible
                    layer.getComp().getView().repaint();
                }
            } else {
                if (handles != null) {
                    hideHandles(layer.getComp(), false);
                }
                gradientLayer = null;
                blendingModePanel.setEnabled(true);
            }
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

        View view = comp.getView();
        loadSettingsFromGradient(gradient, view);
        if (regenerate) {
            regenerateOnDrawable(dr, false, null);
        }

        lastGradient = gradient;
        view.repaint();
    }

    private void loadSettingsFromGradient(Gradient gradient, View view) {
        ignoreRegenerate = true;

        handles = gradient.createHandles(view);
        colorTypeCB.setSelectedItem(gradient.getColorType());
        typeCB.setSelectedItem(gradient.getType());
        cycleMethodCB.setSelectedItem(cycleMethodToString(gradient.getCycleMethod()));
        revertCB.setSelected(gradient.isReverted());
        if (blendingModePanel.isEnabled()) {
            blendingModePanel.setBlendingMode(gradient.getBlendingMode());
            blendingModePanel.setOpacity(gradient.getOpacity());
        }

        setFGColor(gradient.getFgColor(), false);
        setBGColor(gradient.getBgColor(), false);

        ignoreRegenerate = false;
    }

    @Override
    public boolean allowOnlyDrawables() {
        return true;
    }

    @Override
    public Icon createIcon() {
        return new GradientToolIcon();
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

    private static class GradientToolIcon extends Tool.ToolIcon {
        @Override
        public void paintIcon(Graphics2D g) {
            Paint gradient = new GradientPaint(0, 0, Color.BLACK,
                ToolButton.TOOL_ICON_SIZE, 0, Color.WHITE);
            g.setPaint(gradient);
            g.fillRect(0, 0, ToolButton.TOOL_ICON_SIZE, ToolButton.TOOL_ICON_SIZE);
        }
    }
}
