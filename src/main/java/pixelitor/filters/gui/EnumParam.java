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

import com.jhlabs.image.CellularFilter;

import java.util.List;

/**
 * A {@link ChoiceParam} for choosing a value from an enum.
 */
public class EnumParam<E extends Enum<E>> extends ChoiceParam<E> {
    public EnumParam(String name, Class<E> enumClass) {
        super(name, enumClass.getEnumConstants(), RandomizeMode.ALLOW_RANDOMIZE);
    }

    public EnumParam(String name, String presetKey, Class<E> enumClass) {
        this(name, enumClass);
        setPresetKey(presetKey);
    }

    public EnumParam(String name, List<E> choices) {
        super(name, choices, choices.getFirst(), RandomizeMode.ALLOW_RANDOMIZE);
    }

    public EnumParam(String name, String presetKey, List<E> choices) {
        super(name, choices, choices.getFirst(), RandomizeMode.ALLOW_RANDOMIZE);
        setPresetKey(presetKey);
    }

    public static EnumParam<CellularFilter.GridType> forGridType(String name, RangeParam randomnessParam) {
        var param = new EnumParam<CellularFilter.GridType>(name, CellularFilter.GridType.class);
        // enable the randomness slider only if the grid type isn't "Fully Random"
        param.setupEnableOtherIf(randomnessParam, selected ->
            selected != CellularFilter.GridType.RANDOM);
        return param;
    }

    public EnumParam<E> withDefault(E item) {
        defaultValue = item;
        setSelectedItem(item, false);
        return this;
    }
}
