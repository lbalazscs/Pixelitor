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

import pixelitor.layers.ContentLayer;

public class ContentLayerNode extends LayerNode {
    public ContentLayerNode(ContentLayer layer) {
        this("content layer", layer);
    }

    public ContentLayerNode(String name, ContentLayer layer) {
        super(name, layer);

        addInt("translation X", layer.getTx());
        addInt("translation Y", layer.getTy());
    }
}
