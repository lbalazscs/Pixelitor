/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.filters.gui.*;
import pixelitor.filters.impl.GridKaleidoscopeFilter;
import pixelitor.gui.utils.SliderSpinner;

import java.awt.image.BufferedImage;
import java.io.Serial;

public class GridKaleidoscope extends ParametrizedFilter {
    public static final String NAME = "Grid Kaleidoscope";

    @Serial
    private static final long serialVersionUID = 1L;

    private final IntChoiceParam gridType = new IntChoiceParam("Pattern", new IntChoiceParam.Item[]{
        new IntChoiceParam.Item("Squares (2-Fold)", GridKaleidoscopeFilter.GRID_SQUARE),
        new IntChoiceParam.Item("Squares (4-Fold)", GridKaleidoscopeFilter.GRID_SQUARE_8_FOLD),
        new IntChoiceParam.Item("Octagons and Squares (4-Fold)", GridKaleidoscopeFilter.GRID_OCTAGONAL_TRUNCATED),
        new IntChoiceParam.Item("Bricks (2-Fold)", GridKaleidoscopeFilter.GRID_BRICK),
        new IntChoiceParam.Item("Triangles (3-Fold)", GridKaleidoscopeFilter.GRID_TRIANGULAR),
        new IntChoiceParam.Item("Hexagons (6-Fold)", GridKaleidoscopeFilter.GRID_HEXAGONAL),
    });
    private final RangeParam gridSize = new RangeParam("Size", 20, 100, 500, true, SliderSpinner.LabelPosition.NONE_WITH_TICKS);
    private final AngleParam angle = new AngleParam("Rotation", 0);
    private final ImagePositionParam center = new ImagePositionParam("Center");
    private final GroupedRangeParam distortion = new GroupedRangeParam("Wave Distortion", -100, 0, 100, false);
    private final IntChoiceParam edgeAction = IntChoiceParam.forEdgeAction(true);
    private final IntChoiceParam interpolation = IntChoiceParam.forInterpolation();

    public GridKaleidoscope() {
        super(true);

        initParams(
            CompositeParam.border("Grid",
                gridType,
                gridSize,
                angle.withoutBorder()),
            center,
            distortion,
            edgeAction,
            interpolation
        );
    }

    @Override
    protected BufferedImage transform(BufferedImage src, BufferedImage dest) {
        GridKaleidoscopeFilter filter = new GridKaleidoscopeFilter(
            NAME,
            edgeAction.getValue(),
            interpolation.getValue(),
            gridType.getValue(),
            gridSize.getValueAsDouble(),
            angle.getValueInRadians(),
            distortion.getHorizontal(),
            distortion.getVertical(),
            center.getRelativePoint()
        );

        return filter.filter(src, dest);
    }
}
