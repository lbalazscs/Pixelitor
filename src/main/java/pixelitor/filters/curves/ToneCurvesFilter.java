
package pixelitor.filters.curves;

import com.jhlabs.image.Curve;
import com.jhlabs.image.CurvesFilter;
import pixelitor.filters.gui.FilterGUI;
import pixelitor.filters.gui.FilterWithGUI;
import pixelitor.layers.Drawable;
import java.awt.image.BufferedImage;

/**
 * Tone ToneCurvesFilter filter
 *
 * @author Łukasz Kurzaj lukaszkurzaj@gmail.com
 */
public class ToneCurvesFilter extends FilterWithGUI {
    public static final String NAME = "Curves";

    private CurvesFilter filter;
    private ToneCurves curves;

    @Override
    public FilterGUI createGUI(Drawable dr) {
        return new ToneCurvesGUI(this, dr);
    }

    public void setCurves(ToneCurves curves) {
        this.curves = curves;
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        if(filter == null) {
            filter = new CurvesFilter(NAME);
        }
        if (this.curves == null) {
            return src;
        }

        filter.setCurves(
            this.curves.getCurve(ToneCurveType.RGB).curve,
            this.curves.getCurve(ToneCurveType.RED).curve,
            this.curves.getCurve(ToneCurveType.GREEN).curve,
            this.curves.getCurve(ToneCurveType.BLUE).curve
        );

        dest = filter.filter(src, dest);
        return dest;
    }

    @Override
    public void randomizeSettings() {
        // not supported yet
    }
}