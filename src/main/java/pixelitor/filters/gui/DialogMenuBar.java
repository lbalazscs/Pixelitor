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

package pixelitor.filters.gui;

import pixelitor.gui.GUIText;

import javax.swing.*;

/**
 * A menu bar for dialogs that support presets or help.
 * Intended for components that implement {@link DialogMenuOwner}.
 */
public class DialogMenuBar extends JMenuBar {
    private final DialogMenuOwner owner;
    private JMenu presetsMenu;

    public DialogMenuBar(DialogMenuOwner owner) {
        this(owner, true);
    }

    public DialogMenuBar(DialogMenuOwner owner, boolean addPresets) {
        this.owner = owner;

        if (addPresets) {
            addPresetsMenu();
        }

        if (owner.hasHelp()) {
            add(owner.getHelp().createMenu());
        }
    }

    private void addPresetsMenu() {
        presetsMenu = new JMenu(GUIText.PRESETS);

        if (owner.hasBuiltinPresets()) {
            addBuiltInPresets();
        }

        if (owner.shouldHaveUserPresetsMenu()) {
            addUserPresets();
        }

        assert presetsMenu.getMenuComponentCount() > 0;
        add(presetsMenu);
    }

    private void addUserPresets() {
        if (owner.hasBuiltinPresets()) {
            presetsMenu.addSeparator();
        }

        new UserPresetMenuHelper(owner, presetsMenu, presetsMenu)
            .addSaveAndUserPresets();
    }

    private void addBuiltInPresets() {
        JMenu builtinPresets = new JMenu(GUIText.BUILT_IN_PRESETS);
        Preset[] presets = owner.getBuiltinPresets();

        for (Preset preset : presets) {
            builtinPresets.add(preset.createAction(owner));
        }
        presetsMenu.add(builtinPresets);
    }
}
