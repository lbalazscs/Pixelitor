
package pixelitor.filters.curves;

import com.jhlabs.image.CurvesFilter;
import pixelitor.filters.curves.lx.LxCurves;
import pixelitor.filters.gui.FilterGUI;
import pixelitor.filters.gui.FilterWithGUI;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.layers.Drawable;
import java.awt.image.BufferedImage;

/**
 * Tone Curves filter
 * @author Szakul Jarzuk szakuljarzuk@gmail.com
 */
public class Curves extends FilterWithGUI {
    public static final String NAME = "Tone Curves";

    private CurvesFilter filter;
    private LxCurves curves;

    public Curves() {
        //super(ShowOriginal.YES);
    }

    @Override
    public FilterGUI createGUI(Drawable dr) {
        return new CurvesGUI(this, dr);
    }

    public void setCurves(LxCurves curves) {
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

//        System.out.println("p1: " + p1.getValue() + " p2: " + p2.getValue());
//        com.jhlabs.image.Curve curve = new com.jhlabs.image.Curve();
        com.jhlabs.image.Curve curve = this.curves.getActiveCurve().curve;

        System.out.println("transform" + curve.x + "/" + curve.y);
        filter.setCurve(curve);
        dest = filter.filter(src, dest);
        return dest;
    }

    @Override
    public void randomizeSettings() {
        // not supported yet
    }
}