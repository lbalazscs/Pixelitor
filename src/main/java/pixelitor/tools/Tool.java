/*
 * Copyright 2016 Laszlo Balazs-Csiki
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
import pixelitor.gui.GlobalKeyboardWatch;
import pixelitor.gui.ImageComponent;
import pixelitor.gui.ImageComponents;
import pixelitor.history.History;
import pixelitor.history.ImageEdit;
import pixelitor.history.PartialImageEdit;
import pixelitor.layers.ImageLayer;
import pixelitor.selection.IgnoreSelection;
import pixelitor.tools.toolhandlers.ColorPickerToolHandler;
import pixelitor.tools.toolhandlers.CurrentToolHandler;
import pixelitor.tools.toolhandlers.HandToolHandler;
import pixelitor.tools.toolhandlers.ImageLayerCheckHandler;
import pixelitor.tools.toolhandlers.ToolHandler;
import pixelitor.utils.Utils;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * An abstract superclass for all tools
 */
public abstract class Tool {
    private boolean mouseDown = false;
    private boolean altDown = false;

    private ToolButton toolButton;

    private final String name;
    private final String iconFileName;
    private final String toolMessage;
    private final Cursor cursor;
    private final boolean constrainIfShiftDown;
    private final ClipStrategy clipStrategy;

    private boolean endPointInitialized = false;
    protected boolean spaceDragBehavior = false;

    protected UserDrag userDrag;

    private final char activationKeyChar;
    private ToolHandler handlerChainStart;
    private HandToolHandler handToolHandler;

    protected ToolSettingsPanel settingsPanel;
    protected boolean ended = false;

    // a dialog with more settings that will be closed automatically
    // when the user switches to another tool
    protected JDialog toolDialog;

    protected Tool(char activationKeyChar, String name, String iconFileName, String toolMessage, Cursor cursor, boolean allowOnlyImageLayers, boolean handToolForwarding, boolean constrainIfShiftDown, ClipStrategy clipStrategy) {
        this.activationKeyChar = activationKeyChar;
        this.name = name;
        this.iconFileName = iconFileName;
        this.toolMessage = toolMessage;
        this.cursor = cursor;
        this.constrainIfShiftDown = constrainIfShiftDown;
        this.clipStrategy = clipStrategy;

        initHandlerChain(cursor, allowOnlyImageLayers, handToolForwarding);
    }

    private void initHandlerChain(Cursor cursor, boolean allowOnlyImageLayers, boolean handToolForwarding) {
        ToolHandler lastHandler = null;
        if (handToolForwarding) {
            // most tools behave like the hand tool if the space is pressed
            handToolHandler = new HandToolHandler(cursor);
            lastHandler = addHandlerToChain(handToolHandler, lastHandler);
        }
        if (allowOnlyImageLayers) {
            lastHandler = addHandlerToChain(
                    new ImageLayerCheckHandler(this), lastHandler);
        }
        if(doColorPickerForwarding()) {
            // brush tools behave like the color picker if Alt is pressed
            ColorPickerToolHandler colorPickerHandler = new ColorPickerToolHandler();
            lastHandler = addHandlerToChain(colorPickerHandler, lastHandler);
        }
        // if there was no special case, the current tool should handle the events
        addHandlerToChain(new CurrentToolHandler(this), lastHandler);
    }

    protected boolean doColorPickerForwarding() {
        return false;
    }

    /**
     * Adds the new handler to the end of the chain and returns the new end of the chain
     */
    private ToolHandler addHandlerToChain(ToolHandler newHandler, ToolHandler lastOne) {
        if (lastOne == null) {
            handlerChainStart = newHandler;
            return handlerChainStart;
        } else {
            lastOne.setSuccessor(newHandler);
            return newHandler;
        }
    }

    public String getToolMessage() {
        return toolMessage;
    }

    public boolean dispatchMouseClicked(MouseEvent e, ImageComponent ic) {
        // empty for the convenience of subclasses
        return false;
    }

    public void dispatchMousePressed(MouseEvent e, ImageComponent ic) {
        if (mouseDown) {
            // can happen if the tool is changed while drawing, and then changed back
            MouseEvent fake = new MouseEvent((Component) e.getSource(), e.getID(), e.getWhen(), e.getModifiers(),
                    (int)userDrag.getEndX(), (int)userDrag.getEndY(), 1, false);
            dispatchMouseReleased(fake, ic); // try to clean-up
        }
        mouseDown = true;

        userDrag = new UserDrag();
        userDrag.setStartFromMouseEvent(e, ic);

        handlerChainStart.handleMousePressed(e, ic);

        endPointInitialized = false;
    }

    public void dispatchMouseReleased(MouseEvent e, ImageComponent ic) {
        if (!mouseDown) { // can happen if the tool is changed while drawing
            dispatchMousePressed(e, ic); // try to initialize
        }
        mouseDown = false;

        userDrag.setEndFromMouseEvent(e, ic);

        handlerChainStart.handleMouseReleased(e, ic);

        endPointInitialized = false;
    }

