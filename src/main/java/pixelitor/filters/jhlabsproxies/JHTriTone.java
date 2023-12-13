/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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

import com.jhlabs.image.TritoneFilter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.ColorParam;

import java.awt.image.BufferedImage;

import static java.awt.Color.BLACK;
import static java.awt.Color.RED;
import static java.awt.Color.YELLOW;
import static pixelitor.filters.gui.TransparencyPolicy.NO_TRANSPARENCY;

/**
 * Tritone filter based on the JHLabs TritoneFilter
 */
public class JHTriTone extends ParametrizedFilter {
    public static final String NAME = "Tritone";

    private final ColorParam shadowColor = new ColorParam("Shadow Color", BLACK, NO_TRANSPARENCY);
    private final ColorParam midtonesColor = new ColorParam("Midtones Color", RED, NO_TRANSPARENCY);
    private final ColorParam highlightsColor = new ColorParam("Highlights Color", YELLOW, NO_TRANSPARENCY);

    private TritoneFilter filter;

    public JHTriTone() {
        super(true);

        setParams(shadowColor, midtonesColor, highlightsColor);
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new TritoneFilter(NAME);
        }

        filter.setShadowColor(shadowColor.getColor().getRGB());
        filter.setHighColor(highlightsColor.getColor().getRGB());
        filter.setMidColor(midtonesColor.getColor().getRGB());

        return filter.filter(src, dest);
    }
}