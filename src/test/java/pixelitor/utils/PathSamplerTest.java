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

package pixelitor.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pixelitor.utils.PathSampler.Sample;

import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PathSampler tests")
class PathSamplerTest {
    private static final double EPS = 1e-9;

    private static Path2D polyline(double... xy) {
        Path2D path = new Path2D.Double();
        path.moveTo(xy[0], xy[1]);
        for (int i = 2; i < xy.length; i += 2) {
            path.lineTo(xy[i], xy[i + 1]);
        }
        return path;
    }

    private static void assertSample(double x, double y, double angle, Sample actual) {
        assertNotNull(actual);
        assertEquals(x, actual.x(), EPS, "x");
        assertEquals(y, actual.y(), EPS, "y");
        assertEquals(angle, actual.angle(), EPS, "angle");
    }

    @Test
    @DisplayName("a straight line")
    void straightLine() {
        var sampler = new PathSampler(polyline(10, 20, 110, 20));

        assertEquals(100, sampler.length(), EPS);
        assertFalse(sampler.isEmpty());
        assertSample(10, 20, 0, sampler.pointAndAngleAt(0));
        assertSample(35, 20, 0, sampler.pointAndAngleAt(25));
        assertSample(110, 20, 0, sampler.pointAndAngleAt(100));
    }

    @Test
    @DisplayName("distances outside the path have no sample")
    void outsideThePath() {
        var sampler = new PathSampler(polyline(0, 0, 100, 0));

        assertNull(sampler.pointAndAngleAt(-0.001));
        assertNull(sampler.pointAndAngleAt(100.001));
        assertNull(sampler.pointAndAngleAt(Double.NaN));
        assertNull(sampler.pointAndAngleAt(Double.POSITIVE_INFINITY));
        assertNull(sampler.pointAndAngleAt(Double.NEGATIVE_INFINITY));
    }

    @Test
    @DisplayName("the direction changes at the corners")
    void corners() {
        var sampler = new PathSampler(polyline(0, 0, 10, 0, 10, 10));

        assertEquals(20, sampler.length(), EPS);
        assertSample(10, 5, Math.PI / 2, sampler.pointAndAngleAt(15));
        // exactly at the junction, the earlier segment wins
        assertSample(10, 0, 0, sampler.pointAndAngleAt(10));
        assertSample(10, 0.5, Math.PI / 2, sampler.pointAndAngleAt(10.5));
    }

    @Test
    @DisplayName("a closed path includes its closing segment")
    void closedPath() {
        Path2D square = polyline(0, 0, 10, 0, 10, 10, 0, 10);
        square.closePath();
        var sampler = new PathSampler(square);

        assertEquals(40, sampler.length(), EPS);
        assertSample(0, 5, -Math.PI / 2, sampler.pointAndAngleAt(35));
        assertSample(0, 0, -Math.PI / 2, sampler.pointAndAngleAt(40));
    }

    @Test
    @DisplayName("open and closed paths with the same corners are different")
    void openIsNotClosed() {
        Path2D open = polyline(0, 0, 10, 0, 10, 10, 0, 10);
        Path2D closed = new Path2D.Double(open);
        closed.closePath();

        var openSampler = new PathSampler(open);
        var closedSampler = new PathSampler(closed);

        assertEquals(30, openSampler.length(), EPS);
        assertEquals(40, closedSampler.length(), EPS);
        assertNull(openSampler.offsetFrom(closedSampler, 1e-6));
        assertNull(closedSampler.offsetFrom(openSampler, 1e-6));
    }

    @Test
    @DisplayName("the jump between subpaths doesn't count")
    void subpaths() {
        Path2D path = new Path2D.Double();
        path.moveTo(0, 0);
        path.lineTo(10, 0);
        path.moveTo(100, 100);
        path.lineTo(110, 100);
        var sampler = new PathSampler(path);

        assertEquals(20, sampler.length(), EPS);
        assertSample(10, 0, 0, sampler.pointAndAngleAt(10));
        assertSample(100.5, 100, 0, sampler.pointAndAngleAt(10.5));
        assertSample(105, 100, 0, sampler.pointAndAngleAt(15));
    }

    @Test
    @DisplayName("zero-length segments are ignored")
    void degenerateSegments() {
        Path2D path = new Path2D.Double();
        path.moveTo(0, 0);
        path.lineTo(0, 0);
        path.lineTo(10, 0);
        path.lineTo(10, 0);
        path.moveTo(10, 0);
        path.moveTo(50, 50);
        path.lineTo(60, 50);
        var sampler = new PathSampler(path);

        assertEquals(20, sampler.length(), EPS);
        assertSample(0, 0, 0, sampler.pointAndAngleAt(0));
        assertSample(10, 0, 0, sampler.pointAndAngleAt(10));
        assertSample(55, 50, 0, sampler.pointAndAngleAt(15));
    }

