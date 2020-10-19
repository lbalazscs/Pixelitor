/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.filters.ParametrizedFilter;
import pixelitor.utils.OpenInBrowserAction;

import javax.swing.*;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static pixelitor.filters.gui.UserPreset.*;

public class FilterMenuBar extends JMenuBar {
    private final FilterWithGUI filter;
    private JMenu presetsMenu;
    private int numUserPresets = 0;
    private static final boolean CAN_USE_FILE_MANAGER = Desktop.isDesktopSupported()
        && Desktop.getDesktop().isSupported(Desktop.Action.OPEN);

    public FilterMenuBar(FilterWithGUI filter, boolean addPresets) {
        this.filter = filter;

        if (addPresets) {
            addPresetsMenu();
        }

        if (filter.hasHelp()) {
            JMenu helpMenu = new JMenu("Help");
            helpMenu.add(new OpenInBrowserAction("Online Help", filter.getHelpURL()));
            add(helpMenu);
        }
    }

    private void addPresetsMenu() {
        presetsMenu = new JMenu("Presets");

        if (filter.hasBuiltinPresets()) {
            JMenu builtinPresets = new JMenu("Built-in Presets");
            FilterState[] presets = filter.getBuiltinPresets();

            ParametrizedFilter pf = (ParametrizedFilter) filter;
            ParamSet paramSet = pf.getParamSet();

            for (FilterState preset : presets) {
                builtinPresets.add(preset.asAction(paramSet));
            }
            presetsMenu.add(builtinPresets);
        }

        if (filter.canHaveUserPresets()) {
            if (filter.hasBuiltinPresets()) {
                presetsMenu.addSeparator();
            }
            Action savePresetAction = new AbstractAction("Save Preset...") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    filter.saveAsPreset(FilterMenuBar.this);
                }
            };
            JMenuItem savePresetMI = new JMenuItem(savePresetAction);
            savePresetMI.setName("savePreset");
            presetsMenu.add(savePresetMI);
            List<UserPreset> userPresets = loadPresets(filter.getName());
            numUserPresets = userPresets.size();
            if (numUserPresets > 0) {
                addManagePresetsMenu();
                presetsMenu.addSeparator();
            }
            for (UserPreset preset : userPresets) {
                ParamSet paramSet = ((ParametrizedFilter) filter).getParamSet();
                presetsMenu.add(preset.asAction(paramSet));
            }

            add(presetsMenu);
        }
    }

    private void addManagePresetsMenu() {
        if (!CAN_USE_FILE_MANAGER) {
            return;
        }
        presetsMenu.add(new AbstractAction("Manage Presets...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String dirPath = PRESETS_DIR + FILE_SEPARATOR + filter.getName();
                    Desktop.getDesktop().open(new File(dirPath));
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    public void addNewUserPreset(UserPreset preset, ParamSet paramSet) {
        if (numUserPresets == 0) {
            addManagePresetsMenu();
            presetsMenu.addSeparator();
            numUserPresets++;
        }
        Action presetAction = preset.asAction(paramSet);
        JMenuItem presetMI = new JMenuItem(presetAction);
        presetMI.setName(preset.getName());
        presetsMenu.add(presetMI);
        preset.save();
    }
}
