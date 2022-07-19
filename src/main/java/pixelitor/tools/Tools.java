/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.AppContext;
import pixelitor.Composition;
import pixelitor.Views;
import pixelitor.gui.View;
import pixelitor.layers.Layer;
import pixelitor.tools.crop.CropTool;
import pixelitor.tools.gradient.GradientTool;
import pixelitor.tools.gui.ToolSettingsPanelContainer;
import pixelitor.tools.move.MoveTool;
import pixelitor.tools.pen.PenTool;
import pixelitor.tools.shapes.ShapesTool;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.*;

import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;

/**
 * Tool-related static utility methods
 */
public class Tools {
    private Tools() {
    }

    public static final MoveTool MOVE = new MoveTool();
    public static final CropTool CROP = new CropTool();
    public static final SelectionTool SELECTION = new SelectionTool();
    public static final BrushTool BRUSH = new BrushTool();
    public static final CloneTool CLONE = new CloneTool();
    public static final EraserTool ERASER = new EraserTool();
    public static final SmudgeTool SMUDGE = new SmudgeTool();
    public static final GradientTool GRADIENT = new GradientTool();
    public static final PaintBucketTool PAINT_BUCKET = new PaintBucketTool();
    public static final ColorPickerTool COLOR_PICKER = new ColorPickerTool();
    public static final PenTool PEN = new PenTool();
    public static final ShapesTool SHAPES = new ShapesTool();
    public static final HandTool HAND = new HandTool();
    public static final ZoomTool ZOOM = new ZoomTool();

    private static final Tool[] allTools = {
        MOVE, CROP, SELECTION, BRUSH, CLONE, ERASER,
        SMUDGE, GRADIENT, PAINT_BUCKET, COLOR_PICKER,
        PEN, SHAPES, HAND, ZOOM};

    public static Tool currentTool;

    static {
        Views.addActivationListener(new ViewActivationListener() {
            @Override
            public void viewActivated(View oldView, View newView) {
                currentTool.viewActivated(oldView, newView);
            }

            @Override
            public void allViewsClosed() {
                currentTool.allViewsClosed();
            }
        });
    }

    public static void setDefaultTool() {
        String lastToolName = AppPreferences.loadLastToolName();

        boolean found = false;
        for (Tool tool : allTools) {
            if (tool.getShortName().equals(lastToolName)) {
                found = true;
                tool.activate();
                break;
            }
        }
        if (!found) { // ui language changed
            BRUSH.activate();
        }
    }

    @VisibleForTesting
    public static void setCurrentTool(Tool newTool) {
        currentTool = newTool;
    }

    // This doesn't select the button, because it is either
    // called by the button event handler or by testing code!
    // Normally the Tool's activate() method should be called instead.
    public static void start(Tool newTool) {
        // showing the message could be useful even if the tool didn't change
        Messages.showInStatusBar(newTool.getStatusBarMessage());

        Tool previousTool = currentTool;
        if (previousTool == newTool) {
            return;
        }

        if (previousTool != null) {
            previousTool.toolEnded();
            EventDispatcher.toolChanged(previousTool, newTool);
        }

        setCurrentTool(newTool);
        newTool.toolStarted();
        ToolSettingsPanelContainer.get().showSettingsOf(newTool);
    }

    public static Tool[] getAll() {
        return allTools;
    }

    public static Tool getCurrent() {
        return currentTool;
    }

    public static boolean currentIs(Tool t) {
        return currentTool == t;
    }

    public static boolean isShapesDrawing() {
        if (currentTool != SHAPES) {
            return false;
        }
        return SHAPES.shouldDrawOverLayer();
    }

    public static void increaseActiveBrushSize() {
        if (currentTool instanceof AbstractBrushTool abt) {
            abt.increaseBrushSize();
        }
    }

    public static void decreaseActiveBrushSize() {
        if (currentTool instanceof AbstractBrushTool abt) {
            abt.decreaseBrushSize();
        }
    }

