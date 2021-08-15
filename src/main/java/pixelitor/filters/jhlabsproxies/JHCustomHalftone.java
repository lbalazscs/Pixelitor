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

package pixelitor.filters.jhlabsproxies;

import pixelitor.filters.gui.SelectImageParam;

import java.awt.image.BufferedImage;

public class JHCustomHalftone extends JHMaskedHalftone {
    public static final String NAME = "Custom Halftone";

    private final SelectImageParam selectImageParam = new SelectImageParam("Select Image");

    public JHCustomHalftone() {
        setParams(
            selectImageParam,
            softness,
            invert,
            monochrome
        );
    }

    @Override
    protected BufferedImage createMaskImage(BufferedImage src) {
        return selectImageParam.getImage();
//        Optional<Composition> opt = OpenImages.findCompByName("Untitled1");
//        if (opt.isPresent()) {
//            return opt.get().getCompositeImage();
//        }
//        // to avoid exceptions if in an auto test
//        // this filter is selected as a random filter
//        return ImageUtils.copyImage(src);
    }
}
