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

package pixelitor.filters.impl;

import com.jhlabs.image.ImageMath;
import pixelitor.filters.GlassTiles;

import static net.jafama.FastMath.tan;

/**
 * The implementation of the {@link GlassTiles} filter.
 * <p>
 * Every mode partitions the plane into tiles with a periodic phase (tile centers
 * are at multiples of PI in the "angle" coordinates). Inside a tile, each face
 * contributes a displacement of tan(PI/2 * normalizedDistanceToFace), which is zero
 * at the tile center and blows up at the face, so each tile acts as a lens.
 * The per-mode methods differ only in how they find the tile and its faces.
 */
public class TilesFilter extends RotatingEffectFilter {
    public static final int MODE_SQUARES = 0;
    public static final int MODE_OCTAGONS_AND_SQUARES = 1;
    public static final int MODE_CUBES = 2;
    public static final int MODE_BRICK = 3;
    public static final int MODE_HERRINGBONE = 4;
    public static final int MODE_BASKET_WEAVE = 5;
    public static final int MODE_CHEVRON = 6;
    public static final int MODE_TRIANGLES = 7;
    public static final int MODE_HEXAGONS = 8;
    public static final int MODE_TRIHEXAGONAL = 9;
    public static final int MODE_CAIRO = 10;
    public static final int MODE_FISH_SCALES = 11;

    private static final double INV_PI = 1.0 / Math.PI;
    private static final double HALF_PI = Math.PI / 2.0;
    private static final double QUARTER_PI = Math.PI / 4.0;
    private static final double SIXTH_PI = Math.PI / 6.0;
    private static final double SQRT3 = Math.sqrt(3.0);
    private static final double SQRT3_OVER_2 = SQRT3 / 2.0;
    private static final double INV_SQRT3 = 1.0 / SQRT3;
    private static final double INV_SQRT2 = 1.0 / Math.sqrt(2.0);
    private static final double OCTAGON_CORNER_THRESHOLD = Math.PI * INV_SQRT2;
    private static final double SQUARE_SCALE = 1.0 + INV_SQRT2;

    // chevron geometry, in plank units: horizontal extent of one
    // slanted plank and the rise of its long faces per unit of run
    private static final double CHEVRON_ARM_LENGTH = 1.0;
    private static final double CHEVRON_SLOPE = 1.0;
    private static final double CHEVRON_NORM = 1.0 / Math.sqrt(1.0 + CHEVRON_SLOPE * CHEVRON_SLOPE);

    // trihexagonal geometry, in the isotropic frame where the hexagon apothem is PI/2:
    // the hexagons and the triangles share the same edge length
    private static final double TRIHEX_EDGE = Math.PI / Math.sqrt(3.0);
    private static final double TRIHEX_PITCH_X = 2.0 * TRIHEX_EDGE;
    private static final double TRIHEX_PITCH_Y = 2.0 * Math.PI;
    // triangle centroid offsets from the center of the neighboring hexagon
    private static final double TRIHEX_TRI_NEAR_Y = Math.PI / 3.0;
    private static final double TRIHEX_TRI_FAR_Y = 2.0 * Math.PI / 3.0;

    // Cairo geometry, in the isotropic frame where the lattice squares (see cairo())
    // have size PI. The pentagon has four long sides of length PI / sqrt3 and a
    // short base, with angles of 120 degrees at the apex and at the two base vertices,
    // and 90 degrees at the two remaining vertices.
    // In its canonical pose the base is at the bottom, the apex is at the top,
    // the axis of symmetry is the y axis, and the midpoint of the base is the origin.
    // The vertices are: base (+-CAIRO_HALF_BASE, 0), the 90 degree vertices
    // (+-PI/2, PI/2), and the apex (0, CAIRO_APEX_HEIGHT).
    private static final double CAIRO_HALF_BASE = HALF_PI * (1.0 - INV_SQRT3);
    private static final double CAIRO_APEX_HEIGHT = HALF_PI * (1.0 + INV_SQRT3);
    // the centroid of the pentagon, used as the optical center of the lens
    private static final double CAIRO_CENTER_Y = Math.PI * (5.0 + SQRT3) / 18.0;

