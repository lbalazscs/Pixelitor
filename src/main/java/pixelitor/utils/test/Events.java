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

package pixelitor.utils.test;

import pixelitor.Composition;
import pixelitor.gui.ImageComponent;
import pixelitor.gui.ImageComponents;
import pixelitor.history.PixelitorEdit;
import pixelitor.layers.Layer;
import pixelitor.layers.MaskViewMode;

import java.util.LinkedList;
import java.util.List;

/**
 * Events happening inside the app - used for debugging
 */
public class Events {
    private Events() {
        // Utility class
    }

    private static final int MAX_SIZE = 100;

    private static final List<PixelitorEvent> eventList = new LinkedList<>();

    public static void post(PixelitorEvent event) {
        eventList.add(event);

        if (eventList.size() > MAX_SIZE) {
            eventList.remove(0);
        }


    }

    public static void postListenerEvent(String type, Composition comp, Layer layer) {
        post(new PixelitorEvent("[LISTENER] " + type, comp, layer));
    }

    public static void postAssertJEvent(String type) {
        postAssertJEvent(type, null, null);
    }

    public static void postAssertJEvent(String type, Composition comp, Layer layer) {
        post(new PixelitorEvent("[ASSERTJ] " + type, comp, layer));
    }

    public static void postAddToHistoryEvent(PixelitorEdit edit) {
        post(new PixelitorEvent("    [ADD TO HIST] " + edit.getDebugName(), null, null));
    }

    public static void postUndoEvent(PixelitorEdit editToBeUndone) {
        String editName = editToBeUndone.getDebugName();
        post(new PixelitorEvent("    [UNDO " + editName + "]", null, null));
    }

    public static void postRedoEvent(PixelitorEdit editToBeRedone) {
        String editName = editToBeRedone.getDebugName();
        post(new PixelitorEvent("    [REDO " + editName + "]", null, null));
    }

    public static void postMaskViewActivate(MaskViewMode mode, ImageComponent ic, Layer layer) {
        post(new PixelitorEvent("[MASK VIEW " + mode.toString() + "]", ic.getComp(), layer));
    }

    /**
     * An event that signalizes the start of a RandomGUITest step
     */
    public static void postRandomTestEvent(String description) {
        post(new PixelitorEvent("[RAND] " + description, null, null));
    }

    /**
     * Dumps the last events for the given Composition.
     */
    public static void dumpActive() {
        Composition comp = ImageComponents.getActiveCompOrNull();
        eventList.stream()
                .filter(e -> e.isComp(comp))
                .forEach(System.out::println);
    }

    /**
     * Dumps the last events.
     */
    public static void dumpAll() {
        eventList.stream()
                .forEach(System.out::println);
    }
}
