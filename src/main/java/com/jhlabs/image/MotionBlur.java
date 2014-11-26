/*
Copyright 2010-2014 Laszlo Balazs-Csiki

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.jhlabs.image;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImageOp;

/**
 * A common interface for different types of motion blur
 */
public interface MotionBlur extends BufferedImageOp {
    /**
     * Specifies the angle of blur.
     *
     * @param angle the angle of blur.
     * @angle
     * @see #getAngle
     */
    void setAngle(float angle);

    /**
     * Returns the angle of blur.
     *
     * @return the angle of blur.
     * @see #setAngle
     */
    float getAngle();

    /**
     * Set the distance of blur.
     *
     * @param distance the distance of blur.
     * @see #getDistance
     */
    void setDistance(float distance);

    /**
     * Get the distance of blur.
     *
     * @return the distance of blur.
     * @see #setDistance
     */
    float getDistance();

    /**
     * Set the blur rotation.
     *
     * @param rotation the angle of rotation.
     * @see #getRotation
     */
    void setRotation(float rotation);

    /**
     * Get the blur rotation.
     *
     * @return the angle of rotation.
     * @see #setRotation
     */
    float getRotation();

    /**
     * Set the blur zoom.
     *
     * @param zoom the zoom factor.
     * @see #getZoom
     */
    void setZoom(float zoom);

    /**
     * Get the blur zoom.
     *
     * @return the zoom factor.
     * @see #setZoom
     */
    float getZoom();

    /**
     * Set the centre of the effect in the X direction as a proportion of the image size.
     *
     * @param centreX the center
     * @see #getCentreX
     */
    void setCentreX(float centreX);

    /**
     * Get the centre of the effect in the X direction as a proportion of the image size.
     *
     * @return the center
     * @see #setCentreX
     */
    float getCentreX();

    /**
     * Set the centre of the effect in the Y direction as a proportion of the image size.
     *
     * @param centreY the center
     * @see #getCentreY
     */
    void setCentreY(float centreY);

    /**
     * Get the centre of the effect in the Y direction as a proportion of the image size.
     *
     * @return the center
     * @see #setCentreY
     */
    float getCentreY();

    /**
     * Set the centre of the effect as a proportion of the image size.
     *
     * @param centre the center
     * @see #getCentre
     */
    void setCentre(Point2D centre);

    /**
     * Get the centre of the effect as a proportion of the image size.
     *
     * @return the center
     * @see #setCentre
     */
    Point2D getCentre();
}
