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

package pixelitor.tools.toolhandlers;

import pixelitor.tools.Tools;
import pixelitor.tools.util.PMouseEvent;

/**
 * A {@link ToolHandler} that enables temporary color picker
 * functionality for any tool when the Alt key is pressed.
 */
public class ColorPickerToolHandler extends ToolHandler {
    @Override
    boolean mousePressed(PMouseEvent e) {
        if (e.isAltDown()) {
            Tools.COLOR_PICKER.sampleColor(e, false);
            return true;
        }

        // forwards the event to the next handler
        return false;
    }

    @Override
    boolean mouseDragged(PMouseEvent e) {
        if (e.isAltDown()) {
            Tools.COLOR_PICKER.sampleColor(e, false);
            return true;
        }

        // forwards the event to the next handler
        return false;
    }

    @Override
    boolean mouseReleased(PMouseEvent e) {
        // always forwards the event to the next handler
        return false;
    }
}
