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
import com.jhlabs.image.PointillizeFilter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.*;
import pixelitor.utils.Texts;

import java.awt.image.BufferedImage;
import java.io.Serial;

import static java.awt.Color.BLACK;
import static pixelitor.filters.gui.TransparencyMode.RANDOMIZED_ALPHA;

/**
 * Pointillize filter based on the JHLabs {@link PointillizeFilter}.
 */
public class JHPointillize extends ParametrizedFilter {
    @Serial
    private static final long serialVersionUID = 7785086882567467357L;

    public static final String NAME = Texts.i18n("pointillize");

    private final RangeParam gridSize = new RangeParam("Grid Size", 1, 15, 200);
    private final RangeParam dotRadius = new RangeParam("Dot Relative Size (%)", 0, 45, 100);
    private final RangeParam fuzziness = new RangeParam("Fill Fuzziness (%)", 0, 0, 100);
    private final ColorParam fillColor = new ColorParam("Fill Color", BLACK, RANDOMIZED_ALPHA);
    private final BooleanParam fadeEdges = new BooleanParam("Fade Instead of Fill", true);

    private final RangeParam randomness = new RangeParam("Grid Randomness (%)", 0, 0, 100);
    private final EnumParam<CellularFilter.GridType> gridType = EnumParam.forGridType("Grid Type", randomness);

    private final AngleParam angle = new AngleParam("Angle", 0);
    private final RangeParam stretch = new RangeParam("Stretch (%)", 100, 100, 1000);

    private PointillizeFilter filter;

    public JHPointillize() {
        super(true);

        initParams(
            gridSize.withAdjustedRange(0.2),
            gridType,
            randomness,
            fadeEdges,
            fillColor,
            dotRadius,
            fuzziness,
            angle,
            stretch
        ).withAction(paramSet.createReseedCachedAndNoiseAction());

        // when "Fade Instead of Fill" is checked, then "Fill Color",
        // "Dot Relative Size" and "Fill Fuzziness" should be disabled
        fadeEdges.disableOtherWhenChecked(fillColor);
        fadeEdges.disableOtherWhenChecked(dotRadius);
        fadeEdges.disableOtherWhenChecked(fuzziness);
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new PointillizeFilter(NAME);
        }

        filter.setScale(gridSize.getValueAsFloat());
        filter.setRandomness((float) randomness.getPercentage());
        filter.setDotRadius((float) dotRadius.getPercentage());
        filter.setFuzziness((float) fuzziness.getPercentage());
        filter.setGridType(gridType.getSelected());
        filter.setFadeEdges(fadeEdges.isChecked());
        filter.setBackgroundColor(fillColor.getColor().getRGB());

        filter.setStretch((float) stretch.getPercentage());
        filter.setAngle((float) (angle.getValueInRadians() + Math.PI / 2));

        return filter.filter(src, dest);
    }
}
