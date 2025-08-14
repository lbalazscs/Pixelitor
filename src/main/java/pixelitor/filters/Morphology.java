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

import com.jhlabs.composite.DifferenceComposite;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.impl.MorphologyFilter;
import pixelitor.gui.GUIText;
import pixelitor.utils.StatusBarProgressTracker;

import java.awt.Graphics2D;
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
    private static final int OP_GRADIENT = 12;
    private static final int OP_TOP_HAT = 13;
    private static final int OP_BOTTOM_HAT = 14;

    private final IntChoiceParam channel = new IntChoiceParam("Channel", new Item[]{
        new Item("R, G, B", MorphologyFilter.CHANNEL_RGB),
        new Item("YCbCr/Y", MorphologyFilter.CHANNEL_YCBCR),
        new Item("HSV/V", MorphologyFilter.CHANNEL_HSV),
    });

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
        new Item("Gradient (Dilate - Erode)", OP_GRADIENT),
        new Item("Top-Hat (Original - Open)", OP_TOP_HAT),
        new Item("Bottom-Hat (Close - Original)", OP_BOTTOM_HAT),
    });

    public Morphology() {
        super(true);

        initParams(op, channel, kernel, radius);
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        var filter = new MorphologyFilter(NAME);

        int iterations = radius.getValue();
        filter.setIterations(iterations);
        filter.setKernel(kernel.getValue());
        filter.setChannel(channel.getValue());

        int selectedOp = op.getValue();

        if (selectedOp == OP_DILATE || selectedOp == OP_ERODE) {
            filter.setOp(selectedOp);
            dest = filter.filter(src, dest);
        } else {
            // use custom progress tracker with twice as many work units
            var pt = new StatusBarProgressTracker(NAME, 2 * iterations);
            filter.setProgressTracker(pt);

            switch (selectedOp) {
                case OP_OPEN -> dest = open(src, dest, filter);
                case OP_CLOSE -> dest = close(src, dest, filter);
                case OP_GRADIENT -> dest = gradient(src, filter);
                case OP_TOP_HAT -> dest = topHat(src, dest, filter);
                case OP_BOTTOM_HAT -> dest = bottomHat(src, dest, filter);
                default -> throw new IllegalStateException("selectedOp = " + selectedOp);
            }

            pt.finished();
        }

        return dest;
    }

    private static BufferedImage open(BufferedImage src, BufferedImage dest, MorphologyFilter filter) {
        filter.setOp(OP_ERODE);
        dest = filter.filter(src, dest);
        filter.setOp(OP_DILATE);
        dest = filter.filter(dest, dest);
        return dest;
    }

    private static BufferedImage close(BufferedImage src, BufferedImage dest, MorphologyFilter filter) {
        filter.setOp(OP_DILATE);
        dest = filter.filter(src, dest);
        filter.setOp(OP_ERODE);
        dest = filter.filter(dest, dest);
        return dest;
    }

    private static BufferedImage gradient(BufferedImage src, MorphologyFilter filter) {
        BufferedImage dest;
        filter.setOp(OP_DILATE);
        BufferedImage d1 = filter.filter(src, null);

        filter.setOp(OP_ERODE);
        BufferedImage d2 = filter.filter(src, null);

        Graphics2D g = d1.createGraphics();
        g.setComposite(new DifferenceComposite(1.0f));
        g.drawImage(d2, 0, 0, null);
        g.dispose();
        dest = d1;
        return dest;
    }

    private static BufferedImage topHat(BufferedImage src, BufferedImage dest, MorphologyFilter filter) {
        dest = open(src, dest, filter);

        Graphics2D g = dest.createGraphics();
        g.setComposite(new DifferenceComposite(1.0f));
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return dest;
    }

    private static BufferedImage bottomHat(BufferedImage src, BufferedImage dest, MorphologyFilter filter) {
        dest = close(src, dest, filter);

        Graphics2D g = dest.createGraphics();
        g.setComposite(new DifferenceComposite(1.0f));
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return dest;
    }
}
