/*
 * Copyright 2016 Laszlo Balazs-Csiki
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
import com.jhlabs.math.Noise;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.ActionSetting;
import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.GradientParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ReseedNoiseActionSetting;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.utils.CachedFloatRandom;

import java.awt.image.BufferedImage;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;

/**
 * Cells based on the JHLabs CellularFilter
 */
public class JHCells extends FilterWithParametrizedGUI {
    public static final String NAME = "Cells";

    private static final int TYPE_CELLS = 1;
    private static final int TYPE_GRID = 2;
    private static final int TYPE_STRANGE = 3;

    private final GradientParam gradient = new GradientParam("Colors", BLACK, WHITE);

    private final RangeParam scale = new RangeParam("Zoom", 1, 100, 500);
    private final RangeParam stretch = new RangeParam("Stretch (%)", 100, 100, 999);
//    private RangeParam f1Param = new RangeParam("F1", -100, 100, -100);
//    private RangeParam f2Param = new RangeParam("F2", -100, 100, -100);
//    private RangeParam f3Param = new RangeParam("F3", -100, 100, 0);

    private final RangeParam gridRandomness = new RangeParam("Grid Randomness", 1, 1, 100);
    private final IntChoiceParam gridType = IntChoiceParam.getGridTypeChoices("Grid Type", gridRandomness);

    private final IntChoiceParam type = new IntChoiceParam("Type", new IntChoiceParam.Value[]{
//            new IntChoiceParam.Value("Free", 0),
            new IntChoiceParam.Value("Cells", TYPE_CELLS),
            new IntChoiceParam.Value("Grid", TYPE_GRID),
            new IntChoiceParam.Value("Grid 2", TYPE_STRANGE),
    });
    private final RangeParam refineType = new RangeParam("Refine Type", 0, 0, 100);
    private final RangeParam darkLightBalance = new RangeParam("Dark/Light Balance", -20, 0, 20);

    private final AngleParam angle = new AngleParam("Angle", 0);

    private CellularFilter filter;

    public JHCells() {
        super(ShowOriginal.NO);

        ActionSetting reseedAction = new ReseedNoiseActionSetting(e -> {
            CachedFloatRandom.reseedCache();
            Noise.reseed();
        });
        setParamSet(new ParamSet(
                type,
                refineType,
                gridType,
                gridRandomness,
                gradient,
                darkLightBalance,
                scale.adjustRangeToImageSize(0.5),
                stretch,
                angle
        ).withAction(reseedAction));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new CellularFilter(NAME);
        }

        float tune = refineType.getValueAsPercentage();
        float f1, f2, f3;

        switch (type.getValue()) {
            case TYPE_CELLS:
                f1 = 1.0f - tune;
                f2 = tune;
                f3 = -tune / 3;
                break;
            case TYPE_GRID:
                f1 = -1.0f + tune;
                f2 = 1.0f;
                f3 = -tune / 2;
                break;
            case TYPE_STRANGE:
                f1 = -0.5f + tune;
                f2 = 0.5f - tune;
                f3 = 0.15f + tune / 2;
                break;
            default:
                throw new IllegalStateException("type.getValue() = " + type.getValue());
        }

        float bw = darkLightBalance.getValueAsPercentage();
        f1 += bw;
        f2 += bw;
        f3 += bw;

        filter.setScale(scale.getValueAsFloat());
        filter.setStretch(stretch.getValueAsPercentage());
        filter.setAngle((float) (angle.getValueInRadians() + (Math.PI / 2)));
        filter.setF1(f1);
        filter.setF2(f2);
        filter.setF3(f3);
        filter.setGridType(gridType.getValue());
        filter.setRandomness(gridRandomness.getValueAsPercentage());
        filter.setColormap(gradient.getValue());

        dest = filter.filter(src, dest);
        return dest;
    }
}