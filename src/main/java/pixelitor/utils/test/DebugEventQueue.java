/*
 * Copyright 2010-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.utils.test;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * A queue of pixelitor events that is used for debugging
 */
public class DebugEventQueue {
    private DebugEventQueue() {
        // Utility class
    }

    private static final int EVENTS_MAX_SIZE = 20;

    private static final List<PixelitorEvent> list = new LinkedList<>();

    public static void post(PixelitorEvent event) {
        list.add(event);

        if (list.size() > EVENTS_MAX_SIZE) {
            list.remove(0);
        }
    }

    /**
     * Dumps the last events. Called if there are problems found.
     */
    public static void dump() {
        Collections.sort(list);

        int lastEventsSize = list.size();
        System.out.println("DebugEventQueue - the last " + lastEventsSize + " events:");

        for (PixelitorEvent event : list) {
            System.out.println(event.toString());
        }
    }
}
