package pixelitor.filters.jhlabsproxies;

import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.filters.impl.MorphologyFilter;

import java.awt.image.BufferedImage;

/**
 * A morphology filter
 */
public class Morphology extends FilterWithParametrizedGUI {
    private final MorphologyFilter filter = new MorphologyFilter();

    private final RangeParam radius = new RangeParam("Radius", 1, 1, 20);
    private final IntChoiceParam kernel = new IntChoiceParam("Kernel Shape", new IntChoiceParam.Value[]{
            new IntChoiceParam.Value("Diamond", MorphologyFilter.KERNEL_DIAMOND),
            new IntChoiceParam.Value("Square", MorphologyFilter.KERNEL_SQUARE),
    });
    private final IntChoiceParam op = new IntChoiceParam("Operation", new IntChoiceParam.Value[]{
            new IntChoiceParam.Value("Maximum (Dilate)", MorphologyFilter.OP_MAXIMUM),
            new IntChoiceParam.Value("Minimum (Erode)", MorphologyFilter.OP_MINIMUM),
    });

    public Morphology() {
        super(ShowOriginal.YES);
        setParamSet(new ParamSet(radius, kernel, op));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        filter.setIterations(radius.getValue());
        filter.setKernel(kernel.getValue());
        filter.setOp(op.getValue());
        return filter.filter(src, dest);
    }
}
