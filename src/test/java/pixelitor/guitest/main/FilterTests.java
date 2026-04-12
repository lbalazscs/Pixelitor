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

package pixelitor.guitest.main;

import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.JButtonFixture;
import pixelitor.filters.*;
import pixelitor.filters.jhlabsproxies.JHKaleidoscope;
import pixelitor.filters.jhlabsproxies.JHPolarCoordinates;
import pixelitor.filters.levels.Levels;
import pixelitor.filters.lookup.ColorBalance;
import pixelitor.filters.painters.TextSettings;
import pixelitor.gui.GUIText;
import pixelitor.guitest.AppRunner;
import pixelitor.guitest.EDT;
import pixelitor.guitest.FilterOptions;
import pixelitor.guitest.Keyboard;
import pixelitor.history.History;

import java.util.function.Consumer;

import static pixelitor.guitest.GUITestUtils.findButtonByText;
import static pixelitor.guitest.main.MaskMode.NO_MASK;

public class FilterTests {
    // whether filters should be tested with images with a width or height of 1 pixel
    private static final boolean FILTER_TESTS_WITH_HEIGHT_1 = false;
    private static final boolean FILTER_TESTS_WITH_WIDTH_1 = false;

    // whether to expect the default text
    private static boolean textFilterTested = false;

    private final Keyboard keyboard;
    private final AppRunner app;
    private final MaskMode maskMode;
    private final TestConfig config;

    private final TestContext context;

    public FilterTests(TestContext context) {
        this.context = context;

        this.keyboard = context.keyboard();
        this.app = context.app();
        this.maskMode = context.maskMode();
        this.config = context.config();
    }

    void start() {
        context.log(0, "filters");
//        app.setIndexedMode();

        EDT.assertNumViewsIs(1);
        EDT.assertNumLayersInActiveHolderIs(1);

        EDT.requireNoSelection();
        EDT.assertNoTranslation();

        boolean squashImage = FILTER_TESTS_WITH_WIDTH_1 || FILTER_TESTS_WITH_HEIGHT_1;
        if (squashImage) {
            if (FILTER_TESTS_WITH_WIDTH_1 && FILTER_TESTS_WITH_HEIGHT_1) {
                app.resize(1, 1);
            } else if (FILTER_TESTS_WITH_WIDTH_1) {
                app.resize(1, 100);
            } else if (FILTER_TESTS_WITH_HEIGHT_1) {
                app.resize(100, 1);
            }
        }

        testRepeatLast();
        testColorFilters(squashImage);
        testArtisticFilters();
        testBlurSharpenFilters();
        testDisplaceFilters();
        testDistortFilters();
        testFindEdgesFilters();
        testGMICFilters();
        testLightFilters();
        testNoiseFilters();
        testOtherFilters();
        testRenderFilters();
        testTransitionsFilters();

        context.afterTestActions();
    }

    private void testRepeatLast() {
        app.invert();
        app.runMenuCommand("Repeat Invert");
        keyboard.undoRedo("Invert"); // needed by the history checker
    }

