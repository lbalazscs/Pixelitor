/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.gui;

import pixelitor.Views;
import pixelitor.gui.utils.PAction;
import pixelitor.utils.Lazy;

import javax.swing.*;

import static pixelitor.utils.Keys.CTRL_SHIFT_TAB;
import static pixelitor.utils.Keys.CTRL_TAB;

/**
 * An {@link ImageAreaUI} implementation
 * where the edited images are in tabs
 */
public class TabsUI extends JTabbedPane implements ImageAreaUI {
    private final Lazy<JMenu> cachedPlacementMenu = Lazy.of(this::createTabPlacementMenu);
    private boolean userInitiated = true;

    public TabsUI() {
        setTabPlacement(ImageArea.getTabPlacement());
        addChangeListener(e -> tabsChanged());

        InputMap inputMap = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(CTRL_TAB, "navigateNext");
        inputMap.put(CTRL_SHIFT_TAB, "navigatePrevious");
    }

    private void tabsChanged() {
        if (!userInitiated) {
            return;
        }

        int selectedIndex = getSelectedIndex();
        if (selectedIndex != -1) { // it is -1 if all tabs have been closed
            TabViewContainer tab = (TabViewContainer) getComponentAt(selectedIndex);
            tab.activated();
        }
    }

    @Override
    public void activateView(View view) {
        TabViewContainer tab = (TabViewContainer) view.getViewContainer();
        setSelectedIndex(indexOfComponent(tab));
    }

    @Override
    public void addView(View view) {
        TabViewContainer tab = new TabViewContainer(view, this);
        view.setViewContainer(tab);

        int myIndex = getTabCount();

        try {
            userInitiated = false;
            addTab(view.getName(), tab);
        } finally {
            userInitiated = true;
        }

        setTabComponentAt(myIndex, new TabTitleRenderer(view.getName(), tab));
        setSelectedIndex(myIndex);
        tab.activated();
    }

    public static void warnAndCloseTab(TabViewContainer tab) {
        // this will call closeTab
        Views.warnAndClose(tab.getView());
    }

    public void closeTab(TabViewContainer tab) {
        remove(indexOfComponent(tab));
        View view = tab.getView();
        Views.viewClosed(view);
    }

    public void selectTab(TabViewContainer tab) {
        setSelectedIndex(indexOfComponent(tab));
    }

    private JMenu createTabPlacementMenu() {
        JMenu menu = new JMenu("Tab Placement");

        JRadioButtonMenuItem topMI = createTabPlacementMenuItem("Top", TOP);
        JRadioButtonMenuItem bottomMI = createTabPlacementMenuItem("Bottom", BOTTOM);
        JRadioButtonMenuItem leftMI = createTabPlacementMenuItem("Left", LEFT);
        JRadioButtonMenuItem rightMI = createTabPlacementMenuItem("Right", RIGHT);

        ButtonGroup group = new ButtonGroup();
        group.add(topMI);
        group.add(bottomMI);
        group.add(leftMI);
        group.add(rightMI);

        assert tabPlacement == ImageArea.getTabPlacement();
        JRadioButtonMenuItem selected = switch (tabPlacement) {
            case TOP -> topMI;
            case BOTTOM -> bottomMI;
            case LEFT -> leftMI;
            case RIGHT -> rightMI;
            default -> throw new IllegalStateException("tabPlacement = " + tabPlacement);
        };
        selected.setSelected(true);

        menu.add(topMI);
        menu.add(bottomMI);
        menu.add(leftMI);
        menu.add(rightMI);
        return menu;
    }

    private JRadioButtonMenuItem createTabPlacementMenuItem(String name, int pos) {
        return new JRadioButtonMenuItem(new PAction(name, () -> {
            setTabPlacement(pos); // update the GUI
            ImageArea.setTabPlacement(pos); // store it for the saved preferences
        }));
    }

    public JMenu getTabPlacementMenu() {
        return cachedPlacementMenu.get();
    }
}
