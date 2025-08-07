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

import com.jhlabs.image.CellularFilter;
import com.jhlabs.image.TransformFilter;
import com.jhlabs.image.WaveType;

import java.util.Objects;

import static java.lang.String.format;
import static pixelitor.filters.gui.RandomizeMode.ALLOW_RANDOMIZE;
import static pixelitor.filters.gui.RandomizeMode.IGNORE_RANDOMIZE;

/**
 * A {@link ChoiceParam} for selecting a labeled int value.
 */
public class IntChoiceParam extends ChoiceParam<IntChoiceParam.Item> {
    public IntChoiceParam(String name, String[] choices) {
        this(name, toItemArray(choices));
    }

    public IntChoiceParam(String name, Item[] choices) {
        this(name, choices, ALLOW_RANDOMIZE);
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

    public boolean valueIs(int value) {
        return selectedValue.valueIs(value);
    }

    /**
     * Sets the default choice by its integer value.
     */
    public IntChoiceParam withDefaultChoice(int defaultValue) {
        for (Item choice : choices) {
            if (choice.valueIs(defaultValue)) {
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
        public boolean valueIs(int v) {
            return value == v;
        }

        public boolean valueIsNot(int v) {
            return value != v;
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

    private static final Item[] edgeActions = {
        new Item("Repeat Image", TransformFilter.WRAP_AROUND),
        new Item("Reflect Image", TransformFilter.REFLECT),
        new Item("Repeat Edge Pixels", TransformFilter.REPEAT_EDGE),
        new Item("Transparent", TransformFilter.TRANSPARENT),
    };

    public static IntChoiceParam forEdgeAction() {
        return forEdgeAction(false);
    }

    public static IntChoiceParam forEdgeAction(boolean reflectFirst) {
        var param = new IntChoiceParam("Edge Action", edgeActions, ALLOW_RANDOMIZE);
        if (reflectFirst) {
            param.withDefaultChoice(TransformFilter.REFLECT);
        }
        return param;
    }

    private static final Item[] interpolationMethods = {
        new Item("Bilinear (Better)", TransformFilter.BILINEAR),
        new Item("Nearest Neighbour (Faster)", TransformFilter.NEAREST_NEIGHBOUR),
    };

    public static IntChoiceParam forInterpolation() {
        return new IntChoiceParam("Interpolation",
            interpolationMethods, IGNORE_RANDOMIZE);
    }

    private static final Item[] gridTypeChoices = {
        new Item("Fully Random", CellularFilter.GR_RANDOM),
        new Item("Squares", CellularFilter.GR_SQUARE),
        new Item("Hexagons", CellularFilter.GR_HEXAGONAL),
        new Item("Octagons & Squares", CellularFilter.GR_OCTAGONAL),
        new Item("Triangles", CellularFilter.GR_TRIANGULAR),
    };

    private static final Item[] waveTypeChoices = {
        new Item("Sine", WaveType.SINE),
        new Item("Triangle", WaveType.TRIANGLE),
        new Item("Sawtooth", WaveType.SAWTOOTH),
        new Item("Noise", WaveType.NOISE),
    };

    public static IntChoiceParam forWaveType() {
        return new IntChoiceParam("Wave Type", waveTypeChoices);
    }

    public FilterParam configureWaveType(ParamSet paramSet) {
        FilterButtonModel reseedNoise = paramSet.createReseedNoiseAction("Reseed Noise",
            "Reinitialize the randomness of the noise.");

        // enable the "Reseed Noise" button only if the wave type is "Noise"
        setupEnableOtherIf(reseedNoise,
            selected -> selected.valueIs(WaveType.NOISE));

        return withSideButton(reseedNoise);
    }

    public static IntChoiceParam forGridType(String name, RangeParam randomnessParam) {
        var param = new IntChoiceParam(name, gridTypeChoices);
        // enable the randomness slider only if the grid type isn't "Fully Random"
        param.setupEnableOtherIf(randomnessParam, selected ->
            selected.value() != CellularFilter.GR_RANDOM);
        return param;
    }

    @Override
    public String toString() {
        return format("%s[name = '%s', selected = '%s']",
            getClass().getSimpleName(), getName(), selectedValue);
    }
}
