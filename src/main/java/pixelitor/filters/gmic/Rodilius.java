/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.filters.gmic;

import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.gui.RangeParam;

import java.io.Serial;
import java.util.List;

public class Rodilius extends GMICFilter {
    @Serial
    private static final long serialVersionUID = 1L;

    public static final String NAME = "Rodilius";

    private final RangeParam amplitude = new RangeParam("Amplitude", 0, 10, 30);
    private final RangeParam thickness = new RangeParam("Thickness", 0, 10, 100);
    private final RangeParam sharpness = new RangeParam("Sharpness", 0, 300, 1000);
    private final RangeParam orientations = new RangeParam("Orientations", 2, 5, 36);
    private final RangeParam offset = new RangeParam("Offset", 0, 30, 180);
    private final RangeParam smoothness = new RangeParam("Smoothness", 0, 0, 5);

    private final IntChoiceParam colormode = new IntChoiceParam("Color Mode", new Item[]{
        new Item("Lighter", 1),
        new Item("Darker", 0)
    });

    private final IntChoiceParam channel = GMICFilter.createChannelChoice();
    private final IntChoiceParam valueAction = GMICFilter.createValueAction();

    public Rodilius() {
        setParams(amplitude, thickness, sharpness, orientations, offset, smoothness, colormode, channel, valueAction);
    }

    @Override
    public List<String> getArgs() {
        return List.of("fx_rodilius",
            amplitude.getValue() + "," +
                thickness.getValue() + "," +
                sharpness.getValue() + "," +
                orientations.getValue() + "," +
                offset.getValue() + "," +
                smoothness.getValue() + "," +
                colormode.getValue() + "," +
                channel.getValue() + "," +
                valueAction.getValue());
    }
}
