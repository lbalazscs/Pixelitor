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

package pixelitor.tools.brushes;

import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.FilterParam;

import javax.swing.*;
import java.awt.FlowLayout;
import java.util.function.Consumer;

/**
 * The settings of a {@link OnePixelBrush}
 */
public class OnePixelBrushSettings extends BrushSettings {
    private static final String AA_TEXT = "Anti-aliasing";
    private final BooleanParam aaParam =
        new BooleanParam(AA_TEXT, false);
    private JComponent aaGUI;

    @Override
    protected void forEachParam(Consumer<FilterParam> consumer) {
        consumer.accept(aaParam);
    }

    @Override
    protected JPanel createConfigPanel() {
        JPanel p = new JPanel(new FlowLayout());
        p.add(new JLabel(AA_TEXT));
        if (aaGUI == null) {
            aaGUI = aaParam.createGUI("aa");
        }
        p.add(aaGUI);

        return p;
    }

    public boolean hasAA() {
        return aaParam.isChecked();
    }
}