    public void dispatchMouseDragged(MouseEvent e, ImageComponent ic) {
        if (!mouseDown) { // can happen if the tool is changed while drawing
            dispatchMousePressed(e, ic); // try to initialize
        }
        mouseDown = true;

        if (spaceDragBehavior) {
            userDrag.saveEndValues();
        }
        if (constrainIfShiftDown) {
            userDrag.setConstrainPoints(e.isShiftDown());
        }

        userDrag.setEndFromMouseEvent(e, ic);

        if (spaceDragBehavior) {
            if (endPointInitialized && GlobalKeyboardWatch.isSpaceDown()) {
                userDrag.adjustStartForSpaceDownMove();
            }

            endPointInitialized = true;
        }

        handlerChainStart.handleMouseDragged(e, ic);
    }

    void setButton(ToolButton toolButton) {
        this.toolButton = toolButton;
    }

    public ToolButton getButton() {
        return toolButton;
    }

    public abstract void initSettingsPanel();

    public String getName() {
        return name;
    }

    protected String getIconFileName() {
        return iconFileName;
    }

    public char getActivationKeyChar() {
        return activationKeyChar;
    }

    /**
     * Saves the full image or the selected area only if there is a selection
     */
    void saveFullImageForUndo(Composition comp) {
        BufferedImage copy = comp.getActiveMaskOrImageLayer()
                .getImageOrSubImageIfSelected(true, true);

        ImageEdit edit = new ImageEdit(comp, getName(),
                comp.getActiveMaskOrImageLayer(), copy,
                IgnoreSelection.NO, false);
        History.addEdit(edit);
    }

    /**
     * This saving method is used by the brush tools, by the shapes and by the paint bucket.
     * It saves the intersection of the selection (if there is one) with the maximal affected area.
     */
    // TODO currently it does not take the selection into account
    protected void saveSubImageForUndo(BufferedImage originalImage, ToolAffectedArea affectedArea) {
        assert (originalImage != null);
        Rectangle rectangleAffectedByTool = affectedArea.getRectangle();
        if (rectangleAffectedByTool.isEmpty()) {
            return;
        }

        ImageLayer layer = affectedArea.getLayer();
        Composition comp = layer.getComp();

//        Rectangle fullImageBounds = new Rectangle(0, 0, originalImage.getWidth(), originalImage.getHeight());
//        Rectangle saveRectangle = rectangleAffectedByTool.intersection(fullImageBounds);

        Rectangle saveRectangle = SwingUtilities.computeIntersection(
                0, 0, originalImage.getWidth(), originalImage.getHeight(), // full image bounds
                rectangleAffectedByTool
        );

        if (!saveRectangle.isEmpty()) {
            PartialImageEdit edit = new PartialImageEdit(getName(), comp, layer, originalImage, saveRectangle, false);
            History.addEdit(edit);
        }
    }

    protected void toolStarted() {
        ended = false;
        ImageComponents.setCursorForAll(cursor);
    }

    protected void toolEnded() {
        ended = true;

        closeToolDialog();
    }

    protected void closeToolDialog() {
        if (toolDialog != null && toolDialog.isVisible()) {
            toolDialog.setVisible(false);
            toolDialog.dispose();
        }
    }

    public Cursor getCursor() {
        return cursor;
    }

    public void paintOverLayer(Graphics2D g, Composition comp) {
        // empty for the convenience of subclasses
    }

    /**
     * A possibility to paint temporarily something (like marching ants) on the ImageComponent
     * after all the layers have been painted.
     */
    public void paintOverImage(Graphics2D g2, Canvas canvas, ImageComponent callingIC, AffineTransform unscaledTransform) {
        // empty for the convenience of subclasses
    }

    public void dispatchMouseMoved(MouseEvent e, ImageComponent ic) {
        // empty for the convenience of subclasses
    }

    public abstract void mousePressed(MouseEvent e, ImageComponent ic);

    public abstract void mouseDragged(MouseEvent e, ImageComponent ic);

    public abstract void mouseReleased(MouseEvent e, ImageComponent ic);

    public void setSettingsPanel(ToolSettingsPanel settingsPanel) {
        this.settingsPanel = settingsPanel;
    }

    public void randomize() {
        Utils.randomizeGUIWidgetsOn(settingsPanel);
    }

    public UserDrag getUserDrag() {
        return userDrag;
    }

    public void setClip(Graphics2D g, ImageComponent ic) {
        clipStrategy.setClip(g, ic);
    }

    public void spacePressed() {
        if (handToolHandler != null) { // there is hand tool forwarding
            handToolHandler.spacePressed();
        }
    }

    public void spaceReleased() {
        if (handToolHandler != null) { // there is hand tool forwarding
            handToolHandler.spaceReleased();
        }
    }

    public boolean arrowKeyPressed(ArrowKey key) {
        // empty for the convenience of subclasses
        return false; // not consumed
    }

    public void escPressed() {
        // empty by default
    }

    public void altPressed() {
        if (!altDown && doColorPickerForwarding()) {
            ImageComponents.onAllImages(ic -> ic.setCursor(Tools.COLOR_PICKER.getCursor()));
        }
        altDown = true;
    }

    public void altReleased() {
        if(doColorPickerForwarding()) {
            ImageComponents.onAllImages(ic -> ic.setCursor(cursor));
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
        toolNode.addStringChild("Name", getName());
        return toolNode;
    }
}
