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

package pixelitor.filters.jhlabsproxies;

import com.jhlabs.image.BlockFilter;
import pixelitor.colors.Colors;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.impl.BrickBlockFilter;
import pixelitor.filters.impl.HexagonBlockFilter;
import pixelitor.filters.impl.TriangleBlockFilter;
import pixelitor.gui.GUIText;
import pixelitor.utils.ImageUtils;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.HashSet;
import java.util.Set;

import static com.jhlabs.image.ImageMath.HALF_SQRT_3;
import static com.jhlabs.image.ImageMath.SQRT_3;
import static java.awt.Color.GRAY;
import static java.awt.Color.WHITE;

/**
 * Pixelate filter based on the JHLabs {@link BlockFilter}
 * (or alternatively on {@link BrickBlockFilter},
 * {@link TriangleBlockFilter}, or {@link HexagonBlockFilter}).
 */
public class JHPixelate extends ParametrizedFilter {
    public static final String NAME = "Pixelate";

    @Serial
    private static final long serialVersionUID = 152174018715974359L;

    private static final int STYLE_FLAT = 0;
    private static final int STYLE_3D = 1;
    private static final int STYLE_EMBEDDED = 2;

    private static final int TYPE_SQUARE = 0;
    private static final int TYPE_BRICK = 1;
    private static final int TYPE_TRIANGLE = 2;
    private static final int TYPE_HEXAGON = 3;

    private final IntChoiceParam typeParam = new IntChoiceParam(GUIText.TYPE, new Item[]{
        new Item("Squares", TYPE_SQUARE),
        new Item("Brick Wall", TYPE_BRICK),
        new Item("Triangles", TYPE_TRIANGLE),
        new Item("Hexagons", TYPE_HEXAGON),
    });

    private final IntChoiceParam styleParam = new IntChoiceParam("Style", new Item[]{
        new Item("Flat", STYLE_FLAT),
        new Item("3D", STYLE_3D),
        new Item("Embedded", STYLE_EMBEDDED),
    });

    private final RangeParam cellSizeParam = new RangeParam("Cell Size", 3, 20, 200);

    private BlockFilter blockFilter;
    private BrickBlockFilter brickBlockFilter;
    private TriangleBlockFilter triangleBlockFilter;
    private HexagonBlockFilter hexagonBlockFilter;

    public JHPixelate() {
        super(true);

        typeParam.setPresetKey("Type");

        initParams(
            cellSizeParam.withAdjustedRange(0.2),
            typeParam,
            styleParam
        );
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        int style = styleParam.getValue();
        int type = typeParam.getValue();

        int cellSize = cellSizeParam.getValue();

        if (style == STYLE_FLAT || style == STYLE_3D || style == STYLE_EMBEDDED) {
            switch (type) {
                case TYPE_SQUARE -> {
                    if (blockFilter == null) {
                        blockFilter = new BlockFilter(NAME);
                    }
                    blockFilter.setBlockSize(cellSize);
                    dest = blockFilter.filter(src, dest);
                }
                case TYPE_BRICK -> {
                    if (brickBlockFilter == null) {
                        brickBlockFilter = new BrickBlockFilter(NAME);
                    }
                    brickBlockFilter.setHorBlockSize(cellSize * 2);
                    brickBlockFilter.setVerBlockSize(cellSize);
                    dest = brickBlockFilter.filter(src, dest);
                }
                case TYPE_TRIANGLE -> {
                    if (triangleBlockFilter == null) {
                        triangleBlockFilter = new TriangleBlockFilter(NAME);
                    }
                    triangleBlockFilter.setSize(cellSize);
                    dest = triangleBlockFilter.filter(src, dest);
                }
                case TYPE_HEXAGON -> {
                    if (hexagonBlockFilter == null) {
                        hexagonBlockFilter = new HexagonBlockFilter(NAME);
                    }
                    hexagonBlockFilter.setSize(cellSize);
                    dest = hexagonBlockFilter.filter(src, dest);
                }
            }
        }

        if (style == STYLE_3D || style == STYLE_EMBEDDED) {
            int width = dest.getWidth();
            int height = dest.getHeight();

            BufferedImage bumpSource = (style == STYLE_EMBEDDED)
                ? dest
                : createBumpSource(type, cellSize, width, height, src);
            dest = ImageUtils.bumpMap(dest, bumpSource, NAME);
        }

        return dest;
    }

    private static BufferedImage createBumpSource(int type, int cellSize, int width, int height, BufferedImage src) {
        BufferedImage bumpSource = ImageUtils.createImageWithSameCM(src);

        Graphics2D g = bumpSource.createGraphics();
        Colors.fillWith(WHITE, g, width, height);

//        int gapWidth = (cellSize < 15) ? 1 : 2;
        int gapWidth = 1;
        if (gapWidth != 1) {
            g.setStroke(new BasicStroke(gapWidth));
        }
        g.setColor(GRAY);

        switch (type) {
            case TYPE_SQUARE -> renderGrid(g, gapWidth, cellSize, width, height);
            case TYPE_BRICK -> renderBrickGrid(g, cellSize, width, height);
            case TYPE_TRIANGLE -> renderTriangleGrid(g, cellSize, width, height);
            case TYPE_HEXAGON -> renderHexagonGrid(g, cellSize, width, height);
            default -> throw new IllegalStateException("Unexpected value: " + type);
        }

        g.dispose();
        return bumpSource;
    }