    private void testColorFilters(boolean squashedImage) {
        testColorBalance(squashedImage);
        testFilterWithDialog(HueSat.NAME, FilterOptions.STANDARD);
        testFilterWithDialog(Colorize.NAME, FilterOptions.STANDARD);
        testFilterWithDialog(Levels.NAME, FilterOptions.STANDARD);
        testFilterWithDialog(BrightnessContrast.NAME, FilterOptions.STANDARD);
        testFilterWithDialog(Solarize.NAME, FilterOptions.STANDARD);
        testFilterWithDialog(Sepia.NAME, FilterOptions.TRIVIAL);
        testInvert(squashedImage);
        testFilterWithDialog("Channel Invert", FilterOptions.TRIVIAL);
        testFilterWithDialog(ChannelMixer.NAME, FilterOptions.STANDARD, "Swap Red-Green", "Swap Red-Blue", "Swap Green-Blue",
            "R -> G -> B -> R", "R -> B -> G -> R",
            "Average BW", "Luminosity BW", "Sepia");
        testFilterWithDialog("Equalize", FilterOptions.STANDARD);
        testFilterWithDialog("Extract Channel", FilterOptions.STANDARD);
        testNoDialogFilter("Luminosity");
        testNoDialogFilter("Value = max(R,G,B)");
        testNoDialogFilter(ExtractChannelFilter.DESATURATE_NAME);
        testNoDialogFilter(GUIText.HUE);
        testNoDialogFilter("Hue (with colors)");
        testNoDialogFilter(ExtractChannelFilter.SATURATION_NAME);
        testFilterWithDialog("Quantize", FilterOptions.STANDARD);
        testFilterWithDialog(Posterize.NAME, FilterOptions.STANDARD);
        testFilterWithDialog(Threshold.NAME, FilterOptions.STANDARD);
        testFilterWithDialog("Color Threshold", FilterOptions.STANDARD);
        testFilterWithDialog("Tritone", FilterOptions.STANDARD);
        testFilterWithDialog("Gradient Map", FilterOptions.TRIVIAL);
        testNoDialogFilter(GUIText.FG_COLOR);
        testNoDialogFilter(GUIText.BG_COLOR);
        testNoDialogFilter("Transparent");
        testFilterWithDialog("Color Wheel", FilterOptions.RENDERING);
        testFilterWithDialog("Four Color Gradient", FilterOptions.RENDERING);
    }

    private void testArtisticFilters() {
        testFilterWithDialog("Comic Book", FilterOptions.STANDARD);
        testFilterWithDialog("Crystallize", FilterOptions.STANDARD_RESEED);
        testFilterWithDialog("Emboss", FilterOptions.STANDARD);
        testFilterWithDialog("Oil Painting", FilterOptions.STANDARD);
        testFilterWithDialog("Orton Effect", FilterOptions.STANDARD);
        testFilterWithDialog("Photo Collage", FilterOptions.STANDARD_RESEED);
        testFilterWithDialog("Pointillize", FilterOptions.STANDARD_RESEED);
        testFilterWithDialog("Radial Mosaic", FilterOptions.STANDARD);
        testFilterWithDialog("Smear", FilterOptions.STANDARD);
        testFilterWithDialog("Spheres", FilterOptions.STANDARD);
        testFilterWithDialog("Stamp", FilterOptions.STANDARD);
        testFilterWithDialog("Weave", FilterOptions.STANDARD);

        testFilterWithDialog("Dots Halftone", FilterOptions.STANDARD);
        testFilterWithDialog("Striped Halftone", FilterOptions.STANDARD);
        testFilterWithDialog("Concentric Halftone", FilterOptions.STANDARD);
        testFilterWithDialog("Color Halftone", FilterOptions.STANDARD);
        testFilterWithDialog("Ordered Dithering", FilterOptions.STANDARD);
    }

    private void testBlurSharpenFilters() {
        testFilterWithDialog("Box Blur", FilterOptions.STANDARD);
        testFilterWithDialog("Focus", FilterOptions.STANDARD);
        testFilterWithDialog("Gaussian Blur", FilterOptions.STANDARD);
        testFilterWithDialog("Lens Blur", FilterOptions.STANDARD);
        testFilterWithDialog("Motion Blur", FilterOptions.STANDARD);
        testFilterWithDialog("Smart Blur", FilterOptions.STANDARD);
        testFilterWithDialog("Spin and Zoom Blur", FilterOptions.STANDARD);
        testFilterWithDialog("Unsharp Mask", FilterOptions.STANDARD);
    }

    private void testDistortFilters() {
        testFilterWithDialog("Swirl, Pinch, Bulge", FilterOptions.STANDARD);
        testFilterWithDialog("Circle to Square", FilterOptions.STANDARD);
        testFilterWithDialog("Perspective", FilterOptions.STANDARD);
        testFilterWithDialog("Lens Over Image", FilterOptions.STANDARD);
        testFilterWithDialog("Magnify", FilterOptions.STANDARD);
        testFilterWithDialog("Turbulent Distortion", FilterOptions.STANDARD_RESEED);
        testFilterWithDialog("Underwater", FilterOptions.STANDARD_RESEED);
        testFilterWithDialog("Water Ripple", FilterOptions.STANDARD);
        testFilterWithDialog("Waves", FilterOptions.STANDARD);
        testFilterWithDialog("Angular Waves", FilterOptions.STANDARD);
        testFilterWithDialog("Radial Waves", FilterOptions.STANDARD);
        testFilterWithDialog("Glass Tiles", FilterOptions.STANDARD);
        testFilterWithDialog("Polar Glass Tiles", FilterOptions.STANDARD);
        testFilterWithDialog("Frosted Glass", FilterOptions.STANDARD_RESEED);
        testFilterWithDialog(LittlePlanet.NAME, FilterOptions.STANDARD);
        testFilterWithDialog(JHPolarCoordinates.NAME, FilterOptions.STANDARD);
        testFilterWithDialog("Wrap Around Arc", FilterOptions.STANDARD);
    }

