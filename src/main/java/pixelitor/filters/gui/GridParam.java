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

package pixelitor.filters.gui;

import pixelitor.filters.jhlabsproxies.JHWeave;

import javax.swing.*;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A {@link FilterParam} that is the data model for a grid editor filter parameter.
 */
public class GridParam extends AbstractFilterParam {
    /**
     * A named, predefined grid pattern.
     */
    public record Preset(String name, int[][] data) {
    }

    private static final String CUSTOM_PRESET_NAME = "Custom";

    private int[][] data;
    private final int[][] defaultData;
    private final List<GridCellPainter> painters;
    private final int originalWidth;
    private final int originalHeight;

    private final List<Preset> presets;
    private String selectedPresetName;

    public GridParam(String name, List<Preset> presets, List<GridCellPainter> painters) {
        this(name, presets, painters, 0);
    }

    public GridParam(String name, List<Preset> presets, List<GridCellPainter> painters, int defaultPresetIndex) {
        super(name, RandomizeMode.ALLOW_RANDOMIZE);

        if (presets.isEmpty()) {
            throw new IllegalArgumentException();
        }
        if (defaultPresetIndex < 0 || defaultPresetIndex >= presets.size()) {
            throw new IllegalArgumentException("defaultPresetIndex = " + defaultPresetIndex);
        }
        this.presets = List.copyOf(presets);

        if (painters.isEmpty()) {
            throw new IllegalArgumentException();
        }
        this.painters = List.copyOf(painters);

        Preset defaultPreset = this.presets.get(defaultPresetIndex);
        this.defaultData = deepCopy(defaultPreset.data());
        this.data = deepCopy(defaultPreset.data());
        this.selectedPresetName = defaultPreset.name();

        validateData(this.data, this.painters.size());

        this.originalWidth = getGridCols();
        this.originalHeight = getGridRows();
    }

    @Override
    public JComponent createGUI() {
        paramGUI = new GridParamGUI(this);
        syncWithGui();
        return (JComponent) paramGUI;
    }

    @Override
    public boolean isAtDefault() {
        return Arrays.deepEquals(data, defaultData);
    }

    @Override
    public void reset(boolean trigger) {
        setData(defaultData, trigger);
    }

