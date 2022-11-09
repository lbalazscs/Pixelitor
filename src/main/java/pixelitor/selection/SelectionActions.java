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

package pixelitor.selection;

import pixelitor.Composition;
import pixelitor.Views;
import pixelitor.compactions.Crop;
import pixelitor.filters.gui.EnumParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.GUIText;
import pixelitor.gui.View;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.GridBagHelper;
import pixelitor.gui.utils.PAction;
import pixelitor.history.History;
import pixelitor.menus.view.ShowHideSelectionAction;
import pixelitor.tools.Tools;
import pixelitor.tools.pen.Path;
import pixelitor.tools.pen.history.ConvertSelectionToPathEdit;
import pixelitor.utils.Shapes;
import pixelitor.utils.ViewActivationListener;

import javax.swing.*;
import java.awt.GridBagLayout;
import java.awt.Shape;

import static pixelitor.Views.getActiveComp;
import static pixelitor.Views.getActiveSelection;
import static pixelitor.gui.GUIText.CLOSE_DIALOG;
import static pixelitor.tools.pen.PenToolMode.EDIT;
import static pixelitor.utils.Texts.i18n;

/**
 * Static methods for managing the actions that should be enabled
 * only when there is a selection on the active composition.
 */
public final class SelectionActions {
    private static Shape copiedSelShape = null;

    private static final Action crop = new PAction("Crop Selection",
        Crop::selectionCropActiveComp);

    private static final Action deselect = new PAction(i18n("deselect"), () ->
        getActiveComp().deselect(true));

    private static final Action invert = new PAction(i18n("invert_sel"), () ->
        getActiveComp().invertSelection());

    private static final ShowHideSelectionAction showHide = new ShowHideSelectionAction();

    private static final Action convertToPath = new PAction("Convert to Path", () ->
        selectionToPath(getActiveComp(), true));

    private static final Action copySel = new PAction(i18n("copy_sel"),
        SelectionActions::copySelection);

    private static final Action pasteSel = new PAction(i18n("paste_sel"), () ->
        getActiveComp().changeSelection(copiedSelShape));

    static {
        pasteSel.setEnabled(false);
        Views.addActivationListener(new ViewActivationListener() {
            @Override
            public void viewActivated(View oldView, View newView) {
                pasteSel.setEnabled(copiedSelShape != null);
            }

            @Override
            public void allViewsClosed() {
                pasteSel.setEnabled(false);
            }
        });
    }

    private static void copySelection() {
        copiedSelShape = getActiveComp().getSelectionShape();
        pasteSel.setEnabled(true);
    }

    public static void selectionToPath(Composition comp, boolean addToHistory) {
        Shape shape = comp.getSelection().getShape();
        Path oldActivePath = comp.getActivePath();
        comp.deselect(false);
        Path path = Shapes.shapeToPath(shape, comp.getView());
        comp.setActivePath(path);
        Tools.PEN.setPath(path);
        Tools.PEN.startMode(EDIT, false);
        Tools.PEN.activate();

        if (addToHistory) {
            History.add(new ConvertSelectionToPathEdit(comp, shape, oldActivePath));
        }
    }

    private static final Action modify = new PAction(i18n("modify_sel") + "...", () -> {
        JPanel panel = new JPanel(new GridBagLayout());
        var gbh = new GridBagHelper(panel);
        RangeParam amount = new RangeParam("Amount (pixels)", 1, 10, 100);
        EnumParam<SelectionModifyType> type = SelectionModifyType.asParam();

        gbh.addOnlyControl(amount.createGUI("amount"));
        gbh.addLabelAndControl(GUIText.TYPE + ":", type.createGUI("type"));

        new DialogBuilder()
            .content(panel)
            .title(i18n("modify_sel"))
            .okText("Change!")
            .cancelText(CLOSE_DIALOG)
            .validator(d -> {
                modifySelection(type, amount);

                // always return false so that
                // the Change button does not close it
                return false;
            })
            .show();
    });

    private static void modifySelection(EnumParam<SelectionModifyType> type,
                                        RangeParam amount) {
        var selection = getActiveSelection();
        SelectionModifyType selectionModifyType = type.getSelected();
        if (selection != null) {
            selection.modify(selectionModifyType, amount.getValue());
        } else {
            // TODO - the selections was modified so much that it disappeared
            // at least the change button should be disabled
        }
    }

    static {
        update(null);
    }

    private SelectionActions() {
    }

    /**
     * Selection actions must be enabled only if
     * the active composition has a selection
     */
    public static void update(Composition comp) {
        boolean enabled;
        if (comp == null) {
            enabled = false;
        } else {
            assert comp.isActive();
            enabled = comp.hasSelection();
        }

        crop.setEnabled(enabled);
        deselect.setEnabled(enabled);
        invert.setEnabled(enabled);
        showHide.setEnabled(enabled);
        modify.setEnabled(enabled);
        convertToPath.setEnabled(enabled);
        copySel.setEnabled(enabled);
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
