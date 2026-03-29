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

import com.jhlabs.image.KaleidoscopeFilter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.utils.Texts;

import java.awt.image.BufferedImage;
import java.io.Serial;

import static pixelitor.gui.GUIText.ZOOM;

/**
 * A kaleidoscope filter based on the JHLabs {@link KaleidoscopeFilter}.
 */
public class JHKaleidoscope extends ParametrizedFilter {
    public static final String NAME = Texts.i18n("kaleidoscope");

    @Serial
    private static final long serialVersionUID = 4353876794593269727L;

    private final AngleParam angle = new AngleParam("Angle", 0);
    private final AngleParam rotateResult = new AngleParam("Rotate Result", 0);
    private final ImagePositionParam center = new ImagePositionParam("Center");
    private final RangeParam sides = new RangeParam("Sides", 0, 3, 10);

    private final RangeParam zoom = new RangeParam(ZOOM + " (%)", 1, 100, 501);
    private final IntChoiceParam edgeAction = IntChoiceParam.forEdgeAction(true);
    private final IntChoiceParam interpolation = IntChoiceParam.forInterpolation();

    public JHKaleidoscope() {
        super(true);

        zoom.setPresetKey("Zoom (%)");
        initParams(
            center,
            angle,
            sides,
//                radius,
            zoom,
            rotateResult,
            edgeAction,
            interpolation
        );
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        KaleidoscopeFilter filter = new KaleidoscopeFilter(
            NAME,
            edgeAction.getValue(),
            interpolation.getValue(),
            (float) angle.getValueInRadians(),
            (float) rotateResult.getValueInRadians(),
            sides.getValue(),
            center.getRelativePoint(),
            (float) zoom.getPercentage());

        return filter.filter(src, dest);
    }
}
