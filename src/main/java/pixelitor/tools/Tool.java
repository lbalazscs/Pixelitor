/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.Composition;
import pixelitor.GUIMode;
import pixelitor.Views;
import pixelitor.filters.gui.PresetOwner;
import pixelitor.gui.GlobalEvents;
import pixelitor.gui.View;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.gui.utils.Themes;
import pixelitor.gui.utils.VectorIcon;
import pixelitor.layers.Layer;
import pixelitor.tools.gui.ToolButton;
import pixelitor.tools.gui.ToolSettingsPanel;
import pixelitor.tools.toolhandlers.ToolHandlerChain;
import pixelitor.tools.util.ArrowKey;
import pixelitor.tools.util.DraggablePoint;
import pixelitor.tools.util.KeyListener;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.debug.DebugNode;
import pixelitor.utils.debug.Debuggable;

import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;

import static java.awt.Color.BLACK;

/**
 * An abstract superclass for all tools.
 *
 * A tool defines the interaction between the
 * mouse and key events and a {@link Composition}
 */
public abstract class Tool implements KeyListener, PresetOwner, Debuggable {
    private final String name;
    private final String shortName;
    private final String toolMessage;
    private final char activationKey;
    final ToolHandlerChain handlerChain;
    private final Cursor cursor;

    private ToolButton toolButton;
    protected ToolSettingsPanel settingsPanel;

    // holding the alt key down generates continuously
    // altPressed calls, but only the first one is relevant
    protected boolean altDown = false;

    // Whether pixel snapping should be turned on as far as the tool is concerned.
    // The actual snapping also depends on the setting in the Preferences.
    protected boolean pixelSnapping = false;

    protected Tool(String name, char activationKey, String toolMessage, Cursor cursor) {
        this.activationKey = activationKey;
        assert Character.isUpperCase(activationKey);

        this.shortName = name;
        this.name = name + " Tool";
        this.toolMessage = toolMessage;
        this.cursor = cursor;

        handlerChain = new ToolHandlerChain(this, cursor);
    }

    public boolean hasHandToolForwarding() {
        // all tools behave like the hand tool if space is pressed,
        // but the hand tool itself doesn't need the forwarding
        return true;
    }

    public boolean allowOnlyDrawables() {
        return false;
    }

    public boolean hasColorPickerForwarding() {
        return false;
    }

    public String getStatusBarMessage() {
        return name + ": " + toolMessage;
    }

    public void mouseClicked(PMouseEvent e) {
        // empty by default
    }

    public void setButton(ToolButton toolButton) {
        this.toolButton = toolButton;
    }

    public abstract void initSettingsPanel();

    public String getName() {
        return name;
    }

    public String getShortName() {
        return shortName;
    }

    /**
     * Creates the tool's icon. Tools create their icon using
     * vector graphics so that is looks good on HiDPI screens.
     */
    public abstract VectorIcon createIcon();

    public char getActivationKey() {
        return activationKey;
    }

    public boolean hasPixelSnapping() {
        return pixelSnapping;
    }

    protected void toolStarted() {
        GlobalEvents.setKeyListener(this);
        Views.setCursorForAll(cursor);
        View.toolSnappingChanged(pixelSnapping, this == Tools.CROP);
    }

    protected void toolEnded() {
        DraggablePoint.activePoint = null;
        closeToolDialogs();
    }

    protected void closeToolDialogs() {
        // empty by default
    }

    public Cursor getStartingCursor() {
        return cursor;
    }

    /**
     * Paint over the active layer, used only by the shapes tool.
     * The transform of the given Graphics2D is in image space.
     */
    public void paintOverActiveLayer(Graphics2D g) {
        // empty by default
    }