    private void testDisplaceFilters() {
        testFilterWithDialog("Displacement Map", FilterOptions.STANDARD);
        testFilterWithDialog("Drunk Vision", FilterOptions.STANDARD_RESEED);
        testFilterWithDialog("Grid Kaleidoscope", FilterOptions.STANDARD);
        testFilterWithDialog(JHKaleidoscope.NAME, FilterOptions.STANDARD);
        testFilterWithDialog(Mirror.NAME, FilterOptions.STANDARD);
        testFilterWithDialog("Offset", FilterOptions.TRIVIAL);
        testFilterWithDialog("Slice", FilterOptions.STANDARD);
        testFilterWithDialog("Tile Seamless", FilterOptions.STANDARD);
        testFilterWithDialog("Video Feedback", FilterOptions.STANDARD);
    }

    private void testLightFilters() {
        testFilterWithDialog("Bump Map", FilterOptions.STANDARD);
        testFilterWithDialog("Flashlight", FilterOptions.STANDARD);
        testFilterWithDialog("Glint", FilterOptions.STANDARD);
        testFilterWithDialog("Glow", FilterOptions.STANDARD);
        testFilterWithDialog("Rays", FilterOptions.STANDARD);
        testFilterWithDialog("Sparkle", FilterOptions.STANDARD_RESEED);
    }

    private void testNoiseFilters() {
        testFilterWithDialog("Kuwahara", FilterOptions.STANDARD);
        testNoDialogFilter("Reduce Single Pixel Noise");
        testNoDialogFilter("3x3 Median Filter");
        testFilterWithDialog("Add Noise", FilterOptions.STANDARD);
        testFilterWithDialog("Pixelate", FilterOptions.STANDARD);
    }

    private void testRenderFilters() {
        testFilterWithDialog("Clouds", FilterOptions.RENDERING_RESEED);
        testFilterWithDialog("Plasma", FilterOptions.RENDERING_RESEED);
        testFilterWithDialog("Value Noise", FilterOptions.RENDERING_RESEED);

        testFilterWithDialog("Abstract Lights", FilterOptions.RENDERING_RESEED);
        testFilterWithDialog("Brushed Metal", FilterOptions.RENDERING_RESEED);
        testFilterWithDialog("Caustics", FilterOptions.RENDERING_RESEED);
        testFilterWithDialog("Cells", FilterOptions.RENDERING_RESEED);
        testFilterWithDialog("Flow Field", FilterOptions.RENDERING_RESEED);
        testFilterWithDialog("Marble", FilterOptions.RENDERING_RESEED);
        testFilterWithDialog("Voronoi Diagram", FilterOptions.RENDERING_RESEED);
        testFilterWithDialog("Wood", FilterOptions.RENDERING_RESEED);

        // Curves
        testFilterWithDialog("Circle Weave", FilterOptions.SHAPES);
        testFilterWithDialog("Flower of Life", FilterOptions.SHAPES);
        testFilterWithDialog("Grid", FilterOptions.SHAPES);
        testFilterWithDialog("Lissajous Curve", FilterOptions.SHAPES);
        testFilterWithDialog("L-Systems", FilterOptions.SHAPES);
        testFilterWithDialog("Spider Web", FilterOptions.SHAPES);
        testFilterWithDialog("Spiral", FilterOptions.SHAPES);
        testFilterWithDialog("Spirograph", FilterOptions.SHAPES);

        // Fractals
        testFilterWithDialog("Chaos Game", FilterOptions.RENDERING);
        testFilterWithDialog("Fractal Tree", FilterOptions.RENDERING_RESEED);
        testFilterWithDialog("Julia Set", FilterOptions.RENDERING);
        testFilterWithDialog("Mandelbrot Set", FilterOptions.RENDERING);

        // Geometry
        testFilterWithDialog("Border Mask", FilterOptions.RENDERING);
        testFilterWithDialog("Concentric Shapes", FilterOptions.SHAPES);
        testFilterWithDialog("Checker Pattern", FilterOptions.RENDERING);
        testFilterWithDialog("Cubes Pattern", FilterOptions.SHAPES);
        testFilterWithDialog("Penrose Tiling", FilterOptions.SHAPES);
        testFilterWithDialog("Rose", FilterOptions.SHAPES);
        testFilterWithDialog("Starburst", FilterOptions.SHAPES);
        testFilterWithDialog("Stripes", FilterOptions.SHAPES);
        testFilterWithDialog("Truchet Tiles", FilterOptions.RENDERING);
    }

