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

import pixelitor.gui.ImageComponent;
import pixelitor.tools.shapestool.ShapesTool;

import java.awt.event.MouseEvent;
import java.util.Random;

/**
 * Tool-related static utility methods
 */
public class Tools {
    private Tools() {
    }

    public static final MoveTool MOVE = new MoveTool();
    public static final CropTool CROP = new CropTool();
    public static final SelectionTool SELECTION = new SelectionTool();
    //    public static final LassoTool LASSO = new LassoTool();
    public static final BrushTool BRUSH = new BrushTool();
    public static final CloneTool CLONE = new CloneTool();

    public static final EraserTool ERASER = new EraserTool();
    public static final SmudgeTool SMUDGE = new SmudgeTool();
    public static final GradientTool GRADIENT = new GradientTool();
    public static final PaintBucketTool PAINT_BUCKET = new PaintBucketTool();
    public static final ColorPickerTool COLOR_PICKER = new ColorPickerTool();
    public static final ShapesTool SHAPES = new ShapesTool();
    public static final HandTool HAND = new HandTool();
    public static final ZoomTool ZOOM = new ZoomTool();

    static Tool currentTool = BRUSH;

    /**
     * All the subclass tools in an array.
     */
    private static final Tool[] allTools =
            {MOVE, CROP, SELECTION, BRUSH, CLONE, ERASER,
                    SMUDGE,
                    GRADIENT, PAINT_BUCKET, COLOR_PICKER, SHAPES, HAND, ZOOM};

    public static void setDefaultTool() {
        changeTo(BRUSH);
        currentTool.getButton().setSelected(true);
    }

    public static void changeTo(Tool newTool) {
        Tool previousTool = Tools.currentTool;
        previousTool.toolEnded();
        Tools.currentTool = newTool;
        newTool.toolStarted();
        EventDispatcher.toolChanged(previousTool, newTool);
        ToolSettingsPanelContainer.INSTANCE.showSettingsFor(newTool);
    }

    public static Tool[] getAll() {
        return allTools;
    }

    public static Tool getCurrent() {
        return currentTool;
    }

    public static boolean isShapesDrawing() {
        if (currentTool != SHAPES) {
            return false;
        }
        return SHAPES.isDrawing();
    }

    public static void increaseActiveBrushSize() {
        if (currentTool instanceof AbstractBrushTool) {
            ((AbstractBrushTool) currentTool).increaseBrushSize();
        }
    }

    public static void decreaseActiveBrushSize() {
        if (currentTool instanceof AbstractBrushTool) {
            ((AbstractBrushTool) currentTool).decreaseBrushSize();
        }
    }

    public static Tool getRandomTool(Random rand) {
        int index = rand.nextInt(allTools.length);
        return allTools[index];
    }

    public static class EventDispatcher {
        private static boolean mouseDown = false;
        private static PMouseEvent lastEvent;

        private EventDispatcher() {
        }

        public static void mousePressed(MouseEvent e, ImageComponent ic) {
            lastEvent = new PMouseEvent(e, ic);
            currentTool.handlerChain.handleMousePressed(lastEvent);
            mouseDown = true;
        }

        public static void mouseReleased(MouseEvent e, ImageComponent ic) {
            lastEvent = new PMouseEvent(e, ic);
            currentTool.handlerChain.handleMouseReleased(lastEvent);
            mouseDown = false;
        }

        public static void mouseDragged(MouseEvent e, ImageComponent ic) {
            lastEvent = new PMouseEvent(e, ic);
            currentTool.handlerChain.handleMouseDragged(lastEvent);
        }

        public static void mouseClicked(MouseEvent e, ImageComponent ic) {
            lastEvent = new PMouseEvent(e, ic);
            // doesn't need to go through the handler chain
            currentTool.mouseClicked(lastEvent);
            mouseDown = false;
        }

        public static void mouseMoved(MouseEvent e, ImageComponent ic) {
            // doesn't need to go through the handler chain
            currentTool.mouseMoved(e, ic);
        }

        public static void toolChanged(Tool oldTool, Tool newTool) {
            if (mouseDown) {
                // We switched tools via keyboard in the middle of a drag
                // In order to avoid broken internal tool states,
                // simulate a mouse release for the old tool...
                oldTool.handlerChain.handleMouseReleased(lastEvent);

                // ...and a mouse pressed for the new one
                newTool.handlerChain.handleMousePressed(lastEvent);
            }
        }
    }
}
