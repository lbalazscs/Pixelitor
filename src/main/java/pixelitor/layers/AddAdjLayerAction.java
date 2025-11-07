/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.layers;

import pixelitor.Composition;
import pixelitor.Views;
import pixelitor.filters.Colorize;
import pixelitor.filters.Filter;
import pixelitor.filters.GradientMap;
import pixelitor.filters.HueSat;
import pixelitor.filters.curves.ToneCurvesFilter;
import pixelitor.filters.lookup.ColorBalance;
import pixelitor.filters.util.FilterAction;
import pixelitor.filters.util.FilterSearchPanel;
import pixelitor.gui.utils.AbstractViewEnabledAction;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.gui.utils.TaskAction;
import pixelitor.gui.utils.ThemedImageIcon;
import pixelitor.utils.Icons;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.function.Supplier;

/**
 * An Action that adds a new adjustment layer to the active composition.
 */
public class AddAdjLayerAction extends AbstractViewEnabledAction {
    public static final AddAdjLayerAction INSTANCE = new AddAdjLayerAction();

    private JPopupMenu popup;

    // adjustment layer adding actions, used  
    // both in the main menu and in the popup menu
    private static final List<Action> actions = createActions();

    private AddAdjLayerAction() {
        super("Add Adjustment Layer",
            Icons.loadThemed("add_adj_layer.png", ThemedImageIcon.GREEN));
        setToolTip("Adds a new adjustment layer.");
    }

    @Override
    public void onClick(ActionEvent e) {
        if (GUIUtils.isCtrlPressed(e)) {
            addFromFilterSearchDialog();
        } else {
            addFromPopupMenu(e);
        }
    }

    private static void addFromFilterSearchDialog() {
        FilterAction action = FilterSearchPanel.showInDialog("Find Adjustment Layer");
        if (action != null) {
            Filter filter = action.createNewFilterInstance();
            addAdjustmentLayer(filter, filter.getName());
        }
    }

    private void addFromPopupMenu(ActionEvent e) {
        if (popup == null) {
            popup = createActionsPopup();
        }
        Dimension size = popup.getPreferredSize();
        popup.show((JButton) e.getSource(), 0, -size.height);
    }

    @Override
    protected void onClick(Composition comp) {
        // never called, because this class overrides actionPerformed
        throw new UnsupportedOperationException();
    }

    private static JPopupMenu createActionsPopup() {
        JPopupMenu popup = new JPopupMenu();
        for (Action action : actions) {
            popup.add(action);
        }
        popup.pack();
        return popup;
    }

    private static List<Action> createActions() {
        return List.of(
            createAction(ColorBalance::new, ColorBalance.NAME),
            createAction(Colorize::new, Colorize.NAME),
            createAction(ToneCurvesFilter::new, ToneCurvesFilter.NAME),
            createAction(GradientMap::new, GradientMap.NAME),
            createAction(HueSat::new, HueSat.NAME)
//        createAction(Levels::new, Levels.NAME)
        );
    }

    private static Action createAction(Supplier<Filter> factory, String name) {
        return new TaskAction("New " + name, () ->
            addAdjustmentLayer(factory, name));
    }

    private static void addAdjustmentLayer(Supplier<Filter> factory, String name) {
        Filter filter = factory.get();
        filter.setName(name);
        addAdjustmentLayer(filter, name);
    }

    private static void addAdjustmentLayer(Filter filter, String name) {
        Composition comp = Views.getActiveComp();
        AdjustmentLayer layer = new AdjustmentLayer(comp, name, filter);

        comp.getHolderForNewLayers()
            .addWithHistory(layer, "New Adjustment Layer");

        layer.edit();
    }

    public static List<Action> getActions() {
        return actions;
    }
}
