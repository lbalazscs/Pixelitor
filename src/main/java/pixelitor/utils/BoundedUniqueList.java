/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * A data structure for the recent files.
 */
public class BoundedUniqueList<E> {
    private final int maxSize;
    private final List<E> list;

    public BoundedUniqueList(int maxSize) {
        this.maxSize = maxSize;
        list = new ArrayList<>(maxSize);
    }

    public void addIfNotThere(E elem) {
        if (!list.contains(elem)) {
            list.add(elem);
        }
    }

    public void addToFront(E elem) {
        list.remove(elem); // just to be sure
        list.addFirst(elem); // add to the front

        if (list.size() > maxSize) {
            list.remove(maxSize);
        }
    }

    public void clear() {
        list.clear();
    }

    public int size() {
        return list.size();
    }

    public E get(int index) {
        return list.get(index);
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }
}

