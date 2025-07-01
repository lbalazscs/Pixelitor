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

package pixelitor.filters;

import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.impl.MorphologyFilter;
import pixelitor.gui.GUIText;
import pixelitor.utils.StatusBarProgressTracker;

import java.awt.image.BufferedImage;
import java.io.Serial;

import static pixelitor.filters.impl.MorphologyFilter.OP_DILATE;
import static pixelitor.filters.impl.MorphologyFilter.OP_ERODE;

/**
 * A morphology filter
 */
public class Morphology extends ParametrizedFilter {
    public static final String NAME = "Morphology";

    @Serial
    private static final long serialVersionUID = -8455037018593373562L;

    private static final int OP_OPEN = 10;
    private static final int OP_CLOSE = 11;

    private final RangeParam radius = new RangeParam(GUIText.RADIUS, 1, 1, 21);
    private final IntChoiceParam kernel = new IntChoiceParam("Kernel Shape", new Item[]{
        new Item("Diamond", MorphologyFilter.KERNEL_DIAMOND),
        new Item("Square", MorphologyFilter.KERNEL_SQUARE),
    });
    private final IntChoiceParam op = new IntChoiceParam("Operation", new Item[]{
        new Item("Maximum (Dilate)", OP_DILATE),
        new Item("Minimum (Erode)", OP_ERODE),
        new Item("Open (Erode, then Dilate)", OP_OPEN),
        new Item("Close (Dilate, then Erode)", OP_CLOSE),
    });

    public Morphology() {
        super(true);

        initParams(op, kernel, radius);
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        var filter = new MorphologyFilter(NAME);

        int iterations = radius.getValue();
        filter.setIterations(iterations);
        filter.setKernel(kernel.getValue());

        int selectedOp = op.getValue();

        if (selectedOp == OP_DILATE || selectedOp == OP_ERODE) {
            filter.setOp(selectedOp);
            dest = filter.filter(src, dest);
        } else {
            var pt = new StatusBarProgressTracker(NAME, 2 * iterations);
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

            pt.finished();
        }

        return dest;
    }
}
