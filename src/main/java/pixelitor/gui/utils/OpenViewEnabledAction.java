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

package pixelitor.gui.utils;

import pixelitor.Views;
import pixelitor.gui.View;
import pixelitor.utils.ViewActivationListener;

import javax.swing.*;

/**
 * An Action that is enabled only if at least one view is opened.
 */
public abstract class OpenViewEnabledAction extends PAction implements ViewActivationListener {
    protected OpenViewEnabledAction() {
        init();
    }

    protected OpenViewEnabledAction(String name) {
        super(name);
        init();
    }

    protected OpenViewEnabledAction(String name, Icon icon) {
        super(name, icon);
        init();
    }

    private void init() {
        setEnabled(false);
        Views.addActivationListener(this);
    }

    @Override
    public void viewActivated(View oldView, View newView) {
        setEnabled(true);
    }

    @Override
    public void allViewsClosed() {
        setEnabled(false);
    }
}
