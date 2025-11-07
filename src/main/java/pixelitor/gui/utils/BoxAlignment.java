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

package pixelitor.gui.utils;

import static org.jdesktop.swingx.painter.AbstractLayoutPainter.HorizontalAlignment;
import static org.jdesktop.swingx.painter.AbstractLayoutPainter.VerticalAlignment;

/**
 * A two-dimensional text box alignment combining horizontal and vertical positioning.
 */
public enum BoxAlignment {
    CENTER_CENTER("Center", HorizontalAlignment.CENTER, VerticalAlignment.CENTER),
    TOP_CENTER("Top", HorizontalAlignment.CENTER, VerticalAlignment.TOP),
    CENTER_LEFT("Left", HorizontalAlignment.LEFT, VerticalAlignment.CENTER),
    CENTER_RIGHT("Right", HorizontalAlignment.RIGHT, VerticalAlignment.CENTER),
    BOTTOM_CENTER("Bottom", HorizontalAlignment.CENTER, VerticalAlignment.BOTTOM),
    TOP_LEFT("Top Left", HorizontalAlignment.LEFT, VerticalAlignment.TOP),
    TOP_RIGHT("Top Right", HorizontalAlignment.RIGHT, VerticalAlignment.TOP),
    BOTTOM_LEFT("Bottom Left", HorizontalAlignment.LEFT, VerticalAlignment.BOTTOM),
    BOTTOM_RIGHT("Bottom Right", HorizontalAlignment.RIGHT, VerticalAlignment.BOTTOM),

    // special case: text aligned along a path
    PATH("Text Along Path", null, null);

    private final String displayName;
    private final HorizontalAlignment horizontalAlignment;
    private final VerticalAlignment verticalAlignment;

    BoxAlignment(String displayName, HorizontalAlignment horizontalAlignment, VerticalAlignment verticalAlignment) {
        this.displayName = displayName;
        this.horizontalAlignment = horizontalAlignment;
        this.verticalAlignment = verticalAlignment;
    }

    public HorizontalAlignment getHorizontal() {
        return horizontalAlignment;
    }

    public VerticalAlignment getVertical() {
        return verticalAlignment;
    }

    public boolean isPath() {
        return this == PATH;
    }

    @Override
    public String toString() {
        return displayName;
    }

    /**
     * Returns the corresponding {@link BoxAlignment} based
     * on the given horizontal and vertical alignments.
     */
    public static BoxAlignment from(HorizontalAlignment horizontal,
                                    VerticalAlignment vertical) {
        if (horizontal == null || vertical == null) {
            return PATH;
        }
        return switch (horizontal) {
            case CENTER -> switch (vertical) {
                case CENTER -> CENTER_CENTER;
                case TOP -> TOP_CENTER;
                case BOTTOM -> BOTTOM_CENTER;
            };
            case LEFT -> switch (vertical) {
                case CENTER -> CENTER_LEFT;
                case TOP -> TOP_LEFT;
                case BOTTOM -> BOTTOM_LEFT;
            };
            case RIGHT -> switch (vertical) {
                case CENTER -> CENTER_RIGHT;
                case TOP -> TOP_RIGHT;
                case BOTTOM -> BOTTOM_RIGHT;
            };
        };
    }
}
