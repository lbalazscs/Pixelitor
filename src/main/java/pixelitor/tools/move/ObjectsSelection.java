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

package pixelitor.tools.move;

import java.awt.geom.Rectangle2D;

/**
 * Represents objects selection on stage.
 * It keeps track of objects snapping bounding box
 * It keeps track of objects effective bounding box
 * Bounding boxes must be represented in canvas coordinate space
 *
 * @author Łukasz Kurzaj lukaszkurzaj@gmail.com
 */
public class ObjectsSelection {
    private Object object;
    private Rectangle2D snappingBoundingBox;
    private Rectangle2D effectiveBoundingBox;

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    public Rectangle2D getSnappingBoundingBox() {
        return snappingBoundingBox;
    }

    public void setSnappingBoundingBox(Rectangle2D snappingBoundingBox) {
        this.snappingBoundingBox = snappingBoundingBox;
    }

    public Rectangle2D getEffectiveBoundingBox() {
        return effectiveBoundingBox;
    }

    public void setEffectiveBoundingBox(Rectangle2D effectiveBoundingBox) {
        this.effectiveBoundingBox = effectiveBoundingBox;
    }

    public boolean isEmpty() {
        return object == null;
    }
}
