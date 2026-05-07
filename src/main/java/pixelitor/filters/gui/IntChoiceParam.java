/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

import com.jhlabs.image.TransformFilter;
import com.jhlabs.image.WaveType;

import java.util.Objects;

import static java.lang.String.format;

/**
 * A {@link ChoiceParam} for selecting a labeled int value.
 */
public class IntChoiceParam extends ChoiceParam<IntChoiceParam.Item> {
    public IntChoiceParam(String name, String[] choices) {
        this(name, toItemArray(choices));
    }

    public IntChoiceParam(String name, Item[] choices) {
        this(name, choices, RandomizeMode.ALLOW);
    }

    public IntChoiceParam(String name, String presetKey, Item[] choices) {
        this(name, choices, RandomizeMode.ALLOW);
        setPresetKey(presetKey);
    }

    public IntChoiceParam(String name, Item[] choices, RandomizeMode randomizeMode) {
        super(name, choices, randomizeMode);
    }

    private static Item[] toItemArray(String[] input) {
        Item[] out = new Item[input.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = new Item(input[i], i);
        }
        return out;
    } 

    public int getValue() {
        return selectedValue.value();
    }

    public boolean hasValue(int value) {
        return selectedValue.hasValue(value);
    }

    /**
     * Sets the default choice by its integer value.
     */
    public IntChoiceParam withDefaultChoice(int defaultValue) {
        for (Item choice : choices) {
            if (choice.hasValue(defaultValue)) {
                this.defaultValue = choice;
                setSelectedItem(choice, false);
                break;
            }
        }
        return this;
    }

    /**
     * Represents an integer value with a string description.
     */
    public record Item(String name, int value) {
        public boolean hasValue(int v) {
            return value == v;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Item item)) {
                return false;
            }
            return value == item.value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final Item[] EDGE_ACTIONS = {
        new Item("Repeat Image", TransformFilter.WRAP_AROUND),
        new Item("Reflect Image", TransformFilter.REFLECT),
        new Item("Repeat Edge Pixels", TransformFilter.REPEAT_EDGE),
        new Item("Transparent", TransformFilter.TRANSPARENT),
    };

    public static IntChoiceParam forEdgeAction() {
        return forEdgeAction(false);
    }

    public static IntChoiceParam forEdgeAction(boolean reflectFirst) {
        var param = new IntChoiceParam("Edge Action", EDGE_ACTIONS, RandomizeMode.ALLOW);
        if (reflectFirst) {
            param.withDefaultChoice(TransformFilter.REFLECT);
        }
        return param;
    }

    private static final Item[] INTERPOLATION_METHODS = {
        new Item("Bilinear (Smooth, Balanced)", TransformFilter.BILINEAR),
        new Item("Nearest Neighbor (Fastest)", TransformFilter.NEAREST_NEIGHBOR),
        new Item("Bicubic (High Quality)", TransformFilter.BICUBIC),
    };

    public static IntChoiceParam forInterpolation() {
        return new IntChoiceParam("Interpolation",
            INTERPOLATION_METHODS, RandomizeMode.IGNORE);
    }

    private static final Item[] WAVE_TYPE_CHOICES = {
        new Item("Sine", WaveType.SINE),
        new Item("Triangle", WaveType.TRIANGLE),
        new Item("Sawtooth", WaveType.SAWTOOTH),
        new Item("Noise", WaveType.NOISE),
    };

    public static IntChoiceParam forWaveType() {
        return new IntChoiceParam("Wave Type", WAVE_TYPE_CHOICES);
    }

    public FilterParam configureWaveType(ParamSet paramSet) {
        FilterButtonModel reseedNoise = paramSet.createReseedNoiseAction("Reseed Noise",
            "Reinitialize the randomness of the noise.");

        // enable the "Reseed Noise" button only if the wave type is "Noise"
        enableOtherWhen(reseedNoise,
            selected -> selected.hasValue(WaveType.NOISE));

        return withSideButton(reseedNoise);
    }

    @Override
    public String toString() {
        return format("%s[name = '%s', selected = '%s']",
            getClass().getSimpleName(), getName(), selectedValue);
    }
}
