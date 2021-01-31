/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.FilterGUI;
import pixelitor.filters.gui.GridAdjustmentPanel;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.layers.Drawable;

import java.awt.image.BufferedImage;

/**
 * Perspective filter based on the JHLabs PerspectiveFilter
 */
public class JHPerspective extends ParametrizedFilter {
    public static final String NAME = "Perspective";

    private final ImagePositionParam northWest = new ImagePositionParam("Northwest", 0.05f, 0.05f);
    private final ImagePositionParam northEast = new ImagePositionParam("Northeast", 0.95f, 0.05f);
    private final ImagePositionParam southWest = new ImagePositionParam("Southwest", 0.05f, 0.95f);
    private final ImagePositionParam southEast = new ImagePositionParam("Southeast", 0.95f, 0.95f);
    private final IntChoiceParam edgeAction = IntChoiceParam.forEdgeAction();
    private final IntChoiceParam interpolation = IntChoiceParam.forInterpolation();

    public JHPerspective() {
        super(true);

        setParams(
            northWest, northEast, southWest, southEast,
            edgeAction, interpolation
        );
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

        var filter = new PerspectiveFilter(northWestX, northWestY, northEastX, northEastY,
            southEastX, southEastY, southWestX, southWestY, NAME);

        filter.setEdgeAction(edgeAction.getValue());
        filter.setInterpolation(interpolation.getValue());

        return filter.filter(src, dest);
    }

    @Override
    public FilterGUI createGUI(Drawable dr) {
        return new GridAdjustmentPanel(this, dr, false, true);
    }
}