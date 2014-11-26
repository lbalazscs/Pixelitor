/*
 * Copyright 2009-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */

package pixelitor.menus;

import pixelitor.Build;
import pixelitor.Composition;
import pixelitor.ImageComponents;
import pixelitor.layers.Layers;
import pixelitor.selection.Selection;
import pixelitor.tools.AbstractBrushTool;
import pixelitor.tools.Tools;
import pixelitor.utils.Dialogs;

import javax.swing.*;
import java.awt.Shape;
import java.awt.event.ActionEvent;

public final class SelectionActions {

    private static final AbstractAction cropAction = new AbstractAction("Crop") {
        @Override
        public void actionPerformed(ActionEvent e) {
            ImageComponents.selectionCropActiveImage();
        }
    };

    private static final AbstractAction deselectAction = new AbstractAction("Deselect") {
        @Override
        public void actionPerformed(ActionEvent e) {
            ImageComponents.getActiveComp().deselect(true);
        }
    };

    private static final AbstractAction invertSelectionAction = new AbstractAction("Invert Selection") {
        @Override
        public void actionPerformed(ActionEvent e) {
            ImageComponents.getActiveComp().invertSelection();
        }
    };

    private static final AbstractAction traceWithBrush = new TraceAction("Stroke with Current Brush", Tools.BRUSH);
    private static final AbstractAction traceWithEraser = new TraceAction("Stroke with Current Eraser", Tools.ERASER);

    static {
        setEnabled(false, null);
    }

    private SelectionActions() {
    }

    public static void setEnabled(boolean b, Composition comp) {
        if(!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("SelectionActions::setEnabled: not on EDT");
        }

        if (Build.CURRENT.isRobotTest()) {
            if (comp != null) {
                boolean hasSelection = comp.hasSelection();
                if (hasSelection != b) {
                    String name = comp.getName();
                    throw new IllegalStateException("composition " + name +
                            ": hasSelection = " + hasSelection + ", b = " + b);
                }
            }
        }

        cropAction.setEnabled(b);
        traceWithBrush.setEnabled(b);
        traceWithEraser.setEnabled(b);
        deselectAction.setEnabled(b);
        invertSelectionAction.setEnabled(b);
    }

    public static boolean areEnabled() {
        return cropAction.isEnabled();
    }

    public static AbstractAction getCropAction() {
        return cropAction;
    }

    public static AbstractAction getTraceWithBrush() {
        return traceWithBrush;
    }

    public static AbstractAction getTraceWithEraser() {
        return traceWithEraser;
    }

    public static AbstractAction getDeselectAction() {
        return deselectAction;
    }

    public static AbstractAction getInvertSelectionAction() {
        return invertSelectionAction;
    }

    private static class TraceAction extends AbstractAction {
        private final AbstractBrushTool brushTool;

        private TraceAction(String name, AbstractBrushTool brushTool) {
            super(name);
            this.brushTool = brushTool;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!Layers.activeIsImageLayer()) {
                Dialogs.showNotImageLayerDialog();
                return;
            }

            Composition comp = ImageComponents.getActiveComp();
            Selection selection = comp.getSelection();
            if (selection != null) {
                Shape shape = selection.getShape();
                if (shape != null) {
                    brushTool.trace(comp, shape);
                }
            }
        }
    }

}
