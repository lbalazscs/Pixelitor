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
    private ColorParam colorParam = new ColorParam("Color", Color.GRAY, false, false);
    private RangeParam radiusParam = new RangeParam("Length", 0, 500, 100);
    private RangeParam amountParam = new RangeParam("Amount (%)", 0, 100, 50);
    private RangeParam shineParam = new RangeParam("Shine (%)", 0, 100, 10);

    private BrushedMetalFilter filter;

    public JHBrushedMetal() {
        super("Brushed Metal", false, false);
        setParamSet(new ParamSet(
                colorParam,
                radiusParam.adjustRangeToImageSize(0.5),
                amountParam,
                shineParam
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        int radius =  radiusParam.getValue();
        float shine = shineParam.getValueAsPercentage();
        float amount = amountParam.getValueAsPercentage();

        filter = new BrushedMetalFilter(colorParam.getColor().getRGB(),
                radius, amount, true, shine);

        dest = filter.filter(src, dest);
        return dest;
    }
}