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

package pixelitor.menus;

import com.jhlabs.image.AbstractBufferedImageOp;
import pixelitor.filters.Filter;
import pixelitor.filters.util.FilterAction;

import javax.swing.*;
import java.util.function.Supplier;

/**
 * A JMenu with some utility methods
 */
public class PMenu extends JMenu {
    public PMenu(String s) {
        super(s);
    }

    public PMenu(String s, char mnemonic) {
        super(s);
        setMnemonic(mnemonic);
    }

    public void add(Action action, KeyStroke keyStroke) {
        JMenuItem menuItem = new JMenuItem(action);
        menuItem.setAccelerator(keyStroke);
        add(menuItem);
    }

    public void add(Action action, String name) {
        super.add(action).setName(name);
    }

    public void addFilter(String name, Supplier<Filter> supplier) {
        add(new FilterAction(name, supplier));
    }

    public void addFilterWithoutGUI(String name, Supplier<Filter> supplier) {
        add(new FilterAction(name, supplier).withoutDialog());
    }

    public void addFilterWithoutGUI(String name, Supplier<Filter> supplier,
                                    KeyStroke keyStroke) {
        add(new FilterAction(name, supplier).withoutDialog(), keyStroke);
    }

    public void addFilter(String name, Supplier<Filter> supplier, KeyStroke keyStroke) {
        add(new FilterAction(name, supplier), keyStroke);
    }

    /**
     * Simple add for forwarding filters
     */
    public void addForwardingFilter(String name, Supplier<AbstractBufferedImageOp> op) {
        add(FilterAction.forwarding(name, op, true));
    }

    public void addNoGrayForwardingFilter(String name, Supplier<AbstractBufferedImageOp> op) {
        add(FilterAction.forwarding(name, op, false));
    }
}
