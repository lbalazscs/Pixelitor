package pixelitor.filters.jhlabsproxies;

import com.jhlabs.image.EdgeFilter;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.Invert;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.filters.impl.MorphologyFilter;
import pixelitor.filters.lookup.Luminosity;

import java.awt.image.BufferedImage;

/**
 * Contours filter
 */
public class Contours extends FilterWithParametrizedGUI {
    private final RangeParam lineThickness = new RangeParam("Increase Line Thickness", 0, 0, 20);

    public Contours() {
        super(ShowOriginal.YES);
        setParamSet(new ParamSet(lineThickness));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        EdgeFilter edgeFilter = new EdgeFilter();
        edgeFilter.setHEdgeMatrix(EdgeFilter.SOBEL_H);
        edgeFilter.setVEdgeMatrix(EdgeFilter.SOBEL_V);

        dest = edgeFilter.filter(src, dest);
        Invert.invertImage(dest, dest);

        Luminosity luminosity = new Luminosity();
        dest = luminosity.transform(dest, dest);

        int iterations = lineThickness.getValue();
        if (iterations > 0) {
            MorphologyFilter morphologyFilter = new MorphologyFilter();
            morphologyFilter.setIterations(iterations);
            morphologyFilter.setKernel(MorphologyFilter.KERNEL_DIAMOND);
            morphologyFilter.setOp(MorphologyFilter.OP_MINIMUM);

            dest = morphologyFilter.filter(dest, dest);
        }

        return dest;
    }
}