    // The five faces in the order: base, right lower, right upper, left upper,
    // left lower. A point (px, py) is at the distance
    // CAIRO_OFFSET[i] - (CAIRO_NORMAL_X[i] * px + CAIRO_NORMAL_Y[i] * py)
    // inside the pentagon from the face i (the normals point outward).
    private static final double[] CAIRO_NORMAL_X = {0.0, SQRT3_OVER_2, 0.5, -0.5, -SQRT3_OVER_2};
    private static final double[] CAIRO_NORMAL_Y = {-1.0, -0.5, SQRT3_OVER_2, SQRT3_OVER_2, -0.5};
    private static final double[] CAIRO_OFFSET = {
        0.0,
        0.25 * Math.PI * (SQRT3 - 1.0),
        0.25 * Math.PI * (SQRT3 + 1.0),
        0.25 * Math.PI * (SQRT3 + 1.0),
        0.25 * Math.PI * (SQRT3 - 1.0)
    };
    // the distance of each face from its farthest vertex, that is,
    // the largest face distance that occurs inside the pentagon
    private static final double[] CAIRO_SPAN = {
        CAIRO_APEX_HEIGHT,
        SQRT3_OVER_2 * Math.PI,
        CAIRO_APEX_HEIGHT,
        CAIRO_APEX_HEIGHT,
        SQRT3_OVER_2 * Math.PI
    };
    // the per-face lens terms at the center, which are subtracted
    // so that the displacement is zero there
    private static final double[] CAIRO_CENTER_TAN = calcCairoCenterTan();

    // With integer tile sizes and an unrotated pattern, pixel columns/rows can land
    // exactly on a tile face, where tan() is ~1e16. That produces a garbage sample
    // in the interpolation. A small irrational-ish offset keeps the pixels off the faces.
    // TODO slanted edges are still noisy, this is probably an aliasing problem
    private static final double PIXEL_OFFSET = 0.3183;

    private final int mode;
    private final double freqX;
    private final double freqY;
    private final double curvatureX;
    private final double curvatureY;
    private final double curvatureAvg;
    private final double phaseOffsetX;
    private final double phaseOffsetY;

    /**
     * Constructs a new TilesFilter.
     *
     * @param filterName    the name of the filter.
     * @param edgeAction    the edge handling strategy (TRANSPARENT, REPEAT_EDGE, WRAP_AROUND, REFLECT).
     * @param interpolation the interpolation method (NEAREST_NEIGHBOR, BILINEAR, BICUBIC).
     * @param angle         the rotation angle of the tiles (in radians).
     * @param mode          the layout mode (one of the MODE* constants).
     * @param sizeX         the horizontal size of the tiles.
     * @param shiftX        the horizontal phase shift/movement of the tiles.
     * @param sizeY         the vertical size of the tiles.
     * @param shiftY        the vertical phase shift/movement of the tiles.
     * @param curvatureXVal the horizontal curvature intensity.
     * @param curvatureYVal the vertical curvature intensity.
     */
    public TilesFilter(String filterName, int edgeAction, int interpolation, double angle, int mode,
                       double sizeX, double shiftX,
                       double sizeY, double shiftY,
                       double curvatureXVal, double curvatureYVal) {
        super(filterName, edgeAction, interpolation, angle);

        assert sizeX > 0 && sizeY > 0;
        assert mode >= MODE_SQUARES && mode <= MODE_FISH_SCALES;

        this.mode = mode;

        // the triangles/hexagons should be equilateral/regular when sizeX = sizeY
        this.freqX = switch (mode) {
            case MODE_TRIANGLES -> (Math.PI * SQRT3_OVER_2) / sizeX;
            case MODE_HEXAGONS, MODE_TRIHEXAGONAL -> (Math.PI / SQRT3_OVER_2) / sizeX;
            default -> Math.PI / sizeX;
        };

        // the cubes use pointy-top hexagons, so here the rows are the compressed axis
        this.freqY = (mode == MODE_CUBES) ? (Math.PI / SQRT3_OVER_2) / sizeY : Math.PI / sizeY;

        this.phaseOffsetX = shiftX - PIXEL_OFFSET * this.freqX;
        this.phaseOffsetY = shiftY - PIXEL_OFFSET * this.freqY;

        this.curvatureX = (curvatureXVal * curvatureXVal) / 10.0;
        this.curvatureY = (curvatureYVal * curvatureYVal) / 10.0;
        this.curvatureAvg = 0.5 * (this.curvatureX + this.curvatureY);
    }

