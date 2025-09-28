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

package pixelitor.tools.brushes;

import pixelitor.Composition;
import pixelitor.history.PixelitorEdit;
import pixelitor.tools.util.PPoint;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The global history of points drawn by {@link ConnectBrush}.
 * Used not only for undo/redo, but also to draw connecting lines
 * between new points and older points.
 * Being global is useful in the case of symmetry brushes.
 */
public class ConnectBrushHistory {
    // factor influencing distance-based alpha (higher value = faster fade)
    private static final double ALPHA_DISTANCE_FACTOR = 10000.0;

    // stores all strokes; each stroke is a list of points
    private static final List<List<Point2D>> history = new ArrayList<>();

    // the stroke currently being drawn
    private static List<Point2D> currentStroke;

    // total number of points in the history
    private static int numPoints;

    // Index for the next stroke to be added (for undo/redo).
    // This is the number of active strokes in the history.
    // Points beyond this index are for redo and are ignored for drawing.
    private static int nextAddIndex = 0;

    private ConnectBrushHistory() {
        // prevent instantiation
    }

    public static void startNewBrushStroke(PPoint p) {
        currentStroke = new ArrayList<>();

        // remove redoable strokes if any exist beyond the current index
        if (history.size() > nextAddIndex) {
            history.subList(nextAddIndex, history.size()).clear();
        }
        assert history.size() == nextAddIndex;

        history.add(currentStroke);
        nextAddIndex++;

        currentStroke.add(p.toImPoint2D());
        numPoints++;
    }

    /**
     * Adds a point to the current stroke and draws lines connecting it to nearby historical points.
     */
    public static void drawConnectingLines(Graphics2D targetG,
                                           ConnectBrushSettings settings,
                                           PPoint currentPoint, double diamSq) {
        if (history.isEmpty()) {
            nextAddIndex = 0;
            return;
        }
        assert currentStroke != null;

        // add the current point to the ongoing stroke
        Point2D last = currentPoint.toImPoint2D();
        currentStroke.add(last);
        numPoints++;

        if (numPoints <= 2) {
            return; // not enough points to connect
        }
        int baseColorRgb = targetG.getColor().getRGB();
        int baseColorNoAlpha = baseColorRgb & 0x00_FF_FF_FF;

        double offsetFactor = settings.getStyle().getOffsetFactor();
        double density = settings.getDensity();

        // reuse line object for efficiency
        Line2D line = new Line2D.Double();

        // randomly connect with nearby old points
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int i = 0; i < nextAddIndex; i++) {
            List<Point2D> stroke = history.get(i);
            for (Point2D old : stroke) {
                double dx = old.getX() - last.getX();
                double dy = old.getY() - last.getY();
                double distSq = dx * dx + dy * dy;

                // connect if: within diameter, not the same point, and passes density check
                if (distSq < diamSq && distSq > 0 && density > rnd.nextFloat()) {
                    // calculate alpha based on inverse squared distance, clamped to 0-255
                    // closer points result in more opaque lines
                    int alpha = (int) Math.min(255.0, ALPHA_DISTANCE_FACTOR / distSq);

                    // combine calculated alpha with the original color's RGB
                    int lineColorRgba = (alpha << 24) | baseColorNoAlpha;
                    targetG.setColor(new Color(lineColorRgba, true));

                    double xOffset = dx * offsetFactor;
                    double yOffset = dy * offsetFactor;
                    line.setLine(
                        last.getX() - xOffset,
                        last.getY() - yOffset,
                        old.getX() + xOffset,
                        old.getY() + yOffset);

                    targetG.draw(line);
                }
            }
        }
    }

    public static void clear() {
        history.clear();
        currentStroke = null;
        nextAddIndex = 0;
        numPoints = 0;
    }

    private static void undo() {
        if (nextAddIndex > 0) {
            nextAddIndex--; // move back one step in the history
        }
        recalculateNumPoints();
    }

    private static void redo() {
        // don't advance nextAddIndex if there is no corresponding stroke
        // in the history list (because the history was cleared before redo)
        if (nextAddIndex < history.size()) {
            nextAddIndex++; // move forward one step in the history
        }
        recalculateNumPoints();
    }

    /**
     * Recalculates the total number of points across all active strokes.
     */
    private static void recalculateNumPoints() {
        numPoints = 0;
        for (int i = 0; i < nextAddIndex; i++) {
            numPoints += history.get(i).size();
        }
    }

    /**
     * Integrates this history with the application's history.
     */
    public static class Edit extends PixelitorEdit {
        public Edit(Composition comp) {
            super("Connect Brush Stroke", comp);
        }

        @Override
        public void undo() throws CannotUndoException {
            super.undo();
            ConnectBrushHistory.undo();
        }

        @Override
        public void redo() throws CannotRedoException {
            super.redo();
            ConnectBrushHistory.redo();
        }
    }
}
