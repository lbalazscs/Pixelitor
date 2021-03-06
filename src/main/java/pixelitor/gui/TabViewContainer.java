/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.OpenImages;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.gui.utils.PAction;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.event.MouseEvent;
import java.io.File;

import static java.awt.BorderLayout.CENTER;
import static pixelitor.utils.Texts.i18n;

/**
 * A {@link ViewContainer} used in the tabs UI.
 */
public class TabViewContainer extends JComponent implements ViewContainer {
    private final View view;
    private final JScrollPane scrollPane;
    private final TabsUI tabsUI;

    public TabViewContainer(View view, TabsUI tabsUI) {
        this.view = view;
        this.tabsUI = tabsUI;
        scrollPane = new JScrollPane(this.view);
        setLayout(new BorderLayout());
        add(scrollPane, CENTER);
    }

    @Override
    public JScrollPane getScrollPane() {
        return scrollPane;
    }

    @Override
    public void close() {
        tabsUI.closeTab(this);
    }

    @Override
    public void select() {
        tabsUI.selectTab(this);
    }

    @Override
    public void updateTitle(View view) {
        int myIndex = tabsUI.indexOfComponent(this);
        if (myIndex != -1) {
            var tabComponent = (TabTitleRenderer) tabsUI.getTabComponentAt(myIndex);
            tabComponent.setTitle(view.getName());
        }
    }

    @Override
    public void ensurePositiveLocation() {
        // nothing to do
    }

    public void activated() {
        OpenImages.viewActivated(view);
    }

    public View getView() {
        return view;
    }

    void mousePressedOnTab(MouseEvent e) {
        if (e.isPopupTrigger()) {
            showPopup(e);
        } else {
            // for some reason adding a mouse listener
            // to a tab stops the normal tab selection
            // from working, so select it manually
            select();
        }
    }

    void mouseReleasedOnTab(MouseEvent e) {
        if (e.isPopupTrigger()) {
            showPopup(e);
        }
    }

    private void showPopup(MouseEvent mouse) {
        JPopupMenu popup = new JPopupMenu();

        // close the clicked one, even if it is not the active!
        popup.add(new PAction(i18n("close")) {
            @Override
            public void onClick() {
                OpenImages.warnAndClose(view);
            }
        });
        popup.add(new PAction("Close Others") {
            @Override
            public void onClick() {
                OpenImages.warnAndCloseAllBut(view);
            }
        });
        popup.add(OpenImages.CLOSE_UNMODIFIED_ACTION);
        popup.add(OpenImages.CLOSE_ALL_ACTION);

        if (Desktop.isDesktopSupported()) {
            var comp = view.getComp();
            File file = comp.getFile();
            if (file != null && file.exists()) {
                popup.addSeparator();
                popup.add(GUIUtils.createShowInFolderAction(file));

                if (canBePrintedByOS(file)) {
                    popup.add(GUIUtils.createPrintFileAction(comp, file));
                }
            }
        }

        popup.addSeparator();
        popup.add(tabsUI.getTabPlacementMenu());

        popup.show(mouse.getComponent(), mouse.getX(), mouse.getY());
    }

    private static boolean canBePrintedByOS(File file) {
        String fileName = file.getName().toLowerCase();

        if (fileName.endsWith("pxc")) {
            return false;
        }

        if (fileName.endsWith("ora")) {
            return false;
        }

        return Desktop.getDesktop().isSupported(Desktop.Action.PRINT);
    }
}