    @Override
    protected void coreTransformInverse(double x, double y, double[] out) {
        // the periodic tile phase (tile centers sit at multiples of π)
        double phaseX = x * freqX - phaseOffsetX;
        double phaseY = y * freqY - phaseOffsetY;

        switch (mode) {
            case MODE_SQUARES -> displace(x, y, tan(phaseX), tan(phaseY), out);
            case MODE_OCTAGONS_AND_SQUARES -> octagons(x, y, phaseX, phaseY, out);
            case MODE_CUBES -> cubes(x, y, phaseX, phaseY, out);
            case MODE_BRICK -> brick(x, y, phaseX, phaseY, out);
            case MODE_HERRINGBONE -> herringbone(x, y, phaseX, phaseY, out);
            case MODE_BASKET_WEAVE -> basketWeave(x, y, phaseX, phaseY, out);
            case MODE_CHEVRON -> chevron(x, y, phaseX, phaseY, out);
            case MODE_TRIANGLES -> triangles(x, y, phaseX, phaseY, out);
            case MODE_HEXAGONS -> hexagons(x, y, phaseX, phaseY, out);
            case MODE_TRIHEXAGONAL -> trihexagonal(x, y, phaseX, phaseY, out);
            case MODE_CAIRO -> cairo(x, y, phaseX, phaseY, out);
            case MODE_FISH_SCALES -> fishScales(x, y, phaseX, phaseY, out);
            default -> throw new IllegalStateException("Unexpected mode: " + mode);
        }
    }

    private void octagons(double x, double y, double phaseX, double phaseY, double[] out) {
        int col = nearestCell(phaseX);
        double localX = phaseX - col * Math.PI;

        int row = nearestCell(phaseY);
        double localY = phaseY - row * Math.PI;

        if (Math.abs(localX) + Math.abs(localY) > OCTAGON_CORNER_THRESHOLD) {
            // corner accent square (cabochon)
            double cX = (localX >= 0) ? HALF_PI : -HALF_PI;
            double cY = (localY >= 0) ? HALF_PI : -HALF_PI;

            double qX = localX - cX;
            double qY = localY - cY;

            double t1 = tanClamped((qX + qY) * SQUARE_SCALE);
            double t2 = tanClamped((qX - qY) * SQUARE_SCALE);

            double dx = INV_SQRT2 * (t1 + t2);
            double dy = INV_SQRT2 * (t1 - t2);

            out[0] = x + curvatureAvg * dx;
            out[1] = y + curvatureAvg * dy;
        } else {
            // octagon
            double t1 = tanClamped(localX);
            double t2 = tanClamped(localY);
            double t3 = tanClamped(INV_SQRT2 * (localX + localY));
            double t4 = tanClamped(INV_SQRT2 * (localX - localY));

            double dxDiag = INV_SQRT2 * (t3 + t4);
            double dyDiag = INV_SQRT2 * (t3 - t4);

            out[0] = x + curvatureX * t1 + curvatureAvg * dxDiag;
            out[1] = y + curvatureY * t2 + curvatureAvg * dyDiag;
        }
    }

    private static double normalizedScaleX(double phaseX, int row) {
        double offset = ((row & 1) != 0) ? HALF_PI : 0.0;
        double uShifted = phaseX - offset;
        int col = nearestCell(uShifted);
        double du = uShifted - col * Math.PI;
        return Math.clamp(du * (2.0 * INV_PI), -1.0, 1.0);
    }

