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

package pixelitor.tools.pen;

import pixelitor.Composition;
import pixelitor.gui.View;

import java.awt.geom.AffineTransform;
import java.io.Serializable;

/**
 * All the {@link Path} objects that belong to a {@link Composition}
 */
public class Paths implements Serializable {
    private static final long serialVersionUID = 1L;

    // currently there can be only one path, which is called activePath
    // in order to be serialization-compatible with future versions
    private Path activePath;

    public Path getActivePath() {
        return activePath;
    }

    public void setActivePath(Path activePath) {
        this.activePath = activePath;
    }

    public void setView(View view) {
        if (activePath != null) {
            activePath.setView(view);
        }
    }

    public void imCoordsChanged(AffineTransform at) {
        if (activePath != null) {
            activePath.imCoordsChanged(at);
        }
    }
}
