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

package pixelitor.filters.gmic;

import pixelitor.filters.gui.RangeParam;

import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.List;

public class Vibrance extends GMICFilter {
    @Serial
    private static final long serialVersionUID = 1L;

    public static final String NAME = "Vibrance";

    private final RangeParam strength = new RangeParam("Strength", -100, 0, 300);

    public Vibrance() {
        initParams(strength);
    }

    @Override
    public List<String> getArgs() {
        return List.of("fx_vibrance", strength.getPercentageStr(),
            "cut", "0,255"); // workaround
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        if (strength.isZero()) {
            return src;
        }
        return super.transform(src, dest);
    }
}
