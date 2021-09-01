/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class CompositeParamState implements ParamState<CompositeParamState> {
    private final List<ParamState<?>> states;

    public CompositeParamState(FilterParam[] children) {
        this(Arrays.stream(children));
    }

    private CompositeParamState(List<ParamState<?>> states) {
        this.states = states;
    }

    private CompositeParamState(Stream<FilterParam> paramStream) {
        states = paramStream
            .filter(FilterParam::canBeAnimated)
            .map(FilterParam::copyState)
            .collect(toList());
    }

    /**
     * Calculate an interpolated state,
     * where the current object is the starting state
     */
    @Override
    public CompositeParamState interpolate(CompositeParamState endState, double progress) {
        List<ParamState<?>> interpolatedStates = new ArrayList<>();
        for (int i = 0; i < states.size(); i++) {
            // each ParamState is interpolated independently
            interpolatedStates.add(interpolateOne(endState, progress, i));
        }
        return new CompositeParamState(interpolatedStates);
    }

    @SuppressWarnings("unchecked")
    private <T extends ParamState<T>> ParamState<T> interpolateOne(CompositeParamState endState, double progress, int i) {
        // The cast works at runtime because start and end have the same concrete type
        T start = (T) states.get(i);
        T end = (T) endState.get(i);
        return start.interpolate(end, progress);
    }

    public Iterator<ParamState<?>> iterator() {
        return states.iterator();
    }

    public ParamState<?> get(int index) {
        return states.get(index);
    }

    @Override
    public String toSaveString() {
        throw new UnsupportedOperationException();
    }
}
