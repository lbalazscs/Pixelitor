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

import java.io.Serial;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * Allows treating multiple {@link ParamState} objects as a single
 * {@link ParamState} ("Composite design pattern").
 */
public class CompositeParamState implements ParamState<CompositeParamState> {
    @Serial
    private static final long serialVersionUID = 1L;

    private final List<ParamState<?>> children;

    /**
     * Creates a composite state from a list of child states.
     */
    public CompositeParamState(List<ParamState<?>> children) {
        this.children = children;
    }

    /**
     * Creates a composite state by copying the state of all animatable parameters.
     */
    public CompositeParamState(FilterParam[] params) {
        children = Arrays.stream(params)
            .filter(FilterParam::isAnimatable)
            .map(FilterParam::copyState)
            .collect(toList());
    }

    @Override
    public CompositeParamState interpolate(CompositeParamState endState, double progress) {
        int numChildren = children.size();
        if (numChildren != endState.children.size()) {
            throw new IllegalStateException("%d != %d".formatted(
                numChildren, endState.children.size()));
        }
        List<ParamState<?>> interpolatedStates = new ArrayList<>(numChildren);
        for (int i = 0; i < numChildren; i++) {
            // each ParamState is interpolated independently
            interpolatedStates.add(interpolateChild(endState, progress, i));
        }
        return new CompositeParamState(interpolatedStates);
    }

    /**
     * Interpolates a single child state with its corresponding end state.
     */
    @SuppressWarnings("unchecked")
    private <T extends ParamState<T>> ParamState<T> interpolateChild(CompositeParamState endState, double progress, int i) {
        ParamState<?> startChildState = children.get(i);
        ParamState<?> endChildState = endState.get(i);

        if (!startChildState.getClass().equals(endChildState.getClass())) {
            throw new IllegalStateException(String.format(
                "Incompatible types at index %d: %s vs %s",
                i, startChildState.getClass(), endChildState.getClass()));
        }

        // the cast is safe because we checked the runtime types
        T start = (T) startChildState;
        T end = (T) endChildState;

        return start.interpolate(end, progress);
    }

    public Iterator<ParamState<?>> iterator() {
        return children.iterator();
    }

    public ParamState<?> get(int index) {
        return children.get(index);
    }

    @Override
    public String toSaveString() {
        throw new UnsupportedOperationException();
    }
}
