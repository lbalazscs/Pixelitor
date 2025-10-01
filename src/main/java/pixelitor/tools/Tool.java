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

package pixelitor.tools;

import pixelitor.AppMode;
import pixelitor.Composition;
import pixelitor.Views;
import pixelitor.filters.gui.PresetOwner;
import pixelitor.gui.GlobalEvents;
import pixelitor.gui.View;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.layers.Drawable;
import pixelitor.layers.Layer;
import pixelitor.tools.gui.ToolButton;
import pixelitor.tools.gui.ToolSettingsPanel;
import pixelitor.tools.toolhandlers.ToolHandlerChain;
import pixelitor.tools.util.ArrowKey;
import pixelitor.tools.util.DraggablePoint;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.debug.DebugNode;
import pixelitor.utils.debug.Debuggable;

import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Abstract base class for all tools.
 *
 * A tool defines how mouse and keyboard interactions
 * affect a {@link Composition}.
 */
public abstract class Tool implements PresetOwner, Debuggable {
    private final String name;
    private final String shortName;
    private final String toolMessage;
    private final char hotkey;
    private final Cursor cursor;

    // a chain of handlers that can intercept
    // and modify the tool's default behavior
    final ToolHandlerChain eventHandlerChain;

    private ToolButton toolButton;
    protected ToolSettingsPanel settingsPanel;

    // holding the alt key down generates continuously
    // altPressed calls, but only the first one is relevant
    protected boolean altDown = false;

    // Whether pixel snapping should be turned on as far as the tool is concerned.
    // The actual snapping also depends on the Preferences setting.
    protected boolean pixelSnapping = false;

    protected Tool(String name, char hotkey, String toolMessage, Cursor cursor) {
        this.hotkey = hotkey;
        assert Character.isUpperCase(hotkey);

        this.shortName = name;
        this.name = name + " Tool";
        this.toolMessage = toolMessage;
        this.cursor = cursor;

        eventHandlerChain = new ToolHandlerChain(this, cursor);
    }

    /**
     * Initializes the tool's settings panel.
     */
    public abstract void initSettingsPanel(ResourceBundle resources);

    /**
     * A hook for actions to be performed when the tool is activated.
     */
    protected void toolActivated(View view) {
        GlobalEvents.setActiveTool(this);
        Views.setCursorForAll(cursor);
        View.toolSnappingChanged(pixelSnapping, this == Tools.CROP);
    }

    /**
     * A hook for actions to be performed when the tool is deactivated.
     */
    protected void toolDeactivated(View view) {
        DraggablePoint.activePoint = null;
        closeAllDialogs();
    }

    /**
     * Programmatically activates the tool.
     */
    public void activate() {
        if (toolButton != null) {
            // this will also call Tools.start() indirectly via the event handlers.
            toolButton.setSelected(true);
            toolButton.requestFocus();
        } else {
            // if there is no GUI, then start the tool explicitly
            assert AppMode.isUnitTesting();
            Tools.start(this);
        }
    }

    public boolean isActive() {
        return Tools.activeIs(this);
    }

    /**
     * Closes all tool dialogs.
     */
    protected void closeAllDialogs() {
        // empty by default
    }

    /**
     * Creates the painter for the tool's icon.
     */
    public abstract Consumer<Graphics2D> createIconPainter();

    /**
     * Allows tools to paint additional content over the canvas/composition,
     * after all layers have been painted. Useful for visual feedback
     * that is not directly part of the edited image.
     * This method can paint outside of the canvas bounds.
     * The transform of the given Graphics2D is in component space.
     */
    public void paintOverCanvas(Graphics2D g2, Composition comp) {
        // empty by default
    }

    public void mouseMoved(MouseEvent e, View view) {
        // empty by default
    }

    public void mouseEntered(MouseEvent e, View view) {
        // empty by default
    }

    public void mouseExited(MouseEvent e, View view) {
        // empty by default
    }

    public abstract void mousePressed(PMouseEvent e);

    public abstract void mouseDragged(PMouseEvent e);

    public abstract void mouseReleased(PMouseEvent e);

    public void mouseClicked(PMouseEvent e) {
        // empty by default
    }

    public void randomize() {
        GUIUtils.randomizeChildren(settingsPanel);
    }

    /**
     * Handles the space key being pressed. This typically activates
     * temporary hand tool behavior for tools that support it.
     */
    public void spacePressed() {
        eventHandlerChain.spacePressed();
    }

    /**
     * Handles the space key being released, restoring the tool's
     * normal behavior if temporary hand tool mode was active.
     */
    public void spaceReleased() {
        eventHandlerChain.spaceReleased();
    }

