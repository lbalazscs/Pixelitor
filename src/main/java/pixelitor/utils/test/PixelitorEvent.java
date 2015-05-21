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

package pixelitor.utils.test;

import pixelitor.Composition;
import pixelitor.ImageComponents;
import pixelitor.layers.Layer;

import java.awt.Rectangle;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

/**
 * An event that occurred inside Pixelitor.
 * Used for debugging.
 */
public abstract class PixelitorEvent {
    private final String description;
    private final Date date;
    private final Composition comp;
    private final Layer layer;
    private static final Format dateFormatter = new SimpleDateFormat("HH:mm:ss:SSS");

    protected PixelitorEvent(String description) {
        assert description != null;

        this.description = description;

        date = new Date();

        Optional<Composition> opt = ImageComponents.getActiveComp();
        if (opt.isPresent()) {
            comp = opt.get();
            layer = comp.getActiveLayer();
        } else {
            comp = null;
            layer = null;
        }
    }

    public String getMessage() {
        return description;
    }

    @Override
    public String toString() {
        String selectionInfo = "no selection";
        if (comp.hasSelection()) {
            Rectangle rect = comp.getSelection().get().getShapeBounds();
            selectionInfo = String.format("sel. bounds = '%s'", rect.toString());
        }
        return String.format("%s on \"%s/%s\" (%s) at %s",
                description, comp.getName(), layer.getName(), selectionInfo, dateFormatter.format(date));
    }
}
