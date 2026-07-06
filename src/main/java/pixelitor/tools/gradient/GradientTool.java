/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.Views;
import pixelitor.colors.FgBgColors;
import pixelitor.filters.gui.UserPreset;
import pixelitor.gui.BlendingModePanel;
import pixelitor.gui.GUIText;
import pixelitor.gui.View;
import pixelitor.gui.utils.DropDownSlider;
import pixelitor.history.History;
import pixelitor.layers.Drawable;
import pixelitor.layers.GradientFillLayer;
import pixelitor.layers.Layer;
import pixelitor.menus.DrawableAction;
import pixelitor.tools.DragTool;
import pixelitor.tools.ToolIcons;
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
import java.util.ResourceBundle;
import java.util.function.Consumer;

import static pixelitor.colors.FgBgColors.setBgColor;
import static pixelitor.colors.FgBgColors.setFgColor;
import static pixelitor.tools.DragToolState.*;
import static pixelitor.tools.util.DraggablePoint.activePoint;

/**
 * The gradient tool.
 */
public class GradientTool extends DragTool {
    private static final String CYCLE_NONE = "No Cycle";
    private static final String CYCLE_REFLECT = "Reflect";
    private static final String CYCLE_REPEAT = "Repeat";
    public static final String[] CYCLE_METHODS = {
        CYCLE_NONE, CYCLE_REFLECT, CYCLE_REPEAT};

    private JComboBox<GradientColorType> colorTypeCB;
    private JComboBox<GradientType> typeCB;
    private JComboBox<GradientCycling> cycleMethodCB;
    private JCheckBox reverseCB;
    private BlendingModePanel blendingModePanel;

    private GradientHandles handles;

    // the last applied gradient for the current set of handles
    private Gradient lastGradient;

    private boolean skipRegeneration = false;

    private GradientFillLayer gradientLayer;

    public GradientTool() {
        super("Gradient", 'G',
            "<b>click</b> and <b>drag</b> to draw a gradient, " +
                "<b>Shift-drag</b> to constrain the direction. " +
                "Press <b>Esc</b> or <b>click</b> outside to hide the handles.",
            Cursors.DEFAULT, true);
        repositionOnSpace = true;
        pixelSnapping = true;
    }

    @Override
    public void initSettingsPanel(ResourceBundle resources) {
        addTypeSelector();
        addCycleMethodSelector();

        settingsPanel.addSeparator();
        addColorTypeSelector();
        addReverseCheckBox();

        settingsPanel.addSeparator();
        addBlendingModePanel();
    }

    private void addTypeSelector() {
        typeCB = new JComboBox<>(GradientType.values());
        typeCB.addActionListener(_ ->
            regenerateGradient("Change Gradient Type"));
        settingsPanel.addComboBox(GUIText.TYPE + ": ", typeCB, "typeCB");
    }

    private void addCycleMethodSelector() {
        cycleMethodCB = new JComboBox<>(GradientCycling.values());
        cycleMethodCB.addActionListener(_ ->
            regenerateGradient("Change Gradient Cycling"));
        settingsPanel.addComboBox("Cycling: ", cycleMethodCB, "cycleMethodCB");
    }

    private void addColorTypeSelector() {
        colorTypeCB = new JComboBox<>(GradientColorType.values());
        colorTypeCB.addActionListener(_ ->
            regenerateGradient("Change Gradient Colors"));
        settingsPanel.addComboBox("Color: ", colorTypeCB, "colorTypeCB");
    }

    private void addReverseCheckBox() {
        reverseCB = new JCheckBox();
        reverseCB.addActionListener(_ ->
            regenerateGradient("Reverse Gradient"));
        settingsPanel.addWithLabel("Reverse: ", reverseCB, "reverseCB");
    }

    private void addBlendingModePanel() {
        blendingModePanel = new BlendingModePanel(true);
        blendingModePanel.addActionListener(this::blendingModePanelChanged);
        settingsPanel.add(blendingModePanel);
    }

    private void blendingModePanelChanged(ActionEvent e) {
        String editName;
        if (e.getSource() instanceof DropDownSlider) {
            editName = "Change Gradient Opacity";
        } else {
            editName = "Change Gradient Blending Mode";
        }
        regenerateGradient(editName);
    }