    private void cubes(double x, double y, double phaseX, double phaseY, double[] out) {
        // pointy-top hexagon grid: odd rows are shifted horizontally by half a cell
        int row = nearestCell(phaseY);
        double localY = phaseY - row * Math.PI;

        double shiftedX = ((row & 1) != 0) ? phaseX - HALF_PI : phaseX;
        int col = nearestCell(shiftedX);
        double localX = shiftedX - col * Math.PI;

        // check if the point falls in a slanted corner belonging to an adjacent row
        if (Math.abs(localX) + 1.5 * Math.abs(localY) > Math.PI) {
            localX += (localX > 0) ? -HALF_PI : HALF_PI;
            localY += (localY > 0) ? -Math.PI : Math.PI;
        }

        // The hexagon is split into three rhombi by spokes going from the
        // center to the bottom vertex and to the two upper vertices.
        // Each rhombus has two pairs of parallel faces, and the normal
        // distances to them are in [-PI/2, PI/2] inside the rhombus.
        // (The clamping only protects against rounding errors at the borders.)
        // Face normals: nV = (1, 0), nR = (1/2, sqrt3/2), nL = (-1/2, sqrt3/2)
        double dx, dy;
        if (1.5 * localY < -Math.abs(localX)) {
            // top face, bounded by the faces with normals nR and nL
            double tR = tanClamped(localX + 1.5 * localY + HALF_PI);
            double tL = tanClamped(-localX + 1.5 * localY + HALF_PI);

            dx = 0.5 * (tR - tL);
            dy = SQRT3_OVER_2 * (tR + tL);
        } else if (localX < 0) {
            // left face, bounded by the faces with normals nL and nV
            double tL = tanClamped(-localX + 1.5 * localY - HALF_PI);
            double tV = tanClamped(2.0 * localX + HALF_PI);

            dx = tV - 0.5 * tL;
            dy = SQRT3_OVER_2 * tL;
        } else {
            // right face, bounded by the faces with normals nR and nV
            double tR = tanClamped(localX + 1.5 * localY - HALF_PI);
            double tV = tanClamped(2.0 * localX - HALF_PI);

            dx = tV + 0.5 * tR;
            dy = SQRT3_OVER_2 * tR;
        }

        displace(x, y, dx, dy, out);
    }

    private void basketWeave(double x, double y, double phaseX, double phaseY, double[] out) {
        // plank units: one unit is one tile size, cell edges are at integers
        double px = toCellUnits(phaseX);
        double py = toCellUnits(phaseY);

        // 2x2 blocks, each containing two 1x2 planks
        int bx = (int) Math.floor(px / 2.0);
        int by = (int) Math.floor(py / 2.0);
        double fx = px - 2.0 * bx;
        double fy = py - 2.0 * by;

        double nx, ny;
        if (((bx + by) & 1) == 0) {
            // two horizontal planks stacked vertically
            nx = fx - 1.0;                       // half-length is 1
            ny = 2.0 * (fy - Math.floor(fy)) - 1.0; // half-thickness is 0.5
        } else {
            // two vertical planks side by side
            nx = 2.0 * (fx - Math.floor(fx)) - 1.0;
            ny = fy - 1.0;
        }
        lens(x, y, nx, ny, out);
    }

    private void brick(double x, double y, double phaseX, double phaseY, double[] out) {
        int row = nearestCell(phaseY);
        if ((row & 1) != 0) {
            phaseX += HALF_PI;
        }
        displace(x, y, tan(phaseX), tan(phaseY), out);
    }

