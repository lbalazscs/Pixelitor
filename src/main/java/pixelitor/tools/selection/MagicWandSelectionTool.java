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

package pixelitor.tools.selection;

import pixelitor.Composition;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.UserPreset;
import pixelitor.gui.View;
import pixelitor.gui.utils.SliderSpinner;
import pixelitor.selection.SelectionType;
import pixelitor.tools.ToolIcons;
import pixelitor.tools.Tools;
import pixelitor.tools.util.OverlayType;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.Cursors;
import pixelitor.utils.ImageUtils;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.function.Consumer;

import static pixelitor.gui.utils.SliderSpinner.LabelPosition.WEST;

/**
 * A tool that creates selections based on color similarity by clicking.
 */
public class MagicWandSelectionTool extends AbstractSelectionTool {
    private static final String TOLERANCE_TEXT = "Tolerance";

    private final RangeParam toleranceParam = new RangeParam("Tolerance", 0, 20, 255);
    private final SliderSpinner toleranceSlider = new SliderSpinner(toleranceParam, WEST, false);

    public MagicWandSelectionTool() {
        super("Magic Wand Selection", 'W',
            "<b>click</b> on the area you want to select. " +
                "<b>right-click</b> to cancel the selection.",
            Cursors.DEFAULT, false);
        repositionOnSpace = false;
        pixelSnapping = false;
    }

    @Override
    public void initSettingsPanel(ResourceBundle resources) {
        super.initSettingsPanel(resources);

        settingsPanel.add(toleranceSlider);
    }

    @Override
    protected void dragStarted(PMouseEvent e) {
        // ignored, magic wand is click-based
    }

    @Override
    protected void ongoingDrag(PMouseEvent e) {
        // ignored, magic wand is click-based
    }

    @Override
    protected void dragFinished(PMouseEvent e) {
        // ignored, magic wand is click-based
    }

    @Override
    public void mouseClicked(PMouseEvent e) {
        Composition comp = e.getComp();
        initCombinatorAndBuilder(e, SelectionType.MAGIC_WAND);

        if (e.isRight()) {
            // right-click always cancels
            cancelSelection(comp);
        } else if (selectionBuilder != null && e.getClickCount() == 1) {
            try {
                // calculate the selection shape based on the click event
                selectionBuilder.updateDraftSelection(e);
                // combine the new shape with any existing selection
                selectionBuilder.combineShapes();

                // show the final selection
                View view = comp.getView();
                if (view != null) {
                    view.repaint();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                cancelSelection(comp);
            } finally {
                // clean up the builder and combinator
                cancelSelectionBuilder();
                resetCombinator();
            }
        }
    }

    @Override
    protected OverlayType getOverlayType() {
        // no overlay needed for a click-based tool
        return OverlayType.NONE;
    }

    public int getTolerance() {
        return toleranceParam.getValue();
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        super.saveStateTo(preset);

        preset.putInt(TOLERANCE_TEXT, getTolerance());
    }

    @Override
    public void loadUserPreset(UserPreset preset) {
        super.loadUserPreset(preset);

        toleranceParam.setValue(preset.getInt(TOLERANCE_TEXT, 20));
    }

    /**
     * Creates a "Magic Wand" selection path, based on the algorithm described at
     * https://losingfight.com/blog/2007/08/28/how-to-implement-a-magic-wand-tool/
     */
    public static Path2D createSelectionPath(PMouseEvent pm) {
        Composition comp = pm.getComp();
        // the Magic Wand operates on the composite image
        BufferedImage image = comp.getCompositeImage();

        int width = image.getWidth();
        int height = image.getHeight();

        int x = (int) pm.getImX();
        int y = (int) pm.getImY();

        // return an empty shape if the click is outside the image bounds
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return new Path2D.Double();
        }

        int[] pixels = ImageUtils.getPixels(image);
        int tolerance = Tools.MAGIC_WAND.getTolerance();

        // select pixels using flood-fill
        boolean[] mask = new boolean[width * height];
        ImageUtils.floodFill(pixels, width, height, x, y, tolerance,
            // mark the pixels in the segment as true in the mask
            (segY, segX1, segX2) -> {
                int offset = segY * width;
                for (int i = segX1; i <= segX2; i++) {
                    mask[offset + i] = true;
                }
            });

        // convert the selection mask into a vector path
        return createPathFromMask(mask, width, height);
    }

