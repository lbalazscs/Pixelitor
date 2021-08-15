/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

import com.jhlabs.image.TransformFilter;
import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.impl.MirrorFilter;
import pixelitor.gui.GUIText;

import java.awt.image.BufferedImage;

/**
 * Mirror filter
 */
public class Mirror extends ParametrizedFilter {
    public static final String NAME = GUIText.MIRROR;

    private final AngleParam angle = new AngleParam("Angle", 0);
    private final ImagePositionParam center = new ImagePositionParam("Center");
    private final IntChoiceParam edgeAction = IntChoiceParam.forEdgeAction(true);

    private MirrorFilter filter;

    public Mirror() {
        super(true);

        setParams(
            center,
            angle,
            edgeAction
        );
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new MirrorFilter();
        }

        filter.setCenter(center.getRelativeX(), center.getRelativeY(), src);
        filter.setEdgeAction(edgeAction.getValue());
        filter.setInterpolation(TransformFilter.NEAREST_NEIGHBOUR);
        filter.setAngle(angle.getValueInRadians());

        return filter.filter(src, dest);
    }
}

