/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.gui.utils.Themes;
import pixelitor.gui.utils.VectorIcon;
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
import java.util.ResourceBundle;

import static java.awt.MultipleGradientPaint.CycleMethod.NO_CYCLE;
import static java.awt.MultipleGradientPaint.CycleMethod.REFLECT;
import static java.awt.MultipleGradientPaint.CycleMethod.REPEAT;
import static pixelitor.colors.FgBgColors.setBGColor;
import static pixelitor.colors.FgBgColors.setFGColor;
import static pixelitor.tools.DragToolState.AFTER_FIRST_MOUSE_PRESS;
import static pixelitor.tools.DragToolState.IDLE;
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
    private JCheckBox reverseCB;
    private BlendingModePanel blendingModePanel;

    private GradientHandles handles;
    private Gradient lastGradient;
    private boolean ignoreRegenerate = false;

    private GradientFillLayer gradientLayer;

    public GradientTool() {
        super("Gradient", 'G',
            "<b>click</b> and <b>drag</b> to draw a gradient, " +
                "<b>Shift-drag</b> to constrain the direction. " +
                "Press <b>Esc</b> or <b>click</b> outside to hide the handles.",
            Cursors.DEFAULT, true);
        spaceDragStartPoint = true;
        state = IDLE;
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
        typeCB.addActionListener(e ->
            regenerateGradient("Change Gradient Type"));
        settingsPanel.addComboBox(GUIText.TYPE + ": ", typeCB, "typeCB");
    }

    private void addCycleMethodSelector() {
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

    private void addReverseCheckBox() {
        reverseCB = new JCheckBox();
        reverseCB.addActionListener(e ->
            regenerateGradient("Change Gradient Direction"));
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
        if (ignoreRegenerate) {
            return;
        }
        if (handles == null) {
            return;
        }

        if (isEditingGradientLayer()) {
            View view = Views.getActive();
            if (view != null) {
                Drag renderedDrag = handles.toDrag(view);
                if (!renderedDrag.isImClick()) {
                    gradientLayer.setGradient(createGradient(renderedDrag), true);
                }
            }
        } else {
            DrawableAction.run(editName,
                dr -> regenerateOnDrawable(dr, true, editName));
        }
    }

    private boolean isEditingGradientLayer() {
        return gradientLayer != null;
    }

    private void regenerateOnDrawable(Drawable dr,
                                      boolean addToHistory, String editName) {
        // regenerate the gradient if a tool setting
        // was changed while handles are present
        if (handles == null) {
            return;
        }
        View view = Views.getActive();
        if (view != null) {
            Drag renderedDrag = handles.toDrag(view);
            if (!renderedDrag.isImClick()) {
                drawGradient(dr, renderedDrag, addToHistory, editName);
            }
        }
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
            hit.mousePressed(x, y);
        }
        e.repaint();
    }

    @Override
    protected void ongoingDrag(PMouseEvent e) {
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
    protected void dragFinished(PMouseEvent e) {
        Composition comp = e.getComp();
        checkActiveLayer(comp);
        if (drag.isClick()) {
            if (activePoint == null) {
                // clicked outside the handles
                hideHandles(comp, true);
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
            View view = e.getView();
            handles = new GradientHandles(drag.getStart(view), drag.getEnd(view), view);
        }

        if (isEditingGradientLayer()) {
            gradientLayer.setGradient(createGradient(renderedDrag), true);
        } else {
            Drawable dr = comp.getActiveDrawable();
            if (dr != null) {
                drawGradient(dr, renderedDrag, true, null);
            } else {
                throw new IllegalStateException();
            }
        }
    }

    @Override
    public void mouseMoved(MouseEvent e, View view) {
        if (handles == null) {
            // this method only highlights the handle under the mouse
            return;
        }

        DraggablePoint handle = handles.findHandleAt(e.getX(), e.getY());
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

    // TODO for some reason it is necessary to check the active
    //   layer again when the mouse is released - somehow a gradient
    //   layer can be activated without the tool noticing it.
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
    public void resetInitialState() {
        handles = null;
        activePoint = null;
        lastGradient = null;
        gradientLayer = null;
        Views.repaintActive();
    }

    @Override
    public void compReplaced(Composition newComp, boolean reloaded) {
        if (reloaded && handles != null) {
            hideHandles(newComp, false);
        }
        layerActivated(newComp.getActiveLayer());
    }

    @Override
    public void editingTargetChanged(Layer activeLayer) {
        layerActivated(activeLayer);
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
            hideHandles(Views.getActiveComp(), addHistory);
        }
    }

    private void hideHandles(Composition comp, boolean addHistory) {
        if (isEditingGradientLayer()) {
            return;
        }

        if (addHistory) {
            History.add(new GradientHandlesHiddenEdit(comp, lastGradient));
        }

        hideHandlesUnchecked(comp);
    }

    private void hideHandlesUnchecked(Composition comp) {
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
        View view = Views.getActive();
        assert view != null;

        handles.arrowKeyPressed(key, view);
        Drag handleDrag = handles.toDrag(view);

        if (isEditingGradientLayer()) {
            gradientLayer.setGradient(createGradient(handleDrag), true);
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

    private GradientType getType() {
        return (GradientType) typeCB.getSelectedItem();
    }

    private CycleMethod getCycleType() {
        return cycleMethodFromString((String) cycleMethodCB.getSelectedItem());
    }

    private GradientColorType getGradientColorType() {
        return (GradientColorType) colorTypeCB.getSelectedItem();
    }

    private boolean isReversed() {
        return reverseCB.isSelected();
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

        gradient.paintOn(dr);
        dr.update();
        lastGradient = gradient;
    }

    private Gradient createGradient(Drag drag) {
        return new Gradient(drag,
            getType(), getCycleType(), getGradientColorType(), isReversed(),
            blendingModePanel.getBlendingMode(),
            blendingModePanel.getOpacity());
    }

    @Override
    public void paintOverImage(Graphics2D g2, Composition comp) {
        if (!comp.getActiveLayer().isVisible()) {
            return;
        }

        // the superclass draws the drag display
        super.paintOverImage(g2, comp);

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
    protected DragDisplayType getDragDisplayType() {
        if (handles == null) {
            return DragDisplayType.ANGLE_DIST;
        }
        // The handles have to paint their drag display separately,
        // based on the handle positions, and not on the user drag
        return DragDisplayType.NONE;
    }

    // cycle methods can't be put directly in the JComboBox,
    // because they would be all uppercase
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
    protected void toolActivated() {
        super.toolActivated();

        Layer activeLayer = Views.getActiveLayer();
        if (activeLayer != null) {
            layerActivated(activeLayer);
        }
    }

    @Override
    protected void toolDeactivated() {
        super.toolDeactivated();

        resetInitialState();
    }

    private void layerActivated(Layer layer) {
        if (layer.isMaskEditing()) {
            setupMaskEditing(true);
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
                if (handles != null) {
                    hideHandles(layer.getComp(), false);
                }
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

    // called only when editing a gradient layer
    public void updateFrom(GradientFillLayer gfl) {
        Gradient gradient = gfl.getGradient();
        Composition comp = gfl.getComp();
        if (gradient != null) {
            loadSettingsFromGradient(gradient, comp.getView());

            // make the loaded handles visible
            comp.repaint();
        } else {
            // can get here when undoing the first gradient
            hideHandlesUnchecked(comp);
        }
    }

    private void loadSettingsFromGradient(Gradient gradient, View view) {
        ignoreRegenerate = true;

        handles = gradient.createHandles(view);
        colorTypeCB.setSelectedItem(gradient.getColorType());
        typeCB.setSelectedItem(gradient.getType());
        cycleMethodCB.setSelectedItem(cycleMethodToString(gradient.getCycleMethod()));
        reverseCB.setSelected(gradient.isReversed());
        if (blendingModePanel.isEnabled()) {
            blendingModePanel.setBlendingMode(gradient.getBlendingMode(), null);
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
    public void saveStateTo(UserPreset preset) {
        preset.put("Type", getType().toString());
        preset.put("Cycling", (String) cycleMethodCB.getSelectedItem());
        preset.put("Color", getGradientColorType().toString());
        preset.putBoolean("Reverse", isReversed());
        blendingModePanel.saveStateTo(preset);
        FgBgColors.saveStateTo(preset);
    }

    @Override
    public void loadUserPreset(UserPreset preset) {
        ignoreRegenerate = true;

        typeCB.setSelectedItem(preset.getEnum("Type", GradientType.class));
        cycleMethodCB.setSelectedItem(preset.get("Cycling"));
        colorTypeCB.setSelectedItem(preset.getEnum("Color", GradientColorType.class));
        reverseCB.setSelected(preset.getBoolean("Reverse"));
        blendingModePanel.loadStateFrom(preset);
        FgBgColors.loadStateFrom(preset);

        ignoreRegenerate = false;
        regenerateGradient("Load Preset");
    }

    @Override
    public VectorIcon createIcon() {
        return new GradientToolIcon();
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = super.createDebugNode(key);

        node.addAsString("type", getType());
        node.addAsString("cycling", getCycleType());
        node.addAsQuotedString("color", getGradientColorType());
        node.addBoolean("reversed", isReversed());
        node.addFloat("opacity", blendingModePanel.getOpacity());
        node.addAsQuotedString("blending mode", blendingModePanel.getBlendingMode());
        node.addNullableDebuggable("handles", handles);
        node.addNullableDebuggable("last gradient", lastGradient);

        return node;
    }

    private static class GradientToolIcon extends Tool.ToolIcon {
        @Override
        public void paintIcon(Graphics2D g) {
            Color startColor = Color.BLACK;
            Color endColor = Themes.getActive().isDark() ? color : Color.WHITE;
            Paint gradient = new GradientPaint(0, 0, startColor,
                ToolButton.ICON_SIZE, 0, endColor);
            g.setPaint(gradient);
            g.fillRect(0, 0, ToolButton.ICON_SIZE, ToolButton.ICON_SIZE);
        }
    }
}