    public static Tool getRandomTool() {
        return Rnd.chooseFrom(allTools);
    }

    public static void fgBgColorsChanged() {
        currentTool.fgBgColorsChanged();
    }

    public static void coCoordsChanged(View view) {
        currentTool.coCoordsChanged(view);
    }

    public static void imCoordsChanged(Composition comp, AffineTransform at) {
        currentTool.imCoordsChanged(at, comp);
    }

    public static void compReplaced(Composition newComp, boolean reloaded) {
        currentTool.compReplaced(newComp, reloaded);
    }

    /**
     * Called when a new layer or mask is being edited.
     */
    public static void editingTargetChanged(Layer layer) {
        assert currentTool != null || AppContext.isUnitTesting();

        // don't switch from the Move Tool, because it's confusing and
        // bug-prone if the tool is changed during an auto-select
        if (currentTool != MOVE) {
            Tool preferredTool = layer.getPreferredTool();
            if (preferredTool != null && preferredTool != currentTool) {
                preferredTool.activate();
            }
        }

        if (currentTool != null) {
            currentTool.editingTargetChanged(layer);
        }
    }

    public static void firstModalDialogShown() {
        currentTool.firstModalDialogShown();
    }

    public static void firstModalDialogHidden() {
        currentTool.firstModalDialogHidden();
    }

    public static void forceFinish() {
        currentTool.forceFinish();
    }

    public static void setupMaskEditing(boolean maskEditing) {
        currentTool.setupMaskEditing(maskEditing);
    }

    public static class EventDispatcher {
        private static boolean mouseDown = false;
        private static PMouseEvent lastEvent;

        private EventDispatcher() {
        }

        public static void mousePressed(MouseEvent e, View view) {
            lastEvent = new PMouseEvent(e, view);
            currentTool.handlerChain.handleMousePressed(lastEvent);
            mouseDown = true;
        }

        public static void mouseReleased(MouseEvent e, View view) {
            if (!mouseDown) {
                // the "mouse pressed" event was lost/consumed somehow
                // (for example a combo box was open when it happened)
                // and the recovery in mouseDragged didn't happen because
                // it was a click
                return;
            }
            lastEvent = new PMouseEvent(e, view);
            currentTool.handlerChain.handleMouseReleased(lastEvent);
            mouseDown = false;
        }

        public static void mouseDragged(MouseEvent e, View view) {
            lastEvent = new PMouseEvent(e, view);
            if (!mouseDown) {
                // recover from a missing "mouse pressed" event by
                // simulating one
                currentTool.handlerChain.handleMousePressed(lastEvent);
                mouseDown = true;
                return;
            }
            currentTool.handlerChain.handleMouseDragged(lastEvent);
        }

        public static void mouseClicked(MouseEvent e, View view) {
            lastEvent = new PMouseEvent(e, view);
            // doesn't need to go through the handler chain
            currentTool.mouseClicked(lastEvent);
            mouseDown = false;
        }

        public static void mouseMoved(MouseEvent e, View view) {
            // doesn't need to go through the handler chain
            currentTool.mouseMoved(e, view);
        }

        public static void mouseEntered(MouseEvent e, View view) {
            // doesn't need to go through the handler chain
            currentTool.mouseEntered(e, view);
        }

        public static void mouseExited(MouseEvent e, View view) {
            // doesn't need to go through the handler chain
            currentTool.mouseExited(e, view);
        }

        public static void toolChanged(Tool oldTool, Tool newTool) {
            if (mouseDown) {
                // Tools were switched via keyboard hotkey in the
                // middle of a mouse drag.
                // In order to avoid inconsistent tool states,
                // simulate a mouse release for the old tool...
                oldTool.handlerChain.handleMouseReleased(lastEvent);

                // ...and a mouse pressed for the new one
                newTool.handlerChain.handleMousePressed(lastEvent);
            }
        }

        public static boolean isMouseDown() {
            return mouseDown;
        }
    }
}
