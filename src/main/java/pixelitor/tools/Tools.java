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
import pixelitor.gui.ImageComponents;
import pixelitor.tools.crop.CropTool;
import pixelitor.tools.gradient.GradientTool;
import pixelitor.tools.gui.ToolSettingsPanelContainer;
import pixelitor.tools.pen.PenTool;
import pixelitor.tools.shapes.ShapesTool;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.ActiveImageChangeListener;
import pixelitor.utils.Messages;
import pixelitor.utils.RandomUtils;

import java.awt.event.MouseEvent;

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

    public static Tool currentTool = BRUSH;

    static {
        ImageComponents.addActiveImageChangeListener(new ActiveImageChangeListener() {
            @Override
            public void activeImageChanged(ImageComponent oldIC, ImageComponent newIC) {
                currentTool.activeImageHasChanged(oldIC, newIC);
            }

            @Override
            public void noOpenImageAnymore() {
                currentTool.noOpenImageAnymore();
            }
        });
    }

    private static final Tool[] allTools = {
            MOVE, CROP, SELECTION, BRUSH, CLONE, ERASER,
            SMUDGE, GRADIENT, PAINT_BUCKET, COLOR_PICKER,
            PEN, SHAPES, HAND, ZOOM};

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

        Messages.showInStatusBar(newTool.getStatusBarMessage());
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

    public static Tool getRandomTool() {
        return RandomUtils.chooseFrom(allTools);
    }

    public static void fgBgColorsChanged() {
        currentTool.fgBgColorsChanged();
    }

    public static void coCoordsChanged(ImageComponent ic) {
        currentTool.coCoordsChanged(ic);
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
            if (!mouseDown) {
                // the "mouse pressed" event was lost/consumed somehow
                // (for example a combo box was open when it happened)
                // and the recovery in mouseDragged didn't happen because
                // it was a click
                return;
            }
            lastEvent = new PMouseEvent(e, ic);
            currentTool.handlerChain.handleMouseReleased(lastEvent);
            mouseDown = false;
        }

        public static void mouseDragged(MouseEvent e, ImageComponent ic) {
            lastEvent = new PMouseEvent(e, ic);
            if (!mouseDown) {
                // recover from a missing "mouse pressed" event by
                // simulating one
                currentTool.handlerChain.handleMousePressed(lastEvent);
                mouseDown = true;
                return;
            }
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

        public static boolean isMouseDown() {
            return mouseDown;
        }
    }
}
