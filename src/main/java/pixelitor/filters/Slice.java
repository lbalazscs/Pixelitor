/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

import com.jhlabs.image.TransformFilter;
import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.GroupedRangeParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.impl.SliceFilter;

import java.awt.image.BufferedImage;
import java.io.Serial;

/**
 * Slice filter
 */
public class Slice extends ParametrizedFilter {
    public static final String NAME = "Slice";

    @Serial
    private static final long serialVersionUID = -6725097851795366223L;

    private final RangeParam size = new RangeParam("Size", 1, 75, 300);
    private final RangeParam offset = new RangeParam("Offset", 0, 10, 100);
    private final GroupedRangeParam shift = new GroupedRangeParam(
        "Shift Effect (Size %)", 0, 0, 100, false);
    private final AngleParam angle = new AngleParam("Angle", 0);
    private final IntChoiceParam edgeAction = IntChoiceParam.forEdgeAction();

    private SliceFilter filter;

    public Slice() {
        super(true);

        setParams(
            size.withAdjustedRange(0.25),
            offset.withAdjustedRange(0.25),
            shift,
            angle,
            edgeAction
        );
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new SliceFilter(NAME);
        }

        filter.setOffset(offset.getValue());
        filter.setSize(size.getValue());
        filter.setShiftHorizontal(shift.getPercentage(0));
        filter.setShiftVertical(shift.getPercentage(1));
        filter.setAngle(angle.getValueInIntuitiveRadians());
        filter.setEdgeAction(edgeAction.getValue());
        filter.setInterpolation(TransformFilter.NEAREST_NEIGHBOUR); // no difference

        return filter.filter(src, dest);
    }
}