    private void herringbone(double x, double y, double phaseX, double phaseY, double[] out) {
        double px = toCellUnits(phaseX);
        double py = toCellUnits(phaseY);

        int i = (int) Math.floor(px);
        int j = (int) Math.floor(py);

        // The 1x2 herringbone is periodic in (i - j) mod 4, and each residue
        // is one half of a plank:
        // 0: left half of a horizontal plank, 1: its right half,
        // 2: upper half of a vertical plank,  3: its lower half
        int d = Math.floorMod(i - j, 4);
        boolean horizontal = d < 2;

        // plank center
        double cx = switch (d) {
            case 0 -> i + 1.0;
            case 1 -> i;
            default -> i + 0.5;
        };
        double cy = switch (d) {
            case 0, 1 -> j + 0.5;
            case 2 -> j;
            default -> j + 1.0;
        };

        // normalize by the half-extents: (1, 0.5) or (0.5, 1)
        double nx = horizontal ? px - cx : 2.0 * (px - cx);
        double ny = horizontal ? 2.0 * (py - cy) : py - cy;
        lens(x, y, nx, ny, out);
    }

    private void chevron(double x, double y, double phaseX, double phaseY, double[] out) {
        double px = toCellUnits(phaseX);
        double py = toCellUnits(phaseY);

        // vertical seams every CHEVRON_ARM_LENGTH; neighboring columns lean opposite ways
        int col = (int) Math.floor(px / CHEVRON_ARM_LENGTH);
        double lx = px - col * CHEVRON_ARM_LENGTH; // in [0, L)
        boolean even = (col & 1) == 0;

        // the band coordinate w is constant along a plank's long faces
        double tri = even ? lx : CHEVRON_ARM_LENGTH - lx;
        double w = py - CHEVRON_SLOPE * tri;
        double fw = w - Math.floor(w); // position across the plank, in [0, 1)

        // Face normals: n1 = (1, 0) for the vertical seams,
        // n2 = (-+slope, 1) / sqrt(1 + slope^2) for the slanted long faces
        double n2x = (even ? -CHEVRON_SLOPE : CHEVRON_SLOPE) * CHEVRON_NORM;
        double n2y = CHEVRON_NORM;

        double t1 = tanNormalized(2.0 * lx / CHEVRON_ARM_LENGTH - 1.0);
        double t2 = tanNormalized(2.0 * fw - 1.0);

        displace(x, y, t1 + t2 * n2x, t2 * n2y, out);
    }

    private void triangles(double x, double y, double phaseX, double phaseY, double[] out) {
        double halfY = 0.5 * phaseY;
        double t1 = tan(phaseY);
        double t2 = tan(phaseX - halfY - QUARTER_PI);
        double t3 = tan(phaseX + halfY + QUARTER_PI);

        double dx = SQRT3_OVER_2 * (t2 + t3);
        double dy = t1 + 0.5 * (t3 - t2);

        displace(x, y, dx, dy, out);
    }

    private void hexagons(double x, double y, double phaseX, double phaseY, double[] out) {
        int col = nearestCell(phaseX);
        double localX = phaseX - col * Math.PI;

        double shiftedY = ((col & 1) != 0) ? phaseY - HALF_PI : phaseY;
        int row = nearestCell(shiftedY);
        double localY = shiftedY - row * Math.PI;

        // check if the point falls in a slanted corner belonging to an adjacent column
        if (Math.abs(localY) + 1.5 * Math.abs(localX) > Math.PI) {
            localX += (localX > 0) ? -Math.PI : Math.PI;
            localY += (localY > 0) ? -HALF_PI : HALF_PI;
        }

        // normal distances to the 3 pairs of opposite faces
        double u1 = localY;
        double u2 = 0.5 * (localY - 1.5 * localX);
        double u3 = 0.5 * (localY + 1.5 * localX);

        hexagonLens(x, y, u1, u2, u3, out);
    }

