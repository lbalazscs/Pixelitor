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

package pixelitor.filters.transitions;

import com.bric.image.transition.Transition;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.utils.ImageUtils;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;

public abstract class AbstractTransition extends ParametrizedFilter {
    private final RangeParam progress = new RangeParam("Progress (%)", 0, 50, 100);
    protected final BooleanParam invert = new BooleanParam("Invert Transparency");

    protected AbstractTransition() {
        super(true);

        // invert is added in the subclasses because
        // the blind transition doesn't work with it
        initParams(progress);
    }

    abstract Transition createTransition();

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        var frameA = src;
        var frameB = ImageUtils.createImageWithSameCM(src);
        Transition transition = createTransition();

        Graphics2D g2 = dest.createGraphics();
        g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        transition.paint(g2, frameA, frameB, (float) progress.getPercentage(), invert.isChecked());
        g2.dispose();

        return dest;
    }
}
