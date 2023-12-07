/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.colors.Colors;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.*;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.io.IO;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Messages;
import pixelitor.utils.Result;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class GMICFilter extends ParametrizedFilter {
    public static File GMIC_PATH;

    public static final String NAME_ANISOTHROPIC = "Anisothropic Smoothing";
    public static final String NAME_BILATERAL = "Bilateral Smoothing";
    public static final String NAME_BOKEH = "Bokeh";
    public static final String NAME_BOXFITTING = "Box Fitting";
    public static final String NAME_BRUSHIFY = "Brushify";
    public static final String NAME_CUBISM = "Cubism";
    public static final String NAME_GENERIC = "Generic Command";
    public static final String NAME_KUWAHARA = "Kuwahara Smoothing";
    public static final String NAME_LIGHT_GLOW = "Light Glow";
    public static final String NAME_LOCAL_NORMALIZATION = "Local Normalization";
    public static final String NAME_RODILIUS = "Rodilius";
    public static final String NAME_STROKE = "Stroke";
    public static final String NAME_VIBRANCE = "Vibrance";

    private final Supplier<List<String>> argsFactory;

    private GMICFilter(Supplier<List<String>> argsFactory, FilterParam... params) {
        super(true);
        this.argsFactory = argsFactory;

        setParams(params);
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        List<String> args = argsFactory.get();
        System.out.println(String.join(" ", args));

        List<String> command = new ArrayList<>(10);
        command.add(GMIC_PATH.getAbsolutePath());
        command.add("-input");
        command.add("-.png");
        command.addAll(args);
        command.add("-output");
        command.add("-.png");

        Result<BufferedImage, String> result = IO.commandLineFilterImage(src, command);
        if (result.wasSuccess()) {
            return ImageUtils.toSysCompatibleImage(result.get());
        } else {
            Messages.showError("G'MIC Error", result.errorDetail());
            return src;
        }
    }

    @Override
    public boolean canHaveUserPresets() {
        return false;
    }

    @Override
    public boolean canBeSmart() {
        return false;
    }

    private static IntChoiceParam getValueAction() {
        return new IntChoiceParam("Value Action", new String[]{
            "None", "Cut", "Normalize"});
    }

    private static IntChoiceParam getChannelChoice() {
        return new IntChoiceParam("Channels", new String[]{
            "All", "RGBA [All]", "RGB [All]",
            "RGB [Red]", "RGB [Green]", "RGB [Blue]", "RGBA [Alpha]",
            "Linear RGB [All]", "Linear RGB [Red]", "Linear RGB [Green]", "Linear RGB [Blue]",
            "YCbCr [Luminance]", "YCbCr [Blue-Red Chrominances]", "YCbCr [Blue Chrominance]",
            "YCbCr [Red Chrominance]", "YCbCr [Green Chrominance]",
            "Lab [Lightness]", "Lab [ab-Chrominances]",
            "Lab [a-Chrominance]", "Lab [b-Chrominance]",
            "Lch [ch-Chrominances]", "Lch [c-Chrominance]", "Lch [h-Chrominance]",
            "HSV [Hue]", "HSV [Saturation]", "HSV [Value]",
            "HSI [Intensity]", "HSL [Lightness]",
            "CMYK [Cyan]", "CMYK [Magenta]", "CMYK [Yellow]", "CMYK [Key]",
            "YIQ [Luma]", "YIQ [Chromas]",
            "RYB [All]", "RYB [Red]", "RYB [Yellow]", "RYB [Blue]",
        });
    }

    public static GMICFilter createAnisothropic() {
        RangeParam amplitude = new RangeParam("Amplitude", 0, 60, 1000);
        RangeParam sharpness = new RangeParam("Sharpness", 0, 70, 200);
        RangeParam anisotropy = new RangeParam("Anisotropy", 0, 30, 100);
        RangeParam gradientSmoothness = new RangeParam("Gradient Smoothness", 0, 60, 1000);
        RangeParam tensorSmoothness = new RangeParam("Tensor Smoothness", 0, 110, 1000);
        RangeParam spatialPrecision = new RangeParam("Spatial Precision", 10, 80, 200);
        RangeParam angularPrecision = new RangeParam("Angular Precision", 1, 30, 180);
        RangeParam valuePrecision = new RangeParam("Value Precision", 10, 200, 500);
        IntChoiceParam interpolation = new IntChoiceParam("Interpolation", new Item[]{
            new Item("Nearest Neighbor", 0),
            new Item("Linear", 1),
            new Item("Runge-Kutta", 2)
        });
        BooleanParam fastApproximation = new BooleanParam("Fast Approximation", true);
        RangeParam iterations = new RangeParam("Iterations", 1, 1, 10);
        IntChoiceParam channel = getChannelChoice();

        Supplier<List<String>> argsFactory = () -> List.of("fx_smooth_anisotropic",
            amplitude.getValue() + "," +
                sharpness.getPercentage() + "," +
                anisotropy.getPercentage() + "," +
                gradientSmoothness.getPercentage() + "," +
                tensorSmoothness.getPercentage() + "," +
                spatialPrecision.getPercentage() + "," +
                angularPrecision.getValue() + "," +
                valuePrecision.getPercentage() + "," +
                interpolation.getValue() + "," +
                fastApproximation.isCheckedStr() + "," +
                iterations.getValue() + "," +
                channel.getValue());

        return new GMICFilter(argsFactory, amplitude, sharpness, anisotropy,
            gradientSmoothness, tensorSmoothness,
            spatialPrecision, angularPrecision, valuePrecision,
            interpolation, fastApproximation, iterations, channel);
    }

    public static GMICFilter createBilateral() {
        GroupedRangeParam variance = new GroupedRangeParam("Variance",
            new RangeParam[]{
                new RangeParam("Spatial", 0, 10, 100),
                new RangeParam("Value", 0, 7, 100),
            }, false).notLinkable();
        RangeParam iterations = new RangeParam("Iterations", 1, 2, 10);
        IntChoiceParam channel = getChannelChoice();

        Supplier<List<String>> argsFactory = () -> List.of("fx_smooth_bilateral",
            variance.getValue(0) + "," +
                variance.getValue(1) + "," +
                iterations.getValue() + "," +
                channel.getValue());

        return new GMICFilter(argsFactory, variance, iterations, channel);
    }

    public static GMICFilter createBokeh() {
        RangeParam scales = new RangeParam("Number of Scales", 1, 3, 10);
        IntChoiceParam shape = new IntChoiceParam("Shape", new Item[]{
            new Item("Circular", 8),
            new Item("Decagon", 6),
            new Item("Diamond", 2),
            new Item("Hexagon", 4),
            new Item("Octogon", 5),
            new Item("Pentagon", 3),
            new Item("Square", 1),
            new Item("Star", 7),
            new Item("Triangle", 0),
        });

//        ColorParam startColor = new ColorParam("Starting Color", new Color(210, 210, 80, 160));
//        ColorParam endColor = new ColorParam("Ending Color", new Color(170, 130, 20, 110));
        GroupedColorsParam colors = new GroupedColorsParam("Color",
            "Start", new Color(210, 210, 80, 160),
            "End", new Color(170, 130, 20, 110));

//        RangeParam startDensity = new RangeParam("Starting Density", 1, 30, 256);
//        RangeParam endDensity = new RangeParam("Ending Density", 1, 30, 256);
        GroupedRangeParam density = new GroupedRangeParam("Density",
            "Start", "End", 1, 30, 256, false);

        GroupedRangeParam radius = new GroupedRangeParam("Radius",
            new RangeParam[]{
                new RangeParam("Start", 0, 8, 50),
                new RangeParam("End", 0, 20, 50)
            }, false);

        GroupedRangeParam outline = new GroupedRangeParam("Outline",
            new RangeParam[]{
                new RangeParam("Start", 0, 4, 100),
                new RangeParam("End", 0, 20, 100)
            }, false);

        GroupedRangeParam innerShade = new GroupedRangeParam("Inner Shade",
            new RangeParam[]{
                new RangeParam("Start", 0, 30, 100),
                new RangeParam("End", 0, 100, 100)
            }, false);

        GroupedRangeParam smoothness = new GroupedRangeParam("Smoothness",
            new RangeParam[]{
                new RangeParam("Star", 0, 20, 800),
                new RangeParam("End", 0, 200, 800)
            }, false);

        GroupedRangeParam colorDispersion = new GroupedRangeParam("Color Dispersion",
            new RangeParam[]{
                new RangeParam("Start", 0, 70, 100),
                new RangeParam("End", 0, 15, 100)
            }, false);

        Supplier<List<String>> argsFactory = () -> List.of("fx_bokeh",
            scales.getValue() + ","
                + shape.getValue() +
                ",0," + // seed
                density.getValue(0) + "," +
                radius.getValue(0) + "," +
                outline.getValue(0) + "," +
                innerShade.getPercentage(0) + "," +
                smoothness.getPercentage(0) + "," +
                colors.getColorStr(0) + "," +
                colorDispersion.getPercentage(0) + "," +

                density.getValue(1) + "," +
                radius.getValue(1) + "," +
                outline.getValue(1) + "," +
                innerShade.getPercentage(1) + "," +
                smoothness.getPercentage(1) + "," +
                colors.getColorStr(1) + "," +
                colorDispersion.getPercentage(1));

        return new GMICFilter(argsFactory, scales, shape, colors,
            density, radius, outline, innerShade,
            smoothness, colorDispersion);
    }

    public static GMICFilter createBoxFitting() {
        RangeParam minSize = new RangeParam("Minimal Size", 1, 10, 100);
        RangeParam maxSize = new RangeParam("Maximal Size", 1, 50, 100);
        RangeParam density = new RangeParam("Initial Density", 0, 25, 100);
        RangeParam minSpacing = new RangeParam("Minimal Spacing", 1, 1, 100);
        BooleanParam transparency = new BooleanParam("Transparency", false);
        maxSize.ensureHigherValueThan(minSize);

        Supplier<List<String>> argsFactory = () -> List.of("fx_boxfitting",
            minSize.getValue() + "," +
                maxSize.getValue() + "," +
                density.getPercentage() + "," +
                minSpacing.getValue() + "," +
                transparency.isCheckedStr());

        return new GMICFilter(argsFactory, minSize, maxSize, density, minSpacing, transparency);
    }

    public static GMICFilter createBrushify() {
        IntChoiceParam shape = new IntChoiceParam("Shape", new Item[]{
            new Item("Ellipse", 7),
            new Item("Rectangle", 2),
            new Item("Diamond", 3),
            new Item("Pentagon", 4),
            new Item("Hexagon", 5),
            new Item("Octogon", 6),
            new Item("Gaussian", 8),
            new Item("Star", 9),
            new Item("Heart", 10)
        });
        RangeParam ratio = new RangeParam("Ratio", 0, 25, 100);
        RangeParam numberOfSizes = new RangeParam("Number of Sizes", 1, 4, 16);
        RangeParam maximalSize = new RangeParam("Maximal Size", 1, 64, 128);
        RangeParam minimalSize = new RangeParam("Minimal Size", 0, 25, 100);
        RangeParam numberOfOrientations = new RangeParam("Number of Orientations", 1, 12, 24);
        RangeParam fuzziness = new RangeParam("Fuzziness", 0, 0, 10);
        RangeParam smoothness = new RangeParam("Smoothness", 0, 2, 10);
        IntChoiceParam lightType = new IntChoiceParam("Light Type", new Item[]{
            new Item("Full", 4),
            new Item("None", 0),
            new Item("Flat", 1),
            new Item("Darken", 2),
            new Item("Lighten", 3),
        });
        RangeParam lightStrength = new RangeParam("Light Strength", 0, 20, 100);
        RangeParam opacity = new RangeParam("Opacity", 0, 50, 100);
        RangeParam density = new RangeParam("Density", 0, 30, 100);
        RangeParam contourCoherence = new RangeParam("Contour Coherence", 0, 100, 100);
        RangeParam orientationCoherence = new RangeParam("Orientation Coherence", 0, 100, 100);
        RangeParam gradientSmoothness = new RangeParam("Gradient Smoothness", 0, 1, 10);
        RangeParam structureSmoothness = new RangeParam("Structure Smoothness", 0, 5, 10);
        RangeParam primaryAngle = new RangeParam("Primary Angle", -180, 0, 180);
        RangeParam angleDispersion = new RangeParam("Angle Dispersion", 0, 20, 100);

        Supplier<List<String>> argsFactory = () -> List.of("fx_brushify",
            shape.getValue() + "," +
                ratio.getPercentage() + "," +
                numberOfSizes.getValue() + "," +
                maximalSize.getValue() + "," +
                minimalSize.getValue() + "," +
                numberOfOrientations.getValue() + "," +
                fuzziness.getValue() + "," +
                smoothness.getValue() + "," +
                lightType.getValue() + "," +
                lightStrength.getPercentage() + "," +
                opacity.getPercentage() + "," +
                density.getValue() + "," +
                contourCoherence.getPercentage() + "," +
                orientationCoherence.getPercentage() + "," +
                gradientSmoothness.getValue() + "," +
                structureSmoothness.getValue() + "," +
                primaryAngle.getValue() + "," +
                angleDispersion.getPercentage() + ",0");

        return new GMICFilter(argsFactory, shape, ratio,
            numberOfSizes, maximalSize, minimalSize,
            numberOfOrientations, fuzziness, smoothness,
            lightType, lightStrength, opacity,
            density, contourCoherence, orientationCoherence,
            gradientSmoothness, structureSmoothness,
            primaryAngle, angleDispersion);
    }

    public static GMICFilter createCubism() {
        RangeParam iterations = new RangeParam("Iterations", 0, 2, 10);
        RangeParam density = new RangeParam("Density", 0, 50, 200);
        RangeParam thickness = new RangeParam("Thickness", 0, 10, 50);
        RangeParam angle = new RangeParam("Angle", 0, 90, 360);
        RangeParam opacity = new RangeParam("Opacity", 1, 70, 100);
        RangeParam smoothness = new RangeParam("Smoothness", 0, 0, 5);

        Supplier<List<String>> argsFactory = () -> List.of("fx_cubism",
            iterations.getValue() + "," +
                density.getValue() + "," +
                thickness.getValue() + "," +
                angle.getValue() + "," +
                opacity.getPercentage() + "," +
                smoothness.getValue()
        );

        return new GMICFilter(argsFactory, iterations, density,
            thickness, angle, opacity, smoothness);
    }

    public static GMICFilter createGeneric() {
        TextParam textParam = new TextParam("Command",
            "edges 9% normalize 0,255", true);

        Supplier<List<String>> argsFactory = () ->
            Stream.of(textParam.getValue().split(" "))
                .map(String::trim)
                .toList();

        return new GMICFilter(argsFactory, textParam);
    }

    public static GMICFilter createKuwahara() {
        RangeParam iterations = new RangeParam("Iterations", 1, 2, 20);
        RangeParam radius = new RangeParam("Radius", 1, 5, 30);
        IntChoiceParam channel = getChannelChoice();
        IntChoiceParam valueAction = getValueAction();

        Supplier<List<String>> argsFactory = () -> List.of("fx_kuwahara",
            iterations.getValue() + "," +
                radius.getValue() + "," +
                channel.getValue() + "," +
                valueAction.getValue());

        return new GMICFilter(argsFactory, iterations, radius, channel, valueAction);
    }

    public static GMICFilter createLightGlow() {
        RangeParam density = new RangeParam("Density", 0, 30, 100);
        RangeParam amplitude = new RangeParam("Amplitude", 0, 50, 200);
        Item overlay = new Item("Overlay", 8);
        IntChoiceParam mode = new IntChoiceParam("Mode", new Item[]{
            new Item("Burn", 0),
            new Item("Dodge", 1),
            new Item("Freeze", 2),
            new Item("Grain Merge", 3),
            new Item("Hard Light", 4),
            new Item("Interpolation", 5),
            new Item("Lighten", 6),
            new Item("Multiply", 7),
            overlay,
            new Item("Reflect", 9),
            new Item("Soft Light", 10),
            new Item("Stamp", 11),
            new Item("Value", 12)
        }).withDefaultChoice(overlay);
        RangeParam opacity = new RangeParam("Opacity", 0, 80, 100);
        IntChoiceParam channels = getChannelChoice();

        Supplier<List<String>> argsFactory = () -> List.of("fx_lightglow",
            density.getValue() + "," +
                amplitude.getPercentage() + "," +
                mode.getValue() + "," +
                opacity.getPercentage() + "," +
                channels.getValue());

        return new GMICFilter(argsFactory, density, amplitude, mode, opacity, channels);
    }

    public static GMICFilter createLocalNormalization() {
        RangeParam amplitude = new RangeParam("Amplitude", 0, 2, 60);
        RangeParam radius = new RangeParam("Radius", 1, 6, 64);
        RangeParam nSmooth = new RangeParam("Neighborhood Smoothness", 0, 5, 40);
        RangeParam aSmooth = new RangeParam("Average Smoothness", 0, 20, 40);
        BooleanParam cut = new BooleanParam("Constrain Values", true);
        IntChoiceParam channel = getChannelChoice().withDefaultChoice(11);

        Supplier<List<String>> argsFactory = () -> List.of("fx_normalize_local",
            amplitude.getValue() + "," +
                radius.getValue() + "," +
                nSmooth.getValue() + ","
                + aSmooth.getValue() + ","
                + cut.isCheckedStr() + ","
                + channel.getValue());

        return new GMICFilter(argsFactory, amplitude, radius,
            nSmooth, aSmooth, cut, channel);
    }

    public static GMICFilter createRodilius() {
        RangeParam amplitude = new RangeParam("Amplitude", 0, 10, 30);
        RangeParam thickness = new RangeParam("Thickness", 0, 10, 100);
        RangeParam sharpness = new RangeParam("Sharpness", 0, 300, 1000);
        RangeParam orientations = new RangeParam("Orientations", 2, 5, 36);
        RangeParam offset = new RangeParam("Offset", 0, 30, 180);
        RangeParam smoothness = new RangeParam("Smoothness", 0, 0, 5);

        Item darker = new Item("Darker", 1);
        Item lighter = new Item("Lighter", 2);
        IntChoiceParam colormode = new IntChoiceParam("Color Mode", new Item[]{
            darker,
            lighter
        }).withDefaultChoice(lighter);

        IntChoiceParam channel = getChannelChoice();
        IntChoiceParam valueAction = getValueAction();

        Supplier<List<String>> argsFactory = () -> List.of("fx_rodilius",
            amplitude.getValue() + "," +
                thickness.getValue() + "," +
                sharpness.getValue() + "," +
                orientations.getValue() + "," +
                offset.getValue() + "," +
                smoothness.getValue() + "," +
                colormode.getValue() + "," +
                channel.getValue() + "," +
                valueAction.getValue());

        return new GMICFilter(argsFactory, amplitude, thickness, sharpness, orientations, offset, smoothness, colormode, channel, valueAction);
    }

    public static GMICFilter createStroke() {
        RangeParam thickness = new RangeParam("Thickness", 1, 3, 256);
        RangeParam threshold = new RangeParam("Threshold", 0, 50, 100);
        RangeParam smoothness = new RangeParam("Smoothness", 0, 0, 10);
        IntChoiceParam shape = new IntChoiceParam("Shape", new Item[]{
            new Item("Round", 2),
            new Item("Square", 0),
            new Item("Diamond", 1)
        });
        IntChoiceParam direction = new IntChoiceParam("Direction", new Item[]{
            new Item("Outward", 1),
            new Item("Inward", 0)
        });
        RangeParam zoom = new RangeParam("Zoom", 1, 100, 300);
        GroupedRangeParam shift = new GroupedRangeParam("Shift",
            -256, 0, 256);
        GroupedColorsParam strokeColor = new GroupedColorsParam("Stroke Color",
            "Start", Color.WHITE, "End", Color.WHITE);
        GroupedColorsParam fillColor = new GroupedColorsParam("Fill Color",
            "Inside", Colors.TRANSPARENT_BLACK, "Outside", Colors.TRANSPARENT_BLACK);

        Supplier<List<String>> argsFactory = () -> List.of("fx_stroke",
            thickness.getValue() + "," +
                threshold.getValue() + "," +
                smoothness.getValue() + "," +
                shape.getValue() + "," +
                direction.getValue() + "," +
                zoom.getValue() + "," +
                shift.getValue(0) + "," +
                shift.getValue(1) + "," +
                strokeColor.getColorStr(0) + "," +
                strokeColor.getColorStr(1) + "," +
                fillColor.getColorStr(0) + "," +
                fillColor.getColorStr(1) + ",1,1", "blend", "alpha"
        );

        return new GMICFilter(argsFactory, thickness,
            threshold, smoothness, shape, direction, zoom, shift,
            strokeColor, fillColor);
    }

    public static GMICFilter createVibrance() {
        RangeParam strength = new RangeParam("Strength", -100, 50, 300);

        Supplier<List<String>> argsFactory = () -> List.of("fx_vibrance",
            strength.getPercentageStr());

        return new GMICFilter(argsFactory, strength);
    }
}
