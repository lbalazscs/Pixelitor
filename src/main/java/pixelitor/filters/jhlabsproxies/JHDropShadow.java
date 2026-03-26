/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

import com.jhlabs.image.ShadowFilter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.ColorParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.GUIText;

import java.awt.image.BufferedImage;
import java.io.Serial;

import static java.awt.Color.BLACK;
import static pixelitor.filters.gui.TransparencyMode.OPAQUE_ONLY;
import static pixelitor.utils.AngleUnit.INTUITIVE_DEGREES;

/**
 * Drop Shadow filter based on the JHLabs {@link ShadowFilter}.
 */
public class JHDropShadow extends ParametrizedFilter {
    public static final String NAME = "Drop Shadow";

    @Serial
    private static final long serialVersionUID = -3914785189683755908L;

    private final AngleParam angle = new AngleParam("Angle", 315, INTUITIVE_DEGREES);
    private final RangeParam distance = new RangeParam("Distance", 0, 10, 100);
    private final RangeParam opacity = new RangeParam(GUIText.OPACITY, 0, 90, 100);
    private final RangeParam softness = new RangeParam("Softness", 0, 10, 25);
    private final BooleanParam shadowOnly = new BooleanParam("Shadow Only");
    private final ColorParam color = new ColorParam("Color", BLACK, OPAQUE_ONLY);

    public JHDropShadow() {
        super(true);

        opacity.setPresetKey("Opacity (%)");

        initParams(
            angle,
            distance.withAdjustedRange(0.1),
            opacity,
            softness.withAdjustedRange(0.025),
            color,
            shadowOnly
        );
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        ShadowFilter filter = new ShadowFilter(NAME,
            (float) angle.getValueInIntuitiveRadians(),
            distance.getValueAsFloat(),
            (float) opacity.getPercentage(),
            softness.getValueAsFloat(),
            color.getColor().getRGB(),
            shadowOnly.isChecked()
        );

        dest = filter.filter(src, dest);

        return dest;
    }

    @Override
    public boolean supportsGray() {
        return false;
    }
}
