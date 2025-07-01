/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.filters;

import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.GroupedRangeParam;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.impl.GridKaleidoscopeFilter;

import java.awt.image.BufferedImage;
import java.io.Serial;

public class GridKaleidoscope extends ParametrizedFilter {
    public static final String NAME = "Grid Kaleidoscope";

    @Serial
    private static final long serialVersionUID = 1L;

    private final AngleParam angle = new AngleParam("Grid Angle", 0);
    private final GroupedRangeParam gridSize = new GroupedRangeParam("Grid Size (%)", 5, 50, 100);
    private final GroupedRangeParam distortion = new GroupedRangeParam("Distortion", -100, 0, 100, false);
    private final ImagePositionParam center = new ImagePositionParam("Center");
    private final IntChoiceParam style = new IntChoiceParam("Style", new IntChoiceParam.Item[]{
        new IntChoiceParam.Item("Mirror", GridKaleidoscopeFilter.STYLE_MIRROR),
        new IntChoiceParam.Item("Repeat", GridKaleidoscopeFilter.STYLE_REPEAT),
    });
    private final IntChoiceParam edgeAction = IntChoiceParam.forEdgeAction(true);
    private final IntChoiceParam interpolation = IntChoiceParam.forInterpolation();

    private GridKaleidoscopeFilter filter;

    public GridKaleidoscope() {
        super(true);

        initParams(
            gridSize,
            angle,
            center,
            distortion,
            style,
            edgeAction,
            interpolation
        );
    }

    @Override
    protected BufferedImage transform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new GridKaleidoscopeFilter(NAME);
        }

        filter.setGridAngle((float) angle.getValueInRadians());
        filter.setGridSizeX(src.getWidth() * gridSize.getPercentage(0));
        filter.setGridSizeY(src.getHeight() * gridSize.getPercentage(1));
        filter.setDistortionX(distortion.getValue(0));
        filter.setDistortionY(distortion.getValue(1));
        filter.setCenter(center.getRelativePoint());
        filter.setStyle(style.getValue());
        filter.setEdgeAction(edgeAction.getValue());
        filter.setInterpolation(interpolation.getValue());

        return filter.filter(src, dest);
    }
}
