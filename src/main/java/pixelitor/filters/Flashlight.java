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

package pixelitor.filters;

import pixelitor.filters.gui.*;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.impl.FlashlightFilter;
import pixelitor.gui.GUIText;
import pixelitor.utils.BlurredShape;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.Serial;

/**
 * The "Flashlight" filter.
 */
public class Flashlight extends ParametrizedFilter {
    public static final String NAME = "Flashlight";

    @Serial
    private static final long serialVersionUID = 8815249851114990821L;

    private final ImagePositionParam center = new ImagePositionParam("Center");
    private final GroupedRangeParam radius = new GroupedRangeParam(GUIText.RADIUS, 1, 200, 1000, false);
    private final RangeParam softness = new RangeParam("Edge Softness", 0, 20, 100);
    private final IntChoiceParam shape = BlurredShape.getChoices();
    private final IntChoiceParam bg = new IntChoiceParam("Background", new Item[]{
        new Item("Black", FlashlightFilter.BG_BLACK),
        new Item("White", FlashlightFilter.BG_WHITE),
        new Item("Background Color", FlashlightFilter.BG_TOOL_BG),
        new Item("Transparent", FlashlightFilter.BG_TRANSPARENT),
    }, RandomizeMode.IGNORE);
    private final BooleanParam invert = new BooleanParam("Invert");
    private final RangeParam opacity =
        new RangeParam(GUIText.OPACITY, 0, 100, 100);

    public Flashlight() {
        super(true);
        opacity.setPresetKey("Opacity");
        radius.setPresetKey("Radius");

        initParams(
            center,
            radius.withAdjustedRange(1.0),
            softness,
            shape,
            invert,
            bg,
            opacity
        );
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        FlashlightFilter filter = new FlashlightFilter(NAME,
            center.getAbsolutePoint(src),
            radius.getValueAsDouble(0),
            radius.getValueAsDouble(1),
            softness.getPercentage(),
            shape.getValue(),
            bg.getValue(),
            invert.isChecked());

        BufferedImage filtered = filter.filter(src, dest);

        if (opacity.getValue() == 100) {
            return filtered;
        } else {
            float alpha = (float) opacity.getPercentage();
            Graphics2D g = filtered.createGraphics();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f - alpha));
            g.drawImage(src, 0, 0, null);
            g.dispose();
            return filtered;
        }
    }
}
