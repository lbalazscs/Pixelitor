/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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
package pixelitor.utils.debug;

import pixelitor.filters.painters.TextSettings;
import pixelitor.layers.TextLayer;

/**
 * A debugging node for a {@link TextLayer}
 */
public class TextLayerNode extends ContentLayerNode {
    public TextLayerNode(TextLayer layer) {
        this("text layer " + layer.getName(), layer);
    }

    public TextLayerNode(String name, TextLayer layer) {
        super(name, layer);

        TextSettings settings = layer.getSettings();
        if (settings == null) {
            addString("text settings", "NO TEXT SETTINGS!");
        } else {
            add(settings.createDebugNode("text settings"));
        }
    }
}
