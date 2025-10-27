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
import pixelitor.filters.gui.*;
import pixelitor.filters.util.ShapeWithColor;
import pixelitor.io.FileIO;
import pixelitor.utils.Distortion;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Transform;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
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

    private enum CubeType {
        BASIC("Basic", 0, false),
        CORNER_CUT("Corner Cut", 1, false),
        CORNER_CUT2("Corner Cut 2", 2, false),
        CORNER_CUT3("Corner Cut 3", 3, false),
        INTERLOCKING("Interlocking", 0, true),
        SUPERCUBE("Supercube", 0, false);

        private final String displayName;
        private final int numCornerCuts;
        private final boolean isInterlocking;

        CubeType(String displayName, int numCornerCuts, boolean isInterlocking) {
            this.displayName = displayName;
            this.numCornerCuts = numCornerCuts;
            this.isInterlocking = isInterlocking;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private final EnumParam<CubeType> typeParam = new EnumParam<>("Type", CubeType.class);
    private final GroupedRangeParam sizeParam = new GroupedRangeParam("Size", 5, 20, 200);
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
            shapeWithColor.fill(g);

            if (edgeWidth > 0) {
                g.setColor(edgeColorParam.getColor());
                g.draw(shapeWithColor.shape());
            }
        }

        g.dispose();
        return dest;
    }

    /**
     * Creates the list of shapes and applies nonlinear distortion if configured.
     */
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

    /**
     * Creates the list of shapes for the cube pattern.
     */
    private List<ShapeWithColor> createShapes(int width, int height) {
        CubeType type = typeParam.getSelected();
        if (type == CubeType.SUPERCUBE) {
            return createSupercubeShapes(width, height);
        }

        List<ShapeWithColor> shapes = new ArrayList<>();

        Color topColor = topColorParam.getColor();
        Color rightColor = rightColorParam.getColor();
        Color leftColor = leftColorParam.getColor();

        boolean interlocking = type.isInterlocking;

        double size = sizeParam.getValueAsDouble(0);
        double verSize = sizeParam.getValueAsDouble(1);

        double ratio = verSize / size;

        // isometric projection constants
        double longer = size * Math.cos(Math.PI / 6.0);
        double shorter = size * Math.sin(Math.PI / 6.0);

        double horizontalSpacing = interlocking ? 3.0 * longer : 2.0 * longer;
        double verticalSpacing = (interlocking ? size * 0.75 : (size + shorter)) * ratio;

        // add a buffer to ensure the pattern covers the entire image, even when offset
        int numCubesX = (int) (width / horizontalSpacing) + 2;
        int numCubesY = (int) (height / Math.max(1.0, verticalSpacing)) + (interlocking ? 3 : 2);

        double moveHorOffset = transform.getHorOffset(width);
        double moveVerOffset = transform.getVerOffset(height);

        // special y-offset for interlocking to align rows correctly
        double verOffset = moveVerOffset + (interlocking ? -size / 2.0 * ratio : 0);
        for (int row = 0; row < numCubesY; row++) {
            // offset every other row for a staggered pattern
            double horOffset = moveHorOffset + (row % 2 == 0 ? 0 : longer);
            if (interlocking) {
                // the interlocking pattern requires additional horizontal shift
                horOffset *= 1.5;
            }

            for (int col = 0; col < numCubesX; col++) {
                double baseX = horOffset + col * horizontalSpacing;
                double baseY = verOffset + row * verticalSpacing;

                addCubeShapes(shapes, baseX, baseY, longer, shorter, size, ratio, type,
                    topColor, rightColor, leftColor);
            }
        }

        return shapes;
    }

    /**
     * Creates the list of shapes for the "Supercube" cube pattern.
     * This pattern is centered and represents the outer layer of a "cube of cubes".
     */
    private List<ShapeWithColor> createSupercubeShapes(int width, int height) {
        List<ShapeWithColor> shapes = new ArrayList<>();

        Color topColor = topColorParam.getColor();
        Color rightColor = rightColorParam.getColor();
        Color leftColor = leftColorParam.getColor();
        double size = sizeParam.getValueAsDouble(0);
        double verSize = sizeParam.getValueAsDouble(1);
        double ratio = verSize / size;

        // isometric projection constants for a single cube
        double longer = size * Math.cos(Math.PI / 6.0);
        double shorter = size * Math.sin(Math.PI / 6.0);

        // the step from one cube's center vertex to the next
        final double stepFactor = 1.5; // size + gap = 1.5 * size

        // calculate the dimension of the "super-cube" (dim x dim x dim)
        // so that it fits reasonably well within the canvas
        double stepHor = stepFactor * longer;
        // for vertical fit, consider both downward (k-axis) and upward (i/j axes) movement
        double stepVerDown = stepFactor * size * ratio;
        double stepVerUp = stepFactor * shorter * ratio;

        // calculate how many steps fit from the center to each edge
        int dimX = (stepHor > 0) ? (int) (width / 2.0 / stepHor) + 1 : Integer.MAX_VALUE;
        int dimYDown = (stepVerDown > 0) ? (int) (height / 2.0 / stepVerDown) + 1 : Integer.MAX_VALUE;
        int dimYUp = (stepVerUp > 0) ? (int) (height / 2.0 / stepVerUp) + 1 : Integer.MAX_VALUE;

        int dim = Math.min(dimX, Math.min(dimYDown, dimYUp));
        dim = Math.max(1, dim); // ensure at least one cube is drawn

        // define the 3D-to-2D displacement vectors for one step along each axis
        double dx_i = stepFactor * longer;
        double dy_i = -stepFactor * shorter * ratio;

        double dx_j = -stepFactor * longer;
        double dy_j = -stepFactor * shorter * ratio;

        double dx_k = 0;
        double dy_k = stepFactor * size * ratio;

        // the user-selected center is the center of the pattern
        double centerX = transform.getCx(width);
        double centerY = transform.getCy(height);

        // loop from back to front to ensure correct drawing order (painter's algorithm)
        for (int k = dim - 1; k >= 0; k--) {
            for (int j = dim - 1; j >= 0; j--) {
                for (int i = dim - 1; i >= 0; i--) {
                    // only draw cubes on the three visible outer faces of the super-cube
                    if (i == 0 || j == 0 || k == 0) {
                        // calculate the screen position (baseX, baseY) for the cube's front vertex
                        double baseX = centerX + i * dx_i + j * dx_j + k * dx_k;
                        double baseY = centerY + i * dy_i + j * dy_j + k * dy_k;

                        // pass CubeType.BASIC because we want the simple cube shape
                        addCubeShapes(shapes, baseX, baseY, longer, shorter, size, ratio,
                            CubeType.BASIC, topColor, rightColor, leftColor);
                    }
                }
            }
        }

        return shapes;
    }

    private record CubeFaces(Path2D top, Path2D right, Path2D left) {
    }

    /**
     * Adds the faces for a single cube, including any corner cuts, to the list of shapes.
     */
    private static void addCubeShapes(List<ShapeWithColor> shapes,
                                      double baseX, double baseY,
                                      double longer, double shorter, double size, double ratio,
                                      CubeType type,
                                      Color topColor, Color rightColor, Color leftColor) {

        CubeFaces faces = createCubeFaces(baseX, baseY, longer, shorter, size, ratio, type.isInterlocking);

        shapes.add(new ShapeWithColor(faces.top, topColor));
        shapes.add(new ShapeWithColor(faces.right, rightColor));
        shapes.add(new ShapeWithColor(faces.left, leftColor));

        // add smaller, transformed copies of the faces for a carved effect
        int numCornerCuts = type.numCornerCuts;
        for (int i = 0; i < numCornerCuts; i++) {
            double cutRatio = 1.0 - (double) (i + 1) / (numCornerCuts + 1);

            AffineTransform cutTransform = new AffineTransform();
            cutTransform.translate(baseX, baseY);
            // alternate rotation for a carved effect
            if (i % 2 == 0) {
                cutTransform.rotate(Math.PI);
            }
            cutTransform.scale(cutRatio, cutRatio);
            cutTransform.translate(-baseX, -baseY);

            shapes.add(new ShapeWithColor(cutTransform.createTransformedShape(faces.top), topColor));
            shapes.add(new ShapeWithColor(cutTransform.createTransformedShape(faces.right), rightColor));
            shapes.add(new ShapeWithColor(cutTransform.createTransformedShape(faces.left), leftColor));
        }
    }

    /**
     * Creates the three visible faces of a single cube.
     */
    private static CubeFaces createCubeFaces(double baseX, double baseY, double longer, double shorter, double size, double ratio, boolean interlocking) {
        Path2D topFace;
        Path2D rightFace;
        Path2D leftFace;

        if (interlocking) {
            topFace = createInterlockingTop(baseX, baseY, longer, shorter, size, ratio);
            rightFace = createInterlockingRight(baseX, baseY, longer, shorter, size, ratio);
            leftFace = createInterlockingLeft(baseX, baseY, longer, shorter, size, ratio);
        } else {
            topFace = createBasicTop(baseX, baseY, longer, shorter, size, ratio);
            rightFace = createBasicRight(baseX, baseY, longer, shorter, size, ratio);
            leftFace = createBasicLeft(baseX, baseY, longer, shorter, size, ratio);
        }
        return new CubeFaces(topFace, rightFace, leftFace);
    }

    /**
     * Creates the path for the top face of a basic cube.
     */
    private static Path2D createBasicTop(double baseX, double baseY, double longer, double shorter, double size, double ratio) {
        Path2D top = new Path2D.Double();
        top.moveTo(baseX, baseY);
        top.lineTo(baseX - longer, baseY - shorter * ratio);
        top.lineTo(baseX, baseY - size * ratio);
        top.lineTo(baseX + longer, baseY - shorter * ratio);
        top.closePath();
        return top;
    }

    /**
     * Creates the path for the right face of a basic cube.
     */
    private static Path2D createBasicRight(double baseX, double baseY, double longer, double shorter, double size, double ratio) {
        Path2D right = new Path2D.Double();
        right.moveTo(baseX, baseY);
        right.lineTo(baseX + longer, baseY - shorter * ratio);
        right.lineTo(baseX + longer, baseY - shorter * ratio + size * ratio);
        right.lineTo(baseX, baseY + size * ratio);
        right.closePath();
        return right;
    }

    /**
     * Creates the path for the left face of a basic cube.
     */
    private static Path2D createBasicLeft(double baseX, double baseY, double longer, double shorter, double size, double ratio) {
        Path2D left = new Path2D.Double();
        left.moveTo(baseX, baseY);
        left.lineTo(baseX - longer, baseY - shorter * ratio);
        left.lineTo(baseX - longer, baseY - shorter * ratio + size * ratio);
        left.lineTo(baseX, baseY + size * ratio);
        left.closePath();
        return left;
    }

    /**
     * Creates the path for the top face of an interlocking cube.
     */
    private static Path2D createInterlockingTop(double baseX, double baseY, double longer, double shorter, double size, double ratio) {
        Path2D top = new Path2D.Double();
        top.moveTo(baseX, baseY);
        top.lineTo(baseX - longer, baseY - shorter * ratio);
        top.lineTo(baseX - longer / 2.0, baseY - (size / 2.0 + shorter / 2.0) * ratio);
        top.lineTo(baseX, baseY - shorter * ratio);
        top.lineTo(baseX + longer / 2.0, baseY - (size / 2.0 + shorter / 2.0) * ratio);
        top.lineTo(baseX + longer, baseY - shorter * ratio);
        top.closePath();
        return top;
    }

    /**
     * Creates the path for the right face of an interlocking cube.
     */
    private static Path2D createInterlockingRight(double baseX, double baseY, double longer, double shorter, double size, double ratio) {
        Path2D right = new Path2D.Double();
        right.moveTo(baseX, baseY);
        right.lineTo(baseX + longer, baseY - shorter * ratio);
        right.lineTo(baseX + longer, baseY + (-shorter + size / 2.0) * ratio);
        right.lineTo(baseX + longer / 2.0, baseY + (0.25 * size) * ratio);
        right.lineTo(baseX + longer / 2.0, baseY + (-shorter / 2.0 + size) * ratio);
        right.lineTo(baseX, baseY + size * ratio);
        right.closePath();
        return right;
    }

    /**
     * Creates the path for the left face of an interlocking cube.
     */
    private static Path2D createInterlockingLeft(double baseX, double baseY, double longer, double shorter, double size, double ratio) {
        Path2D left = new Path2D.Double();
        left.moveTo(baseX, baseY);
        left.lineTo(baseX - longer, baseY - shorter * ratio);
        left.lineTo(baseX - longer, baseY + (-shorter + size / 2.0) * ratio);
        left.lineTo(baseX - longer / 2.0, baseY + (0.25 * size) * ratio);
        left.lineTo(baseX - longer / 2.0, baseY + (-shorter / 2.0 + size) * ratio);
        left.lineTo(baseX, baseY + size * ratio);
        left.closePath();
        return left;
    }

    /**
     * Exports the generated pattern to an SVG file.
     */
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
        FileIO.saveSVG(svgContent, this);
    }

    @Override
    protected boolean createDefaultDestImg() {
        return false;
    }
}
