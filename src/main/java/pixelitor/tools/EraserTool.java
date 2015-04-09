/*
 * Copyright 2015 Laszlo Balazs-Csiki
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

import pixelitor.Composition;
import pixelitor.layers.ImageLayer;

import java.awt.AlphaComposite;

import static java.awt.AlphaComposite.DST_OUT;

/**
 * The eraser tool.
 */
public class EraserTool extends DirectBrushTool {
    public EraserTool() {
        super('e', "Eraser", "erase_tool_icon.gif", "click and drag to erase pixels");
    }

    @Override
    void createGraphics(Composition comp, ImageLayer layer) {
        super.createGraphics(comp, layer);

        // the color does not matter as long as AlphaComposite.DST_OUT is used
        graphics.setComposite(AlphaComposite.getInstance(DST_OUT, 1.0f));
    }
}