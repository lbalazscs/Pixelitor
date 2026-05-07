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

package pixelitor.guides;

import java.awt.Color;
import java.awt.Stroke;

/**
 * The style of a guide, consisting of colors and a {@link GuideStrokeType}.
 */
public final class GuideStyle {
    private GuideStrokeType strokeType = GuideStrokeType.DASHED;
    private Color primaryColor = Color.BLACK;

    // used only for some styles, currently not configurable via UI
    private Color secondaryColor = Color.WHITE;

    public GuideStrokeType getStrokeType() {
        return strokeType;
    }

    public void setStrokeType(GuideStrokeType strokeType) {
        this.strokeType = strokeType;
    }

    public Stroke getPrimaryStroke() {
        return strokeType.getPrimaryStroke();
    }

    public Stroke getSecondaryStroke() {
        return strokeType.getSecondaryStroke();
    }

    public Color getPrimaryColor() {
        return primaryColor;
    }

    public void setPrimaryColor(Color primaryColor) {
        this.primaryColor = primaryColor;
    }

    public Color getSecondaryColor() {
        return secondaryColor;
    }

    public void setSecondaryColor(Color secondaryColor) {
        this.secondaryColor = secondaryColor;
    }
}
