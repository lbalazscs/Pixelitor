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

package pixelitor.guides;

import java.awt.BasicStroke;
import java.awt.Stroke;

import static java.awt.BasicStroke.CAP_BUTT;
import static java.awt.BasicStroke.JOIN_BEVEL;

/**
 * Stroke types for guides.
 */
public enum GuideStrokeType {
    SOLID("Solid",
        new BasicStroke(1),
        null
    ),
    DOTTED("Dotted",
        new BasicStroke(1, CAP_BUTT, JOIN_BEVEL, 0, new float[]{1, 2}, 0),
        null
    ),
    DASHED("Dashed",
        new BasicStroke(1, CAP_BUTT, JOIN_BEVEL, 0, new float[]{5, 2}, 0),
        null
    ),
    DASHED_DOUBLE("Dashed with Background",
        new BasicStroke(1, CAP_BUTT, JOIN_BEVEL, 0, new float[]{5.0f, 2.0f}, 0),
        new BasicStroke(1, CAP_BUTT, JOIN_BEVEL, 0, new float[]{2.0f, 5.0f}, 2)
    ),
    DASHED_BORDERED("Dashed with Border",
        new BasicStroke(3),
        new BasicStroke(1, CAP_BUTT, JOIN_BEVEL, 0, new float[]{5, 2}, 0)
    );

    private final String displayName;
    private final Stroke strokeA;
    private final Stroke strokeB;

    GuideStrokeType(String displayName, Stroke strokeA, Stroke strokeB) {
        this.displayName = displayName;
        this.strokeA = strokeA;
        this.strokeB = strokeB;
    }

    public Stroke getStrokeA() {
        return strokeA;
    }

    public Stroke getStrokeB() {
        return strokeB;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
