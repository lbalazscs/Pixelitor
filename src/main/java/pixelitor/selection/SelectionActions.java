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

package pixelitor.selection;

import pixelitor.Composition;
import pixelitor.filters.comp.Crop;
import pixelitor.filters.gui.EnumParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.ImageComponents;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.GridBagHelper;
import pixelitor.layers.Drawable;
import pixelitor.menus.MenuAction;
import pixelitor.menus.view.ShowHideAction;
import pixelitor.menus.view.ShowHideSelectionAction;
import pixelitor.tools.AbstractBrushTool;
import pixelitor.tools.Tools;
import pixelitor.tools.pen.Path;
import pixelitor.utils.Messages;
import pixelitor.utils.Shapes;
import pixelitor.utils.test.RandomGUITest;

import javax.swing.*;
import java.awt.GridBagLayout;
import java.awt.Shape;
import java.awt.event.ActionEvent;

import static pixelitor.gui.ImageComponents.getActiveCompOrNull;

/**
 * Static methods for managing the actions that should be enabled
 * only when there is a selection on the active composition.
 */
public final class SelectionActions {

    private static final Action crop = new AbstractAction("Crop") {
        @Override
        public void actionPerformed(ActionEvent e) {
            Crop.selectionCropActiveImage();
        }
    };

    private static final Action deselect = new MenuAction("Deselect") {
        @Override
        public void onClick() {
            getActiveCompOrNull().deselect(true);
        }
    };

    private static final Action invert = new MenuAction("Invert Selection") {
        @Override
        public void onClick() {
            getActiveCompOrNull().invertSelection();
        }
    };

    private static final ShowHideAction showHide = new ShowHideSelectionAction();

    private static final Action traceWithBrush = new TraceAction("Stroke with Current Brush", Tools.BRUSH);
    private static final Action traceWithEraser = new TraceAction("Stroke with Current Eraser", Tools.ERASER);
    private static final Action traceWithSmudge = new TraceAction("Stroke with Current Smudge", Tools.SMUDGE);

    private static final Action convertToPath = new AbstractAction("Convert to Path") {
        @Override
        public void actionPerformed(ActionEvent e) {
            Composition comp = ImageComponents.getActiveCompOrNull();
            Shape shape = comp.getSelection().getShape();
            comp.deselect(true);
            Path path = Shapes.shapeToPath(shape, comp.getIC());
            Tools.PEN.setPath(path);
            Tools.PEN.startEditing(false);
            Tools.PEN.getButton().doClick();
        }
    };

    private static final Action modify = new MenuAction("Modify Selection...") {
        @Override
        public void onClick() {
            JPanel p = new JPanel(new GridBagLayout());
            GridBagHelper gbh = new GridBagHelper(p);
            RangeParam amount = new RangeParam("Amount (pixels)", 1, 10, 100);
            EnumParam<SelectionModifyType> type = new EnumParam<>("Type", SelectionModifyType.class);
            gbh.addLabelWithControl("Amount", amount.createGUI());
            gbh.addLabelWithControl("Type", type.createGUI());

            new DialogBuilder()
                    .content(p)
                    .title("Modify Selection")
                    .okText("Change!")
                    .cancelText("Close")
                    .validator(d -> {
                        modifySelection(type, amount);

                        // always return false so that
                        // the Change button does not close it
                        return false;
                    })
                    .show();
        }
    };

    private static void modifySelection(EnumParam<SelectionModifyType> type, RangeParam amount) {
        Selection selection = getActiveCompOrNull().getSelection();
        SelectionModifyType selectionModifyType = type.getSelected();
        if (selection != null) {
            selection.modify(selectionModifyType, amount.getValue());
        } else {
            // TODO - we modified it so much that it disappeared
            // at least the change button should be disabled
        }
    }

    static {
        setEnabled(false, null);
    }

    private SelectionActions() {
    }

    /**
     * All selection actions must be enabled only if
     * the active composition has a selection
     */
    public static void setEnabled(boolean b, Composition comp) {
        assert SwingUtilities.isEventDispatchThread() : "not EDT thread";

        if (RandomGUITest.isRunning()) {
            if (comp != null) {
                boolean hasSelection = comp.hasSelection();
                if (hasSelection != b) {
                    String name = comp.getName();
                    throw new IllegalStateException("composition " + name +
                            ": hasSelection = " + hasSelection + ", b = " + b);
                }
            }
        }

        crop.setEnabled(b);
        traceWithBrush.setEnabled(b);
        traceWithEraser.setEnabled(b);
        traceWithSmudge.setEnabled(b);
        deselect.setEnabled(b);
        invert.setEnabled(b);
        showHide.setEnabled(b);
        modify.setEnabled(b);
        convertToPath.setEnabled(b);
    }

    public static boolean areEnabled() {
        return crop.isEnabled();
    }

    public static Action getCrop() {
        return crop;
    }

    public static Action getTraceWithBrush() {
        return traceWithBrush;
    }

    public static Action getTraceWithEraser() {
        return traceWithEraser;
    }

    public static Action getTraceWithSmudge() {
        return traceWithSmudge;
    }

    public static Action getDeselect() {
        return deselect;
    }

    public static Action getInvert() {
        return invert;
    }

    public static ShowHideAction getShowHide() {
        return showHide;
    }

    public static Action getConvertToPath() {
        return convertToPath;
    }

    public static Action getModify() {
        return modify;
    }

    /**
     * Strokes a selection with an {@link AbstractBrushTool}
     */
    private static class TraceAction extends MenuAction {
        private final AbstractBrushTool brushTool;

        private TraceAction(String name, AbstractBrushTool brushTool) {
            super(name);
            this.brushTool = brushTool;
        }

        @Override
        public void onClick() {
            ImageComponents.onActiveComp(this::trace);
        }

        private void trace(Composition comp) {
            if (!comp.activeIsDrawable()) {
                Messages.showNotDrawableError();
                return;
            }

            if (comp.hasSelection()) {
                Shape shape = comp.getSelectionShape();
                if (shape != null) {
                    Drawable dr = comp.getActiveDrawableOrThrow();
                    brushTool.trace(dr, shape);
                }
            }
        }
    }
}
