/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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
import java.util.Iterator;
import java.util.List;

/**
 * A data structure for the recent files.
 * It  maintains a list of unique elements with a fixed maximum size.
 * If an element is added that already exists, it will be moved to the front.
 * When the size exceeds the maximum size, the oldest elements are removed.
 */
public class BoundedUniqueList<E> implements Iterable<E> {
    private final int maxSize;
    private final List<E> list;

    public BoundedUniqueList(int maxSize) {
        this.maxSize = maxSize;
        list = new ArrayList<>(maxSize);
    }

    public void addIfAbsent(E elem) {
        if (!list.contains(elem)) {
            list.add(elem);
            maintainBound();
        }
    }

    public void addToFront(E elem) {
        list.remove(elem);
        list.addFirst(elem);

        maintainBound();
    }

    private void maintainBound() {
        if (list.size() > maxSize) {
            list.removeLast();
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

    @Override
    public Iterator<E> iterator() {
        return list.iterator();
    }
}