    private void regenerateGradient(String editName) {
        if (skipRegeneration || handles == null) {
            return;
        }

        Drag renderedDrag = handles.toDrag();
        if (renderedDrag.isImClick()) {
            return; // no change if handles are coincident
        }

        // create the potential new gradient based on current settings
        Gradient newGradient = createGradient(renderedDrag);

        // if the new gradient is identical to the last one, do nothing
        if (newGradient.equals(lastGradient)) {
            return;
        }

        if (isEditingGradientLayer()) {
            gradientLayer.setGradient(newGradient, true);
            lastGradient = newGradient;
        } else {
            DrawableAction.run(editName,
                dr -> drawGradient(dr, newGradient, true, editName));
        }
    }

    private boolean isEditingGradientLayer() {
        return gradientLayer != null;
    }

    @Override
    protected void dragStarted(PMouseEvent e) {
        state = AFTER_FIRST_MOUSE_PRESS;
        if (handles == null) {
            return;
        }
        double x = e.getCoX();
        double y = e.getCoY();
        DraggablePoint hit = handles.findHandleAt(x, y);
        if (hit != null) {
            hit.setActive(true);
            hit.mousePressed(e);
        }
        e.repaint();
        assert state.checkInvariants(this);
    }

    @Override
    protected void ongoingDrag(PMouseEvent e) {
        // the gradient will be drawn only when the mouse is released
        if (activePoint != null) { // a handle is being dragged
            state = TRANSFORM;
            double x = e.getCoX();
            double y = e.getCoY();
            activePoint.mouseDragged(x, y, e.isShiftDown());
        } else {
            // if a new gradient is created from scratch, hide the old handles
            state = INITIAL_DRAG;
            handles = null;
        }
        e.repaint();
        assert state.checkInvariants(this);
    }

    @Override
    protected void dragFinished(PMouseEvent e) {
        Composition comp = e.getComp();
        checkActiveLayer(comp);
        if (drag.isClick()) {
            if (activePoint == null) {
                // clicked outside the handles
                hideHandles(comp, true); // sets state to IDLE
            } else {
                state = TRANSFORM; // user just clicked a handle
            }
            assert state.checkInvariants(this);
            return;
        }

        Drag renderedDrag;
        if (activePoint != null) { // a handle was dragged
            assert handles != null;
            state = TRANSFORM;

            activePoint.mouseReleased(e);
            if (!activePoint.isHitBy(e)) {
                // we can get here if the handle has a
                // constrained position
                DraggablePoint.clearActivePoint();
            }

            renderedDrag = handles.toDrag();
            if (renderedDrag.isImClick()) {
                assert state.checkInvariants(this);
                return;
            }
        } else { // the initial drag just ended
            renderedDrag = drag;
            View view = e.getView();
            handles = new GradientHandles(drag.getStart(view), drag.getEnd(view), view);
            state = TRANSFORM; // handles are now visible
        }

        commitGradient(renderedDrag, comp, null);
        assert state.checkInvariants(this);
    }

    /**
     * Applies the gradient to the appropriate target (gradient
     * fill layer or drawable) and handles history.
     */
    private void commitGradient(Drag renderedDrag, Composition comp, String editName) {
        Gradient newGradient = createGradient(renderedDrag);

        if (isEditingGradientLayer()) {
            gradientLayer.setGradient(newGradient, true);
            lastGradient = newGradient;
        } else {
            Drawable dr = comp.getActiveDrawable();
            if (dr != null) {
                drawGradient(dr, newGradient, true, editName);
            } else {
                throw new IllegalStateException();
            }
        }
    }

    // manages the "hover" effect for gradient handles
    @Override
    public void mouseMoved(MouseEvent e, View view) {
        if (handles == null) {
            // there's no gradient currently being edited with visible handles
            return;
        }

        DraggablePoint handle = handles.findHandleAt(e.getX(), e.getY());
        if (handle != null) {
            // if one of the handles (start, end, or center
            // point of the gradient) is currently under the
            // mouse cursor, then activate it...
            handle.setActive(true);
            view.repaint();
        } else {
            // ...otherwise deactivate any previously active handle
            if (activePoint != null) {
                DraggablePoint.clearActivePoint();
                view.repaint();
            }
        }
    }

