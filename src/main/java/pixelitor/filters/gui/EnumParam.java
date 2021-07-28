/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

/**
 * Just like {@link IntChoiceParam}, this is a model
 * for a JComboBox, but the values are coming from an enum
 */
public class EnumParam<E extends Enum<E>> extends AbstractMultipleChoiceParam<E> {
    public EnumParam(String name, Class<E> enumClass) {
        super(name, enumClass.getEnumConstants(), RandomizePolicy.ALLOW_RANDOMIZE);
    }

    public EnumParam<E> withDefault(E item) {
        defaultChoice = item;
        setSelectedItem(item, false);
        return this;
    }
}
