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

package pixelitor.gui;

import javax.swing.*;
import java.awt.BorderLayout;

/**
 * An {@link ImageWindow} used in the tabs UI.
 */
public class ImageTab extends JComponent implements ImageWindow {
    private final ImageComponent ic;
    private final JScrollPane scrollPane;
    private final TabsUI tabsUI;

    public ImageTab(ImageComponent ic, TabsUI tabsUI) {
        this.ic = ic;
        this.tabsUI = tabsUI;
        scrollPane = new JScrollPane(this.ic);
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
    public void setTitle(String title) {
        // the title for tabs should not be updated
        throw new UnsupportedOperationException("tabs");
    }

    @Override
    public void ensurePositiveLocation() {
        // nothing to do
    }

    public void onActivation() {
        ImageComponents.imageActivated(ic);
    }

    public ImageComponent getIC() {
        return ic;
    }
}
