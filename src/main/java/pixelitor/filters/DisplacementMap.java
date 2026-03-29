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
import pixelitor.filters.impl.DisplacementMapFilter;

import java.awt.image.BufferedImage;

public class DisplacementMap extends ParametrizedFilter {
    public static final String NAME = "Displacement Map";

    private final SelectImageParam imageParam = new SelectImageParam("Displacement Map");
    private final BooleanParam tileParam = new BooleanParam("Tile");
    private final RangeParam amount = new RangeParam(
        "Amount", 0, 10, 100);
    private final AngleParam angle = new AngleParam("Angle", 0);
    private final IntChoiceParam edgeAction = IntChoiceParam.forEdgeAction();
    private final IntChoiceParam interpolation = IntChoiceParam.forInterpolation();

    public DisplacementMap() {
        super(true);

        initParams(
            imageParam,
            tileParam,
            amount.withAdjustedRange(0.2),
            angle,
            edgeAction,
            interpolation
        );
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        var filter = new DisplacementMapFilter(
            NAME,
            edgeAction.getValue(),
            interpolation.getValue(),
            imageParam.getImage(),
            tileParam.isChecked(),
            amount.getValueAsDouble(),
            angle.getValueInRadians()
        );

        return filter.filter(src, dest);
    }

    @Override
    public boolean canBeSmart() {
        return false;
    }
}
