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

package pixelitor.filters.jhlabsproxies;

import com.jhlabs.image.CrystallizeFilter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.ColorParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.utils.Texts;

import java.awt.image.BufferedImage;
import java.io.Serial;

import static java.awt.Color.BLACK;
import static pixelitor.filters.gui.TransparencyMode.ALPHA_ENABLED;

/**
 * Crystallize filter based on the JHLabs CrystallizeFilter
 */
public class JHCrystallize extends ParametrizedFilter {
    @Serial
    private static final long serialVersionUID = -79769649907735220L;

    public static final String NAME = Texts.i18n("crystallize");

    private final RangeParam edgeThickness = new RangeParam("Edge Thickness", 0, 40, 100);
    private final RangeParam size = new RangeParam("Size", 1, 20, 200);
    private final ColorParam edgeColor = new ColorParam("Edge Color", BLACK, ALPHA_ENABLED);
    private final BooleanParam fadeEdges = new BooleanParam("Fade Edges");
    private final RangeParam randomness = new RangeParam("Shape Randomness (%)", 0, 0, 100);
    private final IntChoiceParam gridType = IntChoiceParam.forGridType("Shape", randomness);

    private CrystallizeFilter filter;

    public JHCrystallize() {
        super(true);

        initParams(
            size.withAdjustedRange(0.2),
            edgeThickness,
            gridType,
            randomness,
            edgeColor,
            fadeEdges
        ).withAction(paramSet.createReseedCachedAndNoiseAction());
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new CrystallizeFilter();
        }

        filter.setEdgeThickness((float) edgeThickness.getPercentage());
        filter.setScale(size.getValueAsFloat());
        filter.setRandomness((float) randomness.getPercentage());
        filter.setEdgeColor(edgeColor.getColor().getRGB());
        filter.setGridType(gridType.getValue());
        filter.setFadeEdges(fadeEdges.isChecked());

        return filter.filter(src, dest);
    }
}