    @Override
    public boolean supportsGray() {
        return false;
    }

    public static void renderGrid(Graphics2D g,
                                  int lineWidth, int spacing,
                                  int width, int height) {
        assert lineWidth > 0;
        assert spacing > 0;

        // horizontal lines
        int halfLineThickness = lineWidth / 2;
        for (int y = 0; y < height; y += spacing) {
            int startY = y - halfLineThickness;
            //noinspection SuspiciousNameCombination
            g.fillRect(0, startY, width, lineWidth);
        }

        // vertical lines
        for (int x = 0; x < width; x += spacing) {
            g.fillRect(x - halfLineThickness, 0, lineWidth, height);
        }
    }

    public static void renderBrickGrid(Graphics2D g, int brickHeight,
                                       int width, int height) {
        if (brickHeight < 1) {
            throw new IllegalArgumentException("brickHeight = " + brickHeight);
        }

        int brickWidth = brickHeight * 2;
        int currentY = 0;
        int rowCount = 0;

        while (currentY <= height) {
            // horizontal lines
            g.drawLine(0, currentY, width, currentY);

            // only draw vertical lines if we're not at the last line
            if (currentY < height) {
                // vertical lines
                int horOffset = ((rowCount % 2) == 1) ? brickHeight : 0;
                for (int x = horOffset; x < width; x += brickWidth) {
                    g.drawLine(x, currentY, x, Math.min(currentY + brickHeight, height));
                }
            }

            currentY += brickHeight;
            rowCount++;
        }
    }

    public static void renderTriangleGrid(Graphics2D g, int size,
                                          int width, int height) {
        double halfSize = size / 2.0;
        double triangleHeight = size * HALF_SQRT_3;
        double tan30 = halfSize / triangleHeight;
        double cotan30 = triangleHeight / halfSize;

        // horizontal lines
        double currentY = triangleHeight;
        while (currentY < height) {
            g.draw(new Line2D.Double(0, currentY, width, currentY));
            currentY += triangleHeight;
        }

        // slanted lines downwards to the right
        // starting from the top edge (startX = 0)
        for (double startY = 0; startY <= height; startY += 2 * triangleHeight) {
            double startX = 0;

            double endX = width;
            double endY = startY + width * cotan30;

            // ensure the end point stays within bounds
            if (endY > height) {
                endX = (height - startY) * tan30;
                endY = height;
            }

            g.draw(new Line2D.Double(startX, startY, endX, endY));
        }

        // slanted lines downwards to the right
        // starting from the left edge (startY = 0)
        for (double x = 0; x <= width; x += size) {
            double startX = x;
            double startY = 0;

            double endX = x + height * tan30;
            double endY = height;

            // ensure the end point stays within bounds
            if (endX > width) {
                endY = height - (endX - width) * cotan30;
                endX = width;
            }

            g.draw(new Line2D.Double(startX, startY, endX, endY));
        }

        // slanted lines upwards to the right
        // starting from the top edge (startY = 0)
        for (double x = size; x <= width; x += size) {
            double startX = x;
            double startY = 0;

            double endX = x - height * tan30;
            double endY = height;

            // ensure the end point stays within bounds
            if (endX < 0) {
                endY = height + endX * cotan30;
                endX = 0;
            }

            g.draw(new Line2D.Double(startX, startY, endX, endY));
        }

        // slanted lines upwards to the right
        // starting from the right edge (startX = width)
        int numHorTriangles = width / size;
        int xOffset = (numHorTriangles + 1) * size - width;
        double yOffset = xOffset * cotan30;
        for (double startY = yOffset; startY <= height; startY += 2 * triangleHeight) {
            double startX = width;

            double endX = width - (height - startY) * tan30;
            double endY = height;

            // ensure the end point stays within bounds
            if (endX < 0) {
                endY = startY + width * cotan30;
                endX = 0;
            }

            g.draw(new Line2D.Double(startX, startY, endX, endY));
        }
    }

    // Converts pixel (x, y) to axial (q, r) assuming a flat top hexagon
    private static Point2D.Double pixelToAxial(double x, double y, double s) {
        double q = (2.0 / 3.0 * x) / s;
        double r = (-1.0 / 3.0 * x + SQRT_3 / 3.0 * y) / s;
        return new Point2D.Double(q, r);
    }

