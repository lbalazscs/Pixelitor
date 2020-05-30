/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.OpenImages;
import pixelitor.RunContext;
import pixelitor.layers.Layer;
import pixelitor.utils.Threads;

import java.awt.geom.Rectangle2D;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static java.lang.String.format;

/**
 * An event that occurred inside Pixelitor.
 * Used for debugging.
 */
public class PixelitorEvent {
    private final String message;
    private final LocalTime now;
    private final String threadName;
    private final Composition comp;
    private final Layer layer;
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("HH:mm:ss:SSS");

    public PixelitorEvent(String type, Composition comp, Layer layer) {
        assert type != null;
        if (!RunContext.isDevelopment()) {
            throw new IllegalStateException("should be used only for development");
        }

        now = LocalTime.now();
        if (Threads.calledOnEDT()) {
            threadName = "EDT";
        } else {
            threadName = Threads.threadName();
        }

        if (comp == null) {
            assert layer == null;
            Composition activeComp = OpenImages.getActiveComp();
            if (activeComp != null) {
                this.comp = activeComp;
                this.layer = activeComp.getActiveLayer();
            } else {
                this.comp = null;
                this.layer = null;
            }
        } else {
            this.comp = comp;
            if (layer == null) {
                this.layer = comp.getActiveLayer();
            } else {
                this.layer = layer;
            }
        }

        message = stateAsString(type);
    }

    private String stateAsString(String type) {
        if (comp == null) { // "all images are closed" is also an event
            return format("%s (%s) no composition", type, threadName);
        }

        String layerType = layer.getClass().getSimpleName();
        String formattedDate = dateFormatter.format(now);
        return format("%s (%s) on \"%s/%s\" (%s, %s, %s) at %s",
            type, threadName, comp.getName(), layer.getName(),
            layerType, getSelectionInfo(), getMaskInfo(), formattedDate);
    }

    private String getSelectionInfo() {
        String selectionInfo = "no selection";
        if (comp.hasSelection()) {
            Rectangle2D rect = comp.getSelection().getShapeBounds2D();
            selectionInfo = format("sel. bounds = '%s'", rect);
        }
        return selectionInfo;
    }

    private String getMaskInfo() {
        String maskInfo = "no mask";
        if (layer.hasMask()) {
            maskInfo = format("has mask (enabled = %s, editing = %s, linked = %s)",
                layer.isMaskEnabled(), layer.isMaskEditing(), layer.getMask().isLinked());
        }
        return maskInfo;
    }

    public boolean isComp(Composition c) {
        if (c == null) {
            return true;
        }
        return comp == c;
    }

    @Override
    public String toString() {
        return message;
    }
}
