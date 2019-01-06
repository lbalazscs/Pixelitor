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

package pixelitor.gui;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;

/**
 * An {@link ImageWindow} used in the tabs UI.
 */
public class ImageTab extends JComponent implements ImageWindow {
    private final CompositionView cv;
    private final JScrollPane scrollPane;
    private final TabsUI tabsUI;

    public ImageTab(CompositionView cv, TabsUI tabsUI) {
        this.cv = cv;
        this.tabsUI = tabsUI;
        scrollPane = new JScrollPane(this.cv);
        setLayout(new BorderLayout());
        this.add(scrollPane, BorderLayout.CENTER);
    }

    @Override
    public JScrollPane getScrollPane() {
        return scrollPane;
    }

    @Override
    public void dispose() {
        tabsUI.closeTab(this);
    }

    @Override
    public void select() {
        tabsUI.selectTab(this);
    }

    @Override
    public void updateTitle(CompositionView cv) {
        int myIndex = tabsUI.indexOfComponent(this);
        if (myIndex != -1) {
            TabsUI.TabTitleRenderer tabComponent = (TabsUI.TabTitleRenderer) tabsUI.getTabComponentAt(myIndex);
            tabComponent.setTitle(cv.getName());
        }
    }

    @Override
    public void ensurePositiveLocation() {
        // nothing to do
    }

    public void onActivation() {
        OpenComps.imageActivated(cv);
    }

    public CompositionView getIC() {
        return cv;
    }

    public void showPopup(MouseEvent mouse) {
        JPopupMenu popup = new JPopupMenu();

        // close the clicked one, even if it is not the active!
        popup.add(new AbstractAction("Close") {
            @Override
            public void actionPerformed(ActionEvent e) {
                OpenComps.warnAndClose(cv);
            }
        });
        popup.add(new AbstractAction("Close Others") {
            @Override
            public void actionPerformed(ActionEvent e) {
                OpenComps.warnAndCloseAllBut(cv);
            }
        });
        popup.add(OpenComps.CLOSE_UNMODIFIED_ACTION);
        popup.add(OpenComps.CLOSE_ALL_ACTION);
        popup.addSeparator();
        popup.add(tabsUI.getTabPlacementMenu());

        popup.show(mouse.getComponent(), mouse.getX(), mouse.getY());
    }
}