    /**
     * Renders a grid of hexagons matching the HexagonBlockFilter structure.
     */
    public static void renderHexagonGrid(Graphics2D g, int size,
                                         int width, int height) {
        // Axial coordinates that uniquely identify a hexagon.
        record AxialCoord(int q, int r) implements Comparable<AxialCoord> {
            @Override
            public int compareTo(AxialCoord other) {
                if (this.q != other.q) {
                    return Integer.compare(this.q, other.q);
                }
                return Integer.compare(this.r, other.r);
            }
        }

        // An edge is uniquely defined by the two hexagons it separates.
        record EdgeKey(AxialCoord c1, AxialCoord c2) {
            EdgeKey(AxialCoord c1, AxialCoord c2) {
                // ensure that the edge key is independent of the order of the hexagons
                if (c1.compareTo(c2) < 0) {
                    this.c1 = c1;
                    this.c2 = c2;
                } else {
                    this.c1 = c2;
                    this.c2 = c1;
                }
            }
        }

        double s = size;
        if (s <= 0) {
            return;
        }

        // vertical distance from center to horizontal sides
        double hexHeight = s * HALF_SQRT_3;

        // horizontal distance from center to vertical sides
        double hexWidth = s * 1.5;

        // determine iteration range based on filter's coordinate system
        double W = width;
        double H = height;
        // a buffer to catch hexagons overlapping the edges
        double buffer = s * 1.1;

        // check axial coordinates for the buffered bounding box corners
        Point2D.Double tl = pixelToAxial(-buffer, -buffer, s);
        Point2D.Double tr = pixelToAxial(W + buffer, -buffer, s);
        Point2D.Double bl = pixelToAxial(-buffer, H + buffer, s);
        Point2D.Double br = pixelToAxial(W + buffer, H + buffer, s);

        // the integer iteration range based on the min/max axial coordinates
        int qMin = (int) Math.floor(Math.min(tl.x, bl.x));
        int qMax = (int) Math.ceil(Math.max(tr.x, br.x));
        int rMin = (int) Math.floor(Math.min(tl.y, tr.y));
        int rMax = (int) Math.ceil(Math.max(bl.y, br.y));

        // the set of edges to avoid drawing the same edge twice
        Set<EdgeKey> drawnEdges = new HashSet<>();

        for (int q = qMin; q <= qMax; q++) {
            for (int r = rMin; r <= rMax; r++) {
                // center (cx, cy) for axial coordinates (q, r) in a flat top hexagon
                double cx = hexWidth * q;
                double cy = hexHeight * q + 2.0 * hexHeight * r;

                // calculate the bounding box of this hexagon
                double hexMinX = cx - s;
                double hexMaxX = cx + s;
                double hexMinY = cy - hexHeight;
                double hexMaxY = cy + hexHeight;

                // skip hexagon if its bounding box is entirely outside the canvas
                if (hexMaxX <= 0 || hexMinX >= W || hexMaxY <= 0 || hexMinY >= H) {
                    continue;
                }

                // current hexagon's axial coordinate
                AxialCoord currentCoord = new AxialCoord(q, r);

                // the vertices for the flat-top hexagon centered at (cx, cy)
                Point2D.Double[] vertices = {
                    new Point2D.Double(cx + s, cy),                   // right
                    new Point2D.Double(cx + s / 2.0, cy + hexHeight), // top-right
                    new Point2D.Double(cx - s / 2.0, cy + hexHeight), // top-left
                    new Point2D.Double(cx - s, cy),                   // left
                    new Point2D.Double(cx - s / 2.0, cy - hexHeight), // bottom-left
                    new Point2D.Double(cx + s / 2.0, cy - hexHeight), // bottom-right
                };

                // Define neighbors based on edge index for flat-top grid
                // Matches edge indices to neighbor axial coordinate offsets
                AxialCoord[] neighbors = {
                    new AxialCoord(q + 1, r),     // neighbor for edge 0 (vertices 0-1)
                    new AxialCoord(q, r + 1),     // neighbor for edge 1 (vertices 1-2)
                    new AxialCoord(q - 1, r + 1), // neighbor for edge 2 (vertices 2-3)
                    new AxialCoord(q - 1, r),     // neighbor for edge 3 (vertices 3-4)
                    new AxialCoord(q, r - 1),     // neighbor for edge 4 (vertices 4-5)
                    new AxialCoord(q + 1, r - 1)  // neighbor for edge 5 (vertices 5-0)
                };

                // draw the edges of the hexagon if they haven't been drawn
                for (int i = 0; i < 6; i++) {
                    Point2D p1 = vertices[i];
                    Point2D p2 = vertices[(i + 1) % 6];

                    // create the edge key using the current and neighbor axial coordinates
                    EdgeKey edgeKey = new EdgeKey(currentCoord, neighbors[i]);

                    // draw it if the key was not already present
                    if (drawnEdges.add(edgeKey)) {
                        g.draw(new Line2D.Double(p1, p2));
                    }
                }
            }
        }
    }
}
