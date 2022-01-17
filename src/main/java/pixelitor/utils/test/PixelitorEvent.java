/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.AppContext;
import pixelitor.Composition;
import pixelitor.Views;
import pixelitor.layers.Layer;
import pixelitor.utils.Threads;
import pixelitor.utils.debug.Ansi;

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
        if (!AppContext.isDevelopment()) {
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
            Composition activeComp = Views.getActiveComp();
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

        return format("%s (%s) on \"%s/%s\" (%s, %s, %s) at %s",
            type, Ansi.yellow(threadName), Ansi.red(comp.getName()), layer.getName(),
            layer.getClass().getSimpleName(), getSelectionInfo(), getMaskInfo(),
            dateFormatter.format(now));
    }

    private String getSelectionInfo() {
        String selectionInfo = "no selection";
        if (comp.hasSelection()) {
            Rectangle2D bounds = comp.getSelection().getShapeBounds2D();
            selectionInfo = format("sel. bounds = ['x=%.1f, y=%.1f, w=%.1f, h=%.1f']",
                bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
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

    @Override
    public String toString() {
        return message;
    }
}
