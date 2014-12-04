package pixelitor.filters.jhlabsproxies;

import com.jhlabs.image.ColorHalftoneFilter;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;

import java.awt.image.BufferedImage;

/**
 * ColorHalftone based on the JHLabs ColorHalftoneFilter
 */
public class JHColorHalftone extends FilterWithParametrizedGUI {
    private RangeParam dotRadiusParam = new RangeParam("Dot Radius (pixel %)", 10, 1000, 100);

    private AngleParam cyanScreenAngleParam = new AngleParam("Cyan Screen Angle", 1.8849555921538759);
    private AngleParam magentaScreenAngleParam = new AngleParam("Magenta Screen Angle", 1.0821041362364843);
    private AngleParam yellowScreenAngleParam = new AngleParam("Yellow Screen Angle", 1.5707963267948966);

    private ColorHalftoneFilter filter;

    public JHColorHalftone() {
        super("Color Halftone", true, false);
        setParamSet(new ParamSet(dotRadiusParam.adjustRangeToImageSize(1.2),
                cyanScreenAngleParam,
                magentaScreenAngleParam,
                yellowScreenAngleParam
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        float dotRadius =  dotRadiusParam.getValueAsPercentage();
        if(filter == null) {
            filter = new ColorHalftoneFilter();
        }

        float cyan = (float) cyanScreenAngleParam.getValueInIntuitiveRadians();
        float magenta = (float) magentaScreenAngleParam.getValueInIntuitiveRadians();
        float yellow = (float) yellowScreenAngleParam.getValueInIntuitiveRadians();
        filter.setCyanScreenAngle(cyan);
        filter.setMagentaScreenAngle(magenta);
        filter.setYellowScreenAngle(yellow);
        filter.setdotRadius(dotRadius);

        dest = filter.filter(src, dest);
        return dest;
    }
}