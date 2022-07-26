/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.utils;

import pixelitor.filters.gui.FilterGUI;
import pixelitor.filters.gui.FilterWithGUI;
import pixelitor.layers.Filterable;

import java.awt.image.BufferedImage;

/**
 * A mock filter which was manually created for better control.
 */
public class MockFilter extends FilterWithGUI {
    private int numTransformCalls;

    public MockFilter(String name) {
        setName(name);
    }

    public int getNumTransformCalls() {
        return numTransformCalls;
    }

    @Override
    protected BufferedImage transform(BufferedImage src, BufferedImage dest) {
//        System.out.println("MockFilter::transform: CALLED for " + getName());
        numTransformCalls++;
        return src;
    }

    @Override
    public FilterGUI createGUI(Filterable layer, boolean reset) {
        return null;
    }

    @Override
    public void randomize() {

    }
}