    /**
     * Converts a boolean mask into a {@link Path2D} outline.
     */
    private static Path2D createPathFromMask(boolean[] mask, int width, int height) {
        Path2D.Double path = new Path2D.Double();
        Map<Point, List<Line2D.Double>> edgeMap = new HashMap<>();

        // find all edge segments by checking neighbors
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (mask[y * width + x]) {
                    // check top neighbor
                    if (y == 0 || !mask[(y - 1) * width + x]) {
                        addEdge(edgeMap, new Point(x, y), new Point(x + 1, y));
                    }
                    // check bottom neighbor
                    if (y == height - 1 || !mask[(y + 1) * width + x]) {
                        addEdge(edgeMap, new Point(x, y + 1), new Point(x + 1, y + 1));
                    }
                    // check left neighbor
                    if (x == 0 || !mask[y * width + x - 1]) {
                        addEdge(edgeMap, new Point(x, y), new Point(x, y + 1));
                    }
                    // check right neighbor
                    if (x == width - 1 || !mask[y * width + x + 1]) {
                        addEdge(edgeMap, new Point(x + 1, y), new Point(x + 1, y + 1));
                    }
                }
            }
        }

        // connect the edge segments to form closed paths
        while (!edgeMap.isEmpty()) {
            Point startPoint = edgeMap.keySet().iterator().next();
            List<Line2D.Double> segments = edgeMap.get(startPoint);
            Line2D.Double currentLine = segments.getFirst();

            path.moveTo(startPoint.x, startPoint.y);
            Point currentPoint = startPoint;

            while (true) {
                Point nextPoint = getOtherEndpoint(currentLine, currentPoint);
                path.lineTo(nextPoint.x, nextPoint.y);

                // remove the used line segment from the map
                removeLine(edgeMap, currentLine, currentPoint);
                removeLine(edgeMap, currentLine, nextPoint);

                currentPoint = nextPoint;

                // find the next connected segment
                List<Line2D.Double> nextSegments = edgeMap.get(currentPoint);
                if (nextSegments == null || nextSegments.isEmpty()) {
                    break; // path is complete
                }
                currentLine = nextSegments.getFirst();
            }
            path.closePath();
        }
        return path;
    }

    /**
     * Helper to add a line segment to the edge map.
     */
    private static void addEdge(Map<Point, List<Line2D.Double>> edgeMap, Point p1, Point p2) {
        Line2D.Double line = new Line2D.Double(p1, p2);
        edgeMap.computeIfAbsent(p1, k -> new ArrayList<>()).add(line);
        edgeMap.computeIfAbsent(p2, k -> new ArrayList<>()).add(line);
    }

    /**
     * Helper to get the other endpoint of a line, given one endpoint.
     */
    private static Point getOtherEndpoint(Line2D.Double line, Point p) {
        Point2D p1 = line.getP1();
        Point2D p2 = line.getP2();
        if (p1.getX() == p.x && p1.getY() == p.y) {
            // p matches p1, so return p2
            return new Point((int) p2.getX(), (int) p2.getY());
        } else {
            // p must match p2, so return p1
            return new Point((int) p1.getX(), (int) p1.getY());
        }
    }

    /**
     * Helper to remove a line segment from the map for a specific point.
     */
    private static void removeLine(Map<Point, List<Line2D.Double>> edgeMap, Line2D.Double line, Point p) {
        List<Line2D.Double> segments = edgeMap.get(p);
        if (segments != null) {
            segments.remove(line);
            if (segments.isEmpty()) {
                edgeMap.remove(p);
            }
        }
    }

    @Override
    public Consumer<Graphics2D> createIconPainter() {
        return ToolIcons::paintMagicWandSelectionIcon;
    }
}