    /**
     * Paint on the {@link Composition} after all the layers have been painted.
     * The transform of the given Graphics2D is in component space.
     */
    public void paintOverImage(Graphics2D g2, Composition comp) {
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

    public void setSettingsPanel(ToolSettingsPanel settingsPanel) {
        this.settingsPanel = settingsPanel;
    }

    public boolean hasSettingsPanel() {
        return settingsPanel != null;
    }

    public void randomize() {
        GUIUtils.randomizeChildren(settingsPanel);
    }

    @Override
    public void spacePressed() {
        handlerChain.spacePressed();
    }

    @Override
    public void spaceReleased() {
        handlerChain.spaceReleased();
    }

    /**
     * Returns true if the key event was used for something
     */
    @Override
    public boolean arrowKeyPressed(ArrowKey key) {
        return false; // not consumed
    }

    @Override
    public void escPressed() {
        // empty by default
    }

    @Override
    public void altPressed() {
        if (!altDown && hasColorPickerForwarding()) {
            Views.setCursorForAll(
                Tools.COLOR_PICKER.getStartingCursor());
        }
        altDown = true;
    }

    @Override
    public void altReleased() {
        if (hasColorPickerForwarding()) {
            Views.setCursorForAll(cursor);
        }
        altDown = false;
    }

    @Override
    public void otherKeyPressed(KeyEvent e) {
        // empty by default
    }

    public void allViewsClosed() {
        resetInitialState();
    }

    public void viewActivated(View oldCV, View newCV) {
        assert Tools.currentTool == this;
        if (oldCV != null) {
            oldCV.repaint();
            resetInitialState();
        }
    }

    public void compReplaced(Composition newComp, boolean reloaded) {
        // empty by default
    }

    public void activeLayerChanged(Layer layer) {
        // empty by default
    }

    /**
     * A modal dialog, such as a filter or the color selector, was shown.
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

    public void resetInitialState() {
        // empty by default
    }

    public void fgBgColorsChanged() {
        // empty by default
    }

    /**
     * Called when a layer mask is activated or deactivated
     */
    public void setupMaskEditing(boolean maskEditing) {
        // empty by default
    }

    /**
     * Called when the component space coordinates of the pixels changed,
     * but the image coordinates are still the same (zooming, view resizing).
     *
     * The component coordinates of the widgets must be restored
     * from their image coordinates
     */
    public void coCoordsChanged(View view) {
        // empty by default
    }

    /**
     * Called when the image space coordinates of the pixels changed
     * (image resizing, cropping, canvas enlargement, flipping, etc.),
     * and this change is described by the given {@link AffineTransform}
     *
     * The change in image coords implies a change in component coords,
     * therefore the component space coordinates also have to be recalculated.
     */
    public void imCoordsChanged(AffineTransform at, View view) {
        // empty by default
    }

    public void activate() {
        if (toolButton != null) {
            // this will also call Tools.start() indirectly via the event handlers.
            toolButton.setSelected(true);
            toolButton.requestFocus();
        } else {
            assert GUIMode.isUnitTesting();
            Tools.start(this);
        }
    }

    public boolean isActive() {
        return Tools.currentIs(this);
    }

    /**
     * Returns true if this tool is currently not affecting the
     * composite image in ways other than directly drawing
     * into the pixels of the actual image layer.
     */
    public boolean isDirectDrawing() {
        return true;
    }

    @Override
    public boolean canHaveUserPresets() {
        return true;
    }

    @Override
    public String getPresetDirName() {
        return getName();
    }

    // used for debugging
    public String getStateInfo() {
        return null;
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode toolNode = new DebugNode(key, this);
        toolNode.addString("name", getName());
        return toolNode;
    }

    @Override
    public String toString() {
        return shortName; // so that they can be selected from a JComboBox
    }

    protected abstract static class ToolIcon extends VectorIcon {
        protected ToolIcon() {
            super(Themes.getCurrent().isDark() ? Themes.LIGHT_ICON_COLOR : BLACK,
                ToolButton.TOOL_ICON_SIZE, ToolButton.TOOL_ICON_SIZE);
        }
    }
}
