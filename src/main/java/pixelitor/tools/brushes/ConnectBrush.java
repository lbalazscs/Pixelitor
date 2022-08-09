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

import pixelitor.layers.Drawable;
import pixelitor.tools.util.PPoint;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Line2D;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;

/**
 * "History Connect" brushes based on ideas from "project harmony", see
 * https://github.com/mrdoob/harmony.
 * Also see issue #11
 */
public class ConnectBrush extends AbstractBrush {
    private final ConnectBrushSettings settings;
    private double diamSq;

    public ConnectBrush(ConnectBrushSettings settings, double radius) {
        super(radius);
        this.settings = settings;
    }

    @Override
    public void setRadius(double radius) {
        super.setRadius(radius);
        diamSq = 4 * radius * radius;
    }

    @Override
    public void setTarget(Drawable dr, Graphics2D g) {
        if (dr != this.dr) {
            deleteHistory();
        }
        super.setTarget(dr, g);
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
    }

    @Override
    public void startAt(PPoint p) {
        super.startAt(p);

        if (settings.deleteHistoryForEachStroke()) {
            deleteHistory();
        }
        ConnectBrushHistory.startNewBrushStroke(p);
    }

    @Override
    public void continueTo(PPoint p) {
        float lineWidth = settings.getLineWidth();
        var line = new Line2D.Double();
        Stroke stroke = new BasicStroke(lineWidth);
        targetG.setStroke(stroke);

        line.setLine(p.getImX(), p.getImY(), previous.getImX(), previous.getImY());
        targetG.draw(line);

        ConnectBrushHistory.drawConnectingLines(targetG, settings, p, diamSq);

        repaintComp(p);
        rememberPrevious(p);
    }

    @Override
    public void dispose() {
        deleteHistory();
    }

    @Override
    public double getPreferredSpacing() {
        return 0;
    }

    public static void deleteHistory() {
        ConnectBrushHistory.clear();
    }
}
