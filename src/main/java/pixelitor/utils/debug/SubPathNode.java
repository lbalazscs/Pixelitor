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

package pixelitor.utils.debug;

import pixelitor.tools.pen.SubPath;

public class SubPathNode extends DebugNode {
    public SubPathNode(SubPath subPath) {
        this("SubPath " + subPath.getId(), subPath);
    }

    public SubPathNode(String name, SubPath subPath) {
        super(name, subPath);

        addString("Name", subPath.getId());
        addBoolean("Closed", subPath.isClosed());
        addBoolean("Finished", subPath.isFinished());
        addBoolean("Has Moving Point", subPath.hasMovingPoint());
    }
}
