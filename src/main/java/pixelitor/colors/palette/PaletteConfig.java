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

package pixelitor.colors.palette;

import javax.swing.*;
import java.awt.FlowLayout;

/**
 * An interactive configuration for a {@link Palette}, typically with sliders.
 */
public interface PaletteConfig {
    /**
     * Creates the configuration panel with its controls.
     *
     * @param onChange callback to run when a control changes
     */
    JPanel createConfigPanel(Runnable onChange);

    /**
     * A simple, non-interactive config that returns an empty panel, without any controls.
     */
    PaletteConfig NONE = _ -> new JPanel(
        // ensure that the panel takes up no vertical space
        new FlowLayout(FlowLayout.CENTER, 0, 0));
}
