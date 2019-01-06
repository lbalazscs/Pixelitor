/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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

import java.awt.Color;
import java.awt.Stroke;

/**
 * Guide style consisting of colors and a {@link GuideStrokeType}
 */
public final class GuideStyle {
    private GuideStrokeType strokeType = GuideStrokeType.DASHED;
    private Color colorA = Color.BLACK;
    private Color colorB = Color.WHITE; // not used!

    public GuideStrokeType getStrokeType() {
        return strokeType;
    }

    public void setStrokeType(GuideStrokeType strokeType) {
        this.strokeType = strokeType;
    }

    public Stroke getStrokeA() {
        return strokeType.getStrokeA();
    }

    public Stroke getStrokeB() {
        return strokeType.getStrokeB();
    }

    public Color getColorA() {
        return colorA;
    }

    public void setColorA(Color colorA) {
        this.colorA = colorA;
    }

    public Color getColorB() {
        return colorB;
    }

    public void setColorB(Color colorB) {
        this.colorB = colorB;
    }
}
