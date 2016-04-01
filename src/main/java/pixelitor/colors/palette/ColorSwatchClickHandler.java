/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

import pixelitor.colors.FgBgColors;

import javax.swing.*;
import java.awt.Color;
import java.awt.event.MouseEvent;

public interface ColorSwatchClickHandler {
    void onClick(Color newColor, MouseEvent e);

    public static final ColorSwatchClickHandler STANDARD = (newColor, e) -> {
        boolean rightClick = SwingUtilities.isRightMouseButton(e);
        if (rightClick) {
            FgBgColors.setBG(newColor);
        } else {
            FgBgColors.setFG(newColor);
        }
    };
}
