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

package pixelitor.utils;

import java.util.Arrays;

/**
 * A resizable list of primitive ints, faster than ArrayList<Integer>.
 */
public class IntList {
    private static final int DEFAULT_CAPACITY = 10;

    private int[] data;
    private int size;

    public IntList() {
        this(DEFAULT_CAPACITY);
    }

    public IntList(int initialCapacity) {
        data = new int[initialCapacity];
        size = 0;
    }

    public int get(int index) {
        return data[index];
    }

    public void add(int value) {
        if (size == data.length) {
            int newCapacity = (data.length == 0) ? DEFAULT_CAPACITY : data.length * 2;
            expandData(newCapacity);
        }
        data[size++] = value;
    }

    public void clear() {
        size = 0;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }

    public int[] toArray() {
        int[] result = new int[size];
        System.arraycopy(data, 0, result, 0, size);
        return result;
    }

    private void expandData(int newCapacity) {
        int[] newData = new int[newCapacity];
        System.arraycopy(data, 0, newData, 0, size);
        data = newData;
    }

    @Override
    public String toString() {
        return Arrays.toString(toArray());
    }
}