    /**
     * Returns true if the key event was used
     * and it should not be further processed.
     */
    public boolean arrowKeyPressed(ArrowKey key) {
        return false; // not consumed
    }

    public void escPressed() {
        // empty by default
    }

    public void altPressed() {
        if (!altDown && hasColorPickerForwarding()) {
            Views.setCursorForAll(
                Tools.COLOR_PICKER.getStartingCursor());
        }
        altDown = true;
    }

    public void altReleased() {
        if (hasColorPickerForwarding()) {
            Views.setCursorForAll(cursor);
        }
        altDown = false;
    }

    public void otherKeyPressed(KeyEvent e) {
        // empty by default
    }

    public void allViewsClosed() {
        reset();
    }

    public void viewActivated(View oldView, View newView) {
        assert Tools.activeTool == this;
        if (oldView != null) {
            oldView.repaint();
            reset();
        }
    }

    // Called when a composition is replaced in the active view, but
    // not when a composition is activated because of a new active view.
    public void compReplaced(Composition newComp, boolean reloaded) {
        // empty by default
    }

    /**
     * Called when a new layer becomes active, or when the mask editing state changes.
     * The argument is always the active layer, not the mask.
     */
    public void editingTargetChanged(Layer activeLayer) {
        // empty by default
    }

    /**
     * Called before a modal dialog, such as a filter or the color selector, is shown.
     */
    public void firstModalDialogShown() {
        // empty by default
    }

    public void firstModalDialogHidden() {
        // empty by default
    }

    /**
     * Signals that the tool should finish what it started
     */
    public void forceFinish() {
        // empty by default
    }

    /**
     * Resets the tool to its initial/default state.
     */
    public void reset() {
        // empty by default
    }

    public void fgBgColorsChanged() {
        // empty by default
    }

    /**
     * Called when a layer mask is activated or deactivated
     */
    public void maskEditingChanged(boolean maskEditing) {
        // empty by default
    }

    /**
     * Called when the component space coordinates of the pixels change,
     * but the image coordinates remain the same (zooming, view resizing).
     *
     * The component coordinates of the widgets must be restored
     * from their image coordinates.
     */
    public void coCoordsChanged(View view) {
        // empty by default
    }

    /**
     * Called when the image space coordinates of the pixels change
     * (image resizing, cropping, canvas enlargement, flipping, etc.),
     * and this change is described by the given {@link AffineTransform}
     *
     * The change in image coordinates implies a change in component coordinates,
     * therefore the component space coordinates also have to be recalculated.
     */
    public void imCoordsChanged(AffineTransform at, View view) {
        // empty by default
    }

    /**
     * Whether this tool behaves like the hand tool when the space key is pressed.
     */
    public boolean hasHandToolForwarding() {
        // all tools behave like the hand tool if space is pressed,
        // but the hand tool itself doesn't need the forwarding
        return true;
    }

    /**
     * Whether this tool can only be used with layers that implement {@link Drawable}.
     */
    public boolean allowOnlyDrawables() {
        return false;
    }

    /**
     * Whether this tool behaves like the color picker tool when Alt is pressed.
     */
    public boolean hasColorPickerForwarding() {
        return false;
    }

    public String getStatusBarMessage() {
        return name + ": " + toolMessage;
    }

    public void setButton(ToolButton toolButton) {
        this.toolButton = toolButton;
    }

    public String getName() {
        return name;
    }

    public String getShortName() {
        return shortName;
    }

    public char getHotkey() {
        return hotkey;
    }

    public boolean hasPixelSnapping() {
        return pixelSnapping;
    }

    public Cursor getStartingCursor() {
        return cursor;
    }

    public void setSettingsPanel(ToolSettingsPanel settingsPanel) {
        this.settingsPanel = settingsPanel;
    }

    public boolean hasSettingsPanel() {
        return settingsPanel != null;
    }

    /**
     * Returns true if this tool is not currently affecting the
     * composite image in ways other than directly drawing
     * into the pixels of the actual image layer.
     */
    public boolean isDirectDrawing() {
        return true;
    }

    @Override
    public boolean supportsUserPresets() {
        return true;
    }

    @Override
    public String getPresetDirName() {
        return getName();
    }

    // used for debugging
    public String getStateInfo() {
        return "";
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = new DebugNode(key, this);

        node.addQuotedString("name", getName());
        node.addBoolean("altDown", altDown);
        node.addBoolean("pixelSnapping", pixelSnapping);

        return node;
    }

    @Override
    public String toString() {
        return shortName; // so that they can be selected from a JComboBox
    }
}
