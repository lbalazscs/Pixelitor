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

package pixelitor.layers;

import pixelitor.Composition.LayerAdder;
import pixelitor.Views;
import pixelitor.filters.Colorize;
import pixelitor.filters.Filter;
import pixelitor.filters.GradientMap;
import pixelitor.filters.HueSat;
import pixelitor.filters.curves.ToneCurvesFilter;
import pixelitor.filters.lookup.ColorBalance;
import pixelitor.filters.util.FilterAction;
import pixelitor.filters.util.FilterSearchPanel;
import pixelitor.gui.View;
import pixelitor.gui.utils.NamedAction;
import pixelitor.gui.utils.OpenViewEnabledAction;
import pixelitor.gui.utils.ThemedImageIcon;
import pixelitor.utils.Icons;
import pixelitor.utils.Messages;
import pixelitor.utils.ViewActivationListener;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.function.Supplier;

import static java.awt.event.ActionEvent.CTRL_MASK;

/**
 * An Action that adds a new adjustment layer to the active composition.
 */
public class AddAdjLayerAction extends NamedAction implements ViewActivationListener {
    public static final AddAdjLayerAction INSTANCE = new AddAdjLayerAction();

    public static List<Action> actions = List.of(
        createAction(ColorBalance::new, ColorBalance.NAME),
        createAction(Colorize::new, Colorize.NAME),
        createAction(ToneCurvesFilter::new, ToneCurvesFilter.NAME),
        createAction(GradientMap::new, GradientMap.NAME),
        createAction(HueSat::new, HueSat.NAME)
//        createAction(Levels::new, Levels.NAME)
    );

    private AddAdjLayerAction() {
        super("Add Adjustment Layer",
            Icons.loadThemed("add_adj_layer.png", ThemedImageIcon.GREEN));
        setToolTip("Adds a new adjustment layer.");
        setEnabled(false);
        Views.addActivationListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            boolean ctrlPressed = (e.getModifiers() & CTRL_MASK) == CTRL_MASK;
            if (ctrlPressed) {
                FilterAction action = FilterSearchPanel.showInDialog();
                if (action != null) {
                    Filter filter = action.createNewInstanceFilter();
                    addAdjustmentLayer(filter, filter.getName());
                }
                return;
            }

            JButton source = (JButton) e.getSource();
            JPopupMenu popup = createActionsPopup();
            Dimension size = popup.getPreferredSize();
            popup.show(source, 0, -size.height);
        } catch (Exception ex) {
            Messages.showException(ex);
        }
    }

    private static JPopupMenu createActionsPopup() {
        JPopupMenu popup = new JPopupMenu();
        for (Action action : actions) {
            popup.add(action);
        }
        popup.pack();
        return popup;
    }

    private static Action createAction(Supplier<Filter> factory, String name) {
        return new OpenViewEnabledAction(name + " Adjustment") {
            @Override
            protected void onClick() {
                addAdjustmentLayer(factory, name);
            }
        };
    }

    private static void addAdjustmentLayer(Supplier<Filter> factory, String name) {
        Filter filter = factory.get();
        filter.setName(name);
        addAdjustmentLayer(filter, name);
    }

    private static void addAdjustmentLayer(Filter filter, String name) {
        var comp = Views.getActiveComp();
        var adjustmentLayer = new AdjustmentLayer(comp, name, filter);

        new LayerAdder(comp)
            .withHistory("New Adjustment Layer")
            .add(adjustmentLayer);

        adjustmentLayer.edit();
    }

    @Override
    public void allViewsClosed() {
        setEnabled(false);
    }

    @Override
    public void viewActivated(View oldView, View newView) {
        setEnabled(true);
    }
}