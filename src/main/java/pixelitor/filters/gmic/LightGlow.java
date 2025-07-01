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

package pixelitor.filters.gmic;

import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.gui.RangeParam;

import java.io.Serial;
import java.util.List;

public class LightGlow extends GMICFilter {
    @Serial
    private static final long serialVersionUID = 1L;

    public static final String NAME = "Light Glow";

    private final RangeParam density = new RangeParam("Density", 0, 30, 100);
    private final RangeParam amplitude = new RangeParam("Amplitude", 0, 50, 200);
    private final IntChoiceParam mode = new IntChoiceParam("Mode", new Item[]{
        new Item("Burn", 0),
        new Item("Dodge", 1),
        new Item("Freeze", 2),
        new Item("Grain Merge", 3),
        new Item("Hard Light", 4),
        new Item("Interpolation", 5),
        new Item("Lighten", 6),
        new Item("Multiply", 7),
        new Item("Overlay", 8),
        new Item("Reflect", 9),
        new Item("Soft Light", 10),
        new Item("Stamp", 11),
        new Item("Value", 12)
    }).withDefaultChoice(8);
    private final RangeParam opacity = new RangeParam("Opacity", 0, 80, 100);
    private final IntChoiceParam channels = GMICFilter.createChannelChoice();

    public LightGlow() {
        initParams(density, amplitude, mode, opacity, channels);
    }

    @Override
    public List<String> getArgs() {
        return List.of("fx_lightglow",
            density.getValue() + "," +
                amplitude.getPercentage() + "," +
                mode.getValue() + "," +
                opacity.getPercentage() + "," +
                channels.getValue());
    }
}
