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
import pixelitor.filters.impl.MagnifyFilter;
import pixelitor.gui.GUIText;
import pixelitor.utils.BlurredShape;

import java.awt.image.BufferedImage;
import java.io.Serial;

/**
 * Magnify filter
 */
public class Magnify extends ParametrizedFilter {
    public static final String NAME = "Magnify";

    @Serial
    private static final long serialVersionUID = -7415549925214553939L;

    private final RangeParam magnification = new RangeParam("Magnification (%)", 1, 150, 501);
    private final GroupedRangeParam outerRadius = new GroupedRangeParam(GUIText.RADIUS, 0, 200, 999);
    private final RangeParam softness = new RangeParam("Softness", 0, 100, 1000);
    private final ImagePositionParam center = new ImagePositionParam("Center");
    private final IntChoiceParam shape = BlurredShape.getChoices();
    private final BooleanParam invert = new BooleanParam("Invert");
    private final IntChoiceParam edgeAction = IntChoiceParam.forEdgeAction();
    private final IntChoiceParam interpolation = IntChoiceParam.forInterpolation();

    public Magnify() {
        super(true);

        initParams(
            magnification,
            outerRadius.withAdjustedRange(1.0),
            softness,
            center,
            shape,
            invert,
            edgeAction,
            interpolation
        );
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        int ox = outerRadius.getHorizontal();
        int oy = outerRadius.getVertical();
        double softnessRatio = softness.getPercentage() + 1.0;
        float ix = (float) (ox / softnessRatio);
        float iy = (float) (oy / softnessRatio);

        MagnifyFilter filter = new MagnifyFilter(
            NAME,
            edgeAction.getValue(),
            interpolation.getValue(),
            center.getAbsolutePoint(src),
            magnification.getPercentage(),
            ix,
            iy,
            (float) ox,
            (float) oy,
            shape.getValue(),
            invert.isChecked()
        );

        return filter.filter(src, dest);
    }
}
