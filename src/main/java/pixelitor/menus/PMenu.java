/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.Composition;
import pixelitor.filters.Filter;
import pixelitor.filters.util.FilterAction;
import pixelitor.gui.utils.ViewEnabledAction;

import javax.swing.*;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A JMenu with some utility methods
 */
public class PMenu extends JMenu {
    public PMenu(String text) {
        super(text);
    }

    public PMenu(ResourceBundle i18n, String key) {
        super(i18n.getString(key));
    }

    public PMenu(String text, char mnemonic) {
        super(text);
        setMnemonic(mnemonic);
    }

    public PMenu(ResourceBundle i18n, String key, char mnemonic) {
        super(i18n.getString(key));
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

    public void addViewEnabled(String name, Consumer<Composition> task) {
        add(new ViewEnabledAction(name, task));
    }

    public void addViewEnabled(String name, String toolTip, Consumer<Composition> task) {
        ViewEnabledAction action = new ViewEnabledAction(name, task);
        action.setToolTip(toolTip);
        add(action);
    }

    public void addViewEnabled(String name, String toolTip, Consumer<Composition> task, KeyStroke keyStroke) {
        ViewEnabledAction action = new ViewEnabledAction(name, task);
        action.setToolTip(toolTip);
        add(action, keyStroke);
    }

    public void addViewEnabled(ResourceBundle i18n, String key, Consumer<Composition> task) {
        add(new ViewEnabledAction(i18n.getString(key), task));
    }

    public void addViewEnabledDialog(ResourceBundle i18n, String key, Consumer<Composition> task) {
        addViewEnabled(i18n.getString(key) + "...", task);
    }

    public void addViewEnabled(String name, Consumer<Composition> task, KeyStroke keyStroke) {
        add(new ViewEnabledAction(name, task), keyStroke);
    }

    public void addViewEnabled(ResourceBundle i18n, String key, Consumer<Composition> task, KeyStroke keyStroke) {
        add(new ViewEnabledAction(i18n.getString(key), task), keyStroke);
    }

    public void addViewEnabledDialog(ResourceBundle i18n, String key, Consumer<Composition> task, KeyStroke keyStroke) {
        add(new ViewEnabledAction(i18n.getString(key) + "...", task), keyStroke);
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
