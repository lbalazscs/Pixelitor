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

package com.jhlabs.image;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImageOp;

/**
 * A common interface for different types of motion blur
 */
public interface MotionBlur extends BufferedImageOp {
    /**
     * Sets the angle of blur.
     *
     * @param angle the angle of blur.
     * @angle
     */
    void setAngle(float angle);

    /**
     * Set the distance of blur.
     *
     * @param distance the distance of blur.
     */
    void setDistance(float distance);

    /**
     * Set the blur rotation.
     *
     * @param rotation the angle of rotation.
     */
    void setRotation(float rotation);

    /**
     * Set the blur zoom.
     *
     * @param zoom the zoom factor.
     */
    void setZoom(float zoom);

    /**
     * Set the center of the effect in the X direction as a proportion of the image size.
     *
     * @param centerX the center
     */
    void setCenterX(float centerX);

    /**
     * Set the center of the effect in the Y direction as a proportion of the image size.
     *
     * @param centerY the center
     */
    void setCenterY(float centerY);

    /**
     * Set the center of the effect as a proportion of the image size.
     *
     * @param center the center
     */
    void setCenter(Point2D center);
}