    @Override
    protected void doRandomize() {
        int rows = data.length;
        int cols = data[0].length;
        int numPainters = painters.size();
        int[][] newData = new int[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                newData[r][c] = ThreadLocalRandom.current().nextInt(numPainters);
            }
        }
        setData(newData, false);
    }

    @Override
    public boolean isAnimatable() {
        return false;
    }

    @Override
    public boolean isComplex() {
        return true;
    }

    @Override
    public ParamState<?> copyState() {
        return new GridParamState(deepCopy(data));
    }

    @Override
    public void loadStateFrom(ParamState<?> state, boolean updateGUI) {
        if (state instanceof GridParamState gridState) {
            internalSetData(gridState.data(), true);
            if (updateGUI && paramGUI != null) {
                paramGUI.updateGUI();
            }
        }
    }

    @Override
    public void loadStateFrom(String savedValue) {
        if (savedValue == null || savedValue.isEmpty()) {
            throw new IllegalArgumentException();
        }
        try {
            String[] rows = savedValue.split(";", -1);
            if (rows.length == 0) {
                throw new IllegalArgumentException();
            }

            // determine dimensions from the first row
            String[] firstRowValues = rows[0].split(",", -1);
            int numCols = firstRowValues.length;
            int[][] newData = new int[rows.length][numCols];

            for (int r = 0; r < rows.length; r++) {
                String[] values = rows[r].split(",", -1);
                if (values.length != numCols) {
                    // inconsistent number of columns in the saved data
                    throw new IllegalArgumentException();
                }
                for (int c = 0; c < numCols; c++) {
                    newData[r][c] = Integer.parseInt(values[c].trim());
                }
            }
            // update the GUI, but do not trigger the filter
            internalSetData(newData, true);
            if (paramGUI != null) {
                paramGUI.updateGUI();
            }
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            // recovery for legacy (4.3.0) Weave saved values
            for (Preset preset : JHWeave.WEAVE_PRESETS) {
                if (preset.name().equals(savedValue)) {
                    internalSetData(preset.data(), true);
                    paramGUI.updateGUI();
                    return;
                }
            }
            throw new IllegalArgumentException("Could not parse: \"" + savedValue + "\"", e);
        }
    }

    @Override
    public String getValueAsString() {
        return new GridParamState(data).toSaveString();
    }

    /**
     * Cycles the value of a single cell in the grid and notifies listeners.
     */
    public void cycleData(int row, int col) {
        if (row < 0 || row >= getGridRows() || col < 0 || col >= getGridCols()) {
            return; // out of bounds
        }
        int currentValue = data[row][col];
        int newValue = (currentValue + 1) % painters.size();
        data[row][col] = newValue;
        updateSelectedPreset();
        notifyChange(true);
    }

    /**
     * Notifies the GUI and adjustment listener of a change in the parameter's state.
     */
    private void notifyChange(boolean triggerListener) {
        if (paramGUI != null) {
            paramGUI.updateGUI();
        }
        if (triggerListener) {
            trigger();
        }
    }

    public void trigger() {
        if (adjustmentListener != null) {
            adjustmentListener.paramAdjusted();
        }
    }

    /**
     * Sets an entirely new data grid and notifies listeners.
     */
    public void setData(int[][] newData) {
        setData(newData, true);
    }

    /**
     * Sets an entirely new data grid, with control over listener notification.
     */
    public void setData(int[][] newData, boolean trigger) {
        if (internalSetData(newData, false)) {
            notifyChange(trigger);
        }
    }

    /**
     * Replaces the internal data array after validation, returning true if a change occurred.
     */
    private boolean internalSetData(int[][] newData, boolean force) {
        validateData(newData, painters.size());
        if (!force && Arrays.deepEquals(data, newData)) {
            return false; // no change
        }
        this.data = deepCopy(newData);
        updateSelectedPreset();
        return true; // data changed
    }

    private void updateSelectedPreset() {
        for (Preset preset : presets) {
            if (Arrays.deepEquals(data, preset.data())) {
                selectedPresetName = preset.name();
                return;
            }
        }
        selectedPresetName = CUSTOM_PRESET_NAME;
    }

    /**
     * Returns a list of all available preset names, including "Custom".
     */
    public List<String> getPresetNames() {
        List<String> names = new ArrayList<>();

        for (Preset preset : presets) {
            names.add(preset.name());
        }
        names.add(CUSTOM_PRESET_NAME);

        return names;
    }

    /**
     * Returns the name of the currently selected preset or "Custom".
     */
    public String getSelectedPresetName() {
        return selectedPresetName;
    }

    /**
     * Sets the grid data to match the named preset.
     */
    public void selectPreset(String name) {
        if (CUSTOM_PRESET_NAME.equals(name) || name == null) {
            return; // "Custom" is a state, not a selectable item
        }
        for (Preset preset : presets) {
            if (preset.name().equals(name)) {
                setData(preset.data(), true);
                break;
            }
        }
    }

    /**
     * Returns a direct reference to the internal data array for read-only access; do not modify.
     */
    public int[][] getData() {
        return data;
    }

    /**
     * Returns the list of painters for this grid.
     */
    public List<GridCellPainter> getPainters() {
        return painters;
    }

    /**
     * Dynamically changes the width of the grid.
     * <p>
     * When increasing the width, new columns are padded with 0s and added in an
     * alternating fashion to keep the original grid centered: the first
     * new column is added to the right, the second to the left, and so on.
     * When decreasing, columns are removed in the reverse order.
     */
    public void setGridWidth(int newWidth) {
        int currentWidth = getGridCols();
        if (newWidth == currentWidth) {
            return; // no change
        }

        if (newWidth > currentWidth) {
            for (int i = currentWidth; i < newWidth; i++) {
                incrementGridWidth();
            }
        } else {
            for (int i = currentWidth; i > newWidth; i--) {
                decrementGridWidth();
            }
        }
    }

    /**
     * Dynamically changes the height of the grid.
     * <p>
     * When increasing the height, new rows are padded with 0s and added in an
     * alternating fashion to keep the original grid centered: the first
     * new row is added to the bottom, the second to the top, and so on.
     * When decreasing, rows are removed in the reverse order.
     */
    public void setGridHeight(int newHeight) {
        int currentHeight = getGridRows();
        if (newHeight == currentHeight) {
            return; // no change
        }

        if (newHeight > currentHeight) {
            for (int i = currentHeight; i < newHeight; i++) {
                incrementGridHeight();
            }
        } else {
            for (int i = currentHeight; i > newHeight; i--) {
                decrementGridHeight();
            }
        }
    }

    /**
     * Increases the grid width by one column, adding it to the left or right to maintain centering.
     */
    private void incrementGridWidth() {
        int[][] currentData = getData();
        int currentHeight = getGridRows();
        int currentWidth = getGridCols();
        int newWidth = currentWidth + 1;

        int[][] newData = new int[currentHeight][newWidth];

        int totalColsAdded = currentWidth - originalWidth;
        // alternating logic: 1st=right, 2nd=left, 3rd=right...
        // add left if the number of columns already added is odd
        boolean addLeft = (totalColsAdded % 2 != 0);

        if (addLeft) {
            // add new column at the left (index 0), copy old data after it
            for (int r = 0; r < currentHeight; r++) {
                // newData[r][0] is already 0 by default
                System.arraycopy(currentData[r], 0, newData[r], 1, currentWidth);
            }
        } else {
            // add new column at the right, copy old data first
            for (int r = 0; r < currentHeight; r++) {
                System.arraycopy(currentData[r], 0, newData[r], 0, currentWidth);
                // newData[r][newWidth - 1] is already 0 by default
            }
        }
        setData(newData);
    }

    /**
     * Decreases the grid width by one column, removing it from the left or right to maintain centering.
     */
    private void decrementGridWidth() {
        int[][] currentData = getData();
        int currentHeight = getGridRows();
        int currentWidth = getGridCols();
        int newWidth = currentWidth - 1;

        // prevent shrinking to nothing
        if (newWidth <= 0) {
            return;
        }

        int[][] newData = new int[currentHeight][newWidth];

        int totalColsAdded = currentWidth - originalWidth;
        // remove the last column that was added to reverse the process
        // if totalColsAdded is even (and >0), the last add was to the left
        boolean removeLeft = (totalColsAdded > 0) && (totalColsAdded % 2 == 0);

        if (removeLeft) {
            // remove leftmost column by copying from the second column
            for (int r = 0; r < currentHeight; r++) {
                System.arraycopy(currentData[r], 1, newData[r], 0, newWidth);
            }
        } else {
            // remove rightmost column by copying all but the last column
            for (int r = 0; r < currentHeight; r++) {
                System.arraycopy(currentData[r], 0, newData[r], 0, newWidth);
            }
        }
        setData(newData);
    }

    /**
     * Increases the grid height by one row, adding it to the top or bottom to maintain centering.
     */
    private void incrementGridHeight() {
        int[][] currentData = getData();
        int currentHeight = getGridRows();
        int currentWidth = getGridCols();
        int newHeight = currentHeight + 1;

        int[][] newData = new int[newHeight][currentWidth];

        int totalRowsAdded = currentHeight - originalHeight;
        // alternating logic: 1st=bottom, 2nd=top, 3rd=bottom...
        // add top if the number of rows already added is odd
        boolean addTop = (totalRowsAdded % 2 != 0);

        if (addTop) {
            // add new row at the top (index 0)
            // newData[0] is already an array of 0s
            // copy old data to newData starting from index 1
            System.arraycopy(currentData, 0, newData, 1, currentHeight);
        } else {
            // add new row at the bottom
            // copy old data to newData starting from index 0
            System.arraycopy(currentData, 0, newData, 0, currentHeight);
            // newData[newHeight - 1] is already an array of 0s
        }
        setData(newData);
    }

    /**
     * Decreases the grid height by one row, removing it from the top or bottom to maintain centering.
     */
    private void decrementGridHeight() {
        int[][] currentData = getData();
        int currentHeight = getGridRows();
        int currentWidth = getGridCols();
        int newHeight = currentHeight - 1;

        // prevent shrinking to nothing
        if (newHeight <= 0) {
            return;
        }

        int[][] newData = new int[newHeight][currentWidth];

        int totalRowsAdded = currentHeight - originalHeight;
        // remove the last row that was added to reverse the process
        // if totalRowsAdded is even (and >0), the last add was to the top
        boolean removeTop = (totalRowsAdded > 0) && (totalRowsAdded % 2 == 0);

        if (removeTop) {
            // remove topmost row, copy from currentData[1] onwards
            System.arraycopy(currentData, 1, newData, 0, newHeight);
        } else {
            // remove bottommost row, copy from currentData[0] onwards
            System.arraycopy(currentData, 0, newData, 0, newHeight);
        }
        setData(newData);
    }

    public int getGridRows() {
        return (data != null) ? data.length : 0;
    }

    public int getGridCols() {
        return (data != null && data.length > 0) ? data[0].length : 0;
    }

    /**
     * Validates the integrity of a grid data array.
     */
    private static void validateData(int[][] data, int numPainters) {
        Objects.requireNonNull(data, "Data array cannot be null.");
        if (data.length == 0) {
            throw new IllegalArgumentException("Data array cannot have zero rows.");
        }
        int firstRowLength = data[0].length;
        if (firstRowLength == 0) {
            throw new IllegalArgumentException("Data array cannot have zero columns.");
        }

        for (int r = 0; r < data.length; r++) {
            if (data[r] == null) {
                throw new IllegalArgumentException("Row " + r + " is null.");
            }
            if (data[r].length != firstRowLength) {
                throw new IllegalArgumentException("All rows in the data array must have the same length.");
            }
            for (int c = 0; c < data[r].length; c++) {
                int value = data[r][c];
                if (value < 0 || value >= numPainters) {
                    throw new IllegalArgumentException(
                        "Data value " + value + " at [" + r + "][" + c + "] is out of the valid range [0, " + (numPainters - 1) + "]."
                    );
                }
            }
        }
    }

    /**
     * Creates a deep copy of a 2D integer array.
     */
    private static int[][] deepCopy(int[][] original) {
        if (original == null) {
            return null;
        }
        int[][] result = new int[original.length][];
        for (int i = 0; i < original.length; i++) {
            result[i] = Arrays.copyOf(original[i], original[i].length);
        }
        return result;
    }

    /**
     * Encapsulates the state of a {@link GridParam} as a memento object.
     */
    public record GridParamState(int[][] data) implements ParamState<GridParamState> {
        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public GridParamState interpolate(GridParamState endState, double progress) {
            throw new UnsupportedOperationException(); // not animatable
        }

        @Override
        public String toSaveString() {
            if (data == null) {
                return "";
            }
            return Stream.of(data)
                .map(row -> Arrays.stream(row)
                    .mapToObj(String::valueOf)
                    .collect(Collectors.joining(",")))
                .collect(Collectors.joining(";"));
        }
    }
}
