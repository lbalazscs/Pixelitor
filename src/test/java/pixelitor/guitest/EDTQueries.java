/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.guitest;

import org.assertj.swing.edt.GuiActionRunner;
import pixelitor.gui.ImageComponent;
import pixelitor.gui.ImageComponents;

import java.awt.Rectangle;

/**
 * Queries that run on the EDT, and return their result for other threads
 */
public class EDTQueries {
    private EDTQueries() {
    }

    public static Rectangle getCanvasBounds() {
        return GuiActionRunner.execute(() -> {
            ImageComponent ic = ImageComponents.getActiveIC();
            return ic.getVisibleCanvasBoundsOnScreen();
        });
    }
}
