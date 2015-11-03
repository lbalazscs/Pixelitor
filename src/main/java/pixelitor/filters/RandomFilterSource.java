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

package pixelitor.filters;

import java.util.ArrayList;
import java.util.List;

/**
 * Supplies random filters for the "Random Filter" filter
 */
public class RandomFilterSource {
    private List<Filter> history = new ArrayList<>();

    // the index of the previous filter in history, or -1 if there isn't one
    private int previousIndex = -1;

    // the index of the next filter in history, or the size of the history if there isn't one
    private int nextIndex = 0;

    private Filter lastFilter;

    /**
     * Returns the next filter form the history
     */
    public Filter getNext() {
        assert hasNext();

        Filter filter = history.get(nextIndex);
        assert filter != null;

        previousIndex++;
        nextIndex++;

        return filter;
    }


    /**
     * Returns the previous filter form the history
     */
    public Filter getPrevious() {
        assert previousIndex >= 0;
        assert previousIndex < history.size();
        assert hasPrevious();

        Filter filter = history.get(previousIndex);
        assert filter != null;

        lastFilter = filter;

        previousIndex--;
        nextIndex--;
        return filter;
    }

    public Filter getRandom() {
        Filter randomFilter = FilterUtils.getRandomFilter(filter ->
                (filter != lastFilter
                        && (!(filter instanceof Fade))
                        && (!(filter instanceof RandomFilter))
                        ));

        if (lastFilter != null) { // not the first call
            previousIndex++;
        }

        if (hasNext()) {
            // we went back in history and then started to generate again
            // so we need to throw away the history after the current point
            history = new ArrayList<>(history.subList(0, nextIndex));
        }

        nextIndex++;
        history.add(randomFilter);

        lastFilter = randomFilter;
        return randomFilter;
    }

    public boolean hasPrevious() {
        return previousIndex >= 0 && !history.isEmpty();
    }

    public boolean hasNext() {
        return nextIndex < history.size();
    }

    public Filter getLastFilter() {
        return lastFilter;
    }

    @Override
    public String toString() {
        return '{' +
                "history=" + history +
                ", previousIndex=" + previousIndex +
                ", nextIndex=" + nextIndex +
                ", lastFilter=" + lastFilter +
                '}';
    }
}
