/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

import com.jhlabs.image.CellularFilter;
import com.jhlabs.image.TransformFilter;
import com.jhlabs.image.WaveType;
import pixelitor.utils.RandomUtils;

import javax.swing.event.EventListenerList;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.String.format;
import static pixelitor.filters.gui.FilterSetting.EnabledReason.APP_LOGIC;
import static pixelitor.filters.gui.RandomizePolicy.ALLOW_RANDOMIZE;
import static pixelitor.filters.gui.RandomizePolicy.IGNORE_RANDOMIZE;

/**
 * A filter parameter for selecting a choice from a list of values
 */
public class IntChoiceParam extends AbstractMultipleChoiceParam<IntChoiceParam.Value> {
    private final List<Value> choicesList = new ArrayList<>();

    private Value defaultChoice;
    private Value currentChoice;

    private final EventListenerList listenerList = new EventListenerList();

    public IntChoiceParam(String name, Value[] choices) {
        this(name, choices, ALLOW_RANDOMIZE);
    }

    public IntChoiceParam(String name, Value[] choices, RandomizePolicy randomizePolicy) {
        super(name, randomizePolicy);

        choicesList.addAll(Arrays.asList(choices));

        this.defaultChoice = choices[0];
        currentChoice = defaultChoice;
    }

    @Override
    public boolean isSetToDefault() {
        return defaultChoice.equals(currentChoice);
    }

    @Override
    public void reset(boolean trigger) {
        setSelectedItem(defaultChoice, trigger);
    }

    @Override
    public void randomize() {
        if (randomizePolicy.allow()) {
            Value choice = RandomUtils.chooseFrom(choicesList);
            setCurrentChoice(choice, false);
        }
    }

    public int getValue() {
        return currentChoice.getValue();
    }

    public void setCurrentChoice(Value currentChoice, boolean trigger) {
        setSelectedItem(currentChoice, trigger);
    }

    public IntChoiceParam withDefaultChoice(Value defaultChoice) {
        this.defaultChoice = defaultChoice;
        return this;
    }

    @Override
    public void setSelectedItem(Object item) {
        setSelectedItem(item, true);
    }

    public void setSelectedItem(Object item, boolean trigger) {
        if (!currentChoice.equals(item)) {
            currentChoice = (Value) item;
            fireContentsChanged(this, -1, -1);
            if (trigger) {
                if (adjustmentListener != null) {  // when called from randomize, this is null
                    adjustmentListener.paramAdjusted();
                }
            }
        }
    }

    @Override
    public Object getSelectedItem() {
        return currentChoice;
    }

    @Override
    public int getSize() {
        return choicesList.size();
    }

    @Override
    public Value getElementAt(int index) {
        return choicesList.get(index);
    }

    @Override
    public void addListDataListener(ListDataListener l) {
        listenerList.add(ListDataListener.class, l);
    }

    @Override
    public void removeListDataListener(ListDataListener l) {
        listenerList.remove(ListDataListener.class, l);
    }

    /**
     * Represents an integer value with a string description
     */
    public static class Value {
        private final int value;
        private final String name;

        public Value(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Value other = (Value) o;

            if (this.value != other.value) {
                return false;
            }
            return !(name != null ? !name.equals(other.name) : other.name != null);

        }

        @Override
        public int hashCode() {
            int result = value;
            result = 31 * result + (name != null ? name.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static final Value EDGE_REPEAT_PIXELS = new Value("Repeat Edge Pixels", TransformFilter.REPEAT_EDGE_PIXELS);
    public static final Value EDGE_REFLECT = new Value("Reflect Image", TransformFilter.REFLECT);

    private static final IntChoiceParam.Value[] edgeActions = {
            new Value("Repeat Image", TransformFilter.WRAP_AROUND),
            EDGE_REFLECT,
            EDGE_REPEAT_PIXELS,
            new Value("Transparent", TransformFilter.TRANSPARENT),
    };

    public static IntChoiceParam forEdgeAction() {
        return forEdgeAction(false);
    }

    public static IntChoiceParam forEdgeAction(boolean reflectFirst) {
        IntChoiceParam choice = new IntChoiceParam("Edge Action", edgeActions, ALLOW_RANDOMIZE);
        if(reflectFirst) {
            return choice.withDefaultChoice(EDGE_REFLECT);
        }
        return choice;
    }

    private static final IntChoiceParam.Value[] interpolationChoices = {
            new Value("Bilinear (Better)", TransformFilter.BILINEAR),
            new Value("Nearest Neighbour (Faster)", TransformFilter.NEAREST_NEIGHBOUR),
//            new Value("Nearest Neighbour (OLD)", TransformFilter.NEAREST_NEIGHBOUR_OLD),
//            new Value("Bilinear (OLD)", TransformFilter.BILINEAR_OLD),
    };

    public static IntChoiceParam forInterpolation() {
        return new IntChoiceParam("Interpolation", interpolationChoices, IGNORE_RANDOMIZE);
    }

    private static final IntChoiceParam.Value[] gridTypeChoices = {
            new Value("Random", CellularFilter.GR_RANDOM),
            new Value("Squares", CellularFilter.GR_SQUARE),
            new Value("Hexagons", CellularFilter.GR_HEXAGONAL),
            new Value("Octagons & Squares", CellularFilter.GR_OCTAGONAL),
            new Value("Triangles", CellularFilter.GR_TRIANGULAR),
    };

    public static final IntChoiceParam.Value[] waveTypeChoices = {
            new Value("Sine", WaveType.SINE),
            new Value("Triangle", WaveType.TRIANGLE),
            new Value("Sawtooth", WaveType.SAWTOOTH),
            new Value("Noise", WaveType.NOISE),
    };

    public static IntChoiceParam forWaveType() {
        ReseedNoiseFilterAction reseedNoise = new ReseedNoiseFilterAction("Reseed Noise",
                "Reinitialize the randomness of the noise.");
        IntChoiceParam icp = new IntChoiceParam("Wave Type", waveTypeChoices);
        icp.withAction(reseedNoise);

        // The "Reseed Noise" button should be enabled only if the wave type is "Noise"
        icp.setupEnableOtherIf(reseedNoise,
                selected -> selected.getValue() == WaveType.NOISE);

        return icp;
    }

    public static IntChoiceParam forGridType(String name, RangeParam randomnessParam) {
        randomnessParam.setEnabled(false, APP_LOGIC);
        IntChoiceParam param = new IntChoiceParam(name, gridTypeChoices);
        param.addListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent e) {
                // cannot happen
            }

            @Override
            public void intervalRemoved(ListDataEvent e) {
                // cannot happen
            }

            @Override
            public void contentsChanged(ListDataEvent e) {
                int selectedValue = param.getValue();
                randomnessParam.setEnabled(selectedValue != CellularFilter.GR_RANDOM, APP_LOGIC);
            }
        });
        return param;
    }

    private void fireContentsChanged(Object source, int index0, int index1) {
        Object[] listeners = listenerList.getListenerList();
        ListDataEvent e = null;

        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ListDataListener.class) {
                if (e == null) {
                    e = new ListDataEvent(source, ListDataEvent.CONTENTS_CHANGED, index0, index1);
                }
                ((ListDataListener) listeners[i + 1]).contentsChanged(e);
            }
        }
    }

    @Override
    public String getResetToolTip() {
        return super.getResetToolTip() + " to " + defaultChoice.toString();
    }

    @Override
    public String toString() {
        return format("%s[name = '%s', selected = '%s']",
                getClass().getSimpleName(), getName(), currentChoice.toString());
    }
}
