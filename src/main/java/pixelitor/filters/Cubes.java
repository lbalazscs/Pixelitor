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

import pixelitor.filters.gui.*;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.NonlinTransform;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.Serial;

import static java.awt.Color.BLACK;
import static java.awt.Color.GRAY;
import static java.awt.Color.LIGHT_GRAY;
import static java.awt.Color.WHITE;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static pixelitor.filters.gui.TransparencyPolicy.USER_ONLY_TRANSPARENCY;
import static pixelitor.utils.NonlinTransform.NONE;

/**
 * The Render/Geometry/Cubes Pattern filter.
 */
public class Cubes extends ParametrizedFilter {
    public static final String NAME = "Cubes Pattern";

    @Serial
    private static final long serialVersionUID = 1L;

    private static final int TYPE_BASIC = 0;
    private static final int TYPE_CORNER_CUT = 1;
    private static final int TYPE_CORNER_CUT2 = 2;
    private static final int TYPE_CORNER_CUT3 = 3;
    private static final int TYPE_INTERLOCKING = 4;

    private final IntChoiceParam typeParam = new IntChoiceParam("Type", new Item[]{
        new Item("Basic", TYPE_BASIC),
        new Item("Corner Cut", TYPE_CORNER_CUT),
        new Item("Corner Cut 2", TYPE_CORNER_CUT2),
        new Item("Corner Cut 3", TYPE_CORNER_CUT3),
        new Item("Interlocking", TYPE_INTERLOCKING),
    });
//    private final RangeParam hollowDepthParam = new RangeParam("Hollow Depth (%)", 1, 50, 99);

    private final RangeParam sizeParam = new RangeParam("Size", 5, 20, 200);
    private final ColorParam topColorParam = new ColorParam("Top Color", WHITE, USER_ONLY_TRANSPARENCY);
    private final ColorParam leftColorParam = new ColorParam("Left Color", LIGHT_GRAY, USER_ONLY_TRANSPARENCY);
    private final ColorParam rightColorParam = new ColorParam("Right Color", GRAY, USER_ONLY_TRANSPARENCY);
    private final RangeParam edgeWidthParam = new RangeParam("Edge Width", 0, 0, 10);
    private final ColorParam edgeColorParam = new ColorParam("Edge Color", BLACK, USER_ONLY_TRANSPARENCY);

    private final GroupedRangeParam scale = new GroupedRangeParam("Scale (%)", 1, 100, 500);
    private final AngleParam rotate = new AngleParam("Rotate", 0);
    private final EnumParam<NonlinTransform> distortType = NonlinTransform.asParam();
    private final RangeParam distortAmount = NonlinTransform.createAmountParam();

