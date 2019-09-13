/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.menus.view;

import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.StatusBar;
import pixelitor.layers.LayersContainer;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * An either "Show Hidden" or "Hide All" action,
 * depending on the current visibility
 */
public class ShowHideAllAction extends ShowHideAction {
    public static final Action INSTANCE = new ShowHideAllAction();

    private boolean histogramsWereShown = false;
    private boolean layersWereShown = false;
    private boolean statusBarWasShown = false;
    private boolean toolsWereShown = false;

    private boolean allHidden = false;

    private ShowHideAllAction() {
        super("Show Hidden", "Hide All");
    }

    @Override
    public boolean getVisibilityAtStartUp() {
        return !allHidden;
    }

    @Override
    public boolean getCurrentVisibility() {
        return !allHidden;
    }

    @Override
    public void setVisibility(boolean visible, ActionEvent e) {
        if (e != null) {
            // We want to control this only with null events
            // The "Hide All" JMenuItem gets activated when everything
            // is shown (because of the menu renaming?),
            // and triggers non-null event which would hide all again
            return;
        }

        PixelitorWindow pw = PixelitorWindow.getInstance();
        if (!visible) {
            histogramsWereShown = pw.areHistogramsShown();
            layersWereShown = LayersContainer.areLayersShown();
            statusBarWasShown = StatusBar.INSTANCE.isShown();
            toolsWereShown = pw.areToolsShown();
        }
        if (histogramsWereShown) {
            pw.setHistogramsVisibility(visible, false);
        }

        if (layersWereShown) {
            pw.setLayersVisibility(visible, false);
        }

        if (statusBarWasShown) {
            pw.setStatusBarVisibility(visible, false);
        }

        if (toolsWereShown) {
            pw.setToolsVisibility(visible, false);
        }

        pw.getContentPane().revalidate();

        allHidden = !visible;
    }

}