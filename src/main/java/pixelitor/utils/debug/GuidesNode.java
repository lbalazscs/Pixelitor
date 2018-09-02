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

import pixelitor.guides.Guides;

import java.util.List;

public class GuidesNode extends DebugNode {
    public GuidesNode(Guides guides) {
        this("Guides", guides);
    }

    private GuidesNode(String name, Guides guides) {
        super(name, guides);

        List<Double> horizontals = guides.getHorizontals();
        for (Double h : horizontals) {
            addDouble("horizontal", h);
        }
        List<Double> verticals = guides.getVerticals();
        for (Double v : verticals) {
            addDouble("vertical", v);
        }
    }
}
