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
import pixelitor.filters.util.ColorSpace;
import pixelitor.gui.GUIText;

import java.io.Serial;
import java.util.List;

public class HuffmanGlitches extends GMICFilter {
    @Serial
    private static final long serialVersionUID = 1L;

    public static final String NAME = "Huffman Glitches";

    private final RangeParam noiseLevel = new RangeParam("Noise Level", 0, 30, 100);
    private final IntChoiceParam splitMode = new IntChoiceParam("Split Mode", new Item[] {
        new Item("None", 0),
        new Item("Horizontal Blocs", 1),
        new Item("Vertical Blocs", 2),
        new Item("Patches", 3)
    });
    private final RangeParam blocSize = new RangeParam("Bloc Size", 0, 25, 100);
    private final RangeParam patchOverlap = new RangeParam("Patch Overlap", 0, 0, 50);
    private final IntChoiceParam colorSpace = new IntChoiceParam(GUIText.COLOR_SPACE, ColorSpace.PRESET_KEY, new Item[]{
        new Item("RGB", 0),
        new Item("CMYK", 1),
        new Item("HCY", 2),
        new Item("HSI", 3),
        new Item("HSL", 4),
        new Item("HSV", 5),
        new Item("Jzazbz", 6),
        new Item("Lab", 7),
        new Item("Lch", 8),
        new Item("OKLab", 9),
        new Item("YCbCr", 10),
        new Item("YIQ", 11)
    });
    private final RangeParam quantization = new RangeParam("Quantization", 0, 0, 64);

    public HuffmanGlitches() {
        initParams(
            noiseLevel,
            splitMode,
            blocSize,
            patchOverlap,
            colorSpace,
            quantization
            ).withReseedGmicAction(this);
    }

    @Override
    public List<String> getArgs() {
        return List.of("fx_huffman_glitches",
            noiseLevel.getValue() + "," +
                splitMode.getValue() + "," +
                blocSize.getValue() + "," +
                patchOverlap.getValue() + "," +
                colorSpace.getValue() + "," +
                quantization.getValue() + "," +
                seed
        );
    }
}
