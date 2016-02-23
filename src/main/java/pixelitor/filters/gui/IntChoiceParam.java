/*
 * Copyright 2016 Laszlo Balazs-Csiki
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
import pixelitor.utils.Utils;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static pixelitor.filters.gui.FilterSetting.EnabledReason.APP_LOGIC;
import static pixelitor.filters.gui.RandomizePolicy.ALLOW_RANDOMIZE;
import static pixelitor.filters.gui.RandomizePolicy.IGNORE_RANDOMIZE;

/**
 * A filter parameter for selecting a choice from a list of values
 */
public class IntChoiceParam extends AbstractFilterParam implements ComboBoxModel<IntChoiceParam.Value> {
    private final List<Value> choicesList = new ArrayList<>();

    private Value defaultChoice;
    private Value currentChoice;

    private final EventListenerList listenerList = new EventListenerList();
    private ActionSetting action;

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
    public JComponent createGUI() {
        ChoiceSelector choiceSelector = new ChoiceSelector(this, action);
        paramGUI = choiceSelector;
        setParamGUIEnabledState();
        return choiceSelector;
    }

    @Override
    public boolean isSetToDefault() {
        return defaultChoice.equals(currentChoice);
    }

    @Override
    public void reset(boolean triggerAction) {
        setSelectedItem(defaultChoice, triggerAction);
    }

    @Override
    public void setAdjustmentListener(ParamAdjustmentListener listener) {
        super.setAdjustmentListener(listener);
        if (action != null) {
            action.setAdjustmentListener(listener);
        }
    }

    @Override
    public int getNrOfGridBagCols() {
        return 2;
    }

    @Override
    public void randomize() {
        if (randomizePolicy.allowRandomize()) {
            Random rnd = new Random();
            int randomIndex = rnd.nextInt(choicesList.size());
            setCurrentChoice(choicesList.get(randomIndex), false);
        }
    }

    public int getValue() {
        return currentChoice.getIntValue();
    }

    public void setCurrentChoice(Value currentChoice, boolean trigger) {
        setSelectedItem(currentChoice, trigger);
    }

    public void setDefaultChoice(Value defaultChoice) {
        this.defaultChoice = defaultChoice;
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

    public IntChoiceParam withAction(ActionSetting action) {
        this.action = action;
        return this;
    }

    /**
     * Represents an integer value with a description
     */
    public static class Value {
        private final int intValue;
        private final String description;

        public Value(String description, int intValue) {
            this.description = description;
            this.intValue = intValue;
        }

        public int getIntValue() {
            return intValue;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Value value = (Value) o;

            if (intValue != value.intValue) {
                return false;
            }
            return !(description != null ? !description.equals(value.description) : value.description != null);

        }

        @Override
        public int hashCode() {
            int result = intValue;
            result = 31 * result + (description != null ? description.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return description;
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

    public static IntChoiceParam getEdgeActionChoices() {
        return getEdgeActionChoices(false);
    }

    public static IntChoiceParam getEdgeActionChoices(boolean reflectFirst) {
        IntChoiceParam choice = new IntChoiceParam("Edge Action", edgeActions, ALLOW_RANDOMIZE);
        if(reflectFirst) {
            choice.setDefaultChoice(EDGE_REFLECT);
        }
        return choice;
    }

    private static final IntChoiceParam.Value[] interpolationChoices = {
            new Value("Bilinear (Better)", TransformFilter.BILINEAR),
            new Value("Nearest Neighbour (Faster)", TransformFilter.NEAREST_NEIGHBOUR),
//            new Value("Nearest Neighbour (OLD)", TransformFilter.NEAREST_NEIGHBOUR_OLD),
//            new Value("Bilinear (OLD)", TransformFilter.BILINEAR_OLD),
    };

    public static IntChoiceParam getInterpolationChoices() {
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
            new IntChoiceParam.Value("Sine", WaveType.SINE),
            new IntChoiceParam.Value("Triangle", WaveType.TRIANGLE),
            new IntChoiceParam.Value("Sawtooth", WaveType.SAWTOOTH),
            new IntChoiceParam.Value("Noise", WaveType.NOISE),
    };

    public static IntChoiceParam getWaveTypeChoices() {
        ReseedNoiseActionSetting reseedNoise = new ReseedNoiseActionSetting("Reseed Noise",
                "Reinitialize the randomness of the noise.");
        IntChoiceParam icp = new IntChoiceParam("Wave Type", waveTypeChoices);
        icp.withAction(reseedNoise);

        // The "Reseed Noise" button should be enabled only if the wave type is "Noise"
        Utils.setupEnableOtherIf(icp, reseedNoise,
                selected -> selected.getIntValue() == WaveType.NOISE);

        return icp;
    }

    public static IntChoiceParam getGridTypeChoices(String name, RangeParam randomnessParam) {
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

    protected void fireContentsChanged(Object source, int index0, int index1) {
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
    public void considerImageSize(Rectangle bounds) {
    }

    @Override
    public boolean canBeAnimated() {
        return false;
    }

    @Override
    public ParamState copyState() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setState(ParamState state) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return String.format("%s[name = '%s', selected = '%s']",
                getClass().getSimpleName(), getName(), currentChoice.toString());
    }
}
