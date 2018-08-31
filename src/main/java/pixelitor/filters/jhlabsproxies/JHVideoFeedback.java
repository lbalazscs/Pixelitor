/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.filters.gui.ShowOriginal;

import java.awt.image.BufferedImage;

/**
 * Video Feedback filter based on the JHLabs FeedbackFilter
 */
public class JHVideoFeedback extends ParametrizedFilter {
    public static final String NAME = "Video Feedback";

    private final RangeParam iterations = new RangeParam("Iterations", 2, 3, 30);
    private final ImagePositionParam center = new ImagePositionParam("Center");
    private final RangeParam rotation = new RangeParam("Rotation (degrees/iteration)", -30, 0, 30);
    private final RangeParam zoom = new RangeParam("Zoom (percent/iteration)", -100, -10, -5);
    private final RangeParam startOpacity = new RangeParam("Start Opacity (%)", 0, 100, 100);
    private final RangeParam endOpacity = new RangeParam("End Opacity (%)", 0, 100, 100);

    private FeedbackFilter filter;

    public JHVideoFeedback() {
        super(ShowOriginal.YES);

        setParams(
                iterations,
                center,
                zoom,
                rotation,
                startOpacity,
                endOpacity
        );
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if ((rotation.getValue() == 0) && (zoom.getValue() == 0)) {
            return src;
        }

        if (filter == null) {
            filter = new FeedbackFilter(NAME);
        }

        filter.setRotation(rotation.getValueInRadians());
        filter.setZoom(zoom.getValueAsPercentage());
        filter.setCentreX(center.getRelativeX());
        filter.setCentreY(center.getRelativeY());
        filter.setIterations(iterations.getValue());
        filter.setStartAlpha(startOpacity.getValueAsPercentage());
        filter.setEndAlpha(endOpacity.getValueAsPercentage());

        dest = filter.filter(src, dest);
        return dest;
    }
}