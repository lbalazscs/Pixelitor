/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

import com.jhlabs.image.HalftoneFilter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.*;
import pixelitor.filters.gui.IntChoiceParam.Item;

import java.awt.image.BufferedImage;
import java.io.Serial;

/**
 * A halftone filter that uses a user-selected custom image as the threshold mask.
 * If the selected image is smaller than the target image, it will be tiled
 * across the canvas according to the selected dot grid pattern.
 */
public class JHCustomHalftone extends ParametrizedFilter {
    @Serial
    private static final long serialVersionUID = 1L;

    public static final String NAME = "Custom Halftone";

    private final SelectImageParam maskImage = new SelectImageParam("Select Mask Image");

    private final IntChoiceParam dotGrid = new IntChoiceParam("Dot Grid", new Item[]{
        new Item("Triangle", HalftoneFilter.GRID_TRIANGLE),
        new Item("Square", HalftoneFilter.GRID_SQUARE),
        new Item("Rings", HalftoneFilter.GRID_RINGS),
    });

    private final ImagePositionParam center = new ImagePositionParam("Rings Center");

    private final RangeParam softness = new RangeParam("Softness", 0, 10, 100);
    private final BooleanParam invert = new BooleanParam("Invert Pattern");
    private final BooleanParam monochrome = new BooleanParam("Monochrome", true);

    public JHCustomHalftone() {
        super(true);

        // enable the center selector only if the rings grid is selected
        dotGrid.setupEnableOtherIf(center, item -> item.valueIs(HalftoneFilter.GRID_RINGS));

        initParams(
            maskImage,
            dotGrid,
            center,
            softness,
            invert,
            monochrome
        );
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        var filter = new HalftoneFilter(getName());

        filter.setMask(maskImage.getImage());
        filter.setMonochrome(monochrome.isChecked());
        filter.setSoftness((float) softness.getPercentage());
        filter.setInvert(invert.isChecked());
        filter.setGridType(dotGrid.getValue());
        filter.setCenter(center.getAbsolutePoint(src));

        return filter.filter(src, dest);
    }

    @Override
    public boolean canBeSmart() {
        return false;
    }
}
