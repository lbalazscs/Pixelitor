/*
 * Copyright 2016 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor. If not, see <http://www.gnu.org/licenses/>.
 */

package pixelitor.filters.jhlabsproxies;

import com.jhlabs.image.PerspectiveFilter;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.AdjustPanel;
import pixelitor.filters.gui.GridAdjustmentPanel;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.layers.ImageLayer;

import java.awt.image.BufferedImage;

/**
 * Perspective based on the JHLabs PerspectiveFilter
 */
public class JHPerspective extends FilterWithParametrizedGUI {
    public static final String NAME = "Perspective";

    private final ImagePositionParam northWest = new ImagePositionParam("North West", 0.05f, 0.05f);
    private final ImagePositionParam northEast = new ImagePositionParam("North East", 0.95f, 0.05f);
    private final ImagePositionParam southWest = new ImagePositionParam("South West", 0.05f, 0.95f);
    private final ImagePositionParam southEast = new ImagePositionParam("South East", 0.95f, 0.95f);

    private final IntChoiceParam edgeAction = IntChoiceParam.getEdgeActionChoices();
    private final IntChoiceParam interpolation = IntChoiceParam.getInterpolationChoices();

    public JHPerspective() {
        super(ShowOriginal.YES);
        setParamSet(new ParamSet(
                northWest, northEast, southWest, southEast,
                edgeAction, interpolation
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        float northWestX = northWest.getRelativeX();
        float northWestY = northWest.getRelativeY();
        float northEastX = northEast.getRelativeX();
        float northEastY = northEast.getRelativeY();
        float southWestX = southWest.getRelativeX();
        float southWestY = southWest.getRelativeY();
        float southEastX = southEast.getRelativeX();
        float southEastY = southEast.getRelativeY();

        PerspectiveFilter filter = new PerspectiveFilter(northWestX, northWestY, northEastX, northEastY,
                southEastX, southEastY, southWestX, southWestY, NAME);

        filter.setEdgeAction(edgeAction.getValue());
        filter.setInterpolation(interpolation.getValue());

        dest = filter.filter(src, dest);
        return dest;
    }

    @Override
    public AdjustPanel createAdjustPanel(ImageLayer layer) {
        return new GridAdjustmentPanel(this, layer, false, ShowOriginal.YES);
    }
}