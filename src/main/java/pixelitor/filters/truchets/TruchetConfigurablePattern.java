package pixelitor.filters.truchets;

import java.util.ArrayList;

public class TruchetConfigurablePattern implements TruchetPattern {

    ArrayList<ArrayList<Integer>> pattern = new ArrayList<>();
    int rotation;
    boolean symmetricAboutHorizontal;
    boolean symmetricAboutVertical;

    public TruchetConfigurablePattern(int rows, int columns) {
        updateRows(rows);
        updateColumns(columns);
    }

    public void update(int rows, int column) {
        updateRows(rows);
        updateColumns(column);
    }

    public void updateRows(int rows) {
        if (rotation != 0) {
            rows = rows & 0xfffffffe;
        }
        while (rows < getRows()) {
            removeRow();
        }
        while (rows > getRows()) {
            addRow();
        }
    }

    public void addRow() {
        if (pattern.size() == 0) {
            ArrayList<Integer> list = new ArrayList<>();
            list.add(0);
            pattern.add(list);
            return;
        }
        ArrayList<Integer> newRow = new ArrayList<>(pattern.get(pattern.size() / 2));
        pattern.add(pattern.size() / 2, newRow);
    }

    public void removeRow() {
        pattern.remove(pattern.size() / 2);
    }

    public void updateColumns(int columns) {
        if (rotation != 0) {
            columns = columns & 0xfffffffe;
        }
        while (columns < getColumns()) {
            removeColumn();
        }
        while (columns > getColumns()) {
            addColumn();
        }
    }

    public void addColumn() {
        for (ArrayList<Integer> row : pattern) {
            row.add(row.size() / 2, row.get(row.size() / 2));
        }
    }

    public void removeColumn() {
        for (ArrayList<Integer> row : pattern) {
            row.remove(row.size() / 2);
        }
    }

    @Override
    public int getRows() {
        return pattern.size();
    }

    @Override
    public int getColumns() {
        return pattern.get(0).size();
    }

    @Override
    public int getState(int row, int column) {
        return pattern.get(row).get(column);
    }

    public void setState(int row, int column, int state) {
        if (rotation != 0 && (row >= (getRows() + 1) / 2 || column >= (getColumns() + 1) / 2)) {
            return;
        } else if ((symmetricAboutHorizontal && row >= (getRows() + 1) / 2) ||
            (symmetricAboutVertical && column >= (getColumns() + 1) / 2)) {
            return;
        }
        pattern.get(row).set(column, state);
        if (rotation != 0) {
            if (rotation == 1) {
                pattern.get(column).set(getColumns() - row - 1, state);
                pattern.get(getRows() - column - 1).set(row, state);
                pattern.get(getRows() - row - 1).set(getColumns() - column - 1, state);
            }
        } else {
            if (symmetricAboutVertical) {
                pattern.get(row).set(getColumns() - column - 1, state);
            }
            if (symmetricAboutHorizontal) {
                pattern.get(getRows() - row - 1).set(column, state);
            }
            if (symmetricAboutVertical && symmetricAboutHorizontal) {
                pattern.get(getRows() - row - 1).set(getColumns() - column - 1, state);
            }
        }
    }

    public void setSymmetricAboutHorizontal(boolean symmetricAboutHorizontal) {
        this.symmetricAboutHorizontal = symmetricAboutHorizontal;
        if (symmetricAboutHorizontal) {
            for (int i = 0; i < getRows() / 2; i++) {
                var target = pattern.get(getRows() - i - 1);
                target.clear();
                target.addAll(pattern.get(i));
            }
        }
    }

    public void setSymmetricAboutVertical(boolean symmetricAboutVertical) {
        this.symmetricAboutVertical = symmetricAboutVertical;
        if (symmetricAboutVertical) {
            for (ArrayList<Integer> row : pattern) {
                for (int i = 0; i < getColumns() / 2; i++) {
                    row.set(getColumns() - i - 1, row.get(i));
                }
            }
        }
    }

    @Override
    public void sharePatternTweaks(int row, int column, TileState tileState) {
        if (rotation == 0) {
            tileState.flipAboutHorizontal = symmetricAboutHorizontal && row >= getRows() / 2;
            tileState.flipAboutVertical = symmetricAboutVertical && column >= getColumns() / 2;
        } else {
            if (rotation == 1) {
                if (column >= (getColumns() + 1) / 2) {
                    tileState.rotation += 1;
                    if (row >= (getRows() + 1) / 2) {
                        tileState.rotation += 1;
                    }
                } else if (row >= (getRows() + 1) / 2) {
                    tileState.rotation += 3;
                }
            }
        }
    }

    public void setRotation(int rotation) {
        this.rotation = rotation;
        if (rotation > 0) {
            symmetricAboutVertical = false;
            symmetricAboutHorizontal = false;
        }
    }
}