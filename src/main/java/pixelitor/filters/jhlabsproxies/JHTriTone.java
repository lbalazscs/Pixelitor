package pixelitor.filters.jhlabsproxies;

import com.jhlabs.image.TritoneFilter;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.ColorParam;
import pixelitor.filters.gui.ParamSet;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * Tritone based on the JHLabs TritoneFilter
 */
public class JHTriTone extends FilterWithParametrizedGUI {
    private ColorParam shadowColorParam = new ColorParam("Shadow Color:", Color.BLACK, false, false);
    private ColorParam midtonesColorParam = new ColorParam("Midtones Color:", Color.RED, false, false);
    private ColorParam highlightsColorParam = new ColorParam("Highlights Color:", Color.YELLOW, false, false);

    private TritoneFilter filter;

    public JHTriTone() {
        super("Tritone", true, false);
        setParamSet(new ParamSet(
                shadowColorParam, midtonesColorParam, highlightsColorParam
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if(filter == null) {
            filter = new TritoneFilter();
        }

        filter.setShadowColor(shadowColorParam.getColor().getRGB());
        filter.setHighColor(highlightsColorParam.getColor().getRGB());
        filter.setMidColor(midtonesColorParam.getColor().getRGB());

        dest = filter.filter(src, dest);
        return dest;
    }
}