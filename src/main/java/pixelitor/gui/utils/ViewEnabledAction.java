/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

import javax.swing.*;
import java.util.function.Consumer;

/**
 * An {@link AbstractViewEnabledAction} implementation that delegates to a given task.
 */
public class ViewEnabledAction extends AbstractViewEnabledAction {
    private final Consumer<Composition> task;

    public ViewEnabledAction(String name, Consumer<Composition> task) {
        super(name);
        this.task = task;
    }

    public ViewEnabledAction(String name, Icon icon, Consumer<Composition> task) {
        super(name, icon);
        this.task = task;
    }

    @Override
    protected void onClick(Composition comp) {
        task.accept(comp);
    }
}
