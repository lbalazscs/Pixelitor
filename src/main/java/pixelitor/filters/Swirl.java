package pixelitor.filters;

import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.impl.SwirlFilter;

import java.awt.image.BufferedImage;

/**
 * Swirl filter
 */
public class Swirl extends FilterWithParametrizedGUI {
    private RangeParam amountParam = new RangeParam("Amount", -400, 400, 100);
    private RangeParam radiusParam = new RangeParam("Radius", 0, 1000, 300);
    private RangeParam zoomParam = new RangeParam("Zoom (%)", 1, 500, 100);
    private AngleParam rotateResultParam = new AngleParam("Rotate Result", 0);

    private ImagePositionParam centerParam = new ImagePositionParam("Center");
    private IntChoiceParam edgeActionParam =  IntChoiceParam.getEdgeActionChoices();
    private IntChoiceParam interpolationParam = IntChoiceParam.getInterpolationChoices();

    private SwirlFilter filter;

    public Swirl() {
        super("Swirl", true, false);
        setParamSet(new ParamSet(
                amountParam,
                radiusParam.adjustRangeAccordingToImage(1.0),
                centerParam,
                zoomParam,
                rotateResultParam,
                edgeActionParam,
                interpolationParam
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if(filter == null) {
            filter = new SwirlFilter();
        }

        filter.setSwirlAmount(amountParam.getValueAsPercentage());
        filter.setRadius(radiusParam.getValue());
        filter.setZoom(zoomParam.getValueAsPercentage());
        filter.setRotateResultAngle((float) rotateResultParam.getValueInIntuitiveRadians());

        filter.setCenterX(centerParam.getRelativeX());
        filter.setCenterY(centerParam.getRelativeY());
        filter.setEdgeAction(edgeActionParam.getValue());
        filter.setInterpolation(interpolationParam.getValue());

        dest = filter.filter(src, dest);
        return dest;
    }
}