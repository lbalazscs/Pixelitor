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
import java.util.List;

/**
 * A menu bar for dialogs that support presets or help.
 * Intended for components that implement {@link DialogMenuOwner}.
 */
public class DialogMenuBar extends JMenuBar {
    public DialogMenuBar(DialogMenuOwner owner) {
        if (hasPresets(owner)) {
            add(createPresetsMenu(owner));
        }

        Help help = owner.getHelp();
        if (help != null) {
            add(help.createMenu());
        }
    }

    /**
     * Returns true if a {@link DialogMenuBar} for this owner would have at least one menu.
     */
    public static boolean hasMenus(DialogMenuOwner owner) {
        return hasPresets(owner) || owner.getHelp() != null;
    }

    private static boolean hasPresets(DialogMenuOwner owner) {
        return owner.hasBuiltinPresets() || owner.shouldHaveUserPresetsMenu();
    }

    private static JMenu createPresetsMenu(DialogMenuOwner owner) {
        JMenu presetsMenu = new JMenu(GUIText.PRESETS);

        List<Preset> builtinPresets = owner.getBuiltinPresets();
        if (!builtinPresets.isEmpty()) {
            presetsMenu.add(createBuiltinPresetsMenu(owner, builtinPresets));
        }

        if (owner.shouldHaveUserPresetsMenu()) {
            addUserPresets(owner, presetsMenu);
        }

        assert presetsMenu.getMenuComponentCount() > 0;
        return presetsMenu;
    }

    private static JMenu createBuiltinPresetsMenu(DialogMenuOwner owner, List<Preset> presets) {
        JMenu builtinPresets = new JMenu(GUIText.BUILT_IN_PRESETS);

        for (Preset preset : presets) {
            builtinPresets.add(preset.createAction(owner));
        }
        return builtinPresets;
    }

    private static void addUserPresets(DialogMenuOwner owner, JMenu presetsMenu) {
        if (owner.hasBuiltinPresets()) {
            presetsMenu.addSeparator();
        }

        new UserPresetMenuHelper(owner, presetsMenu, presetsMenu)
            .addSaveAndUserPresets();
    }
}
