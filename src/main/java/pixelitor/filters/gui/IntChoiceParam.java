/*
 * Copyright 2010-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.filters.gui;

import com.jhlabs.image.CellularFilter;
import com.jhlabs.image.TransformFilter;
import com.jhlabs.image.WaveType;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * A GUIParam for selecting a choice from a list of values
 */
public class IntChoiceParam extends AbstractListModel<IntChoiceParam.Value> implements ComboBoxModel<IntChoiceParam.Value>, GUIParam {
    private final String name;
    private final List<Value> choicesList = new ArrayList<>();

    private Value defaultChoice;
    private Value currentChoice;
    private ParamAdjustmentListener adjustmentListener;
    private boolean dontTrigger = false;
    private final boolean ignoreRandomize;
    private boolean finalAnimationSettingMode;

    public IntChoiceParam(String name, Value[] choices) {
        this(name, choices, false);
    }

    public IntChoiceParam(String name, Value[] choices, boolean ignoreRandomize) {
        this.ignoreRandomize = ignoreRandomize;
        this.name = name;

        choicesList.addAll(Arrays.asList(choices));

        this.defaultChoice = choices[0];
        currentChoice = defaultChoice;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isSetToDefault() {
        return defaultChoice.equals(currentChoice);
    }

    @Override
    public JComponent createGUI() {
        IntChoiceSelector choiceSelector = new IntChoiceSelector(this);
        if(finalAnimationSettingMode) {
            choiceSelector.setEnabled(false);
        }
        return choiceSelector;
    }

    @Override
    public void reset(boolean triggerAction) {
        if (!triggerAction) {
            dontTrigger = true;
        }
        setSelectedItem(defaultChoice);
        dontTrigger = false;
    }

    @Override
    public void setAdjustmentListener(ParamAdjustmentListener listener) {
        adjustmentListener = listener;
    }

    @Override
    public int getNrOfGridBagCols() {
        return 2;
    }

    @Override
    public void randomize() {
        if(finalAnimationSettingMode) {
            assert !canBeAnimated();
            return;
        }
        if (!ignoreRandomize) {
            Random rnd = new Random();
            int randomIndex = rnd.nextInt(choicesList.size());
            dontTrigger = true;
            setCurrentChoice(choicesList.get(randomIndex));
            dontTrigger = false;
        }
    }

    public int getValue() {
        return currentChoice.getIntValue();
    }

//    public Value getCurrentChoice() {
//        return (Value) getSelectedItem();
//    }

    public void setCurrentChoice(Value currentChoice) {
        setSelectedItem(currentChoice);
    }

    public void setDefaultChoice(Value defaultChoice) {
        this.defaultChoice = defaultChoice;
    }

    @Override
    public void setSelectedItem(Object anItem) {
        if (!currentChoice.equals(anItem)) {
            currentChoice = (Value) anItem;
            fireContentsChanged(this, -1, -1);
            if (!dontTrigger) {
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
        IntChoiceParam choice = new IntChoiceParam("Edge Action", edgeActions);
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
        return new IntChoiceParam("Interpolation", interpolationChoices);
    }

    private static final IntChoiceParam.Value[] gridTypeChoices = {
            new Value("Random", CellularFilter.GR_RANDOM),
            new Value("Squares", CellularFilter.GR_SQUARE),
            new Value("Hexagons", CellularFilter.GR_HEXAGONAL),
            new Value("Octagons & Squares", CellularFilter.GR_OCTAGONAL),
            new Value("Triangles", CellularFilter.GR_TRIANGULAR),
    };

    private static final IntChoiceParam.Value[] waveTypeChoices = {
            new IntChoiceParam.Value("Sine", WaveType.SINE),
            new IntChoiceParam.Value("Triangle", WaveType.TRIANGLE),
            new IntChoiceParam.Value("Sawtooth", WaveType.SAWTOOTH),
            new IntChoiceParam.Value("Noise", WaveType.NOISE),
    };

    public static IntChoiceParam getWaveTypeChoices() {
        return new IntChoiceParam("Wave Type", waveTypeChoices);
    }

//    private static final IntChoiceParam.Value[] rndGeneratorChoices = {
//            new IntChoiceParam.Value("Faster", CellularFilter.RND_GENERATOR_MSX_INT),
//            new IntChoiceParam.Value("Faster 2", CellularFilter.RND_GENERATOR_MSX_LONG),
//            new IntChoiceParam.Value("Uniform (Slower)", CellularFilter.RND_GENERATOR_UNIFORM),
//    };
//
//    public static IntChoiceParam getRndGeneratorChoices() {
//        return new IntChoiceParam("Randomness Type", rndGeneratorChoices);
//    }

    public static IntChoiceParam getGridTypeChoices(String name, final RangeParam randomnessParam) {
        randomnessParam.setEnabledLogically(false);
        final IntChoiceParam param = new IntChoiceParam(name, gridTypeChoices);
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
                randomnessParam.setEnabledLogically(selectedValue != CellularFilter.GR_RANDOM);
            }
        });
        return param;
    }


    @Override
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
    public void setDontTrigger(boolean b) {
        dontTrigger = b;
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
    public void setEnabledLogically(boolean b) {
        // TODO
    }

    @Override
    public void setFinalAnimationSettingMode(boolean b) {
        finalAnimationSettingMode = b;
    }

    @Override
    public String toString() {
        return String.format("%s[name = '%s', selected = '%s']",
                getClass().getSimpleName(), name, currentChoice.toString());
    }
}
