/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.tools.util.PPoint;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;

/**
 * "History Connect" brushes based on ideas from "project harmony", see
 * https://github.com/mrdoob/harmony
 * https://github.com/lbalazscs/Pixelitor/issues/11
 */
public class ConnectBrush extends AbstractBrush {
    private final List<HistoryPoint> history = new ArrayList<>();
    private final ConnectBrushSettings settings;
    private int diamSq;

    public ConnectBrush(ConnectBrushSettings settings, int radius) {
        super(radius);
        this.settings = settings;
        settings.setBrush(this);
    }

    @Override
    public void setRadius(int radius) {
        super.setRadius(radius);
        diamSq = 4 * radius * radius;
    }

    @Override
    public void setTarget(Composition comp, Graphics2D g) {
        if (comp != this.comp) {
            deleteHistory();
        }
        super.setTarget(comp, g);
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
    }

    @Override
    public void onStrokeStart(PPoint p) {
        super.onStrokeStart(p);

        if (settings.deleteHistoryForEachStroke()) {
            deleteHistory();
        }
        history.add(new HistoryPoint(p.getImX(), p.getImY()));
    }

    @Override
    public void onNewStrokePoint(PPoint p) {
        int currentColor = targetG.getColor().getRGB();
        int currentColorZeroAlpha = currentColor & 0x00FFFFFF;

        HistoryPoint last = new HistoryPoint(p.getImX(), p.getImY());
        history.add(last);
        Line2D.Double line = new Line2D.Double();

        setupLineWidth();

        if (history.size() > 2) {
            line.setLine(p.getImX(), p.getImY(), previous.getImX(), previous.getImY());
            targetG.draw(line);

            double offSet = settings.getStyle().getOffset();
            double density = settings.getDensity();

            for (int i = history.size() - 3; i >= 0; i--) {
                HistoryPoint old = history.get(i);
                double dx = old.x - last.x;
                double dy = old.y - last.y;
                double distSq = dx * dx + dy * dy;
                if (distSq < diamSq && distSq > 0 && density > Math.random()) {
                    int alpha = (int) Math.min(255.0, 10000 / distSq);

                    int rgbWithAlpha = (alpha << 24) | currentColorZeroAlpha;

                    targetG.setColor(new Color(rgbWithAlpha, true));
                    double xOffset = dx * offSet;
                    double yOffset = dy * offSet;
                    line.setLine(last.x - xOffset, last.y - yOffset,
                            old.x + xOffset, old.y + yOffset);

                    targetG.draw(line);
                }
            }
        }
        updateComp(p);
        rememberPrevious(p);
    }

    private void setupLineWidth() {
        float lineWidth = settings.getLineWidth();
        Stroke stroke = new BasicStroke(lineWidth);
        targetG.setStroke(stroke);
    }

    @Override
    public void dispose() {
        deleteHistory();
    }

    @Override
    public double getPreferredSpacing() {
        return 0;
    }

    public void deleteHistory() {
        history.clear();
    }

    private static class HistoryPoint {
        double x, y;

        HistoryPoint(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }
}
