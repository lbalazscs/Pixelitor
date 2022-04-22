/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The history of {@link ConnectBrush}. Not just for undo/redo,
 * the drawing is also based on connecting with older points.
 */
public class ConnectBrushHistory {
    // a brush stroke is a list of points, and the history is a list of strokes
    private static final List<List<HistoryPoint>> history = new ArrayList<>();
    private static List<HistoryPoint> lastStroke;
    private static int numPoints;
    private static int indexOfNextAdd = 0;

    private ConnectBrushHistory() {
    }

    public static void startNewBrushStroke(PPoint p) {
        lastStroke = new ArrayList<>();

        // the entries after indexOfNextAdd will never be
        // redone, they can be discarded
        if (history.size() > indexOfNextAdd) {
            history.subList(indexOfNextAdd, history.size()).clear();
        }

        assert history.size() == indexOfNextAdd;
        history.add(lastStroke);
        indexOfNextAdd++;

        lastStroke.add(new HistoryPoint(p.getImX(), p.getImY()));
        numPoints++;
    }

    public static void drawConnectingLines(Graphics2D targetG,
                                           ConnectBrushSettings settings,
                                           PPoint p, double diamSq) {
        if (history.isEmpty()) {
            indexOfNextAdd = 0;
            return;
        }

        HistoryPoint last = new HistoryPoint(p.getImX(), p.getImY());
        lastStroke.add(last);
        numPoints++;

        if (numPoints > 2) {
            int currentColor = targetG.getColor().getRGB();
            int currentColorZeroAlpha = currentColor & 0x00_FF_FF_FF;

            double offSet = settings.getStyle().getOffset();
            double density = settings.getDensity();

            var line = new Line2D.Double();

            // randomly connect with nearby old points
            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            for (int i = 0; i < indexOfNextAdd; i++) {
                List<HistoryPoint> stroke = history.get(i);
                for (HistoryPoint old : stroke) {
                    double dx = old.x - last.x;
                    double dy = old.y - last.y;
                    double distSq = dx * dx + dy * dy;
                    if (distSq < diamSq && distSq > 0 && density > rnd.nextFloat()) {
                        int alpha = (int) Math.min(255.0, 10000 / distSq);

                        int lineColor = alpha << 24 | currentColorZeroAlpha;

                        targetG.setColor(new Color(lineColor, true));
                        double xOffset = dx * offSet;
                        double yOffset = dy * offSet;
                        line.setLine(last.x - xOffset, last.y - yOffset,
                            old.x + xOffset, old.y + yOffset);

                        targetG.draw(line);
                    }
                }
            }
        }
    }

    public static void clear() {
        history.clear();
        indexOfNextAdd = 0;
        numPoints = 0;
    }

    private static void undo() {
        if (indexOfNextAdd > 0) {
            indexOfNextAdd--;
        }
    }

    private static void redo() {
        indexOfNextAdd++;
    }

    private record HistoryPoint(double x, double y) {
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