    @Test
    @DisplayName("a path without length is empty")
    void emptyPaths() {
        var nothing = new PathSampler(new Path2D.Double());
        assertTrue(nothing.isEmpty());
        assertEquals(0, nothing.length());
        assertNull(nothing.pointAndAngleAt(0));

        Path2D onlyAMove = new Path2D.Double();
        onlyAMove.moveTo(5, 5);
        var point = new PathSampler(onlyAMove);
        assertTrue(point.isEmpty());
        assertNull(point.pointAndAngleAt(0));

        assertEquals(0, nothing.offsetFrom(point, 1e-6).getX());
    }

    @Test
    @DisplayName("segments with invalid coordinates are ignored")
    void nonFiniteCoordinates() {
        Path2D path = new Path2D.Double();
        path.moveTo(0, 0);
        path.lineTo(Double.NaN, 5);
        path.lineTo(Double.POSITIVE_INFINITY, 0);
        path.lineTo(10, 0);
        var sampler = new PathSampler(path);

        assertEquals(10, sampler.length(), EPS);
        assertSample(5, 0, 0, sampler.pointAndAngleAt(5));
    }

    @Test
    @DisplayName("curves are flattened, and every sample lies on the curve")
    void curve() {
        double radius = 50;
        var sampler = new PathSampler(new Ellipse2D.Double(0, 0, 2 * radius, 2 * radius));

        double expected = 2 * Math.PI * radius;
        // the polyline is slightly shorter than the circle
        assertTrue(sampler.length() < expected * 1.001);
        assertTrue(sampler.length() > expected * 0.98);

        int steps = 200;
        for (int i = 0; i <= steps; i++) {
            Sample s = sampler.pointAndAngleAt(sampler.length() * i / steps);
            assertNotNull(s);
            double distFromCenter = Point2D.distance(s.x(), s.y(), radius, radius);
            assertTrue(distFromCenter > radius - 1.1 && distFromCenter < radius + 0.1,
                "distance from the center: " + distFromCenter);
        }
    }

    @Test
    @DisplayName("offsetFrom recognizes a translated path")
    void translatedPath() {
        Path2D path = new Path2D.Double();
        path.moveTo(0, 0);
        path.curveTo(50, 100, 150, -100, 200, 0);
        path.lineTo(200, 100);

        var original = new PathSampler(path);
        Point2D same = new PathSampler(path).offsetFrom(original, 1e-6);
        assertNotNull(same);
        assertEquals(0, same.getX());
        assertEquals(0, same.getY());

        Path2D moved = new Path2D.Double(path);
        moved.transform(AffineTransform.getTranslateInstance(37.25, -1234.5));
        Point2D offset = new PathSampler(moved).offsetFrom(original, 1e-6);
        assertNotNull(offset);
        assertEquals(37.25, offset.getX(), 1e-9);
        assertEquals(-1234.5, offset.getY(), 1e-9);

        // the samples move together with the path
        Sample a = original.pointAndAngleAt(123);
        Sample b = new PathSampler(moved).pointAndAngleAt(123);
        assertEquals(a.x() + 37.25, b.x(), 1e-6);
        assertEquals(a.y() - 1234.5, b.y(), 1e-6);
        assertEquals(a.angle(), b.angle(), 1e-9);
    }

    @Test
    @DisplayName("offsetFrom rejects paths that are not translations")
    void changedPath() {
        var original = new PathSampler(polyline(0, 0, 100, 0, 100, 100));

        // one point moved
        assertNull(new PathSampler(polyline(0, 0, 100, 0, 100, 101)).offsetFrom(original, 1e-6));
        // an extra vertex
        assertNull(new PathSampler(polyline(0, 0, 100, 0, 100, 50, 100, 100)).offsetFrom(original, 1e-6));
        // rotated
        Path2D rotated = polyline(0, 0, 100, 0, 100, 100);
        rotated.transform(AffineTransform.getRotateInstance(0.1));
        assertNull(new PathSampler(rotated).offsetFrom(original, 1e-6));
        // scaled
        Path2D scaled = polyline(0, 0, 100, 0, 100, 100);
        scaled.transform(AffineTransform.getScaleInstance(1.0001, 1));
        assertNull(new PathSampler(scaled).offsetFrom(original, 1e-6));
        // empty vs. non-empty
        assertNull(new PathSampler(new Path2D.Double()).offsetFrom(original, 1e-6));
    }

    @Test
    @DisplayName("offsetFrom tolerates only the given amount of noise")
    void tolerance() {
        var original = new PathSampler(polyline(0, 0, 100, 0, 100, 100));
        var noisy = new PathSampler(polyline(1e-9, 0, 100, 1e-9, 100 - 1e-9, 100));

        assertNotNull(noisy.offsetFrom(original, 1e-6));
        assertNull(noisy.offsetFrom(original, 1e-12));
    }
}
