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

package pixelitor.filters.jhlabsproxies;

import com.jhlabs.image.CellularFilter;
import com.jhlabs.image.CrystallizeFilter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.*;
import pixelitor.utils.Texts;

import java.awt.image.BufferedImage;
import java.io.Serial;

import static java.awt.Color.BLACK;
import static pixelitor.filters.gui.TransparencyMode.RANDOMIZED_ALPHA;

/**
 * Crystallize filter based on the JHLabs {@link CrystallizeFilter}.
 */
public class JHCrystallize extends ParametrizedFilter {
    @Serial
    private static final long serialVersionUID = -79769649907735220L;

    public static final String NAME = Texts.i18n("crystallize");

    private final RangeParam size = new RangeParam("Size", 1, 20, 200);
    private final RangeParam randomness = new RangeParam("Shape Randomness (%)", 0, 0, 100);
    private final EnumParam<CellularFilter.GridType> gridType = EnumParam.forGridType("Shape", randomness);

    // edge group
    private final RangeParam edgeThickness = new RangeParam("Thickness", 0, 40, 100);
    private final ColorParam edgeColor = new ColorParam("Color", BLACK, RANDOMIZED_ALPHA);
    private final BooleanParam fadeEdges = new BooleanParam("Fade");

    private final AngleParam angle = new AngleParam("Angle", 0);
    private final RangeParam stretch = new RangeParam("Stretch (%)", 100, 100, 1000);

    public JHCrystallize() {
        super(true);

        initParams(
            size.withAdjustedRange(0.2),
            gridType,
            randomness,
            CompositeParam.bordered("Edge",
                edgeThickness.withPresetKey("Edge Thickness"),
                edgeColor.withPresetKey("Edge Color"),
                fadeEdges.withPresetKey("Fade Edges")),
            angle,
            stretch
        ).withAction(paramSet.createReseedCachedAndNoiseAction());
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        CrystallizeFilter filter = new CrystallizeFilter(NAME,
            size.getValueAsFloat(),
            (float) stretch.getPercentage(),
            (float) (angle.getValueInRadians() + Math.PI / 2),
            gridType.getSelected(),
            (float) randomness.getPercentage(),
            (float) edgeThickness.getPercentage(),
            edgeColor.getColor().getRGB(),
            fadeEdges.isChecked());

        return filter.filter(src, dest);
    }
}
