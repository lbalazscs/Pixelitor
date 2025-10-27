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

package pixelitor.gui;

import pixelitor.AppMode;
import pixelitor.Composition;
import pixelitor.Views;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.gui.utils.TaskAction;
import pixelitor.utils.debug.Debug;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Locale;

import static java.awt.BorderLayout.CENTER;
import static pixelitor.utils.Texts.i18n;

/**
 * A {@link ViewContainer} used in the tabs UI.
 */
public class TabViewContainer extends JComponent implements ViewContainer {
    private static final boolean DESKTOP_PRINT_SUPPORTED = Desktop.isDesktopSupported()
        && Desktop.getDesktop().isSupported(Desktop.Action.PRINT);
    private final View view;
    private final JScrollPane scrollPane;
    private final TabsUI tabsUI;
    private TabTitleRenderer titleRenderer;
    
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

    public void setTitleRenderer(TabTitleRenderer titleRenderer) {
        this.titleRenderer = titleRenderer;
    }

    @Override
    public void updateTitle(View view) {
        if (titleRenderer != null) {
            titleRenderer.setTitle(view.getName());
        }
    }

    public void activated() {
        Views.viewActivated(view);
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

    private void showPopup(MouseEvent e) {
        JPopupMenu popup = new JPopupMenu();

        addRenameAction(popup);
        popup.addSeparator();
        addCloseActions(popup);
        if (Desktop.isDesktopSupported()) {
            addFileActions(e, popup);
        }
        popup.addSeparator();
        popup.add(tabsUI.getTabPlacementMenu());
        if (AppMode.isDevelopment()) {
            addDebugAction(popup);
        }

        popup.show(e.getComponent(), e.getX(), e.getY());
    }

    private void addRenameAction(JPopupMenu popup) {
        popup.add(new TaskAction("Rename...", () ->
            view.getComp().renameInteractively(this)));
    }

    private void addCloseActions(JPopupMenu popup) {
        // close the clicked one, even if it isn't the active!
        popup.add(new TaskAction(i18n("close"), () ->
            Views.warnAndClose(view)));

        popup.add(new TaskAction("Close Others", () ->
            Views.warnAndCloseAllBut(view)));
        popup.add(Views.CLOSE_ALL_UNMODIFIED_ACTION);
        popup.add(Views.CLOSE_ALL_ACTION);
    }

    private void addFileActions(MouseEvent e, JPopupMenu popup) {
        Composition comp = view.getComp();
        File file = comp.getFile();
        if (file != null && file.exists()) {
            popup.addSeparator();
            popup.add(GUIUtils.createShowInFolderAction(file));

            if (canBePrintedByOS(file)) {
                popup.add(GUIUtils.createPrintFileAction(
                    comp, file, e.getComponent()));
            }
        }
    }

    private static boolean canBePrintedByOS(File file) {
        if (!DESKTOP_PRINT_SUPPORTED) {
            return false;
        }

        String fileName = file.getName().toLowerCase(Locale.ROOT);
        if (fileName.endsWith("pxc") || fileName.endsWith("ora")) {
            return false;
        }

        return true;
    }

    private void addDebugAction(JPopupMenu popup) {
        popup.add(new TaskAction("Debug View...", () ->
            Debug.showTree(view, "View " + view.getName())));
    }
}
