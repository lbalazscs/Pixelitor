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

package pixelitor.filters;

import pixelitor.Canvas;
import pixelitor.Views;
import pixelitor.colors.Colors;
import pixelitor.filters.gui.ColorParam;
import pixelitor.filters.gui.FilterButtonModel;
import pixelitor.filters.gui.Help;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.util.ShapeWithColor;
import pixelitor.io.FileIO;
import pixelitor.utils.Distortion;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Transform;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.List;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static pixelitor.filters.gui.TransparencyMode.MANUAL_ALPHA_ONLY;

/**
 * The Render/Geometry/Rose filter, generating a polar rose curve.
 */
public class Rose extends ParametrizedFilter {
    public static final String NAME = "Rose";

    @Serial
    private static final long serialVersionUID = 1L;

    private final RangeParam nParam = new RangeParam("n", 1, 4, 20);
    private final RangeParam dParam = new RangeParam("d", 1, 7, 20);
    private final ColorParam bgColor = new ColorParam("Background Color", BLACK, MANUAL_ALPHA_ONLY);
    private final ColorParam fgColor = new ColorParam("Foreground Color", WHITE, MANUAL_ALPHA_ONLY);

    private final Transform transform = new Transform();

    public Rose() {
        super(false);

        initParams(
            nParam,
            dParam,
            bgColor,
            fgColor,
            transform.createDialogParam()
        ).withAction(FilterButtonModel.createExportSvg(this::exportSVG));

        help = Help.fromWikiURL("https://en.wikipedia.org/wiki/Rose_(mathematics)");
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

        Shape shape = createShape(width, height);

        g.fill(shape);

        g.dispose();
        return dest;
    }

    private Shape createShape(int width, int height) {
        Path2D path = new Path2D.Double(Path2D.WIND_EVEN_ODD);
        double radius = Math.min(width, height) / 2.0;
        double cx = transform.getCx(width);
        double cy = transform.getCy(height);

        int n = nParam.getValue();
        int d = dParam.getValue();
        double k = n / (double) d;

        double maxAngle = calcMaxAngle(n, d);
        boolean firstPoint = true;
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
        Shape shape = path;

        if (transform.hasNonlinDistort()) {
            Distortion distortion = transform.createDistortion(width, height);
            shape = distortion.distort(shape);
        }
        AffineTransform at = transform.calcAffineTransform(width, height);
        if (at != null) {
            shape = at.createTransformedShape(shape);
        }

        return shape;
    }

    /**
     * Calculates the angle at which the curve begins to repeat.
     */
    private static double calcMaxAngle(int n, int d) {
        boolean nIsEven = n % 2 == 0;
        boolean dIsEven = d % 2 == 0;

        if (nIsEven && dIsEven) {
            //noinspection TailRecursion
            return calcMaxAngle(n / 2, d / 2);
        } else if (nIsEven || dIsEven) {
            return 2 * d * Math.PI;
        } else {
            return d * Math.PI;
        }
    }

    private void exportSVG() {
        Canvas canvas = Views.getActiveComp().getCanvas();
        List<ShapeWithColor> shapes = List.of(new ShapeWithColor(
            createShape(canvas.getWidth(), canvas.getHeight()),
            fgColor.getColor()
        ));
        String svgContent = ShapeWithColor.createSvgContent(shapes, canvas, bgColor.getColor());
        FileIO.saveSVG(svgContent, "rose.svg");
    }

    @Override
    protected boolean createDefaultDestImg() {
        return false;
    }
}