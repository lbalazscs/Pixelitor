/*
 * Copyright 2010-2014 Laszlo Balazs-Csiki
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
package pixelitor.menus.view;

import pixelitor.ImageComponent;
import pixelitor.ImageComponents;
import pixelitor.menus.OpenImageEnabledRadioButtonMenuItem;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 *
 */
public class ZoomMenuItem extends OpenImageEnabledRadioButtonMenuItem {

    public ZoomMenuItem(final ZoomLevel zoomLevel) {
        super(zoomLevel.toString());

        addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ImageComponent ic = ImageComponents.getActiveImageComponent();
                ic.setZoom(zoomLevel, false);
            }
        });
    }
}
