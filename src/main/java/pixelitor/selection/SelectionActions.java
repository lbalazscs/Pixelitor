/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.gui.OpenComps;
import pixelitor.gui.View;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.GridBagHelper;
import pixelitor.history.History;
import pixelitor.menus.MenuAction;
import pixelitor.menus.view.ShowHideSelectionAction;
import pixelitor.tools.Tools;
import pixelitor.tools.pen.Path;
import pixelitor.tools.pen.history.ConvertSelectionToPathEdit;
import pixelitor.utils.CompActivationListener;
import pixelitor.utils.Shapes;

import javax.swing.*;
import java.awt.GridBagLayout;
import java.awt.Shape;
import java.awt.event.ActionEvent;

import static pixelitor.gui.OpenComps.getActiveCompOrNull;
import static pixelitor.gui.OpenComps.getActiveSelection;
import static pixelitor.tools.pen.PenToolMode.EDIT;

/**
 * Static methods for managing the actions that should be enabled
 * only when there is a selection on the active composition.
 */
public final class SelectionActions {
    private static Shape copiedSelShape = null;

    private static final Action crop = new AbstractAction("Crop Selection") {
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

    private static final ShowHideSelectionAction showHide = new ShowHideSelectionAction();

    private static final Action convertToPath = new AbstractAction("Convert to Path") {
        @Override
        public void actionPerformed(ActionEvent e) {
            Composition comp = OpenComps.getActiveCompOrNull();
            selectionToPath(comp, true);
        }
    };

    private static final Action copySel = new MenuAction("Copy Selection") {
        @Override
        public void onClick() {
            copiedSelShape = getActiveCompOrNull().getSelectionShape();
            pasteSel.setEnabled(true);
        }
    };

    private static final Action pasteSel = new MenuAction("Paste Selection") {
        @Override
        public void onClick() {
            getActiveCompOrNull().changeSelectionFromShape(copiedSelShape);
        }
    };

    static {
        pasteSel.setEnabled(false);
        OpenComps.addActivationListener(new CompActivationListener() {
            @Override
            public void compActivated(View oldView, View newView) {
                pasteSel.setEnabled(copiedSelShape != null);
            }

            @Override
            public void allCompsClosed() {
                pasteSel.setEnabled(false);
            }
        });
    }

    public static void selectionToPath(Composition comp, boolean addToHistory) {
        Shape shape = comp.getSelection().getShape();
        Path oldActivePath = comp.getActivePath();
        comp.deselect(false);
        Path path = Shapes.shapeToPath(shape, comp.getView());
        comp.setActivePath(path);
        Tools.PEN.setPath(path);
        Tools.PEN.startRestrictedMode(EDIT, false);
        Tools.PEN.activate();

        if (addToHistory) {
            History.addEdit(new ConvertSelectionToPathEdit(comp, shape, oldActivePath));
        }
    }

    private static final Action modify = new MenuAction("Modify Selection...") {
        @Override
        public void onClick() {
            JPanel panel = new JPanel(new GridBagLayout());
            GridBagHelper gbh = new GridBagHelper(panel);
            RangeParam amount = new RangeParam("Amount (pixels)", 1, 10, 100);
            EnumParam<SelectionModifyType> type = SelectionModifyType.asParam();

            gbh.addLabelWithControl("Amount", amount.createGUI());
            gbh.addLabelWithControl("Type", type.createGUI());

            new DialogBuilder()
                    .content(panel)
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

    private static void modifySelection(EnumParam<SelectionModifyType> type,
                                        RangeParam amount) {
        Selection selection = getActiveSelection();
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
        assert comp == null || OpenComps.getActiveCompOrNull() == comp;

        crop.setEnabled(b);
        deselect.setEnabled(b);
        invert.setEnabled(b);
        showHide.setEnabled(b);
        modify.setEnabled(b);
        convertToPath.setEnabled(b);
        copySel.setEnabled(b);
    }

    public static boolean areEnabled() {
        return crop.isEnabled();
    }

    public static Action getCrop() {
        return crop;
    }

    public static Action getDeselect() {
        return deselect;
    }

    public static Action getInvert() {
        return invert;
    }

    public static ShowHideSelectionAction getShowHide() {
        return showHide;
    }

    public static Action getConvertToPath() {
        return convertToPath;
    }

    public static Action getModify() {
        return modify;
    }

    public static Action getCopy() {
        return copySel;
    }

    public static Action getPaste() {
        return pasteSel;
    }
}