    /**
     * Trihexagonal (kagome) tiling: regular hexagons and equilateral triangles
     * with the same edge length, so every edge separates a hexagon from a triangle.
     * The tile size is the flat-to-flat size of the hexagons.
     */
    private void trihexagonal(double x, double y, double phaseX, double phaseY, double[] out) {
        // isotropic coordinates: the hexagon apothem is PI/2, the edge is TRIHEX_EDGE
        double px = SQRT3_OVER_2 * phaseX;
        double py = phaseY;

        // The hexagons touch each other only at their vertices. Their centers form
        // a triangular lattice with spacing 2 * edge, which is the union of two
        // rectangular lattices: one at the origin, and one shifted by (edge, PI).
        // Find the nearest center in each, then pick the closer of the two.
        double ax = wrapToNearest(px, TRIHEX_PITCH_X);
        double ay = wrapToNearest(py, TRIHEX_PITCH_Y);
        double bx = wrapToNearest(px - TRIHEX_EDGE, TRIHEX_PITCH_X);
        double by = wrapToNearest(py - Math.PI, TRIHEX_PITCH_Y);

        // position relative to the nearest hexagon center
        double localX, localY;
        if (ax * ax + ay * ay <= bx * bx + by * by) {
            localX = ax;
            localY = ay;
        } else {
            localX = bx;
            localY = by;
        }

        // normal distances to the 3 pairs of opposite hexagon faces
        // (normals (0, 1), (-sqrt3/2, 1/2), (sqrt3/2, 1/2), as in hexagons())
        double u1 = localY;
        double u2 = 0.5 * localY - SQRT3_OVER_2 * localX;
        double u3 = 0.5 * localY + SQRT3_OVER_2 * localX;

        if (Math.abs(u1) <= HALF_PI && Math.abs(u2) <= HALF_PI && Math.abs(u3) <= HALF_PI) {
            hexagonLens(x, y, u1, u2, u3, out);
            return;
        }

        // Outside the hexagon, but inside its Voronoi cell, the point belongs to
        // one of the six triangles around it. The sector (bounded by the
        // spokes toward the hexagon vertices) is given by the signs of u1..u3,
        // and it determines the triangle's centroid and orientation.
        double cx, cy;
        boolean up; // apex pointing up
        if (u1 >= 0) {
            if (u2 < 0) {
                cx = TRIHEX_EDGE;
                cy = TRIHEX_TRI_NEAR_Y;
                up = false;
            } else if (u3 < 0) {
                cx = -TRIHEX_EDGE;
                cy = TRIHEX_TRI_NEAR_Y;
                up = false;
            } else {
                cx = 0.0;
                cy = TRIHEX_TRI_FAR_Y;
                up = true;
            }
        } else {
            if (u2 > 0) {
                cx = -TRIHEX_EDGE;
                cy = -TRIHEX_TRI_NEAR_Y;
                up = true;
            } else if (u3 > 0) {
                cx = TRIHEX_EDGE;
                cy = -TRIHEX_TRI_NEAR_Y;
                up = true;
            } else {
                cx = 0.0;
                cy = -TRIHEX_TRI_FAR_Y;
                up = false;
            }
        }

        // A down-pointing triangle is an up-pointing one mirrored through
        // its centroid, so reflect the local coordinates and the result.
        double s = up ? 1.0 : -1.0;
        double lx = s * (localX - cx);
        double ly = s * (localY - cy);

        // Outward face normals of the up triangle: (0, -1), (sqrt3/2, 1/2), (-sqrt3/2, 1/2).
        // The face distance h ranges from the inradius PI/6 (on the face) to
        // -PI/3 (at the opposite vertex). It is mapped linearly to w in
        // [PI/2, -PI/2], with w = PI/6 at the centroid (the same mapping that
        // the Triangles mode uses), so the displacement is zero at the centroid
        // and blows up outward at all three faces.
        double w1 = SIXTH_PI - 2.0 * ly;
        double w2 = SIXTH_PI + 2.0 * (SQRT3_OVER_2 * lx + 0.5 * ly);
        double w3 = SIXTH_PI + 2.0 * (0.5 * ly - SQRT3_OVER_2 * lx);

        double t1 = tanClamped(w1);
        double t2 = tanClamped(w2);
        double t3 = tanClamped(w3);

        double dx = s * SQRT3_OVER_2 * (t2 - t3);
        double dy = s * (0.5 * (t2 + t3) - t1);

        displace(x, y, dx, dy, out);
    }

