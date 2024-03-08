package pixelitor.filters.truchets;

import com.jhlabs.image.ImageMath;

import java.awt.Point;
import java.util.ArrayList;
import java.util.stream.Stream;

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

    public int getRowMidInclusive() {
        return getRows() / 2;
    }

    public int getRowMidExclusive() {
        return (getRows() + 1) / 2;
    }

    public int getColMidInclusive() {
        return getColumns() / 2;
    }

    public int getColMidExclusive() {
        return (getColumns() + 1) / 2;
    }

    public int reflectAboutHorizontal(int row) {
        return getRows() - row - 1;
    }

    public int reflectAboutVertical(int column) {
        return getColumns() - column - 1;
    }

    public boolean isRowOnMargin(int row) {
        return getRows() % 2 == 1 && getRows() / 2 == row;
    }

    public boolean isColOnMargin(int column) {
        return getColumns() % 2 == 1 && getColumns() / 2 == column;
    }

    @Override
    public int getState(int row, int column) {
        return pattern.get(row).get(column);
    }

    public void setState(int row, int column, int state) {
        if (rotation != 0) {
            rotateAndSetState(row, column, 0, state);
            switch (rotation) {
                case 1 -> {
                    if (isRowOnMargin(row) && isColOnMargin(column)) {
                        // Do nothing
                    } else if (isRowOnMargin(row) || isColOnMargin(column)) {
                        rotateAndSetState(row, column, 2, state);
                    } else {
                        rotateAndSetState(row, column, 1, state);
                        rotateAndSetState(row, column, 2, state);
                        rotateAndSetState(row, column, 3, state);
                    }
                }
                case 2 -> {
                    if (isRowOnMargin(row) && isColOnMargin(column)) {
                        // Do nothing
                    } else {
                        rotateAndSetState(row, column, 2, state);
                    }
                }
            }
        } else {
            setStateFreely(row, column, state);
            if (symmetricAboutHorizontal && !isRowOnMargin(row)) {
                setStateFreely(reflectAboutHorizontal(row), column, state);
            }
            if (symmetricAboutVertical && !isColOnMargin(column)) {
                setStateFreely(row, reflectAboutVertical(column), state);
            }
            if (symmetricAboutHorizontal && !isRowOnMargin(row) &&
                symmetricAboutVertical && !isColOnMargin(column)) {
                setStateFreely(reflectAboutHorizontal(row), reflectAboutVertical(column), state);
            }
        }
    }

    protected void setStateFreely(int row, int column, int state) {
        pattern.get(row).set(column, state);
    }

    private void rotateAndSetState(int row, int column, int quarters, int state) {
        switch (ImageMath.mod(quarters, 4)) {
            case 0 -> setStateFreely(row, column, state);
            case 1 -> setStateFreely(column, reflectAboutVertical(row), state);
            case 2 -> setStateFreely(reflectAboutHorizontal(row), reflectAboutVertical(column), state);
            case 3 -> setStateFreely(reflectAboutHorizontal(column), row, state);
        }
    }

    public void setSymmetricAboutHorizontal(boolean symmetricAboutHorizontal) {
        this.symmetricAboutHorizontal = symmetricAboutHorizontal;
        if (symmetricAboutHorizontal) {
            for (int row = 0; row < getRowMidInclusive(); row++) {
                var target = pattern.get(reflectAboutHorizontal(row));
                target.clear();
                target.addAll(pattern.get(row));
            }
        }
    }

    public void setSymmetricAboutVertical(boolean symmetricAboutVertical) {
        this.symmetricAboutVertical = symmetricAboutVertical;
        if (symmetricAboutVertical) {
            for (ArrayList<Integer> row : pattern) {
                for (int col = 0; col < getColMidInclusive(); col++) {
                    row.set(reflectAboutVertical(col), row.get(col));
                }
            }
        }
    }

    @Override
    public void sharePatternTweaks(int row, int column, TileState tileState) {
        if (rotation == 0) {
            tileState.flipAboutHorizontal = symmetricAboutHorizontal && row >= getRowMidExclusive();
            tileState.flipAboutVertical = symmetricAboutVertical && column >= getColMidExclusive();
        } else if (rotation == 1) {
            if (column >= getColMidExclusive()) {
                tileState.rotation += 1;
                if (row >= getRowMidExclusive()) {
                    tileState.rotation += 1;
                }
            } else if (row >= getRowMidExclusive()) {
                tileState.rotation += 3;
            }
        } else if (rotation == 2) {
            if (column >= getColMidExclusive()) {
                tileState.rotation += 2;
            }
        }
    }

    public void setRotation(int rotation) {
        this.rotation = rotation;
        if (rotation > 0) {
            symmetricAboutVertical = false;
            symmetricAboutHorizontal = false;
        }
        if (rotation == 1) {
            for (int row = 0; row < getRowMidInclusive(); row++) {
                for (int col = 0; col < getColMidInclusive(); col++) {
                    setState(row, col, getState(row, col));
                }
            }
        } else if (rotation == 2) {
            for (int row = 0; row < getRows(); row++) {
                for (int col = 0; col < getColMidExclusive(); col++) {
                    setState(row, col, getState(row, col));
                }
            }
        }
    }

    @Override
    public Stream<Point> streamHighlightRule(int mouseX, int mouseY) {
        return Stream.of(new Point(mouseX, mouseY));
    }
}