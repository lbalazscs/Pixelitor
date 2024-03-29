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

package pixelitor.gui;

import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.GridBagHelper;

import javax.swing.*;
import java.awt.GridBagLayout;

public class IndexedModePanel extends JPanel {
    private final JCheckBox transparencyCB;

    public IndexedModePanel() {
        super(new GridBagLayout());
        GridBagHelper gbh = new GridBagHelper(this);
        transparencyCB = new JCheckBox("", true);
        gbh.addLabelAndControl("Transparency", transparencyCB);
    }

    public boolean supportTransparency() {
        return transparencyCB.isSelected();
    }

    public void showInDialog() {
        new DialogBuilder()
            .title("Convert to Indexed")
            .content(this)
            .show();
    }
}
