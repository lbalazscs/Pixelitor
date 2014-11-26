package pixelitor.filters.jhlabsproxies;

import com.jhlabs.image.DitherFilter;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;

import java.awt.image.BufferedImage;

/**
 * Dither based on the JHLabs DitherFilter
 */
public class JHDither extends FilterWithParametrizedGUI {
    private RangeParam levelsParam = new RangeParam("Levels", 2, 100, 8);
    private BooleanParam colorDitherParam = new BooleanParam("Color Dither", true);
    private IntChoiceParam matrixMethod = new IntChoiceParam("Matrix Type", new IntChoiceParam.Value[] {
            new IntChoiceParam.Value("2x2", DitherFilter.MATRIX_2x2),
            new IntChoiceParam.Value("4x4 Square", DitherFilter.MATRIX_4x4_SQUARE),
            new IntChoiceParam.Value("4x4 Ordered", DitherFilter.MATRIX_4x4_ORDERED),
            new IntChoiceParam.Value("4x4 Lines", DitherFilter.MATRIX_4x4_LINES),
            new IntChoiceParam.Value("6x6 Halftone", DitherFilter.MATRIX_6x6_HALFTONE),
            new IntChoiceParam.Value("6x6 Ordered", DitherFilter.MATRIX_6x6_ORDERED),
            new IntChoiceParam.Value("8x8 Ordered", DitherFilter.MATRIX_8x8_ORDERED),
            new IntChoiceParam.Value("Cluster 3", DitherFilter.MATRIX_CLUSTER3),
            new IntChoiceParam.Value("Cluster 4", DitherFilter.MATRIX_CLUSTER4),
            new IntChoiceParam.Value("Cluster 8", DitherFilter.MATRIX_CLUSTER8),
    });

    private DitherFilter filter;

    public JHDither() {
        super("Dither", true, false);
        setParamSet(new ParamSet(
                levelsParam,
                colorDitherParam,
                matrixMethod
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if(filter == null) {
            filter = new DitherFilter();
        }

        int levels =  levelsParam.getValue();
        boolean colorDither = colorDitherParam.getValue();

        filter.setLevels(levels);
        filter.setColorDither(colorDither);
        filter.setMatrixMethod(matrixMethod.getValue());

        filter.initialize();

        dest = filter.filter(src, dest);
        return dest;
    }
}