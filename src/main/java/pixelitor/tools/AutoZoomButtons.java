/*
 * Copyright (c) 2015 Laszlo Balazs-Csiki
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

package pixelitor.tools;

import pixelitor.ImageComponents;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class AutoZoomButtons {
    private AutoZoomButtons() {
    }

    public static final Action FIT_SCREEN_ACTION = new AbstractAction("Fit Screen") {
        @Override
        public void actionPerformed(ActionEvent e) {
            ImageComponents.fitActiveToScreen();
        }
    };

    public static final Action ACTUAL_PIXELS_ACTION = new AbstractAction("Actual Pixels") {
        @Override
        public void actionPerformed(ActionEvent e) {
            ImageComponents.fitActiveToActualPixels();
        }
    };

}
