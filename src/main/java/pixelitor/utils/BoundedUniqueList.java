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
        list = new ArrayList<>();
    }

    public void addIfNotThere(E elem) {
        if (!list.contains(elem)) {
            list.add(elem);
        }
    }

    public void addToFront(E elem) {
        if (list.contains(elem)) {
            list.remove(elem);
        }
        list.add(0, elem); // add to the front

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

