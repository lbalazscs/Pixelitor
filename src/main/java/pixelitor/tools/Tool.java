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

import pixelitor.Build;
import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.gui.GlobalKeyboardWatch;
import pixelitor.gui.ImageComponent;
import pixelitor.gui.ImageComponents;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.tools.gui.ToolButton;
import pixelitor.tools.gui.ToolSettingsPanel;
import pixelitor.tools.toolhandlers.ToolHandlerChain;
import pixelitor.tools.util.ArrowKey;
import pixelitor.tools.util.KeyListener;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.debug.DebugNode;

import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;

/**
 * An abstract superclass for all tools.
 *
 * A tool defines the interaction between the user's
 * mouse and key events and an {@link ImageComponent}
 */
public abstract class Tool implements KeyListener {
    private boolean altDown = false;

    private ToolButton toolButton;

    private final String name;
    private final String iconFileName;
    private final String toolMessage;
    private final Cursor cursor;
    private final ClipStrategy clipStrategy;

    private final char activationKeyChar;

    protected ToolSettingsPanel settingsPanel;
    protected boolean ended = false;

    final ToolHandlerChain handlerChain;

    protected Tool(String name, char activationKeyChar, String iconFileName,
                   String toolMessage, Cursor cursor,
                   boolean allowOnlyDrawables, boolean handToolForwarding,
                   ClipStrategy clipStrategy) {
        this.activationKeyChar = activationKeyChar;
        this.name = name;
        this.iconFileName = iconFileName;
        this.toolMessage = toolMessage;
        this.cursor = cursor;
        this.clipStrategy = clipStrategy;

        handlerChain = new ToolHandlerChain(this, cursor,
                allowOnlyDrawables, handToolForwarding);
    }

    public boolean doColorPickerForwarding() {
        return false;
    }

    public String getStatusBarMessage() {
        return "<html>" + name + " Tool: " + toolMessage;
    }

    public void mouseClicked(PMouseEvent e) {
        // empty for the convenience of subclasses
    }

    public void setButton(ToolButton toolButton) {
        this.toolButton = toolButton;
    }

    public ToolButton getButton() {
        return toolButton;
    }

    public abstract void initSettingsPanel();

    public String getName() {
        return name;
    }

    public String getIconFileName() {
        return iconFileName;
    }

    public char getActivationKeyChar() {
        return activationKeyChar;
    }

    protected void toolStarted() {
        ended = false;

        GlobalKeyboardWatch.setKeyListener(this);
        ImageComponents.setCursorForAll(cursor);
    }

    protected void toolEnded() {
        ended = true;

        closeToolDialogs();
    }

    protected void closeToolDialogs() {
        // empty instead of abstract for the convenience of subclasses
    }

    public Cursor getStartingCursor() {
        return cursor;
    }

    public void paintOverLayer(Graphics2D g, Composition comp) {
        // empty instead of abstract for the convenience of subclasses
    }

    /**
     * A possibility to paint temporarily something on the
     * {@link ImageComponent} after all the layers have been painted.
     */
    public void paintOverImage(Graphics2D g2, Canvas canvas,
                               ImageComponent ic,
                               AffineTransform componentTransform,
                               AffineTransform imageTransform) {
        // empty instead of abstract for the convenience of subclasses
    }

    public void mouseMoved(MouseEvent e, ImageComponent ic) {
        // empty instead of abstract for the convenience of subclasses
    }

    public abstract void mousePressed(PMouseEvent e);

    public abstract void mouseDragged(PMouseEvent e);

    public abstract void mouseReleased(PMouseEvent e);

    public void setSettingsPanel(ToolSettingsPanel settingsPanel) {
        this.settingsPanel = settingsPanel;
    }

    public void randomize() {
        GUIUtils.randomizeGUIWidgetsOn(settingsPanel);
    }

    public void setClipFor(Graphics2D g, ImageComponent ic) {
        clipStrategy.setClipFor(g, ic);
    }

    @Override
    public void spacePressed() {
        handlerChain.spacePressed();
    }

    @Override
    public void spaceReleased() {
        handlerChain.spaceReleased();
    }

    @Override
    public boolean arrowKeyPressed(ArrowKey key) {
        // empty for the convenience of subclasses
        return false; // not consumed
    }

    @Override
    public void escPressed() {
        // empty by default
    }

    @Override
    public void shiftPressed() {
        // empty by default
    }

    @Override
    public void shiftReleased() {
        // empty by default
    }

    @Override
    public void altPressed() {
        if (!altDown && doColorPickerForwarding()) {
            ImageComponents.setCursorForAll(
                    Tools.COLOR_PICKER.getStartingCursor());
        }
        altDown = true;
    }

    @Override
    public void altReleased() {
        if(doColorPickerForwarding()) {
            ImageComponents.setCursorForAll(cursor);
        }
        altDown = false;
    }

    @Override
    public String toString() {
        return name; // so that they can be easily selected from a JComboBox
    }

    // used for debugging
    public String getStateInfo() {
        return null;
    }

    public DebugNode getDebugNode() {
        DebugNode toolNode = new DebugNode("Active Tool", this);
        toolNode.addString("Name", getName());
        return toolNode;
    }

    public void noOpenImageAnymore() {
        resetStateToInitial();
    }

    public void activeImageHasChanged(ImageComponent oldIC, ImageComponent newIC) {
        assert Tools.currentTool == this;
        if (oldIC != null) {
            oldIC.repaint();
        }
        resetStateToInitial();
    }

    public void compReplaced(Composition oldComp, Composition newComp) {
        // empty instead of abstract for the convenience of subclasses
    }

    public void resetStateToInitial() {
        // empty instead of abstract for the convenience of subclasses
    }

    public void fgBgColorsChanged() {
        // empty instead of abstract for the convenience of subclasses
    }

    /**
     * The component coordinates have changed in the given active {@link ImageComponent}.
     * A lot of events can trigger this, such as zooming, or changing the canvasStartX
     * by resizing.
     * TODO image resizing cannot be handled by simply passing the new ImageComponent,
     * and only the selections are resized (they are resized explicitly) but no other tool
     */
    public void coCoordsChanged(ImageComponent ic) {
        // empty instead of abstract for the convenience of subclasses
    }

    public void activate() {
        if (toolButton != null) {
            toolButton.doClick();
        } else {
            assert Build.isTesting();
            Tools.changeTo(this);
        }
    }

    public boolean isActive() {
        return Tools.currentIs(this);
    }
}
