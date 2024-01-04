/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.filters.gmic;

import pixelitor.filters.gui.GroupedRangeParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.gui.RangeParam;

import java.io.Serial;
import java.util.List;

public class Random3DObjects extends GMICFilter {
    @Serial
    private static final long serialVersionUID = 1L;

    public static final String NAME = "Random 3D Objects";

    private final IntChoiceParam type = new IntChoiceParam("Type", new Item[] {
        new Item("Cubes", 0),
        new Item("Cones", 1),
        new Item("Cylinders", 2),
        new Item("Spheres", 3),
        new Item("Toruses", 4)
    });
    private final RangeParam density = new RangeParam("Density", 1, 50, 300);
    private final RangeParam size = new RangeParam("Size", 1, 3, 20);
    private final RangeParam zRange = new RangeParam("Z-Range", 0, 100, 300);
    private final RangeParam fov = new RangeParam("Field of View", 1, 45, 90);
    private final RangeParam xLight = new RangeParam("X", -100, 0, 100);
    private final RangeParam yLight = new RangeParam("Y", -100, 0, 100);
    private final RangeParam zLight = new RangeParam("Z", -100, -100, 0);
    private final RangeParam specularLightness = new RangeParam("Lightness", 0, 50, 100);
    private final RangeParam specularShininess = new RangeParam("Shininess", 0, 70, 300);
    private final IntChoiceParam rendering = new IntChoiceParam("Rendering", new Item[] {
        new Item("Dots", 0),
        new Item("Wireframe", 1),
        new Item("Flat", 2),
        new Item("Flat-Shaded", 3),
        new Item("Gouraud", 4),
        new Item("Phong", 5)
    }).withDefaultChoice(3);
    private final RangeParam opacity = new RangeParam("Opacity", 0, 100, 100);

    public Random3DObjects() {
        setParams(
            type,
            density,
            size,
            zRange,
            fov,
            new GroupedRangeParam("Light", new RangeParam[]{
                xLight,
                yLight,
                zLight
            }, false).notLinkable(),
            new GroupedRangeParam("Specular", new RangeParam[]{
                specularLightness,
                specularShininess,
            }, false).notLinkable(),
            rendering,
            opacity
        ).withReseedGmicAction(this);
    }

    @Override
    public List<String> getArgs() {
        return List.of("srand", String.valueOf(seed), "fx_random3d",
            type.getValue() + "," +
            density.getValue() + "," +
            size.getValue() + "," +
            zRange.getValue() + "," +
            fov.getValue() + "," +
            xLight.getValue() + "," +
            yLight.getValue() + "," +
            zLight.getValue() + "," +
            specularLightness.getPercentage() + "," +
            specularShininess.getPercentage() + "," +
            rendering.getValue() + "," +
            opacity.getPercentage()
        );
    }
}

