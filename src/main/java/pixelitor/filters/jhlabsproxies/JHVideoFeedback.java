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

package pixelitor.filters.jhlabsproxies;

import com.jhlabs.image.FeedbackFilter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.RangeParam;

import java.awt.image.BufferedImage;
import java.io.Serial;

/**
 * Video Feedback filter based on the JHLabs FeedbackFilter
 */
public class JHVideoFeedback extends ParametrizedFilter {
    public static final String NAME = "Video Feedback";

    @Serial
    private static final long serialVersionUID = 6619788137062818386L;

    private final RangeParam iterations = new RangeParam("Iterations", 2, 3, 30);
    private final ImagePositionParam center = new ImagePositionParam("Center");
    private final RangeParam rotation = new RangeParam("Rotation (Degrees/Iteration)", -30, 0, 30);
    private final RangeParam zoom = new RangeParam("Zoom (Percent/Iteration)", -100, -10, -4);
    private final RangeParam startOpacity = new RangeParam("Start Opacity (%)", 0, 100, 100);
    private final RangeParam endOpacity = new RangeParam("End Opacity (%)", 0, 100, 100);

    private FeedbackFilter filter;

    public JHVideoFeedback() {
        super(true);

        initParams(
            iterations,
            center,
            zoom,
            rotation,
            startOpacity,
            endOpacity
        );
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        if (rotation.isZero() && zoom.isZero()) {
            return src;
        }

        if (filter == null) {
            filter = new FeedbackFilter(NAME);
        }

        filter.setRotation(rotation.getValueInRadians());
        filter.setZoom((float) zoom.getPercentage());
        filter.setCenter(center.getRelativePoint());
        filter.setIterations(iterations.getValue());
        filter.setStartAlpha((float) startOpacity.getPercentage());
        filter.setEndAlpha((float) endOpacity.getPercentage());

        return filter.filter(src, dest);
    }
}