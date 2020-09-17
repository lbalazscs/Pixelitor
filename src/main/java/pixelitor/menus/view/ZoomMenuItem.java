/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.menus.view;

import pixelitor.OpenImages;
import pixelitor.gui.View;
import pixelitor.menus.OpenImageAwareRadioButtonMenuItem;

/**
 * A menu item in the {@link ZoomMenu} that represents a zoom level
 */
public class ZoomMenuItem extends OpenImageAwareRadioButtonMenuItem {

    public ZoomMenuItem(ZoomLevel zoomLevel) {
        super(zoomLevel.toString());

        addActionListener(e -> {
            View view = OpenImages.getActiveView();
            view.setZoom(zoomLevel);
        });
    }
}
