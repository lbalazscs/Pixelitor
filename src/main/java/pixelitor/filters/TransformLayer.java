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

import pixelitor.Views;
import pixelitor.colors.Colors;
import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.ColorParam;
import pixelitor.filters.gui.GroupedRangeParam;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.gui.GUIText;
import pixelitor.layers.Drawable;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.Serial;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.KEY_INTERPOLATION;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC;
import static pixelitor.colors.Colors.TRANSPARENT_BLACK;
import static pixelitor.filters.gui.TransparencyMode.MANUAL_ALPHA_ONLY;

/**
 * A filter for applying arbitrary affine transformations to an image.
 */
public class TransformLayer extends ParametrizedFilter {
    @Serial
    private static final long serialVersionUID = 9039647019976237114L;

    public static final String NAME = "Transform Layer";

    private final ImagePositionParam center = new ImagePositionParam("Pivot Point");
    private final AngleParam angleParam = new AngleParam("Rotate Angle", 0);
    private final ColorParam bgColorParam = new ColorParam(GUIText.BG_COLOR, TRANSPARENT_BLACK, MANUAL_ALPHA_ONLY);
    private final GroupedRangeParam scaleParam = new GroupedRangeParam("Scale (%)", -500, 100, 500);
    private final GroupedRangeParam shearParam = new GroupedRangeParam("Shear", -500, 0, 500, false);

    public TransformLayer() {
        super(true);

        bgColorParam.setPresetKey("Background Color");
        shearParam.setLinked(false);

        initParams(
            center,
            angleParam,
            scaleParam,
            shearParam,
            bgColorParam
        );
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        Graphics2D g = createDestGraphics(dest);
        Colors.fillWith(bgColorParam.getColor(), g, dest.getWidth(), dest.getHeight());

        AffineTransform transform = calcTransform(src);
        g.drawImage(src, transform, null);

        g.dispose();
        return dest;
    }

    private static Graphics2D createDestGraphics(BufferedImage dest) {
        Graphics2D g = dest.createGraphics();
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        g.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BICUBIC);
        return g;
    }

    private AffineTransform calcTransform(BufferedImage src) {
        Point2D pivotPoint = calcPivotPoint(src);
        var transform = calcRotateTransform(pivotPoint);
        applyScaling(transform, pivotPoint);
        applyShearing(transform, pivotPoint);
        return transform;
    }

    private Point2D calcPivotPoint(BufferedImage src) {
        // TODO if this runs as a smart filter, then it shouldn't
        //   assume that the active layer is the owner of the image
        Drawable dr = Views.getActiveDrawable();
        int tx = -dr.getTx();
        int ty = -dr.getTy();

        double pivotX = (tx + src.getWidth()) * center.getRelativeX();
        double pivotY = (ty + src.getHeight()) * center.getRelativeY();
        return new Point2D.Double(pivotX, pivotY);
    }

    private AffineTransform calcRotateTransform(Point2D pivotPoint) {
        double angle = angleParam.getValueInRadians();
        return AffineTransform.getRotateInstance(
            angle, pivotPoint.getX(), pivotPoint.getY());
    }

    private void applyScaling(AffineTransform transform, Point2D pivotPoint) {
        int scaleX = scaleParam.getValue(0);
        int scaleY = scaleParam.getValue(1);
        if (scaleX != 100 || scaleY != 100) {
            transform.translate(pivotPoint.getX(), pivotPoint.getY());
            transform.scale(scaleX / 100.0, scaleY / 100.0);
            transform.translate(-pivotPoint.getX(), -pivotPoint.getY());
        }
    }

    private void applyShearing(AffineTransform transform, Point2D pivotPoint) {
        int shearX = shearParam.getValue(0);
        int shearY = shearParam.getValue(1);
        if (shearX != 0 || shearY != 0) {
            transform.translate(pivotPoint.getX(), pivotPoint.getY());
            transform.shear(shearX / 100.0, shearY / 100.0);
            transform.translate(-pivotPoint.getX(), -pivotPoint.getY());
        }
    }
}