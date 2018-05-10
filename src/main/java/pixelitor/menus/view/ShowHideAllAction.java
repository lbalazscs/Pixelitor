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
    public void setVisibility(boolean value) {
        PixelitorWindow pw = PixelitorWindow.getInstance();
        if (!value) {
            histogramsWereShown = pw.areHistogramsShown();
            layersWereShown = LayersContainer.areLayersShown();
            statusBarWasShown = StatusBar.INSTANCE.isShown();
            toolsWereShown = pw.areToolsShown();
        }
        if (histogramsWereShown) {
            pw.setHistogramsVisibility(value, false);
        }

        if (layersWereShown) {
            pw.setLayersVisibility(value, false);
        }

        if (statusBarWasShown) {
            pw.setStatusBarVisibility(value, false);
        }

        if (toolsWereShown) {
            pw.setToolsVisibility(value, false);
        }

        pw.getContentPane().revalidate();

        allHidden = !value;
    }

}