    public Cubes() {
        super(false);

        distortType.setupEnableOtherIf(distortAmount, NonlinTransform::hasAmount);
        edgeWidthParam.setupEnableOtherIfNotZero(edgeColorParam);
//        typeParam.setupEnableOtherIf(hollowDepthParam, selectedType ->
//            selectedType.valueIs(TYPE_HOLLOWED));

        setParams(
            typeParam,
            sizeParam,
//            hollowDepthParam,
            topColorParam,
            leftColorParam,
            rightColorParam,
            edgeWidthParam,
            edgeColorParam,
            new DialogParam("Transform",
                distortType, distortAmount, rotate, scale)
        );
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        dest = ImageUtils.createImageWithSameCM(src);

        int width = dest.getWidth();
        int height = dest.getHeight();

        Graphics2D g = dest.createGraphics();
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

        double sx = scale.getPercentage(0);
        double sy = scale.getPercentage(0);
        double angle = rotate.getValueInRadians();
        double cx = width / 2.0;
        double cy = height / 2.0;
        if (sx != 1 || sy != 1) {
            g.translate(cx, cy);
            g.scale(sx, sy);
            g.translate(-cx, -cy);
        }
        if (angle != 0) {
            g.rotate(angle, cx, cy);
        }

        NonlinTransform distortion = distortType.getSelected();
        double amount = distortAmount.getValueAsDouble();
        Point2D pivotPoint = new Point2D.Double(cx, cy);

        float edgeWidth = edgeWidthParam.getValueAsFloat();
        boolean renderEdges = edgeWidth > 0;
        Color edgeColor = null;
        if (renderEdges) {
            g.setStroke(new BasicStroke(edgeWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
            edgeColor = edgeColorParam.getColor();
        }

        Color topColor = topColorParam.getColor();
        Color rightColor = rightColorParam.getColor();
        Color leftColor = leftColorParam.getColor();

        int type = typeParam.getSelected().getValue();
        boolean interlocking = type == TYPE_INTERLOCKING;
//        double hollowDepth = hollowDepthParam.getPercentage();

        double size = sizeParam.getValueAsDouble();

        double longer = size * Math.cos(Math.PI / 6);
        double shorter = size * Math.sin(Math.PI / 6);

        double horizontalSpacing = interlocking ? 3 * longer : 2 * longer;
        double verticalSpacing = size + shorter;
        if (interlocking) {
            verticalSpacing = size * 0.75;
        }
        int numCubesX = (int) (width / horizontalSpacing) + 2;

        int numCubesY = (int) (height / verticalSpacing)
            + (interlocking ? 3 : 2);

        int numCornerCuts = switch (type) {
            case TYPE_BASIC, TYPE_INTERLOCKING -> 0;
            case TYPE_CORNER_CUT -> 1;
            case TYPE_CORNER_CUT2 -> 2;
            case TYPE_CORNER_CUT3 -> 3;
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };

        double verOffset = interlocking ? -size / 2 : 0;
        for (int row = 0; row < numCubesY; row++) {
            double horOffset = row % 2 == 0 ? 0 : longer;
            if (interlocking) {
                horOffset *= 1.5;
            }
            for (int col = 0; col < numCubesX; col++) {
                // base point (shared vertex)
                double baseX = horOffset + col * horizontalSpacing;
                double baseY = verOffset + row * verticalSpacing;

                Path2D topFace = createTop(baseX, baseY, longer, shorter, interlocking, size);
                renderShape(topFace, g, distortion, pivotPoint, amount, width, height, topColor, edgeColor);

                Path2D rightFace = createRight(baseX, baseY, longer, shorter, interlocking, size);
                renderShape(rightFace, g, distortion, pivotPoint, amount, width, height, rightColor, edgeColor);

                Path2D leftFace = createLeft(baseX, baseY, longer, shorter, interlocking, size);
                renderShape(leftFace, g, distortion, pivotPoint, amount, width, height, leftColor, edgeColor);

                for (int cut = 0; cut < numCornerCuts; cut++) {
                    double cutRatio = 1.0 - (double) (cut + 1) / (numCornerCuts + 1);

                    AffineTransform carvedCube = new AffineTransform();
                    carvedCube.translate(baseX, baseY);
                    if (cut % 2 == 0) {
                        carvedCube.rotate(Math.PI);
                    }
                    carvedCube.scale(cutRatio, cutRatio);
                    carvedCube.translate(-baseX, -baseY);

                    Shape miniTopFace = carvedCube.createTransformedShape(topFace);
                    renderShape(miniTopFace, g, distortion, pivotPoint, amount, width, height, topColor, edgeColor);

                    Shape miniLeftFace = carvedCube.createTransformedShape(leftFace);
                    renderShape(miniLeftFace, g, distortion, pivotPoint, amount, width, height, leftColor, edgeColor);

                    Shape miniRightFace = carvedCube.createTransformedShape(rightFace);
                    renderShape(miniRightFace, g, distortion, pivotPoint, amount, width, height, rightColor, edgeColor);
                }
//                Shapes.fillCircle(baseX, baseY, 5, Color.RED, g);
            }
        }

        g.dispose();
        return dest;
    }

    private static Path2D createTop(double baseX, double baseY, double longer, double shorter, boolean interlocking, double size) {
        Path2D top = new Path2D.Double();
        top.moveTo(baseX, baseY);
        top.lineTo(baseX - longer, baseY - shorter);
        if (interlocking) {
            top.lineTo(baseX - longer / 2, baseY - size / 2 - shorter / 2);
            top.lineTo(baseX, baseY - shorter);
            top.lineTo(baseX + longer / 2, baseY - size / 2 - shorter / 2);
        } else {
            top.lineTo(baseX, baseY - size);
        }
        top.lineTo(baseX + longer, baseY - shorter);
        top.closePath();
        return top;
    }

    private static Path2D createRight(double baseX, double baseY, double longer, double shorter, boolean interlocking, double size) {
        Path2D right = new Path2D.Double();
        right.moveTo(baseX, baseY);
        right.lineTo(baseX + longer, baseY - shorter);
        if (interlocking) {
            right.lineTo(baseX + longer, baseY - shorter + size / 2);
            right.lineTo(baseX + longer / 2, baseY + 0.25 * size);
            right.lineTo(baseX + longer / 2, baseY - shorter / 2 + size);
        } else {
            right.lineTo(baseX + longer, baseY - shorter + size);
        }
        right.lineTo(baseX, baseY + size);
        right.closePath();
        return right;
    }

    private static Path2D createLeft(double baseX, double baseY, double longer, double shorter, boolean interlocking, double size) {
        Path2D left = new Path2D.Double();
        left.moveTo(baseX, baseY);
        left.lineTo(baseX - longer, baseY - shorter);
        if (interlocking) {
            left.lineTo(baseX - longer, baseY - shorter + size / 2);
            left.lineTo(baseX - longer / 2, baseY + 0.25 * size);
            left.lineTo(baseX - longer / 2, baseY - shorter / 2 + size);
        } else {
            left.lineTo(baseX - longer, baseY - shorter + size);
        }
        left.lineTo(baseX, baseY + size);
        left.closePath();
        return left;
    }

    private static void renderShape(Shape shape, Graphics2D g, NonlinTransform distortion, Point2D pivotPoint, double amount, int width, int height, Color color, Color edgeColor) {
        if (distortion != NONE) {
            shape = distortion.transform(shape, pivotPoint, amount, width, height);
        }
        g.setColor(color);
        g.fill(shape);

        if (edgeColor != null) {
            g.setColor(edgeColor);
            g.draw(shape);
        }
    }

    @Override
    protected boolean createDefaultDestImg() {
        return false;
    }
}