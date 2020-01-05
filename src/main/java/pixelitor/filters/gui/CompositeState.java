/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

/**
 * Captures the state of a list of animatable {@link FilterParam}s.
 * It acts as the state of a {@link ParamSet}, and also as a composite {@link ParamState}
 * for composite {@link FilterParam}s, like the {@link DialogParam}.
 */
public class CompositeState implements ParamState<CompositeState>, Iterable<ParamState<?>> {
    private final List<ParamState<?>> states;

    public CompositeState(ParamSet paramSet) {
        this(paramSet.getParams().stream());
    }

    public CompositeState(FilterParam[] children) {
        this(Arrays.stream(children));
    }

    private CompositeState(Stream<FilterParam> paramStream) {
        states = paramStream
                .filter(FilterParam::canBeAnimated)
                .map(FilterParam::copyState)
                .collect(toList());
    }

    private CompositeState(List<ParamState<?>> states) {
        for (ParamState<?> state : states) {
            assert state != null;
        }
        this.states = states;
    }

    private ParamState<?> getParamState(int index) {
        return states.get(index);
    }

    /**
     * Calculate an interpolated state,
     * where the current object is the starting state
     */
    @Override
    public CompositeState interpolate(CompositeState endState, double progress) {
        List<ParamState<?>> interpolatedStates = new ArrayList<>();
        for (int i = 0; i < states.size(); i++) {
            // each ParamState is interpolated independently

            // if you know how to get rid of the raw
            // types here, let me know...
            @SuppressWarnings("rawtypes")
            ParamState state = states.get(i);

            ParamState<?> endParamState = endState.getParamState(i);

            @SuppressWarnings("unchecked")
            ParamState<?> interpolated = state.interpolate(
                    endParamState,
                    progress);

            interpolatedStates.add(interpolated);
        }
        return new CompositeState(interpolatedStates);
    }

    @Override
    public Iterator<ParamState<?>> iterator() {
        return states.iterator();
    }
}
