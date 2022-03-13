/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.gui.utils.PAction;

import javax.swing.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Captures the state of a list of {@link FilterParam}s.
 *
 * If it is used for animation interpolation,
 * then it contains only the animatable {@link FilterParam}s.
 * It is also used for the built-in presets.
 */
public class FilterState implements Preset {
    private final Map<String, ParamState<?>> states;
    private String name; // used only for presets

    // Whether the filter is reset before applying this state.
    // There's a difference if not all fields are set.
    private boolean reset;

    public FilterState(ParamSet paramSet, boolean animOnly) {
        this(paramSet.getParams().stream(), animOnly);
    }

    public FilterState(FilterParam[] children) {
        // for simplicity include all, even for animation (rarely used anyway)
        this(Arrays.stream(children), false);
    }

    private FilterState(Stream<FilterParam> paramStream, boolean animOnly) {
        if (animOnly) {
            paramStream = paramStream.filter(FilterParam::canBeAnimated);
        }
        states = paramStream.collect(
            Collectors.toMap(FilterSetting::getName, FilterParam::copyState));
    }

    private FilterState(Map<String, ParamState<?>> states) {
        this.states = states;
    }

    public FilterState(String name) {
        states = new HashMap<>();
        this.name = name;
    }

    public FilterState interpolate(FilterState endState, double progress) {
        Map<String, ParamState<?>> interpolatedStates = new HashMap<>();

        for (Map.Entry<String, ParamState<?>> entry : states.entrySet()) {
            // each ParamState is interpolated independently
            String paramName = entry.getKey();

            @SuppressWarnings("rawtypes")
            ParamState state = entry.getValue();
            ParamState<?> endParamState = endState.get(paramName);

            @SuppressWarnings("unchecked")
            ParamState<?> interpolated = state.interpolate(endParamState, progress);
            interpolatedStates.put(paramName, interpolated);
        }
        return new FilterState(interpolatedStates);
    }

    public ParamState<?> get(String name) {
        return states.get(name);
    }

    public FilterState with(FilterParam param, ParamState<?> state) {
        states.put(param.getName(), state);
        return this;
    }

    public FilterState withReset() {
        reset = true;
        return this;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Action asAction(PresetOwner owner) {
        return new PAction(name) {
            @Override
            public void onClick() {
                owner.loadFilterState(FilterState.this, reset);
            }
        };
    }

    public String toDebugString() {
        return name + ": " + states;
    }

    @Override
    public String toString() {
        return name;
    }
}
