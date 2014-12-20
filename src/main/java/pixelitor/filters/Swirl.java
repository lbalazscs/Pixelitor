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
    private RangeParam amount = new RangeParam("Amount", -400, 400, 100);
    private RangeParam radius = new RangeParam("Radius", 0, 1000, 300);
    private RangeParam zoom = new RangeParam("Zoom (%)", 1, 500, 100);
    private AngleParam rotateResult = new AngleParam("Rotate Result", 0);

    private ImagePositionParam center = new ImagePositionParam("Center");
    private IntChoiceParam edgeAction = IntChoiceParam.getEdgeActionChoices();
    private IntChoiceParam interpolation = IntChoiceParam.getInterpolationChoices();

    private SwirlFilter filter;

    public Swirl() {
        super("Swirl", true, false);
        setParamSet(new ParamSet(
                amount,
                radius.adjustRangeToImageSize(1.0),
                center,
                zoom,
                rotateResult,
                edgeAction,
                interpolation
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if(filter == null) {
            filter = new SwirlFilter();
        }

        filter.setSwirlAmount(amount.getValueAsPercentage());
        filter.setRadius(radius.getValueAsFloat());
        filter.setZoom(zoom.getValueAsPercentage());
        filter.setRotateResultAngle((float) rotateResult.getValueInIntuitiveRadians());

        filter.setCenterX(center.getRelativeX());
        filter.setCenterY(center.getRelativeY());
        filter.setEdgeAction(edgeAction.getValue());
        filter.setInterpolation(interpolation.getValue());

        dest = filter.filter(src, dest);
        return dest;
    }
}