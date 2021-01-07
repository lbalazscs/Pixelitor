/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.tools;

import pixelitor.gui.View;

import java.awt.Graphics2D;
import java.awt.Shape;

/**
 * How the clipping shape of {@link Graphics2D} is set when painting the
 * active {@link View}.
 * Each tool has its own {@link ClipStrategy}.
 */
public enum ClipStrategy {
    /**
     * The painting is allowed to leave the canvas,
     * but not the internal frame/tab.
     * This is exactly what the original Swing clip did, but
     * that one was overridden previously in {@link View}.
     */
    FULL {
        @Override
        public void setClip(Graphics2D g, View view, Shape originalClip) {
            g.setClip(originalClip);
        }
    },
    /**
     * The painting is not allowed to leave the canvas.
     * This should be used if a tool only acts on the image,
     * without helper handles that can be outside the canvas.
     */
    CANVAS {
        @Override
        public void setClip(Graphics2D g, View view, Shape originalClip) {
            // empty: the canvas clipping has been already set
        }
    },
    /**
     * The tool itself will set its own custom clipping.
     */
    CUSTOM {
        @Override
        public void setClip(Graphics2D g, View view, Shape originalClip) {
            // empty: it will be set later in the tool
        }
    };

    /**
     * Called when the active {@link View} is painted
     */
    public abstract void setClip(Graphics2D g, View view, Shape originalClip);
}
