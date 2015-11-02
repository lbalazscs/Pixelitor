package pixelitor.utils;

import net.jafama.FastMath;

/**
 * Calculates whether a point is inside or outside a blurred ellipse.
 * It assumes that the outer/inner radius ratios are the same
 * for the x and y radii.
 */
public class BlurredEllipse {
    private final double cx;
    private final double cy;

    private final boolean linkedRadius;
    private final double innerRadiusY;
    private double innerRadius2;
    private double innerRadiusX2;
    private double innerRadiusY2;

    private final double outerRadiusX;
    private final double outerRadiusY;
    private double outerRadius2;
    private double outerRadiusX2;
    private double outerRadiusY2;

    private final double yRadiusDifference;

    public BlurredEllipse(double centerX, double centerY, double innerRadiusX, double innerRadiusY, double outerRadiusX, double outerRadiusY) {
        this.cx = centerX;
        this.cy = centerY;
        this.innerRadiusY = innerRadiusY;
        this.outerRadiusX = outerRadiusX;
        this.outerRadiusY = outerRadiusY;

        linkedRadius = innerRadiusX == innerRadiusY;

        if (linkedRadius) {
            innerRadius2 = innerRadiusX * innerRadiusX;
            outerRadius2 = outerRadiusX * outerRadiusX;
        } else {
            innerRadiusX2 = innerRadiusX * innerRadiusX;
            innerRadiusY2 = innerRadiusY * innerRadiusY;

            outerRadiusX2 = outerRadiusX * outerRadiusX;
            outerRadiusY2 = outerRadiusY * outerRadiusY;
        }

        yRadiusDifference = outerRadiusY - innerRadiusY;
    }

    /**
     * Returns 1.0 if the given coordinate is completely inside the ellipse,
     * 0.0 if it is completely outside, and
     * a number between 0.0 and 1.0 if it is in the blurred area
     */
    public double isOutside(int x, int y) {
        double dx = x - cx;
        double dy = y - cy;
        double dist2 = dx * dx + dy * dy;

        if (linkedRadius) { // circle
            if (dist2 > outerRadius2) { // outside
                return 1.0;
            } else if (dist2 < innerRadius2) { // innermost region
                return 0.0;
            } else { // between the inner and outer radius
                double distance = Math.sqrt(dist2);
                double ratio = (distance - innerRadiusY) / yRadiusDifference; // gives a value between 0 and 1

//                double trigRatio = (FastMath.cos(ratio * Math.PI) + 1.0) / 2.0;
                // 1- smooth step is faster than cosine interpolation
                // http://en.wikipedia.org/wiki/Smoothstep
                // http://www.wolframalpha.com/input/?i=Plot[{%281+%2B+Cos[a+*+Pi]%29%2F2%2C+1+-+3+*+a+*+a+%2B+2+*+a+*+a+*a}%2C+{a%2C+0%2C+1}]
                double trigRatio = 1 + ratio * ratio * (2 * ratio - 3);
                return 1.0 - trigRatio;
            }
        } else { // ellipsis
            double dx2 = dx * dx;
            double dy2 = dy * dy;

            if (dy2 >= (outerRadiusY2 - (outerRadiusY2 * dx2) / outerRadiusX2)) {  // outside
                return 1.0;
            }
            if (dy2 <= (innerRadiusY2 - (innerRadiusY2 * dx2) / innerRadiusX2)) { // innermost region
                return 0.0;
            } else { // between the inner and outer radius
                // we are on an ellipse with unknown a and b semi major/minor axes
                // but we know that a/b = outerRadiusX/outerRadiusY
                double ellipseDistortion = outerRadiusX / outerRadiusY;
                double b = Math.sqrt(ellipseDistortion * ellipseDistortion * dy2 + dx2) / ellipseDistortion;
                // now we can calculate how far away we are between the two ellipses
                double ratio = (b - innerRadiusY) / yRadiusDifference; // gives a value between 0 and 1
                double trigRatio = (FastMath.cos(ratio * Math.PI) + 1.0) / 2.0;

                return 1.0 - trigRatio;
            }
        }
    }
}
