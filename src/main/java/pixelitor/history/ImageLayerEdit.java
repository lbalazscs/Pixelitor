/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.history;

import pixelitor.Composition;
import pixelitor.layers.ImageLayer;

import java.awt.image.BufferedImage;

public class ImageLayerEdit extends PixelitorEdit {
    private final ImageLayer layer;
    private final BufferedImage oldImage;
    private final BufferedImage newImage;

    public ImageLayerEdit(String name, Composition comp,
                          ImageLayer layer, BufferedImage oldImage) {
        super(name, comp);

        this.layer = layer;
        this.oldImage = oldImage;
        // capture final state
        this.newImage = layer.getImage();
    }

    @Override
    public void undo() {
        super.undo();
        layer.setImage(oldImage);
        layer.getHolder().update(false);
    }

    @Override
    public void redo() {
        super.redo();
        layer.setImage(newImage);
        layer.getHolder().update(false);
    }
}
