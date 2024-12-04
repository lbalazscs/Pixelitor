/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.gui.utils.TaskAction;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Captures the state of a list of {@link FilterParam}s.
 *
 * This class can be used for two purposes: (1) storing filter
 * states for built-in presets or (2) supporting animation interpolation
 * between different states. When used for animation, it contains only
 * animatable parameters. For presets, it contains all parameters.
 *
 * Unlike {@link UserPreset}s, {@link FilterState}s can't be saved to disk.
 */
public class FilterState implements Preset {
    private final Map<String, ParamState<?>> states;
    private String name; // used only for presets

    // Whether the filter is reset before applying this state.
    // There's a difference when not all fields are set.
    private boolean shouldResetBeforeApplying;

    /**
     * Creates a {@link FilterState} from a {@link ParamSet},
     * optionally including only animatable parameters.
     */
    public FilterState(ParamSet paramSet, boolean animOnly) {
        Stream<FilterParam> paramStream = paramSet.getParams().stream();
        if (animOnly) {
            paramStream = paramStream.filter(FilterParam::isAnimatable);
        }
        states = paramStream.collect(
            Collectors.toMap(FilterSetting::getName, FilterParam::copyState));
    }

    /**
     * Creates a named empty {@link FilterState}. Can be used
     * as a starting point for building a state incrementally.
     */
    public FilterState(String name) {
        states = new HashMap<>();
        this.name = name;
    }

    private FilterState(Map<String, ParamState<?>> states) {
        this.states = states;
    }

    /**
     * Creates a new {@link FilterState} by interpolating between this state and an end state.
     */
    public FilterState interpolate(FilterState endState, double progress) {
        if (progress < 0.0 || progress > 1.0) {
            throw new IllegalArgumentException("progress = " + progress);
        }
        
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

    /**
     * Returns the state of a specific parameter.
     */
    public ParamState<?> get(String name) {
        return states.get(name);
    }

    /**
     * Adds or updates a {@link ParamState} in this {@link FilterState}.
     * Useful when building a state incrementally.
     */
    public FilterState with(FilterParam param, ParamState<?> state) {
        states.put(param.getName(), state);
        return this;
    }

    /**
     * Marks this state to reset the filter before applying.
     * This ensures a clean slate before applying partial states.
     */
    public FilterState withReset() {
        shouldResetBeforeApplying = true;
        return this;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Action createAction(PresetOwner owner) {
        return new TaskAction(name, () -> owner.loadFilterState(this, shouldResetBeforeApplying));
    }

    public String toDebugString() {
        return name + ": " + states;
    }

    @Override
    public String toString() {
        return name;
    }
}
