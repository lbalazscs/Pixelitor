/*
 * Copyright 2010-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.filters;

import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.ColorParam;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.utils.ImageUtils;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

/**
 * Starburst
 */
public class Starburst extends FilterWithParametrizedGUI {
    private final RangeParam numberOfRaysParam = new RangeParam("Number of Rays", 2, 100, 10);
    private final ImagePositionParam centerParam = new ImagePositionParam("Center");
    private final ColorParam bgColorParam = new ColorParam("Backround Color:", Color.WHITE, false, false);
    private final ColorParam fgColorParam = new ColorParam("Rays Color:", Color.BLACK, false, false);
    private final BooleanParam randomColorsParam = new BooleanParam("Use Random Colors for Rays", false, true);

    public Starburst() {
        super("Starburst ", true, false);
        setParamSet(new ParamSet(
                numberOfRaysParam,
                bgColorParam,
                fgColorParam,
                randomColorsParam,
                centerParam
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {

        int width = dest.getWidth();
        int height = dest.getHeight();

        Graphics2D g = dest.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(bgColorParam.getColor());
        g.fillRect(0, 0, width, height);

        float cx = width * centerParam.getRelativeX();
        float cy = height * centerParam.getRelativeY();

        g.setColor(fgColorParam.getColor());

        int numberOfRays = numberOfRaysParam.getValue();
        boolean useRandomColors = randomColorsParam.getValue();

        double averageRayAngle = 180.0 / numberOfRays;
        double angleBetweenRays = 360.0 / numberOfRays;

//        System.out.println("Starburst.transform numberOfRays = " + numberOfRays);
//        System.out.println("Starburst.transform averageRayAngle = " + averageRayAngle);
//        System.out.println("Starburst.transform angleBetweenRays = " + angleBetweenRays);

        double startAngle = 0;


        for (int i = 0; i < numberOfRays; i++) {
//            System.out.println("Starburst.transform startAngle = " + startAngle);

            Point2D.Float p1 = intersectRectangleWithRay(cx, cy, width, height, startAngle);
            Point2D.Float p2 = intersectRectangleWithRay(cx, cy, width, height, startAngle + averageRayAngle);

            if (p1 != null && p2 != null) {
//                System.out.println("    Starburst.transform p1.x = " + p1.getX() + ", p1.y = " + p1.getY());
//                System.out.println("    Starburst.transform p2.x = " + p2.getX() + ", p2.y = " + p2.getY());

                GeneralPath shape = new GeneralPath();
                shape.moveTo(cx, cy);

                double p1x = p1.getX();
                double p1y = p1.getY();
                double p2x = p2.getX();
                double p2y = p2.getY();

                shape.lineTo(p1x, p1y);

                if ((p1x != p2x) && (p1y != p2y)) {   // corner case: not a triangle
                    int cornerX, cornerY;
                    if (p1x == width || p2x == width) {
                        cornerX = width;
                    } else if (p1x == 0 || p2x == 0) {
                        cornerX = 0;
                    } else {
                        throw new IllegalStateException();
                    }

                    if (p1y == height || p2y == height) {
                        cornerY = height;
                    } else if (p1y == 0 || p2y == 0) {
                        cornerY = 0;
                    } else {
                        throw new IllegalStateException();
                    }

                    shape.lineTo(cornerX, cornerY);
                }

                shape.lineTo(p2x, p2y);
                shape.closePath();

                if (useRandomColors) {
                    g.setColor(ImageUtils.getRandomColor(false));
                }

                g.fill(shape);
            } else {
//                    System.out.println("    Starburst.transform NO TRIANGLE");
            }

            startAngle += angleBetweenRays;
        }

        g.dispose();

        return dest;
    }

    private static Point2D.Float intersectRectangleWithRay(float cx, float cy, int width, int height, double thetaDegrees) {
        double thetaRadians = Math.toRadians(thetaDegrees);
//        System.out.println("Starburst.intersectRectangleWithRay thetaDegrees = " + thetaDegrees + ", thetaRadians = " + thetaRadians);

        if (thetaDegrees == 0) {
            if (cy >= 0 && cy <= height) {
//                System.out.println("Starburst.intersectRectangleWithRay EAST SIMPLE FOUND");
                return new Point2D.Float(width, cy);
            } else {
                return null;
            }
        } else if (thetaDegrees == 180) {
            if (cy >= 0 && cy <= height) {
//                System.out.println("Starburst.intersectRectangleWithRay WEST SIMPLE FOUND");
                return new Point2D.Float(0, cy);
            } else {
                return null;
            }
        }


        if (thetaDegrees > 0 && thetaDegrees < 180) {
            // check north
            int ix = (int) (cx + (cy / Math.tan(thetaRadians)));
            if (ix >= 0 && ix <= width) {
//                System.out.println("Starburst.intersectRectangleWithRay NORTH FOUND ix = " + ix);
                return new Point2D.Float(ix, 0);
            } else {
//                System.out.println("Failed NORTH CHECK ix = " + ix);
            }
        } else {
            // check south
//            int ix = (int) (cx + ((height - cy) / Math.tan(thetaRadians + Math.PI)));
            int ix = (int) (cx - ((height - cy) / Math.tan(thetaRadians)));
            if (ix >= 0 && ix <= width) {
//                System.out.println("Starburst.intersectRectangleWithRay SOUTH FOUND ix = " + ix);
                return new Point2D.Float(ix, height);
            }
        }

        if ((thetaDegrees >= 0 && thetaDegrees < 90) || (thetaDegrees > 270 && thetaDegrees <= 360)) {
            // check east
            int iy = (int) (cy - Math.tan(thetaRadians) * (width - cx));
            if (iy >= 0 && iy <= height) {
//                System.out.println("Starburst.intersectRectangleWithRay EAST FOUND iy = " + iy);
                return new Point2D.Float(width, iy);
            }
        }

        if (thetaDegrees > 90 && thetaDegrees < 270) {
            // check west
            int iy = (int) (cy + Math.tan(thetaRadians + +Math.PI) * cx);
            if (iy >= 0 && iy <= height) {
//                System.out.println("Starburst.intersectRectangleWithRay WEST FOUND iy = " + iy);
                return new Point2D.Float(0, iy);
            }
        }


//        System.out.println("  Starburst.intersectRectangleWithRay NO INTERSECTION FOUND");
        // no intersection found
        return null;
    }
}