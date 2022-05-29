/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

import com.jhlabs.image.PointillizeFilter;
import com.jhlabs.math.Noise;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.ColorParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.utils.CachedFloatRandom;

import java.awt.image.BufferedImage;

import static java.awt.Color.BLACK;
import static pixelitor.filters.gui.ColorParam.TransparencyPolicy.FREE_TRANSPARENCY;
import static pixelitor.filters.gui.ReseedActions.reseedByCalling;

/**
 * Pointillize filter based on the JHLabs PointillizeFilter
 */
public class JHPointillize extends ParametrizedFilter {
    public static final String NAME = "Pointillize";

    private final RangeParam gridSize = new RangeParam("Grid Size", 1, 15, 200);
    private final RangeParam dotSize = new RangeParam("Dot Relative Size (%)", 0, 45, 100);
    private final RangeParam fuzziness = new RangeParam("Fill Fuzziness (%)", 0, 0, 100);
    private final ColorParam edgeColor = new ColorParam("Fill Color", BLACK, FREE_TRANSPARENCY);
    private final BooleanParam fadeEdges = new BooleanParam("Fade Instead of Fill", true);

    private final RangeParam randomness = new RangeParam("Grid Randomness (%)", 0, 0, 100);
    private final IntChoiceParam gridType = IntChoiceParam.forGridType("Grid Type", randomness);

    private PointillizeFilter filter;

    public JHPointillize() {
        super(true);

        setParams(
            gridSize.withAdjustedRange(0.2),
            gridType,
            randomness,
            fadeEdges,
            edgeColor,
            dotSize,
            fuzziness
        ).withAction(reseedByCalling(() -> {
            CachedFloatRandom.reseedCache();
            Noise.reseed();
        }));

        fadeEdges.setupDisableOtherIfChecked(edgeColor);
        fadeEdges.setupDisableOtherIfChecked(dotSize);
        fadeEdges.setupDisableOtherIfChecked(fuzziness);
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new PointillizeFilter();
        }

        // there is an angle property, but it does not work as expected
        filter.setScale(gridSize.getValueAsFloat());
        filter.setRandomness(randomness.getPercentageValF());
        filter.setEdgeThickness(dotSize.getPercentageValF());
        filter.setFuzziness(fuzziness.getPercentageValF());
        filter.setGridType(gridType.getValue());
        filter.setFadeEdges(fadeEdges.isChecked());
        filter.setEdgeColor(edgeColor.getColor().getRGB());
//        filter.setRndGenerator(rndGen.getValue());

        return filter.filter(src, dest);
    }
}