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

package pixelitor.tools;

import pixelitor.AppMode;
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
import pixelitor.utils.AppPreferences;
import pixelitor.utils.Messages;
import pixelitor.utils.Rnd;
import pixelitor.utils.ViewActivationListener;

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

    public static Tool activeTool;

    static {
        Views.addActivationListener(new ViewActivationListener() {
            @Override
            public void viewActivated(View oldView, View newView) {
                activeTool.viewActivated(oldView, newView);
            }

            @Override
            public void allViewsClosed() {
                activeTool.allViewsClosed();
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

    public static void setActiveTool(Tool newTool) {
        activeTool = newTool;
    }

    // This doesn't select the button, because it is either
    // called by the button event handler or by testing code!
    // Normally the Tool's activate() method should be called instead.
    public static void start(Tool newTool) {
        // showing the message could be useful even if the tool didn't change
        Messages.showStatusMessage(newTool.getStatusBarMessage());

        Tool previousTool = activeTool;
        if (previousTool == newTool) {
            return;
        }

        if (previousTool != null) {
            previousTool.toolDeactivated();
            EventDispatcher.toolChanged(previousTool, newTool);
        }

        setActiveTool(newTool);
        newTool.toolActivated();
        ToolSettingsPanelContainer.get().showSettingsOf(newTool);
    }

    public static Tool[] getAll() {
        return allTools;
    }

    public static Tool getActive() {
        return activeTool;
    }

    public static boolean activeIs(Tool t) {
        return activeTool == t;
    }

    public static boolean isShapesDrawing() {
        return activeTool == SHAPES && SHAPES.shouldDrawOverLayer();
    }

    public static void increaseBrushSize() {
        if (activeTool instanceof AbstractBrushTool abt) {
            abt.increaseBrushSize();
        }
    }

    public static void decreaseBrushSize() {
        if (activeTool instanceof AbstractBrushTool abt) {
            abt.decreaseBrushSize();
        }
    }

    public static Tool getRandomTool() {
        return Rnd.chooseFrom(allTools);
    }

    public static void fgBgColorsChanged() {
        activeTool.fgBgColorsChanged();
    }

    public static void coCoordsChanged(View view) {
        activeTool.coCoordsChanged(view);
    }

    public static void imCoordsChanged(AffineTransform at, View view) {
        activeTool.imCoordsChanged(at, view);
    }

    public static void compReplaced(Composition newComp, boolean reloaded) {
        activeTool.compReplaced(newComp, reloaded);
    }

    public static void editingTargetChanged(Layer activeLayer) {
        if (AppMode.isUnitTesting()) {
            return;
        }
        assert activeTool != null;

        // don't switch from the Move Tool, because it's confusing and
        // bug-prone if the tool is changed during an auto-select
        if (activeTool != MOVE) {
            Tool preferredTool = activeLayer.getPreferredTool();
            if (preferredTool != null && preferredTool != activeTool) {
                preferredTool.activate();
            }
        }

        if (activeTool != null) {
            activeTool.editingTargetChanged(activeLayer);
        }
    }

    public static void firstModalDialogShown() {
        activeTool.firstModalDialogShown();
    }

    public static void firstModalDialogHidden() {
        activeTool.firstModalDialogHidden();
    }

    public static void forceFinish() {
        activeTool.forceFinish();
    }

    public static void maskEditingChanged(boolean maskEditing) {
        activeTool.maskEditingChanged(maskEditing);
    }

    public static class EventDispatcher {
        private static boolean mouseDown = false;
        private static PMouseEvent lastEvent;

        private EventDispatcher() {
        }

        public static void mousePressed(MouseEvent e, View view) {
            lastEvent = new PMouseEvent(e, view);
            activeTool.eventHandlerChain.handleMousePressed(lastEvent);
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
            activeTool.eventHandlerChain.handleMouseReleased(lastEvent);
            mouseDown = false;
        }

        public static void mouseDragged(MouseEvent e, View view) {
            lastEvent = new PMouseEvent(e, view);
            if (!mouseDown) {
                // recover from a missing "mouse pressed" event by
                // simulating one
                activeTool.eventHandlerChain.handleMousePressed(lastEvent);
                mouseDown = true;
                return;
            }
            activeTool.eventHandlerChain.handleMouseDragged(lastEvent);
        }

        public static void mouseClicked(MouseEvent e, View view) {
            lastEvent = new PMouseEvent(e, view);
            // doesn't need to go through the handler chain
            activeTool.mouseClicked(lastEvent);
            mouseDown = false;
        }

        public static void mouseMoved(MouseEvent e, View view) {
            // doesn't need to go through the handler chain
            activeTool.mouseMoved(e, view);
        }

        public static void mouseEntered(MouseEvent e, View view) {
            // doesn't need to go through the handler chain
            activeTool.mouseEntered(e, view);
        }

        public static void mouseExited(MouseEvent e, View view) {
            // doesn't need to go through the handler chain
            activeTool.mouseExited(e, view);
        }

        public static void toolChanged(Tool oldTool, Tool newTool) {
            if (mouseDown) {
                // Tools were switched via keyboard hotkey in the
                // middle of a mouse drag.
                // In order to avoid inconsistent tool states,
                // simulate a mouse release for the old tool...
                oldTool.eventHandlerChain.handleMouseReleased(lastEvent);

                // ...and a mouse pressed for the new one
                newTool.eventHandlerChain.handleMousePressed(lastEvent);
            }
        }

        public static boolean isMouseDown() {
            return mouseDown;
        }
    }
}