    /**
     * Offset of v from the nearest multiple of pitch.
     */
    private static double wrapToNearest(double v, double pitch) {
        return v - pitch * Math.floor(v / pitch + 0.5);
    }

    /**
     * Cairo pentagonal tiling: congruent pentagons in four orientations. The tile
     * size is the width of a pentagon, which is the distance between two neighboring
     * four-way vertices along an axis.
     * <p>
     * Construction: the plane is divided into a checkerboard of squares of size PI.
     * Every square contains an "H" made of a short base segment through its center
     * and four spokes from the ends of the base to the four corners. The base is
     * horizontal in the even squares and vertical in the odd ones. The H splits its
     * square into two trapezoids (touching the sides that are parallel to the
     * base) and two triangles (touching the other two sides). Since the squares
     * next to each other have perpendicular bases, a trapezoid and the triangle
     * across its outer side together form one pentagon.
     * <p>
     * The pentagon is a rotated copy of the canonical pose (see the constants),
     * where a lens like in the Hexagons mode is defined: with the outward face
     * normal n_i and the normalized inward distance d_i / span_i of the point from
     * the face i, the displacement is sum(n_i * (tan(PI/2 * (1 - d_i / span_i)) - c_i)),
     * where c_i is the same term at the center of the pentagon.
     * Each term blows up outward at its own face, and the constants make the
     * displacement zero at the center. Unlike in the modes with pairs of parallel
     * faces, the terms don't need a sign flip inside, because 1 - d_i / span_i
     * stays in [0, 1] in the entire pentagon.
     */
    private void cairo(double x, double y, double phaseX, double phaseY, double[] out) {
        // the squares are centered at the integer multiples of PI
        int col = nearestCell(phaseX);
        int row = nearestCell(phaseY);
        double u = phaseX - col * Math.PI;
        double v = phaseY - row * Math.PI;

        // "along" runs parallel to the base of the H in this square
        boolean horizontal = ((col + row) & 1) == 0;
        double along = horizontal ? u : v;
        double across = horizontal ? v : u;

        // upX, upY: the axis of the pentagon (from its base to its apex),
        // relX, relY: the position relative to the middle of its base
        int upX, upY;
        double relX = u;
        double relY = v;
        if (Math.abs(along) > CAIRO_HALF_BASE + Math.abs(across) * INV_SQRT3) {
            // a triangle: the apex of a pentagon whose base is in the next square
            int side = (along >= 0) ? 1 : -1;
            if (horizontal) {
                upX = -side;
                upY = 0;
            } else {
                upX = 0;
                upY = -side;
            }
            // move the origin to the center of the next square
            relX += Math.PI * upX;
            relY += Math.PI * upY;
        } else {
            // a trapezoid: the base part of a pentagon that has its base here
            int side = (across >= 0) ? 1 : -1;
            if (horizontal) {
                upX = 0;
                upY = side;
            } else {
                upX = side;
                upY = 0;
            }
        }

        // rotate to the canonical pose, where the pentagon points up:
        // the x-axis is (upY, -upX), the y-axis is (upX, upY)
        double px = relX * upY - relY * upX;
        double py = relX * upX + relY * upY;

        double dx = 0.0;
        double dy = 0.0;
        for (int i = 0; i < 5; i++) {
            double nx = CAIRO_NORMAL_X[i];
            double ny = CAIRO_NORMAL_Y[i];
            double faceDist = CAIRO_OFFSET[i] - (nx * px + ny * py);

            // PI/2 on the face, decreasing inwards (clamping only protects
            // against rounding errors at the borders)
            double w = Math.clamp(HALF_PI * (1.0 - faceDist / CAIRO_SPAN[i]), 0.0, HALF_PI);
            double t = tan(w) - CAIRO_CENTER_TAN[i];
            dx += nx * t;
            dy += ny * t;
        }

        // rotate the displacement back
        displace(x, y, dx * upY + dy * upX, dy * upY - dx * upX, out);
    }

