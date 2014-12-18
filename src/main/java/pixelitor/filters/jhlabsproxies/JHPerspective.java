package pixelitor.filters.jhlabsproxies;

import com.jhlabs.image.PerspectiveFilter;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.AdjustPanel;
import pixelitor.filters.gui.GeographicalAdjustmentPanel;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;

import java.awt.image.BufferedImage;

/**
 * Perspective based on the JHLabs PerspectiveFilter
 */
public class JHPerspective extends FilterWithParametrizedGUI {
    private ImagePositionParam northWestParam = new ImagePositionParam("North West", 0.05f, 0.05f);
    private ImagePositionParam northEastParam = new ImagePositionParam("North East", 0.95f, 0.05f);
    private ImagePositionParam southWestParam = new ImagePositionParam("South West", 0.05f, 0.95f);
    private ImagePositionParam southEastParam = new ImagePositionParam("South East", 0.95f, 0.95f);

    private IntChoiceParam edgeActionParam =  IntChoiceParam.getEdgeActionChoices();
    private IntChoiceParam interpolationParam = IntChoiceParam.getInterpolationChoices();

    private PerspectiveFilter filter;

    public JHPerspective() {
        super("Perspective", true, false);
        setParamSet(new ParamSet(
                northWestParam, northEastParam, southWestParam, southEastParam,
                edgeActionParam, interpolationParam
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        float northWestX = northWestParam.getRelativeX();
        float northWestY = northWestParam.getRelativeY();
        float northEastX = northEastParam.getRelativeX();
        float northEastY = northEastParam.getRelativeY();
        float southWestX = southWestParam.getRelativeX();
        float southWestY = southWestParam.getRelativeY();
        float southEastX = southEastParam.getRelativeX();
        float southEastY = southEastParam.getRelativeY();

        filter = new PerspectiveFilter(northWestX, northWestY, northEastX, northEastY,
                southEastX, southEastY, southWestX, southWestY);

        filter.setEdgeAction(edgeActionParam.getValue());
        filter.setInterpolation(interpolationParam.getValue());

        dest = filter.filter(src, dest);
        return dest;
    }

    @Override
    public AdjustPanel createAdjustPanel() {
        return new GeographicalAdjustmentPanel(this, false);
    }

}