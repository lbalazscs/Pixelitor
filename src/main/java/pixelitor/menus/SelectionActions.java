/*
 * Copyright 2015 Laszlo Balazs-Csiki
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

package pixelitor.menus;

import pixelitor.Build;
import pixelitor.Composition;
import pixelitor.ImageComponents;
import pixelitor.ImageDisplay;
import pixelitor.PixelitorWindow;
import pixelitor.filters.gui.EnumParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.history.AddToHistory;
import pixelitor.selection.Selection;
import pixelitor.tools.AbstractBrushTool;
import pixelitor.tools.Tools;
import pixelitor.utils.GridBagHelper;
import pixelitor.utils.Messages;
import pixelitor.utils.OKCancelDialog;

import javax.swing.*;
import java.awt.GridBagLayout;
import java.awt.Shape;
import java.awt.event.ActionEvent;

import static pixelitor.ImageComponents.getActiveComp;

/**
 * Static methods for managing the selection actions
 */
public final class SelectionActions {

    private static final Action cropAction = new AbstractAction("Crop") {
        @Override
        public void actionPerformed(ActionEvent e) {
            ImageComponents.selectionCropActiveImage();
        }
    };

    private static final Action deselectAction = new MenuAction("Deselect") {
        @Override
        public void onClick() {
            getActiveComp().get().deselect(AddToHistory.YES);
        }
    };

    private static final Action invertSelectionAction = new MenuAction("Invert Selection") {
        @Override
        public void onClick() {
            getActiveComp().get().invertSelection();
        }
    };

    private static final Action traceWithBrush = new TraceAction("Stroke with Current Brush", Tools.BRUSH);
    private static final Action traceWithEraser = new TraceAction("Stroke with Current Eraser", Tools.ERASER);

    private static final Action modifyAction = new MenuAction("Modify...") {
        @Override
        public void onClick() {
            JPanel p = new JPanel(new GridBagLayout());
            GridBagHelper gbh = new GridBagHelper(p);
            RangeParam amount = new RangeParam("Amount (pixels)", 1, 10, 100);
            EnumParam<SelectionModifyType> type = new EnumParam<>("Type", SelectionModifyType.class);
            gbh.addLabelWithControl("Amount", amount.createGUI());
            gbh.addLabelWithControl("Type", type.createGUI());

            OKCancelDialog d = new OKCancelDialog(p, PixelitorWindow.getInstance(),
                    "Modify Selection", "Change!", "Close") {
                @Override
                protected void dialogAccepted() {
                    Selection selection = getActiveComp().get().getSelection().get();
                    SelectionModifyType selectionModifyType = (SelectionModifyType) type.getSelectedItem();
                    selection.modify(selectionModifyType, amount.getValue());
                }
            };
            d.setVisible(true);
        }
    };

    static {
        setEnabled(false, null);
    }

    private SelectionActions() {
    }

    public static void setEnabled(boolean b, Composition comp) {
        assert SwingUtilities.isEventDispatchThread() : "not EDT thread";

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
        modifyAction.setEnabled(b);
    }

    public static boolean areEnabled() {
        return cropAction.isEnabled();
    }

    public static Action getCropAction() {
        return cropAction;
    }

    public static Action getTraceWithBrush() {
        return traceWithBrush;
    }

    public static Action getTraceWithEraser() {
        return traceWithEraser;
    }

    public static Action getDeselectAction() {
        return deselectAction;
    }

    public static Action getInvertSelectionAction() {
        return invertSelectionAction;
    }

    public static Action getModifyAction() {
        return modifyAction;
    }

    private static class TraceAction extends MenuAction {
        private final AbstractBrushTool brushTool;

        private TraceAction(String name, AbstractBrushTool brushTool) {
            super(name);
            this.brushTool = brushTool;
        }

        @Override
        public void onClick() {
            ImageDisplay ic = ImageComponents.getActiveIC();
            if(ic == null) {
                return;
            }
            Composition comp = ic.getComp();

            if (!comp.activeIsImageLayer()) {
                Messages.showNotImageLayerError();
                return;
            }

            getActiveComp()
                    .flatMap(Composition::getSelection)
                    .ifPresent(selection -> {
                        Shape shape = selection.getShape();
                        if (shape != null) {
                            brushTool.trace(getActiveComp().get(), shape);
                        }
                    });
        }
    }
}
