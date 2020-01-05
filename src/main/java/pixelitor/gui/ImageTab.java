/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.io.File;

import static java.awt.BorderLayout.CENTER;

/**
 * A {@link ViewContainer} used in the tabs UI.
 */
public class ImageTab extends JComponent implements ViewContainer {
    private final View view;
    private final JScrollPane scrollPane;
    private final TabsUI tabsUI;

    public ImageTab(View view, TabsUI tabsUI) {
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
    public void dispose() {
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
            TabsUI.TabTitleRenderer tabComponent = (TabsUI.TabTitleRenderer) tabsUI.getTabComponentAt(myIndex);
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

    public void showPopup(MouseEvent mouse) {
        JPopupMenu popup = new JPopupMenu();

        // close the clicked one, even if it is not the active!
        popup.add(new AbstractAction("Close") {
            @Override
            public void actionPerformed(ActionEvent e) {
                OpenImages.warnAndClose(view);
            }
        });
        popup.add(new AbstractAction("Close Others") {
            @Override
            public void actionPerformed(ActionEvent e) {
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

                String fileName = file.getName();
                if (!fileName.endsWith("pxc")
                        && !fileName.endsWith("ora")
                        && Desktop.getDesktop().isSupported(Desktop.Action.PRINT)) {
                    popup.add(GUIUtils.createPrintFileAction(comp, file));
                }
            }
        }

        popup.addSeparator();
        popup.add(tabsUI.getTabPlacementMenu());

        popup.show(mouse.getComponent(), mouse.getX(), mouse.getY());
    }
}
