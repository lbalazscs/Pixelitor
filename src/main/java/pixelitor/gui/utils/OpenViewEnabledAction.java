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

import pixelitor.Composition;
import pixelitor.Views;
import pixelitor.gui.View;
import pixelitor.utils.Messages;
import pixelitor.utils.ViewActivationListener;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.function.Consumer;

/**
 * An Action that is enabled only if at least one view is opened.
 */
public class OpenViewEnabledAction extends NamedAction implements ViewActivationListener {
    private final Consumer<Composition> task;

    protected OpenViewEnabledAction(Consumer<Composition> task) {
        this.task = task;
        init();
    }

    public OpenViewEnabledAction(String name, Consumer<Composition> task) {
        super(name);
        this.task = task;
        init();
    }

    protected OpenViewEnabledAction(String name, Icon icon, Consumer<Composition> task) {
        super(name, icon);
        this.task = task;
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

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            task.accept(Views.getActiveComp());
        } catch (Exception ex) {
            Messages.showException(ex);
        }
    }

    // for subclasses, where it's not practical to pass a task
    public abstract static class Checked extends OpenViewEnabledAction {
        protected Checked() {
            super(null);
        }

        protected Checked(String name) {
            super(name, null);
        }

        protected Checked(String name, Icon icon) {
            super(name, icon, null);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                onClick(Views.getActiveComp());
            } catch (Exception ex) {
                Messages.showException(ex);
            }
        }

        protected abstract void onClick(Composition comp);
    }
}
