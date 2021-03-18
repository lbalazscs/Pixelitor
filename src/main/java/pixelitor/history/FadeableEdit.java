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
package pixelitor.history;

import pixelitor.Composition;
import pixelitor.layers.Drawable;

import java.awt.image.BufferedImage;

/**
 * Abstract superclass of all PixelitorEdits that can be faded
 */
public abstract class FadeableEdit extends PixelitorEdit {
    private final Drawable fadingLayer;

    // actually some ImageEdits are possibly not fadeable
    protected boolean fadeable = true;

    FadeableEdit(String name, Composition comp, Drawable fadingLayer) {
        super(name, comp);
        this.fadingLayer = fadingLayer;
    }

    public abstract BufferedImage getBackupImage();

    public boolean isFadeable() {
        return fadeable;
    }

    public void setFadeable(boolean fadeable) {
        this.fadeable = fadeable;
    }

    public Drawable getFadingLayer() {
        return fadingLayer;
    }
}
