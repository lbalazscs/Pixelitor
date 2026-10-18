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

package pixelitor.autopaint;

import com.jhlabs.math.Noise;
import net.jafama.FastMath;
import pixelitor.Canvas;

import java.awt.geom.Point2D;
import java.util.random.RandomGenerator;

/**
 * Determines how the angle of each auto-painted stroke is generated.
 */
public enum StrokeDirection {
    RANDOM("Random"), // uniform randomness
    RADIAL("Radial"), // points away from the canvas center
    CIRCULAR("Circular"), // tangent to the circle around the center
    NOISE("Noise"); // follows a smooth noise field

    // number of noise lattice cells along the canvas height
    private static final float NOISE_SCALE = 1.5f;

    private final String displayName;

    StrokeDirection(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    double calcAngle(Point2D start, Canvas canvas, RandomGenerator rng) {
        return switch (this) {
            case RANDOM -> rng.nextDouble() * Math.TAU;
            case RADIAL -> calcRadialAngle(start, canvas);
            case CIRCULAR -> calcRadialAngle(start, canvas) + Math.PI / 2.0;
            case NOISE -> {
                float scale = NOISE_SCALE / canvas.getHeight();
                float nx = (float) (start.getX() * scale);
                float ny = (float) (start.getY() * scale);

                // noise2 returns values in roughly [-1, 1], but they
                // cluster near 0, so multiplying only by π would result
                // in mostly rightward flow => 2π spreads the directions
                yield Noise.noise2(nx, ny) * Math.TAU;
            }
        };
    }

    /**
     * Calculates the angle from the canvas center to the given image-space point.
     */
    private static double calcRadialAngle(Point2D point, Canvas canvas) {
        Point2D center = canvas.getImCenter();
        return FastMath.atan2(point.getY() - center.getY(),
            point.getX() - center.getX());
    }
}
