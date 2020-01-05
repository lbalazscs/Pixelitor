/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

import com.jhlabs.image.HalftoneFilter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.IntChoiceParam.Value;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ShowOriginal;

import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.image.BufferedImage;

/**
 * Abstract base class for filters that use {@link HalftoneFilter}
 */
public abstract class JHMaskedHalftone extends ParametrizedFilter {
    protected static final int REPETITION_REFLECT = 1;
    protected static final int REPETITION_REPEAT = 2;

    protected final BooleanParam monochrome = new BooleanParam("Monochrome", true);
    protected final BooleanParam invert = new BooleanParam("Invert Pattern", false);
    protected final RangeParam stripesDistance = new RangeParam("Stripes Distance (px)", 1, 20, 101);
    protected final IntChoiceParam repetitionType = new IntChoiceParam("Stripes Type", new Value[]{
            new Value("Symmetric", REPETITION_REFLECT),
            new Value("One-sided", REPETITION_REPEAT),
    });
    protected final RangeParam shiftStripes = new RangeParam("Shift Stripes (%)", 0, 0, 100);
    protected final RangeParam softness = new RangeParam("Softness", 0, 10, 100);
    protected CycleMethod cycleMethod;
    protected float distanceCorrection;

    protected JHMaskedHalftone() {
        super(ShowOriginal.YES);
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        setupHelperVariables();

        BufferedImage stripes = createMaskImage(src);

        var filter = new HalftoneFilter(getName());
        filter.setMask(stripes);
        filter.setMonochrome(monochrome.isChecked());
        filter.setSoftness(softness.getPercentageValF());
        filter.setInvert(invert.isChecked());

        return filter.filter(src, dest);
    }

    private void setupHelperVariables() {
        int repetition = repetitionType.getValue();
        if (repetition == REPETITION_REFLECT) {
            cycleMethod = CycleMethod.REFLECT;
            distanceCorrection = 2.0f;
        } else if (repetition == REPETITION_REPEAT) {
            cycleMethod = CycleMethod.REPEAT;
            distanceCorrection = 1.0f;
        } else {
            throw new IllegalStateException("repetition = " + repetition);
        }
    }

    protected abstract BufferedImage createMaskImage(BufferedImage src);
}
