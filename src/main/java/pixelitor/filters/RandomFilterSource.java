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

package pixelitor.filters;

import pixelitor.filters.util.Filters;

import java.util.ArrayList;
import java.util.List;

/**
 * Supplies random filters for the "Random Filter" filter
 * It is not reused across "Random Filter" dialog sessions
 */
public class RandomFilterSource {
    // the history of the random filters generated so far
    private final List<Filter> history = new ArrayList<>();

    // points to the filter currently being displayed
    // -1 means empty state
    private int currentIndex = -1;

    /**
     * Returns the next filter from the history
     */
    public Filter getNext() {
        // makes sense only if we already went back in history
        assert hasNext();

        currentIndex++;
        return history.get(currentIndex);
    }

    /**
     * Returns the previous filter from the history
     */
    public Filter getPrevious() {
        // makes sense only if we already picked
        // a second random filter
        assert hasPrevious();

        currentIndex--;
        return history.get(currentIndex);
    }

    public Filter selectNewFilter() {
        // ensure we don't pick the same filter twice in a row
        Filter currentFilter = getCurrentFilter();

        Filter randomFilter = Filters.getRandomFilter(filter ->
            filter != currentFilter
                && !(filter instanceof RandomFilter));

        if (currentIndex < history.size() - 1) { // the user went back
            // truncate forward history, because a new path was taken
            history.subList(currentIndex + 1, history.size()).clear();
        }

        history.add(randomFilter);
        currentIndex++;

        return randomFilter;
    }

    public boolean hasPrevious() {
        return currentIndex > 0;
    }

    public boolean hasNext() {
        return currentIndex < history.size() - 1;
    }

    /**
     * Returns the filter currently pointed to by currentIndex,
     * or null if the history is empty.
     */
    public Filter getCurrentFilter() {
        if (currentIndex >= 0 && currentIndex < history.size()) {
            return history.get(currentIndex);
        }
        return null;
    }

    @Override
    public String toString() {
        return '{' +
            "history=" + history +
            ", currentIndex=" + currentIndex +
            '}';
    }
}
