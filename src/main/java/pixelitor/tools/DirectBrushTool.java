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

package pixelitor.tools;

import java.awt.Cursor;

/**
 * A brush tool that draws directly into the image of
 * the current image layer
 */
public abstract class DirectBrushTool extends AbstractBrushTool {
    protected DirectBrushTool(char activationKeyChar, String name, String iconFileName, String toolMessage, Cursor cursor) {
        super(activationKeyChar, name, iconFileName, toolMessage, cursor);
        drawStrategy = DrawStrategy.DIRECT;
    }
}
