package pixelitor.filters.truchets;

import java.awt.Point;
import java.util.*;
import java.util.stream.Stream;

public class TruchetProceduralPattern extends TruchetConfigurablePattern {

    HashMap<Integer, ArrayList<Point>> forwardMap = new HashMap<>();
    HashMap<Point, Integer> reverseMap = new HashMap<>();
    private boolean isInternalCall = true;

    public TruchetProceduralPattern(int rows, int columns) {
        super(rows, columns);
    }

    public void setState(ProceduralStateSpace selectedItem) {
        isInternalCall = true;
        Queue<Point> stash = new LinkedList<>();
        forwardMap.values().forEach(points -> {
            stash.addAll(points);
            points.clear();
        });
        forwardMap.clear();
        reverseMap.clear();
        Random r = new Random();
        var space = selectedItem.createState(getColumns(), getRows(), 12, r);
        for (int i = 0; i < getRows(); i++) {
            for (int j = 0; j < getColumns(); j++) {
                int state = selectedItem.getState(getColumns(), getRows(), j, i, 12, r, space);
                setState(i, j, state);

                var point = stash.isEmpty() ? new Point() : stash.poll();
                point.setLocation(j, i);
                if (!forwardMap.containsKey(state)) {
                    forwardMap.put(state, new ArrayList<>());
                }
                forwardMap.get(state).add(point);
                reverseMap.put(point, state);
            }
        }
        stash.clear();
        isInternalCall = false;
    }

    @Override
    public void setState(int row, int column, int state) {
        if (isInternalCall) {
            super.setState(row, column, state);
        } else {
            int stateReference /* Which may not reflect the actual state in pattern! */
                = reverseMap.get(new Point(column, row));
            forwardMap.get(stateReference).forEach(point -> super.setState(point.y, point.x, state));
        }
    }

    @Override
    public Stream<Point> streamHighlightRule(int mouseX, int mouseY) {
        Integer state = reverseMap.get(new Point(mouseX, mouseY));
        System.out.println("forwardMap.get(state) = " + forwardMap.get(state));
        return forwardMap.get(state).stream();
    }
}