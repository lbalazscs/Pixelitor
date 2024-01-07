package pixelitor.filters.gmic;

import java.io.Serial;
import java.util.List;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.ColorParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.gui.RangeParam;

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
    private final IntChoiceParam colorSpace = new IntChoiceParam("Color Space", new Item[] {
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
    private final RangeParam randomSeed = new RangeParam("Random Seed", 0, 0, 65536);

    public HuffmanGlitches() {
        setParams(
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
