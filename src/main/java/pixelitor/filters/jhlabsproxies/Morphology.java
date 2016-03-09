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

import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.filters.impl.MorphologyFilter;
import pixelitor.utils.BasicProgressTracker;
import pixelitor.utils.ProgressTracker;

import java.awt.image.BufferedImage;

import static pixelitor.filters.impl.MorphologyFilter.OP_DILATE;
import static pixelitor.filters.impl.MorphologyFilter.OP_ERODE;

/**
 * A morphology filter
 */
public class Morphology extends FilterWithParametrizedGUI {
    public static final String NAME = "Morphology";

    private static final int OP_OPEN = 10;
    private static final int OP_CLOSE = 11;

    private final RangeParam radius = new RangeParam("Radius", 1, 1, 20);
    private final IntChoiceParam kernel = new IntChoiceParam("Kernel Shape", new IntChoiceParam.Value[]{
            new IntChoiceParam.Value("Diamond", MorphologyFilter.KERNEL_DIAMOND),
            new IntChoiceParam.Value("Square", MorphologyFilter.KERNEL_SQUARE),
    });
    private final IntChoiceParam op = new IntChoiceParam("Operation", new IntChoiceParam.Value[]{
            new IntChoiceParam.Value("Maximum (Dilate)", OP_DILATE),
            new IntChoiceParam.Value("Minimum (Erode)", OP_ERODE),
            new IntChoiceParam.Value("Open (Erode, then Dilate)", OP_OPEN),
            new IntChoiceParam.Value("Close (Dilate, then Erode)", OP_CLOSE),
    });

    public Morphology() {
        super(ShowOriginal.YES);
        setParamSet(new ParamSet(op, kernel, radius));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        MorphologyFilter filter = new MorphologyFilter(NAME);

        int iterations = radius.getValue();
        filter.setIterations(iterations);
        filter.setKernel(kernel.getValue());

        int selectedOp = op.getValue();

        if (selectedOp == OP_DILATE || selectedOp == OP_ERODE) {
            filter.setOp(selectedOp);
            dest = filter.filter(src, dest);
        } else {
            ProgressTracker pt = new BasicProgressTracker(NAME, 2 * iterations);
            filter.setProgressTracker(pt);

            if (selectedOp == OP_OPEN) {
                filter.setOp(OP_ERODE);
                dest = filter.filter(src, dest);
                filter.setOp(OP_DILATE);
                dest = filter.filter(dest, dest);
            } else if (selectedOp == OP_CLOSE) {
                filter.setOp(OP_DILATE);
                dest = filter.filter(src, dest);
                filter.setOp(OP_ERODE);
                dest = filter.filter(dest, dest);
            } else {
                throw new IllegalStateException("selectedOp = " + selectedOp);
            }

            pt.finish();
        }

        return dest;
    }
}