    private void testFindEdgesFilters() {
        testFilterWithDialog("Canny", FilterOptions.STANDARD);
        testFilterWithDialog("Convolution Edge Detection", FilterOptions.STANDARD);
        testFilterWithDialog("Difference of Gaussians", FilterOptions.STANDARD);
        testNoDialogFilter("Laplacian");
    }

    private void testGMICFilters() {
        app.resize(200);

        // Artistic
        testFilterWithDialog("Bokeh", FilterOptions.STANDARD_RESEED);
        testFilterWithDialog("Box Fitting", FilterOptions.STANDARD_RESEED);
        testFilterWithDialog("Brushify", FilterOptions.STANDARD_RESEED);
        testFilterWithDialog("Cubism", FilterOptions.STANDARD_RESEED);
        testFilterWithDialog("Huffman Glitches", FilterOptions.STANDARD_RESEED);
        testFilterWithDialog("Random 3D Objects", FilterOptions.STANDARD_RESEED);
        testFilterWithDialog("Rodilius", FilterOptions.STANDARD);
        testFilterWithDialog("Voronoi", FilterOptions.STANDARD_RESEED);

        // Blur/Sharpen
        testFilterWithDialog("Anisotropic Smoothing", FilterOptions.STANDARD);
        testFilterWithDialog("Bilateral Smoothing", FilterOptions.STANDARD);

        testFilterWithDialog("G'MIC Command", FilterOptions.STANDARD);
        testFilterWithDialog("Light Glow", FilterOptions.STANDARD);
        testFilterWithDialog("Local Normalization", FilterOptions.STANDARD);
        testFilterWithDialog("Stroke", FilterOptions.STANDARD);
        testFilterWithDialog("Vibrance", FilterOptions.STANDARD);

        // the image was reduced in size at the start of the GMIC filter tests
        app.closeCurrentView(AppRunner.ExpectConfirmation.YES);
        app.openFileWithDialog("Open...", config.getInputDir(), "a.jpg");
        maskMode.apply(context);
    }

    private void testOtherFilters() {
        testFilterWithDialog("Drop Shadow", FilterOptions.STANDARD);
        testFilterWithDialog("Morphology", FilterOptions.STANDARD);
//        testRandomFilter();
        testText();
        testFilterWithDialog("Transform Layer", FilterOptions.STANDARD);

        testConvolution();

        testFilterWithDialog("Channel to Transparency", FilterOptions.STANDARD);
        testNoDialogFilter("Invert Transparency");
    }

    private void testConvolution() {
        testFilterWithDialog("Custom 3x3 Convolution", FilterOptions.NONE, "Corner Blur", "\"Gaussian\" Blur", "Mean Blur", "Sharpen",
            "Edge Detection", "Edge Detection 2", "Horizontal Edge Detection",
            "Vertical Edge Detection", "Emboss", "Emboss 2", "Color Emboss",
            "Reset All", "Randomize");
        testFilterWithDialog("Custom 5x5 Convolution", FilterOptions.NONE, "Diamond Blur", "Motion Blur",
            "Find Horizontal Edges", "Find Vertical Edges",
            "Find / Edges", "Find \\ Edges", "Sharpen",
            "Reset All", "Randomize");
    }

