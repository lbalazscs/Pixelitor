/*
 * Copyright 2015 Laszlo Balazs-Csiki
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
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.ActionParam;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.ColorParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ReseedNoiseActionParam;
import pixelitor.utils.CachedFloatRandom;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * Pointillize based on the JHLabs PointillizeFilter
 */
public class JHPointillize extends FilterWithParametrizedGUI {
    private final RangeParam gridSize = new RangeParam("Grid Size", 1, 200, 15);
    private final RangeParam dotSize = new RangeParam("Dot Relative Size (%)", 0, 100, 45);
    private final RangeParam fuzziness = new RangeParam("Fuzziness (%)", 0, 100, 0);
    private final ColorParam edgeColor = new ColorParam("Fill Color", Color.BLACK, true, true);
    private final BooleanParam fadeEdges = new BooleanParam("Fade Instead of Fill", true);

    private final RangeParam randomness = new RangeParam("Grid Randomness (%)", 0, 100, 0);
    private final IntChoiceParam gridType = IntChoiceParam.getGridTypeChoices("Grid Type", randomness);

    private final ActionParam reseedAction = new ReseedNoiseActionParam(e -> {
        CachedFloatRandom.reseedCache();
        Noise.reseed();
    });


    private PointillizeFilter filter;

    public JHPointillize() {
        super("Pointillize", true, false);
        setParamSet(new ParamSet(
                gridSize.adjustRangeToImageSize(0.2),
                gridType,
                randomness,
                fadeEdges,
                edgeColor,
                dotSize,
                fuzziness,
                reseedAction
//                rndGen
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new PointillizeFilter();
        }

        // there is an angle property but it does not work as expected
        filter.setScale(gridSize.getValueAsFloat());
        filter.setRandomness(randomness.getValueAsPercentage());
        filter.setEdgeThickness(dotSize.getValueAsPercentage());
        filter.setFuzziness(fuzziness.getValueAsPercentage());
        filter.setGridType(gridType.getValue());
        filter.setFadeEdges(fadeEdges.isChecked());
        filter.setEdgeColor(edgeColor.getColor().getRGB());
//        filter.setRndGenerator(rndGen.getValue());

        dest = filter.filter(src, dest);
        return dest;
    }
}