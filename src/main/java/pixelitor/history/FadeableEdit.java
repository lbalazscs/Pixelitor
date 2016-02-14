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
package pixelitor.history;

import pixelitor.Composition;
import pixelitor.layers.ImageLayer;

import java.awt.image.BufferedImage;

/**
 * Abstract superclass of all PixelitorEdits that can be faded
 */
public abstract class FadeableEdit extends PixelitorEdit {
    private final ImageLayer fadingLayer;
    private boolean died = false;  // the variable "alive" in AbstractUndoableEdit is private...

    // actually some ImageEdits are possibly not fadeable
    protected boolean fadeable = true;

    FadeableEdit(Composition comp, ImageLayer fadingLayer, String name) {
        super(comp, name);
        this.fadingLayer = fadingLayer;
    }

    public abstract BufferedImage getBackupImage();

    @Override
    public void die() {
        super.die();

        died = true;
    }

    public boolean isAlive() {
        return !died;
    }

    public boolean isFadeable() {
        return fadeable;
    }

    public void setFadeable(boolean fadeable) {
        this.fadeable = fadeable;
    }

    public ImageLayer getFadingLayer() {
        return fadingLayer;
    }
}
