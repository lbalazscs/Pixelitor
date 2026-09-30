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

package pixelitor.tools.brushes;

/**
 * Defines the edge style (soft or hard) of a {@link CopyBrush} image.
 */
public enum CopyBrushType {
    SOFT("Soft") {
        @Override
        EdgeStyle createEdge(int diameter) {
            return new EdgeStyle.SoftEdge(diameter);
        }
    }, HARD("Hard") {
        @Override
        EdgeStyle createEdge(int diameter) {
            return new EdgeStyle.HardEdge(diameter);
        }
    };

    public static final String PRESET_KEY = "Brush Type";
    private final String displayName;

    CopyBrushType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Creates a new edge style for a stamp of the given size.
     */
    abstract EdgeStyle createEdge(int diameter);

    @Override
    public String toString() {
        return displayName;
    }
}
