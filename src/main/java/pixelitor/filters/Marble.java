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
import pixelitor.filters.impl.MarbleTextureFilter;
import pixelitor.gui.GUIText;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.List;

/**
 * Marble filter.
 */
public class Marble extends ParametrizedFilter {
    public static final String NAME = "Marble";

    @Serial
    private static final long serialVersionUID = -4289737664285529580L;

    private final IntChoiceParam type = new IntChoiceParam(GUIText.TYPE, new Item[]{
        new Item("Lines", MarbleTextureFilter.TYPE_LINES),
        new Item("Rings", MarbleTextureFilter.TYPE_RINGS),
        new Item("Spiral", MarbleTextureFilter.TYPE_SPIRAL),
        new Item("Grid", MarbleTextureFilter.TYPE_GRID),
        new Item("Star", MarbleTextureFilter.TYPE_STAR),
    });

    private final IntChoiceParam shape = new IntChoiceParam("Shape", new Item[]{
        new Item("Circle", MarbleTextureFilter.SHAPE_CIRCLE),
        new Item("Square", MarbleTextureFilter.SHAPE_SQUARE),
    });

    private final ImagePositionParam center = new ImagePositionParam("Center");
    private final IntChoiceParam waveType = IntChoiceParam.forWaveType();

    private final RangeParam zoom = new RangeParam(GUIText.ZOOM, 1, 10, 200);
    private final AngleParam rotation = new AngleParam("Rotation", 0);
    private final RangeParam distortion = new RangeParam("Distortion", 0, 25, 100);
    private final RangeParam time = new RangeParam("Time (Phase)", 0, 0, 100);

    private final RangeParam detailsLevel = new RangeParam("Level", 0, 3, 8);
    private final RangeParam detailsStrength = new RangeParam("Strength", 0, 12, 48);

    private final BooleanParam smoothDetails = new BooleanParam("Smoother");

    private static final GradientPreset MALACHITE = new GradientPreset("Malachite",
        new float[]{0.0f, 0.5f, 1.0f},
        new Color[]{
            new Color(1, 14, 5),
            new Color(20, 50, 38),
            new Color(235, 255, 251),
        });

    private static final GradientPreset EMPERADOR = new GradientPreset("Emperador",
        new float[]{0.0f, 0.40f, 0.75f, 1.0f},
        new Color[]{
            new Color(18, 10, 7),
            new Color(82, 46, 28),
            new Color(180, 126, 78),
            new Color(250, 242, 230),
        });

    private static final GradientPreset CARRARA = new GradientPreset("Carrara",
        new float[]{0.0f, 0.25f, 0.65f, 1.0f},
        new Color[]{
            new Color(24, 27, 32),
            new Color(105, 112, 120),
            new Color(210, 214, 218),
            new Color(253, 253, 255),
        });

    private static final GradientPreset PORTORO = new GradientPreset("Portoro",
        new float[]{0.0f, 0.55f, 0.80f, 0.95f, 1.0f},
        new Color[]{
            new Color(8, 8, 10),
            new Color(28, 25, 22),
            new Color(165, 110, 40),
            new Color(245, 190, 80),
            new Color(255, 245, 205),
        });

    private static final GradientPreset LAPIS = new GradientPreset("Lapis",
        new float[]{0.0f, 0.45f, 0.80f, 1.0f},
        new Color[]{
            new Color(5, 10, 28),
            new Color(18, 48, 110),
            new Color(75, 140, 200),
            new Color(240, 248, 255),
        });

    private static final GradientPreset ROSE_ONYX = new GradientPreset("Rose Onyx",
        new float[]{0.0f, 0.40f, 0.75f, 1.0f},
        new Color[]{
            new Color(50, 20, 28),
            new Color(155, 82, 95),
            new Color(230, 172, 180),
            new Color(255, 245, 246),
        });


    private final GradientParam gradient = new GradientParam("Colors",
        List.of(MALACHITE, EMPERADOR, CARRARA, PORTORO, LAPIS, ROSE_ONYX));

    public Marble() {
        super(false);

        smoothDetails.setPresetKey("Smoother Details");
        var details = CompositeParam.bordered("Details",
            detailsLevel, detailsStrength, smoothDetails);

        // enable the shape selector only for the RINGS and SPIRAL types
        type.enableOtherWhen(shape, item ->
            item.hasValue(MarbleTextureFilter.TYPE_RINGS, MarbleTextureFilter.TYPE_SPIRAL));

        type.setPresetKey("Type");
        zoom.setPresetKey("Zoom");
        rotation.setPresetKey("Angle"); // legacy

        initParams(
            CompositeParam.horizontal("Type", type, shape),
            center,
            waveType,
            time,
            rotation,
            zoom.withAdjustedRange(0.25),
            distortion,
            details,
            gradient
        ).withReseedNoiseAction();
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        double angleShift = Math.PI / 2;
        if (type.getValue() == MarbleTextureFilter.TYPE_GRID) {
            angleShift = Math.PI / 4;
        }

        var filter = new MarbleTextureFilter(
            type.getValue(),
            shape.getValue(),
            center.getAbsolutePoint(src),
            waveType.getValue(),
            rotation.getValueInRadians() + angleShift,
            zoom.getValueAsDouble(),
            distortion.getValueAsDouble() / 5.0,
            detailsLevel.getValue(),
            detailsStrength.getValueAsDouble() / 4.0,
            gradient.getColorMap(),
            smoothDetails.isChecked(),
            time.getValueAsDouble() / 5.0
        );

        return filter.filter(src, dest);
    }
}
