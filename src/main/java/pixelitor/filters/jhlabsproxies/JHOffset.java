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

import com.jhlabs.image.OffsetFilter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.IntChoiceParam;

import java.awt.image.BufferedImage;
import java.io.Serial;

/**
 * Offset filter based on the JHLabs {@link OffsetFilter}.
 */
public class JHOffset extends ParametrizedFilter {
    public static final String NAME = "Offset";

    @Serial
    private static final long serialVersionUID = -4148462098219318362L;

    private final ImagePositionParam center =
        new ImagePositionParam("Translate Top Left Point To");

    private final IntChoiceParam edgeAction = IntChoiceParam.forEdgeAction();

    public JHOffset() {
        super(true);

        initParams(center, edgeAction);
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        OffsetFilter filter = new OffsetFilter(NAME,
            edgeAction.getValue(), center.getAbsolutePoint(src));

        return filter.filter(src, dest);
    }
}