    // TODO investigate why re-checking the active layer is needed
    //  on mouse release - a gradient layer can seemingly become
    //  active without notifying the tool
    private void checkActiveLayer(Composition comp) {
        Layer layer = comp.getActiveLayer();
        if (layer instanceof GradientFillLayer gfl) {
            gradientLayer = gfl;
        } else {
            gradientLayer = null;
        }
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
    public void imCoordsChanged(AffineTransform at, View view) {
        if (handles != null) {
            handles.imCoordsChanged(at, view);
        }
    }

    @Override
    public void reset() {
        handles = null;
        DraggablePoint.clearActivePoint();
        lastGradient = null;
        gradientLayer = null;
        state = IDLE;
        assert state.checkInvariants(this);
        Views.repaintActive();
    }

    @Override
    public void compReplaced(Composition newComp, boolean reloaded) {
        if (reloaded) {
            hideHandles(newComp, false);
        }
        layerActivated(newComp.getActiveLayer());
        assert state.checkInvariants(this);
    }

    @Override
    public void editingTargetChanged(Layer activeLayer, boolean toolActivation) {
        layerActivated(activeLayer);
        assert state.checkInvariants(this);
    }

    @Override
    public void forceFinish() {
        hideHandles(Views.getActiveComp(), false);
        assert state.checkInvariants(this);
    }

    @Override
    public void escPressed() {
        hideHandles(Views.getActiveComp(), true);
        assert state.checkInvariants(this);
    }

    private void hideHandles(Composition comp, boolean addToHistory) {
        if (handles == null || isEditingGradientLayer()) {
            return;
        }

        if (addToHistory) {
            History.add(new GradientHandlesHiddenEdit(comp, lastGradient));
        }

        hideHandlesInternal(comp);
    }

    private void hideHandlesInternal(Composition comp) {
        handles = null;
        DraggablePoint.clearActivePoint();
        lastGradient = null;
        state = IDLE;
        comp.repaint();
    }

    public boolean hasHandles() {
        return handles != null;
    }

    @Override
    public boolean arrowKeyPressed(ArrowKey key) {
        if (handles == null) {
            return false;
        }

        View view = Views.getActive();
        assert view != null;

        handles.arrowKeyPressed(key, view);
        Drag handleDrag = handles.toDrag(); // drag after arrow key nudge

        Composition comp = view.getComp();
        String editName = key.isShiftDown() ? "Shift-nudge Gradient" : "Nudge Gradient";
        commitGradient(handleDrag, comp, editName);

        assert state.checkInvariants(this);
        return true;
    }

    private GradientType getType() {
        return (GradientType) typeCB.getSelectedItem();
    }

    private CycleMethod getCycleMethod() {
        return ((GradientCycling) cycleMethodCB.getSelectedItem()).getCycleMethod();
    }

    private GradientColorType getGradientColorType() {
        return (GradientColorType) colorTypeCB.getSelectedItem();
    }

    private boolean isReversed() {
        return reverseCB.isSelected();
    }

    private void drawGradient(Drawable dr, Gradient gradient,
                              boolean addToHistory, String editName) {
        if (addToHistory) {
            // check if this is the first gradient application since handles were created/shown
            boolean isFirst = lastGradient == null;
            if (isFirst) {
                History.add(new NewGradientEdit(dr, gradient));
            } else {
                History.add(new GradientChangeEdit(editName, dr, lastGradient, gradient));
            }
        }

        gradient.paintOn(dr);
        dr.update();
        lastGradient = gradient;
    }

    private Gradient createGradient(Drag drag) {
        return new Gradient(drag,
            getType(), getCycleMethod(), getGradientColorType(), isReversed(),
            blendingModePanel.getBlendingMode(),
            blendingModePanel.getOpacity());
    }

    @Override
    public void paintOverCanvas(Graphics2D g2, Composition comp) {
        if (!comp.getActiveLayer().isVisible()) {
            return;
        }

        // the superclass draws the measurement overlay
        super.paintOverCanvas(g2, comp);

        if (handles != null) {
            handles.paint(g2);
        } else {
            if (drag != null && drag.isDragging()) {
                // during the first drag, when there are no handles yet,
                // paint only the arrow
                drag.drawCoDirectionArrow(g2);
            }
        }
    }

    @Override
    protected OverlayType getOverlayType() {
        if (handles == null) {
            // for the initial drag, show angle and distance
            return OverlayType.ANGLE_DIST;
        }
        // the handles paint their measurement overlay separately,
        // based on their positions, and not on the user drag
        return OverlayType.NONE;
    }

    @Override
    protected void toolDeactivated(View view) {
        super.toolDeactivated(view);

        reset();
    }

    private void layerActivated(Layer layer) {
        if (layer.isMaskEditing()) {
            maskEditingChanged(true);
            gradientLayer = null;
        } else {
            if (layer instanceof GradientFillLayer gfl) {
                if (gfl == gradientLayer) {
                    return; // not a new layer
                }
                gradientLayer = gfl;
                blendingModePanel.setEnabled(false);
                updateFrom(gfl);
            } else {
                gradientLayer = null;
                hideHandles(layer.getComp(), false);
                blendingModePanel.setEnabled(true);
            }
        }
    }

    @Override
    public void maskEditingChanged(boolean editing) {
        blendingModePanel.setEnabled(!editing);
    }

    // called only by history edits
    public void restoreGradient(Gradient gradient, boolean regenerate, Drawable dr) {
        Composition comp = dr.getComp();
        if (gradient == null) {
            hideHandles(comp, false);
            return;
        }

        View view = comp.getView();
        loadSettingsFromGradient(gradient, view);
        if (regenerate) {
            Drag dragFromHandles = handles.toDrag();
            drawGradient(dr, createGradient(dragFromHandles), false, null);
        }

        lastGradient = gradient;
        view.repaint();
    }

    // called only when editing a gradient layer
    public void updateFrom(GradientFillLayer gfl) {
        Gradient gradient = gfl.getGradient();
        Composition comp = gfl.getComp();
        if (gradient != null) {
            loadSettingsFromGradient(gradient, comp.getView());
            lastGradient = gradient;

            // make the loaded handles visible
            comp.repaint();
        } else {
            // can get here when undoing the first gradient
            hideHandlesInternal(comp);
        }
    }

    private void loadSettingsFromGradient(Gradient gradient, View view) {
        skipRegeneration = true;
        handles = gradient.createHandles(view);
        state = TRANSFORM; // handles were loaded and are active

        colorTypeCB.setSelectedItem(gradient.getColorType());
        typeCB.setSelectedItem(gradient.getType());
        cycleMethodCB.setSelectedItem(GradientCycling.from(gradient.getCycleMethod()));
        reverseCB.setSelected(gradient.isReversed());
        if (blendingModePanel.isEnabled()) {
            blendingModePanel.setBlendingMode(gradient.getBlendingMode(), null);
            blendingModePanel.setOpacity(gradient.getOpacity());
        }

        setFgColor(gradient.getFgColor(), false);
        setBgColor(gradient.getBgColor(), false);

        skipRegeneration = false;
    }

    @Override
    public boolean requiresDrawables() {
        return true;
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        preset.put(GradientType.PRESET_KEY, getType().name());
        preset.put(GradientCycling.PRESET_KEY, ((GradientCycling) cycleMethodCB.getSelectedItem()).name());
        preset.put(GradientColorType.PRESET_KEY, getGradientColorType().name());
        preset.putBoolean("Reverse", isReversed());
        blendingModePanel.saveStateTo(preset);
        FgBgColors.saveStateTo(preset);
    }

    @Override
    public void loadUserPreset(UserPreset preset) {
        skipRegeneration = true;

        typeCB.setSelectedItem(preset.getEnum(GradientType.PRESET_KEY, GradientType.class));
        cycleMethodCB.setSelectedItem(GradientCycling.fromPresetString(preset.get(GradientCycling.PRESET_KEY)));
        colorTypeCB.setSelectedItem(preset.getEnum(GradientColorType.PRESET_KEY, GradientColorType.class));
        reverseCB.setSelected(preset.getBoolean("Reverse"));
        blendingModePanel.loadStateFrom(preset);
        FgBgColors.loadStateFrom(preset, false);

        skipRegeneration = false;
        regenerateGradient("Load Preset");
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = super.createDebugNode(key);

        node.addAsString("type", getType());
        node.addAsString("cycling", getCycleMethod());
        node.addAsQuotedString("color", getGradientColorType());
        node.addBoolean("reversed", isReversed());
        node.addFloat("opacity", blendingModePanel.getOpacity());
        node.addAsQuotedString("blending mode", blendingModePanel.getBlendingMode());
        node.addNullableDebuggable("handles", handles);
        node.addNullableDebuggable("last gradient", lastGradient);

        return node;
    }

    @Override
    public Consumer<Graphics2D> createIconPainter() {
        return ToolIcons::paintGradientIcon;
    }
}
