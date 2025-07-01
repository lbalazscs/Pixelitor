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
import pixelitor.filters.gui.ColorParam;
import pixelitor.filters.gui.FilterButtonModel;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.util.ShapeWithColor;
import pixelitor.io.FileIO;
import pixelitor.utils.Distortion;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Transform;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import static java.awt.Color.BLACK;
import static java.awt.Color.GRAY;
import static java.awt.Color.LIGHT_GRAY;
import static java.awt.Color.WHITE;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static pixelitor.filters.gui.TransparencyMode.MANUAL_ALPHA_ONLY;

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

    private final RangeParam sizeParam = new RangeParam("Size", 5, 20, 200);
    private final ColorParam topColorParam = new ColorParam("Top Color", WHITE, MANUAL_ALPHA_ONLY);
    private final ColorParam leftColorParam = new ColorParam("Left Color", LIGHT_GRAY, MANUAL_ALPHA_ONLY);
    private final ColorParam rightColorParam = new ColorParam("Right Color", GRAY, MANUAL_ALPHA_ONLY);
    private final RangeParam edgeWidthParam = new RangeParam("Edge Width", 0, 0, 10);
    private final ColorParam edgeColorParam = new ColorParam("Edge Color", BLACK, MANUAL_ALPHA_ONLY);

    private final Transform transform = new Transform();

    public Cubes() {
        super(false);

        edgeWidthParam.setupEnableOtherIfNotZero(edgeColorParam);

        initParams(
            typeParam,
            sizeParam,
            topColorParam,
            leftColorParam,
            rightColorParam,
            edgeWidthParam,
            edgeColorParam,
            transform.createDialogParam()
        ).withAction(FilterButtonModel.createExportSvg(this::exportSVG));
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        dest = ImageUtils.createImageWithSameCM(src);
        int width = dest.getWidth();
        int height = dest.getHeight();

        Graphics2D g = dest.createGraphics();
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

        AffineTransform at = transform.calcAffineTransform(width, height);
        if (at != null) {
            g.transform(at);
        }

        float edgeWidth = edgeWidthParam.getValueAsFloat();
        if (edgeWidth > 0) {
            g.setStroke(new BasicStroke(edgeWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
        }

        List<ShapeWithColor> shapes = createDistortedShapes(width, height);

        for (ShapeWithColor shapeWithColor : shapes) {
            g.setColor(shapeWithColor.color());
            g.fill(shapeWithColor.shape());

            if (edgeWidth > 0) {
                g.setColor(edgeColorParam.getColor());
                g.draw(shapeWithColor.shape());
            }
        }

        g.dispose();
        return dest;
    }

    private List<ShapeWithColor> createDistortedShapes(int width, int height) {
        List<ShapeWithColor> shapes = createShapes(width, height);

        if (transform.hasNonlinDistort()) {
            Distortion distortion = transform.createDistortion(width, height);
            shapes = shapes.stream()
                .map(sc -> sc.distort(distortion))
                .toList();
        }
        return shapes;
    }

    private List<ShapeWithColor> createShapes(int width, int height) {
        List<ShapeWithColor> shapes = new ArrayList<>();

        Color topColor = topColorParam.getColor();
        Color rightColor = rightColorParam.getColor();
        Color leftColor = leftColorParam.getColor();

        int type = typeParam.getValue();
        boolean interlocking = type == TYPE_INTERLOCKING;

        double size = sizeParam.getValueAsDouble();
        double longer = size * Math.cos(Math.PI / 6);
        double shorter = size * Math.sin(Math.PI / 6);

        double horizontalSpacing = interlocking ? 3 * longer : 2 * longer;
        double verticalSpacing = interlocking ? size * 0.75 : (size + shorter);

        int numCubesX = (int) (width / horizontalSpacing) + 2;
        int numCubesY = (int) (height / verticalSpacing) + (interlocking ? 3 : 2);

        int numCornerCuts = switch (type) {
            case TYPE_BASIC, TYPE_INTERLOCKING -> 0;
            case TYPE_CORNER_CUT -> 1;
            case TYPE_CORNER_CUT2 -> 2;
            case TYPE_CORNER_CUT3 -> 3;
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };

        double moveHorOffset = transform.getHorOffset(width);
        double moveVerOffset = transform.getVerOffset(height);

        double verOffset = moveVerOffset + (interlocking ? -size / 2 : 0);
        for (int row = 0; row < numCubesY; row++) {
            double horOffset = moveHorOffset + (row % 2 == 0 ? 0 : longer);
            if (interlocking) {
                horOffset *= 1.5;
            }

            for (int col = 0; col < numCubesX; col++) {
                double baseX = horOffset + col * horizontalSpacing;
                double baseY = verOffset + row * verticalSpacing;

                addCubeShapes(shapes, baseX, baseY, longer, shorter, interlocking, size,
                    topColor, rightColor, leftColor, numCornerCuts);
            }
        }

        return shapes;
    }

    private void addCubeShapes(List<ShapeWithColor> shapes,
                               double baseX, double baseY,
                               double longer, double shorter,
                               boolean interlocking, double size,
                               Color topColor, Color rightColor, Color leftColor,
                               int numCornerCuts) {

        Path2D topFace = createTop(baseX, baseY, longer, shorter, interlocking, size);
        Path2D rightFace = createRight(baseX, baseY, longer, shorter, interlocking, size);
        Path2D leftFace = createLeft(baseX, baseY, longer, shorter, interlocking, size);

        shapes.add(new ShapeWithColor(topFace, topColor));
        shapes.add(new ShapeWithColor(rightFace, rightColor));
        shapes.add(new ShapeWithColor(leftFace, leftColor));

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
            Shape miniRightFace = carvedCube.createTransformedShape(rightFace);
            Shape miniLeftFace = carvedCube.createTransformedShape(leftFace);

            shapes.add(new ShapeWithColor(miniTopFace, topColor));
            shapes.add(new ShapeWithColor(miniRightFace, rightColor));
            shapes.add(new ShapeWithColor(miniLeftFace, leftColor));
        }
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

    private void exportSVG() {
        Canvas canvas = Views.getActiveComp().getCanvas();
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        List<ShapeWithColor> shapes = createDistortedShapes(width, height);
        AffineTransform at = transform.calcAffineTransform(width, height);
        if (at != null) {
            shapes = shapes.stream()
                .map(s -> s.transform(at))
                .toList();
        }
        String svgContent = ShapeWithColor.createSvgContent(shapes, canvas, null,
            edgeWidthParam.getValue(), edgeColorParam.getColor());
        FileIO.saveSVG(svgContent, "cubes.svg");
    }

    @Override
    protected boolean createDefaultDestImg() {
        return false;
    }
}