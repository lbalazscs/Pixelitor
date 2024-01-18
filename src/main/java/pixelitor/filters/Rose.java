/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.filters;

import pixelitor.colors.Colors;
import pixelitor.filters.gui.*;
import pixelitor.utils.ImageUtils;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.Serial;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static pixelitor.filters.gui.TransparencyPolicy.USER_ONLY_TRANSPARENCY;

/**
 * Rose curve filter
 */
public class Rose extends ParametrizedFilter {
    public static final String NAME = "Rose";

    @Serial
    private static final long serialVersionUID = 1L;

    private final RangeParam nParam = new RangeParam("n", 1, 4, 20);
    private final RangeParam dParam = new RangeParam("d", 1, 7, 20);
    private final ImagePositionParam center = new ImagePositionParam("Center");
    private final ColorParam bgColor = new ColorParam("Background Color", BLACK, USER_ONLY_TRANSPARENCY);
    private final ColorParam fgColor = new ColorParam("Foreground Color", WHITE, USER_ONLY_TRANSPARENCY);
    private final GroupedRangeParam scale = new GroupedRangeParam("Scale (%)", 1, 100, 500);
    private final AngleParam rotate = new AngleParam("Rotate", 0);

    public Rose() {
        super(false);

        setParams(
            nParam,
            dParam,
            bgColor,
            fgColor,
            center,
            scale,
            rotate
        );

        helpURL = "https://en.wikipedia.org/wiki/Rose_(mathematics)";
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        dest = ImageUtils.copyImage(src);

        int width = dest.getWidth();
        int height = dest.getHeight();

        Graphics2D g = dest.createGraphics();
        Colors.fillWith(bgColor.getColor(), g, width, height);
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        g.setColor(fgColor.getColor());

        Path2D path = new Path2D.Double(Path2D.WIND_EVEN_ODD);
        boolean firstPoint = true;
        double radius = Math.min(width, height) / 2.0;
        double cx = width * center.getRelativeX();
        double cy = height * center.getRelativeY();

        int n = nParam.getValue();
        int d = dParam.getValue();
        double k = n / (double) d;

        double maxAngle = calcMaxAngle(n, d);
        for (double a = 0; a < maxAngle; a += 0.01) {
            double cosKA = Math.cos(k * a);
            double x = cx + radius * cosKA * Math.cos(a);
            double y = cy + radius * cosKA * Math.sin(a);

            if (firstPoint) {
                path.moveTo(x, y);
                firstPoint = false;
            } else {
                path.lineTo(x, y);
            }
        }
        path.closePath();

        int scaleX = scale.getValue(0);
        int scaleY = scale.getValue(1);
        Shape shape;
        if (scaleX != 100 || scaleY != 100) {
            AffineTransform at = AffineTransform.getTranslateInstance(cx, cy);
            at.scale(scaleX / 100.0, scaleY / 100.0);
            at.translate(-cx, -cy);
            shape = at.createTransformedShape(path);
        } else {
            shape = path;
        }

        double theta = rotate.getValueInRadians();
        if (theta != 0) {
            shape = AffineTransform.getRotateInstance(theta, cx, cy).createTransformedShape(shape);
        }

        g.fill(shape);

        g.dispose();
        return dest;
    }

    private static double calcMaxAngle(int n, int d) {
        boolean nIsEven = n % 2 == 0;
        boolean dIsEven = d % 2 == 0;
        if (nIsEven) {
            if (dIsEven) {
                //noinspection TailRecursion
                return calcMaxAngle(n / 2, d / 2);
            } else {
                return 2 * d * Math.PI;
            }
        } else {
            if (dIsEven) {
                return 2 * d * Math.PI;
            } else {
                return d * Math.PI;
            }
        }
    }

    @Override
    protected boolean createDefaultDestImg() {
        return false;
    }
}