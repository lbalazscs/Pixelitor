package pixelitor.filters.jhlabsproxies;

import com.jhlabs.image.BrushedMetalFilter;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.ColorParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * BrushedMetal based on the JHLabs BrushedMetalFilter
 */
public class JHBrushedMetal extends FilterWithParametrizedGUI {
    private ColorParam color = new ColorParam("Color", Color.GRAY, false, false);
    private RangeParam radius = new RangeParam("Length", 0, 500, 100);
    private RangeParam amount = new RangeParam("Amount (%)", 0, 100, 50);
    private RangeParam shine = new RangeParam("Shine (%)", 0, 100, 10);

    private BrushedMetalFilter filter;

    public JHBrushedMetal() {
        super("Brushed Metal", false, false);
        setParamSet(new ParamSet(
                color,
                radius.adjustRangeToImageSize(0.5),
                amount,
                shine
        ));
        listNamePrefix = "Render ";
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {

        filter = new BrushedMetalFilter(color.getColor().getRGB(),
                radius.getValue(),
                amount.getValueAsPercentage(),
                true,
                shine.getValueAsPercentage());

        dest = filter.filter(src, dest);
        return dest;
    }
}