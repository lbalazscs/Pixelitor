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
 * The history of {@link ConnectBrush}. Not just for undo/redo,
 * the drawing is also based on connecting with older points.
 */
public class ConnectBrushHistory {
    // a brush stroke is a list of points, and the history is a list of strokes
    private static final List<List<Point2D>> history = new ArrayList<>();
    private static List<Point2D> currentStroke;
    private static int numPoints;

    // index for the next stroke to be added (for undo/redo)
    private static int nextAddIndex = 0;

    private ConnectBrushHistory() {
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

    public static void drawConnectingLines(Graphics2D targetG,
                                           ConnectBrushSettings settings,
                                           PPoint currentPoint, double diamSq) {
        if (history.isEmpty()) {
            nextAddIndex = 0;
            return;
        }

        Point2D last = currentPoint.toImPoint2D();
        currentStroke.add(last);
        numPoints++;

        if (numPoints <= 2) {
            return; // not enough points to connect
        }
        int baseColor = targetG.getColor().getRGB();
        int baseColorNoAlpha = baseColor & 0x00_FF_FF_FF;

        double offset = settings.getStyle().getOffset();
        double density = settings.getDensity();

        Line2D line = new Line2D.Double();

        // randomly connect with nearby old points
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int i = 0; i < nextAddIndex; i++) {
            List<Point2D> stroke = history.get(i);
            for (Point2D old : stroke) {
                double dx = old.getX() - last.getX();
                double dy = old.getY() - last.getY();
                double distSq = dx * dx + dy * dy;
                if (distSq < diamSq && distSq > 0 && density > rnd.nextFloat()) {
                    // calculate the line opacity based on distance
                    int alpha = (int) Math.min(255.0, 10000 / distSq);

                    // combine the opacity with the base color
                    int lineColor = alpha << 24 | baseColorNoAlpha;

                    targetG.setColor(new Color(lineColor, true));
                    double xOffset = dx * offset;
                    double yOffset = dy * offset;
                    line.setLine(last.getX() - xOffset, last.getY() - yOffset,
                        old.getX() + xOffset, old.getY() + yOffset);

                    targetG.draw(line);
                }
            }
        }
    }

    public static void clear() {
        history.clear();
        nextAddIndex = 0;
        numPoints = 0;
    }

    private static void undo() {
        if (nextAddIndex > 0) {
            nextAddIndex--; // move back one step in the history
        }
    }

    private static void redo() {
        nextAddIndex++; // move forward one step in the history
    }

    public static class Edit extends PixelitorEdit {
        public Edit(Composition comp) {
            super("Connect Brush History", comp);
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