    private void testTransitionsFilters() {
        testFilterWithDialog("2D Transitions", FilterOptions.STANDARD);
        testFilterWithDialog("Blinds Transition", FilterOptions.STANDARD);
        testFilterWithDialog("Checkerboard Transition", FilterOptions.STANDARD);
        testFilterWithDialog("Goo Transition", FilterOptions.STANDARD);
        testFilterWithDialog("Shapes Grid Transition", FilterOptions.STANDARD);
    }

    private void testColorBalance(boolean squashedImage) {
        app.runWithSelectionTranslationCombinations(squashedImage, () ->
            testFilterWithDialog(ColorBalance.NAME, FilterOptions.STANDARD), context);
    }

    private void testInvert(boolean squashedImage) {
        app.runWithSelectionTranslationCombinations(squashedImage, () ->
            testNoDialogFilter(Invert.NAME), context);
    }

    private void testText() {
        if (context.skip()) {
            return;
        }
        context.log(1, "filter Text");

        app.runMenuCommand("Text...");
        var dialog = app.findFilterDialog();

        app.testTextDialog(dialog, textFilterTested ?
            "my text" : TextSettings.DEFAULT_TEXT);

        findButtonByText(dialog, "OK").click();
        afterFilterRunActions("Text");

        textFilterTested = true;
    }

    private void testRandomFilter() {
        context.log(1, "random filter");

        app.runMenuCommand("Random Filter...");
        var dialog = app.findFilterDialog();
        var nextRandomButton = findButtonByText(dialog, "Next Random Filter");
        var backButton = findButtonByText(dialog, "Back");
        var forwardButton = findButtonByText(dialog, "Forward");

        nextRandomButton.requireEnabled();
        backButton.requireDisabled();
        forwardButton.requireDisabled();

        nextRandomButton.click();
        backButton.requireEnabled();
        forwardButton.requireDisabled();

        nextRandomButton.click();
        backButton.click();
        forwardButton.requireEnabled();

        backButton.click();
        forwardButton.click();
        nextRandomButton.click();

        findButtonByText(dialog, "OK").click();

        afterFilterRunActions("Random Filter");
    }

    private void testNoDialogFilter(String name) {
        if (context.skip()) {
            return;
        }
        context.log(1, "filter " + name);

        app.runMenuCommand(name);

        afterFilterRunActions(name);
    }

    private void testFilterWithDialog(String name,
                                      FilterOptions options,
                                      String... extraButtonsToClick) {
        if (context.skip()) {
            return;
        }
        context.log(1, "filter " + name);

        boolean testPresets = !config.isQuick() && maskMode == NO_MASK;

        Consumer<DialogFixture> extraButtonClicker = dialog -> {
            for (String buttonText : extraButtonsToClick) {
                JButtonFixture button = findButtonByText(dialog, buttonText);
                if (button.isEnabled()) { // channel mixer presets might not be enabled
                    button.click();
                }
            }
        };

        app.runFilterWithDialog(name, options, testPresets, extraButtonClicker);

        afterFilterRunActions(name);
    }

    private void afterFilterRunActions(String filterName) {
        // it could happen that a filter returns the source image,
        // and then nothing is put into the history
        if (History.getLastEditName().equals(filterName)) {
            keyboard.undoRedoUndo(filterName);
        }

        context.checkConsistency();
    }

    private void stressTestFilterWithDialog(String name, FilterOptions options, boolean resizeToSmall) {
        if (resizeToSmall) {
            app.resize(200);
            app.runMenuCommand("Zoom In");
            app.runMenuCommand("Zoom In");
        }

        String nameWithoutDots = name.substring(0, name.length() - 3);
        context.log(1, "filter " + nameWithoutDots);

        app.runMenuCommand(name);
        var dialog = app.findFilterDialog();

        int max = 1000;
        for (int i = 0; i < max; i++) {
            System.out.println("MainGuiTest stress testing " + nameWithoutDots + ": " + (i + 1) + " of " + max);
            if (options.randomize()) {
                findButtonByText(dialog, "Randomize Settings").click();
            }
            if (options.reseed()) {
                findButtonByText(dialog, "Reseed").click();
                findButtonByText(dialog, "Reseed").click();
                findButtonByText(dialog, "Reseed").click();
            }
        }

        dialog.button("ok").click();
    }
}
