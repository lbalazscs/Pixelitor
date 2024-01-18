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
package pixelitor.filters.jhlabsproxies;

import com.jhlabs.image.DiffuseFilter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.FilterButtonModel;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.RangeParam;

import java.awt.image.BufferedImage;
import java.io.Serial;

import static pixelitor.filters.gui.IntChoiceParam.EDGE_REPEAT_PIXELS;

/**
 * Frosted Glass filter based on the JHLabs DiffuseFilter
 */
public class JHFrostedGlass extends ParametrizedFilter {
    public static final String NAME = "Frosted Glass";

    @Serial
    private static final long serialVersionUID = -1688362920985195721L;

    private final RangeParam amount = new RangeParam("Amount", 0, 10, 100);

    private final IntChoiceParam edgeAction = IntChoiceParam.forEdgeAction();
    private final IntChoiceParam interpolation = IntChoiceParam.forInterpolation();
    private DiffuseFilter filter;

    public JHFrostedGlass() {
        super(true);

        setParams(amount.withAdjustedRange(0.1),
            interpolation,
            edgeAction.withDefaultChoice(EDGE_REPEAT_PIXELS)
        ).withAction(FilterButtonModel.createNoOpReseed());
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        if (amount.isZero()) {
            return src;
        }

        if (filter == null) {
            filter = new DiffuseFilter(NAME);
        }

        filter.setScale(amount.getValueAsFloat());
        filter.setEdgeAction(edgeAction.getValue());
        filter.setInterpolation(interpolation.getValue());

        return filter.filter(src, dest);
    }
}