    private void fishScales(double x, double y, double phaseX, double phaseY, double[] out) {
        // find candidate row r such that phaseY is in [r * PI, (r + 1) * PI)
        double yNorm = phaseY * INV_PI;
        int r = (int) Math.floor(yNorm);
        double nxR = normalizedScaleX(phaseX, r);

        // bottom boundary curve of row r: an ellipse reaching depth 1.0 in normalized units
        double normBound = r + Math.sqrt(Math.max(0.0, 1.0 - nxR * nxR));

        int row;
        double nx;
        double ny;

        if (yNorm < normBound) {
            // point is above the row's bottom boundary arc: belongs to row r
            row = r;
            nx = nxR;
            ny = Math.clamp(yNorm - row, 0.0, 1.0);
        } else {
            // point is below the arc: belongs to the staggered tile in row r + 1
            row = r + 1;
            nx = normalizedScaleX(phaseX, row);
            ny = Math.clamp(yNorm - row, -1.0, 0.0);
        }

        // in normalized tile coordinates (nx, ny) in [-1, 1] x [-1, 1],
        // the tile is bounded by the bottom convex arc and the top concave arcs
        double rLen = ImageMath.hypot(nx, ny);
        double dnorm;
        if (ny >= 0.0) {
            // lower half: distance to the elliptical arc normal is simply rLen
            dnorm = rLen;
        } else {
            // upper half: exact closed-form radial distance to the overlapping arcs of the row above
            double absNx = Math.abs(nx);
            dnorm = (absNx - ny) + Math.sqrt(Math.max(0.0, -2.0 * absNx * ny));
        }

        double dx = 0.0;
        double dy = 0.0;
        if (rLen > 1e-9) {
            double angle = Math.min(dnorm, 0.995) * HALF_PI;
            double mag = tan(angle);
            double invR = 1.0 / rLen;
            dx = (nx * invR) * mag;
            dy = (ny * invR) * mag;
        }

        displace(x, y, dx, dy, out);
    }

    /**
     * The index of the tile whose center (a multiple of PI) is nearest to the angle.
     */
    private static int nearestCell(double angle) {
        return (int) Math.floor(toCellUnits(angle));
    }

    /**
     * Converts an angle to cell units: cell edges are at integers and
     * cell centers (multiples of PI in angle units) at half-integers.
     */
    private static double toCellUnits(double angle) {
        return angle * INV_PI + 0.5;
    }

    private void hexagonLens(double x, double y, double u1, double u2, double u3, double[] out) {
        double t1 = tanClamped(u1);
        double t2 = tanClamped(u2);
        double t3 = tanClamped(u3);
        displace(x, y, SQRT3_OVER_2 * (t3 - t2), t1 + 0.5 * (t2 + t3), out);
    }

    /**
     * Lens for an axis-aligned rectangular tile. The normalized coordinates
     * are in [-1, 1], where +-1 means "on the face".
     */
    private void lens(double x, double y, double nx, double ny, double[] out) {
        displace(x, y, tanNormalized(nx), tanNormalized(ny), out);
    }

    /**
     * tan(n * PI/2) for a normalized coordinate n, where +-1 is on the face.
     */
    private static double tanNormalized(double n) {
        return tanClamped(n * HALF_PI);
    }

    /**
     * tan() with the argument clamped to [-PI/2, PI/2], so that rounding errors
     * near a tile border can't push it past the asymptote and flip the sign.
     */
    private static double tanClamped(double v) {
        return tan(Math.clamp(v, -HALF_PI, HALF_PI));
    }

    private void displace(double x, double y, double dx, double dy, double[] out) {
        out[0] = x + curvatureX * dx;
        out[1] = y + curvatureY * dy;
    }

    private static double[] calcCairoCenterTan() {
        double[] result = new double[CAIRO_OFFSET.length];
        for (int i = 0; i < result.length; i++) {
            double faceDist = CAIRO_OFFSET[i] - CAIRO_NORMAL_Y[i] * CAIRO_CENTER_Y;
            result[i] = tan(HALF_PI * (1.0 - faceDist / CAIRO_SPAN[i]));
        }
        return result;
    }
}
