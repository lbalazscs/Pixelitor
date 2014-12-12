/*
 * Copyright 2009-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.filters;

import pixelitor.filters.gui.AdjustPanel;
import pixelitor.filters.gui.FilterWithGUI;

import java.awt.image.BufferedImage;

public class RandomFilter extends FilterWithGUI {
    public RandomFilter() {
        super("Random Filter");
    }

    @Override
    public AdjustPanel getAdjustPanel() {
        return new RandomFilterAdjustPanel(this);
    }

    @Override
    protected BufferedImage transform(BufferedImage src, BufferedImage dest) {
        return null;
    }

    @Override
    public void randomizeSettings() {

    }
}
