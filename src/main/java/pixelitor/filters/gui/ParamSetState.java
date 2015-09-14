/*
 * Copyright 2015 Laszlo Balazs-Csiki
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
import java.util.Iterator;
import java.util.List;

/**
 * Captures the state of all filter parameters in a given filter
 * (like the "Memento" design pattern)
 * Params that cannot be animated are not included
 */
public class ParamSetState implements Iterable<ParamState> {
    private List<ParamState> states = new ArrayList<>();

    public ParamSetState(ParamSet originator) {
        List<FilterParam> params = originator.getParamList();
        for (FilterParam param : params) {
            if(param.canBeAnimated()) {
                ParamState state = param.copyState();
                if (state == null) {
                    throw new IllegalArgumentException("State is null for the param " + param.getName());
                }
                states.add(state);
            }
        }
    }

    private ParamSetState(List<ParamState> states) {
        for (ParamState state : states) {
            assert state != null;
        }
        this.states = states;
    }

    ParamState getParamState(int index) {
        return states.get(index);
    }

    /**
     * Calculate an interpolated ParamSetState, where the current object is the starting state
     */
    public ParamSetState interpolate(ParamSetState endState, double progress) {
        List<ParamState> interpolatedStates = new ArrayList<>();
        for (int i = 0; i < states.size(); i++) {
            // each ParamState is interpolated independently
            ParamState state = states.get(i);
            ParamState endParamState = endState.getParamState(i);
            ParamState interpolated = state.interpolate(endParamState, progress);
            interpolatedStates.add(interpolated);
        }
        return new ParamSetState(interpolatedStates);
    }

    @Override
    public Iterator<ParamState> iterator() {
        return states.iterator();
    }
}
