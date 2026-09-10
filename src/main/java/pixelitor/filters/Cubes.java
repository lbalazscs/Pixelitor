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

import static com.jhlabs.image.ImageMath.COS_30;
import static java.awt.Color.*;
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
        SUPERCUBE("Supercube", 0, false),
        PYRAMID("Pyramid", 0, false);

        private final String displayName;
        private final boolean isInterlocking;
        private final double[] cutScales;

        CubeType(String displayName, int numCornerCuts, boolean isInterlocking) {
            this.displayName = displayName;
            this.isInterlocking = isInterlocking;
            this.cutScales = new double[numCornerCuts];
            for (int i = 0; i < numCornerCuts; i++) {
                double cutRatio = 1.0 - (double) (i + 1) / (numCornerCuts + 1);

                // alternate rotation for a carved effect (negative factor produces the 180° rotation)
                this.cutScales[i] = (i % 2 == 0) ? -cutRatio : cutRatio;
            }
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private final EnumParam<CubeType> typeParam = new EnumParam<>("Type", CubeType.class);
    private final GroupedRangeParam sizeParam = new GroupedRangeParam("Size", 5, 20, 200);
    private final RangeParam gapParam = new RangeParam("Gap", 0, 100, 100);
    private final ColorParam topColorParam = new ColorParam("Top Color", WHITE, MANUAL_ALPHA_ONLY);
    private final ColorParam leftColorParam = new ColorParam("Left Color", LIGHT_GRAY, MANUAL_ALPHA_ONLY);
    private final ColorParam rightColorParam = new ColorParam("Right Color", GRAY, MANUAL_ALPHA_ONLY);
    private final RangeParam edgeWidthParam = new RangeParam("Edge Width", 0, 0, 10);
    private final ColorParam edgeColorParam = new ColorParam("Edge Color", BLACK, MANUAL_ALPHA_ONLY);

    private final Transform transform = new Transform();

    public Cubes() {
        super(false);

        // enable the gap selector only for SUPERCUBE and PYRAMID
        typeParam.enableOtherWhen(gapParam, type -> type == CubeType.SUPERCUBE || type == CubeType.PYRAMID);

        // enable the edge color selector only if edge width > 0
        edgeWidthParam.enableOtherWhenNotZero(edgeColorParam);

        initParams(
            typeParam,
            sizeParam,
            gapParam,
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

        renderShapes(g, createDistortedShapes(width, height));

        g.dispose();
        return dest;
    }

    private void renderShapes(Graphics2D g, List<ShapeWithColor> shapes) {
        float edgeWidth = edgeWidthParam.getValueAsFloat();
        if (edgeWidth > 0) {
            g.setStroke(new BasicStroke(edgeWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
            Color edgeColor = edgeColorParam.getColor();
            for (ShapeWithColor shapeWithColor : shapes) {
                shapeWithColor.fill(g);
                g.setColor(edgeColor);
                g.draw(shapeWithColor.shape());
            }
        } else {
            for (ShapeWithColor shapeWithColor : shapes) {
                shapeWithColor.fill(g);
            }
        }
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
        CubeType type = typeParam.getValue();
        if (type == CubeType.SUPERCUBE) {
            return createSupercubeShapes(width, height);
        } else if (type == CubeType.PYRAMID) {
            return createPyramidShapes(width, height);
        }

        List<ShapeWithColor> shapes = new ArrayList<>();

        Color topColor = topColorParam.getColor();
        Color rightColor = rightColorParam.getColor();
        Color leftColor = leftColorParam.getColor();

        boolean interlocking = type.isInterlocking;

        double size = sizeParam.getValueAsDouble(0);
        double h = sizeParam.getValueAsDouble(1);
        double w = size * COS_30;

        double horizontalSpacing = interlocking ? 3.0 * w : 2.0 * w;
        double verticalSpacing = (interlocking ? 0.75 : 1.5) * h;

        // add a buffer to ensure the pattern covers the entire image, even when offset
        int numCubesX = (int) (width / horizontalSpacing) + 2;
        int numCubesY = (int) (height / Math.max(1.0, verticalSpacing)) + (interlocking ? 3 : 2);

        double moveHorOffset = transform.getHorOffset(width);
        double moveVerOffset = transform.getVerOffset(height);

        // special y-offset for interlocking to align rows correctly
        double verOffset = moveVerOffset + (interlocking ? -0.5 * h : 0);
        for (int row = 0; row < numCubesY; row++) {
            // offset every other row for a staggered pattern
            double rowStagger = (row % 2 == 0) ? 0.0 : (interlocking ? 1.5 * w : w);
            double horOffset = moveHorOffset + rowStagger;

            for (int col = 0; col < numCubesX; col++) {
                double baseX = horOffset + col * horizontalSpacing;
                double baseY = verOffset + row * verticalSpacing;

                addCubeShapes(shapes, baseX, baseY, w, h, type,
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
        Color topColor = topColorParam.getColor();
        Color rightColor = rightColorParam.getColor();
        Color leftColor = leftColorParam.getColor();
        double size = sizeParam.getValueAsDouble(0);
        double h = sizeParam.getValueAsDouble(1);

        // isometric projection half-width for a single cube
        double w = size * COS_30;

        // the step from one cube's center vertex to the next
        double stepFactor = 1.0 + gapParam.getPercentage() * 0.5; // size + gap = 1.5 * size

        // calculate the dimension of the "super-cube" (dim x dim x dim)
        // so that it fits reasonably well within the canvas
        double stepHor = stepFactor * w;
        double stepVerDown = stepFactor * h;
        double stepVerUp = stepFactor * 0.5 * h;

        // calculate how many steps fit from the center to each edge
        int dimX = (stepHor > 0) ? (int) (width / 2.0 / stepHor) + 1 : Integer.MAX_VALUE;
        int dimY = (stepVerDown > 0) ? (int) (height / 2.0 / stepVerDown) + 1 : Integer.MAX_VALUE;

        int dim = Math.max(1, Math.min(dimX, dimY));

        // define the 3D-to-2D displacement vectors for one step along each axis
        double dx_i = stepHor;
        double dy_i = -stepVerUp;

        double dx_j = -stepHor;
        double dy_j = -stepVerUp;

        double dx_k = 0;
        double dy_k = stepVerDown;

        // the user-selected center is the center of the pattern
        double centerX = transform.getCx(width);
        double centerY = transform.getCy(height);

        int outerCubes = dim * dim * dim - (dim - 1) * (dim - 1) * (dim - 1);
        List<ShapeWithColor> shapes = new ArrayList<>(outerCubes * 3);

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
                        addCubeShapes(shapes, baseX, baseY, w, h,
                            CubeType.BASIC, topColor, rightColor, leftColor);
                    }
                }
            }
        }

        return shapes;
    }

    /**
     * Creates the list of shapes for the "Pyramid" cube pattern.
     * This pattern is a stepped ziggurat, where each level has fewer cubes.
     * Only the two outer square rings of each level are rendered, as the inner ones are occluded.
     */
    private List<ShapeWithColor> createPyramidShapes(int width, int height) {
        Color topColor = topColorParam.getColor();
        Color rightColor = rightColorParam.getColor();
        Color leftColor = leftColorParam.getColor();
        double size = sizeParam.getValueAsDouble(0);
        double h = sizeParam.getValueAsDouble(1);
        double w = size * COS_30;

        // The step factor determines the gap between cubes.
        // 1.5 has maximal gap; 1.0 makes cubes touch.
        double stepFactor = 1.0 + gapParam.getPercentage() * 0.5;

        // Isometric projection step vectors
        double stepHor = stepFactor * w;
        double stepVerUp = stepFactor * 0.5 * h;
        double stepVerDown = stepFactor * h;

        // Calculate the maximum number of levels that fit in the canvas
        int maxNWidth = (int) Math.max(0, (width - 2 * w) / (4 * stepFactor * w)) + 1;
        int maxNHeight = (int) Math.max(0, (height / (2 * h) - 1) / stepFactor) + 1;
        int N = Math.max(1, Math.min(maxNWidth, maxNHeight));
        // Cap to avoid excessive shape generation
        N = Math.min(N, 50);

        // Center the pyramid vertically around the user-selected center
        double centerX = transform.getCx(width);
        double centerY = transform.getCy(height);
        double offsetY = -(N - 1) * stepFactor * h;
        double baseYOffset = centerY + offsetY;

        // Estimate the number of shapes to pre-size the list.
        // Each rendered cube adds 3 shapes (top, right, left).
        int estimatedCubes = 0;
        for (int k = 0; k < N; k++) {
            if (k == 0) {
                estimatedCubes += 1; // single cube
            } else if (k == 1) {
                estimatedCubes += 9; // all 9 cubes form the two outer rings
            } else {
                // two outer square rings: total cubes minus the inner (2k-3) x (2k-3) square
                estimatedCubes += 16 * k - 8;
            }
        }
        List<ShapeWithColor> shapes = new ArrayList<>(estimatedCubes * 3);

        // Draw from bottom to top (k from N-1 down to 0) and back to front
        for (int k = N - 1; k >= 0; k--) {
            // For level k, i and j range from -k to k.
            // Only draw the two outer square rings to avoid inner occluded cubes.
            for (int j = k; j >= -k; j--) {
                for (int i = k; i >= -k; i--) {
                    if (Math.max(Math.abs(i), Math.abs(j)) >= k - 1) {
                        double baseX = centerX + (i - j) * stepHor;
                        double baseY = baseYOffset + k * stepVerDown - (i + j) * stepVerUp;

                        addCubeShapes(shapes, baseX, baseY, w, h,
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
                                      double w, double h,
                                      CubeType type,
                                      Color topColor, Color rightColor, Color leftColor) {

        CubeFaces faces = createCubeFaces(baseX, baseY, w, h, type.isInterlocking);

        shapes.add(new ShapeWithColor(faces.top, topColor));
        shapes.add(new ShapeWithColor(faces.right, rightColor));
        shapes.add(new ShapeWithColor(faces.left, leftColor));

        // add smaller, transformed copies of the faces for a carved effect
        for (double s : type.cutScales) {
            // scaling around (baseX, baseY): x' = s*x + (1 - s)*baseX
            AffineTransform cutTransform = new AffineTransform(
                s, 0, 0, s, (1.0 - s) * baseX, (1.0 - s) * baseY);

            shapes.add(new ShapeWithColor(cutTransform.createTransformedShape(faces.top), topColor));
            shapes.add(new ShapeWithColor(cutTransform.createTransformedShape(faces.right), rightColor));
            shapes.add(new ShapeWithColor(cutTransform.createTransformedShape(faces.left), leftColor));
        }
    }

    /**
     * Creates the three visible faces of a single cube.
     */
    private static CubeFaces createCubeFaces(double baseX, double baseY, double w, double h, boolean interlocking) {
        if (interlocking) {
            return new CubeFaces(
                createInterlockingTop(baseX, baseY, w, h),
                createInterlockingRight(baseX, baseY, w, h),
                createInterlockingLeft(baseX, baseY, w, h)
            );
        }
        return new CubeFaces(
            createBasicTop(baseX, baseY, w, h),
            createBasicRight(baseX, baseY, w, h),
            createBasicLeft(baseX, baseY, w, h)
        );
    }

    /**
     * Creates the path for the top face of a basic cube.
     */
    private static Path2D createBasicTop(double x, double y, double w, double h) {
        double halfH = h * 0.5;
        Path2D top = new Path2D.Double(Path2D.WIND_NON_ZERO, 4);
        top.moveTo(x, y);
        top.lineTo(x - w, y - halfH);
        top.lineTo(x, y - h);
        top.lineTo(x + w, y - halfH);
        top.closePath();
        return top;
    }

    /**
     * Creates the path for the right face of a basic cube.
     */
    private static Path2D createBasicRight(double x, double y, double w, double h) {
        double halfH = h * 0.5;
        Path2D right = new Path2D.Double(Path2D.WIND_NON_ZERO, 4);
        right.moveTo(x, y);
        right.lineTo(x + w, y - halfH);
        right.lineTo(x + w, y + halfH);
        right.lineTo(x, y + h);
        right.closePath();
        return right;
    }

    /**
     * Creates the path for the left face of a basic cube.
     */
    private static Path2D createBasicLeft(double x, double y, double w, double h) {
        double halfH = h * 0.5;
        Path2D left = new Path2D.Double(Path2D.WIND_NON_ZERO, 4);
        left.moveTo(x, y);
        left.lineTo(x - w, y - halfH);
        left.lineTo(x - w, y + halfH);
        left.lineTo(x, y + h);
        left.closePath();
        return left;
    }

    /**
     * Creates the path for the top face of an interlocking cube.
     */
    private static Path2D createInterlockingTop(double x, double y, double w, double h) {
        double halfW = w * 0.5;
        double halfH = h * 0.5;
        double threeQuarterH = h * 0.75;

        Path2D top = new Path2D.Double(Path2D.WIND_NON_ZERO, 6);
        top.moveTo(x, y);
        top.lineTo(x - w, y - halfH);
        top.lineTo(x - halfW, y - threeQuarterH);
        top.lineTo(x, y - halfH);
        top.lineTo(x + halfW, y - threeQuarterH);
        top.lineTo(x + w, y - halfH);
        top.closePath();
        return top;
    }

    /**
     * Creates the path for the right face of an interlocking cube.
     */
    private static Path2D createInterlockingRight(double x, double y, double w, double h) {
        double halfW = w * 0.5;
        double halfH = h * 0.5;

        Path2D right = new Path2D.Double(Path2D.WIND_NON_ZERO, 6);
        right.moveTo(x, y);
        right.lineTo(x + w, y - halfH);
        right.lineTo(x + w, y);
        right.lineTo(x + halfW, y + h * 0.25);
        right.lineTo(x + halfW, y + h * 0.75);
        right.lineTo(x, y + h);
        right.closePath();
        return right;
    }

    /**
     * Creates the path for the left face of an interlocking cube.
     */
    private static Path2D createInterlockingLeft(double x, double y, double w, double h) {
        double halfW = w * 0.5;
        double halfH = h * 0.5;

        Path2D left = new Path2D.Double(Path2D.WIND_NON_ZERO, 6);
        left.moveTo(x, y);
        left.lineTo(x - w, y - halfH);
        left.lineTo(x - w, y);
        left.lineTo(x - halfW, y + h * 0.25);
        left.lineTo(x - halfW, y + h * 0.75);
        left.lineTo(x, y + h);
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
