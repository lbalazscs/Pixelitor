/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.utils.test;

import org.jdesktop.swingx.painter.AbstractLayoutPainter;
import org.jdesktop.swingx.painter.effects.ShadowPathEffect;
import pixelitor.Build;
import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.MessageHandler;
import pixelitor.NewImage;
import pixelitor.automate.SingleDirChooserPanel;
import pixelitor.colors.FgBgColors;
import pixelitor.colors.FillType;
import pixelitor.filters.ColorWheel;
import pixelitor.filters.ValueNoise;
import pixelitor.filters.jhlabsproxies.JHDropShadow;
import pixelitor.filters.painters.AreaEffects;
import pixelitor.filters.painters.TextFilter;
import pixelitor.filters.painters.TextSettings;
import pixelitor.gui.ImageComponents;
import pixelitor.io.Directories;
import pixelitor.io.OutputFormat;
import pixelitor.layers.BlendingMode;
import pixelitor.layers.Drawable;
import pixelitor.layers.ImageLayer;
import pixelitor.tools.ImDrag;
import pixelitor.tools.gradient.GradientTool;
import pixelitor.tools.gradient.GradientType;
import pixelitor.utils.Messages;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.io.File;

import static java.awt.Color.WHITE;
import static java.awt.MultipleGradientPaint.CycleMethod.REFLECT;
import static pixelitor.ChangeReason.FILTER_WITHOUT_DIALOG;
import static pixelitor.tools.gradient.GradientColorType.BLACK_TO_WHITE;

/**
 * Static methods for creating the splash images
 */
public class SplashImageCreator {
    //    public static final String SPLASH_SCREEN_FONT = "Comic Sans MS";
    private static final String SPLASH_SCREEN_FONT = "DejaVu Sans Light";

    private SplashImageCreator() {
    }

    public static void saveManySplashImages() {
        boolean okPressed = SingleDirChooserPanel.selectOutputDir(true);
        if (!okPressed) {
            return;
        }
        int numCreatedImages = 32;

        MessageHandler msgHandler = Messages.getMessageHandler();
        String msg = String.format("Save %d Splash Images: ", numCreatedImages);
        msgHandler.startProgress(msg, numCreatedImages);
        File lastSaveDir = Directories.getLastSaveDir();

        for (int i = 0; i < numCreatedImages; i++) {
            OutputFormat outputFormat = OutputFormat.getLastUsed();

            String fileName = String.format("splash%04d.%s", i, outputFormat.toString());

            msgHandler.updateProgress(i);

            createSplashImage();

            ImageComponents.onActiveICAndComp((ic, comp) -> {
                ic.paintImmediately(ic.getBounds());
                File f = new File(lastSaveDir, fileName);

                outputFormat.saveComp(comp, f, false);

                ic.close();
                ValueNoise.reseed();
            });
        }
        msgHandler.stopProgress();
        msgHandler.showInStatusBar(String.format("Finished saving splash images to %s", lastSaveDir));
    }

    public static void createSplashImage() {
        assert EventQueue.isDispatchThread();

        Composition comp = NewImage.addNewImage(FillType.WHITE, 400, 247, "Splash");
        ImageLayer layer = (ImageLayer) comp.getLayer(0);

        layer.setName("Color Wheel", true);
        new ColorWheel().startOn(layer, FILTER_WITHOUT_DIALOG);

        layer = addNewLayer(comp, "Value Noise");
        ValueNoise valueNoise = new ValueNoise();
        valueNoise.setDetails(7);
        valueNoise.startOn(layer, FILTER_WITHOUT_DIALOG);
        layer.setOpacity(0.3f, true, true, true);
        layer.setBlendingMode(BlendingMode.SCREEN, true, true, true);

        layer = addNewLayer(comp, "Gradient");
        addRadialBWGradientToActiveDrawable(layer, true);
        layer.setOpacity(0.4f, true, true, true);
        layer.setBlendingMode(BlendingMode.LUMINOSITY, true, true, true);

        FgBgColors.setFG(WHITE);
        Font font = new Font(SPLASH_SCREEN_FONT, Font.BOLD, 48);
        layer = addRasterizedTextLayer(comp, "Pixelitor", WHITE, font, -17, BlendingMode.NORMAL, 0.9f, false);
        addDropShadow(layer);

        font = new Font(SPLASH_SCREEN_FONT, Font.BOLD, 22);
        layer = addRasterizedTextLayer(comp, "Loading...", WHITE, font, -70, BlendingMode.NORMAL, 0.9f, false);
        addDropShadow(layer);

        font = new Font(SPLASH_SCREEN_FONT, Font.PLAIN, 20);
        layer = addRasterizedTextLayer(comp, "version " + Build.VERSION_NUMBER, WHITE, font, 50, BlendingMode.NORMAL, 0.9f, false);
        addDropShadow(layer);

//        font = new Font(Font.SANS_SERIF, Font.PLAIN, 10);
//        addRasterizedTextLayer(ic, new Date().toString(), font, 0.8f, 100, false);

    }

    private static void addDropShadow(ImageLayer layer) {
        JHDropShadow dropShadow = new JHDropShadow();
        dropShadow.setDistance(5);
        dropShadow.setSoftness(5);
        dropShadow.setOpacity(0.7f);
        dropShadow.startOn(layer, FILTER_WITHOUT_DIALOG);
    }

    private static ImageLayer addNewLayer(Composition comp, String name) {
        ImageLayer imageLayer = comp.addNewEmptyLayer(name, false);
        imageLayer.setName(name, true);
        return imageLayer;
    }

    private static void addRasterizedTextLayer(Composition comp, String text, int translationY) {
        Font font = new Font(Font.SANS_SERIF, Font.BOLD, 20);
        addRasterizedTextLayer(comp, text, WHITE, font, translationY, BlendingMode.NORMAL, 1.0f, false);
    }

    private static ImageLayer addRasterizedTextLayer(Composition comp, String text, Color textColor, Font font, int translationY, BlendingMode blendingMode, float opacity, boolean dropShadow) {
        ImageLayer layer = addNewLayer(comp, text);
        TextFilter textFilter = TextFilter.getInstance();

        AreaEffects effects = null;
        if (dropShadow) {
            effects = new AreaEffects();
            effects.setDropShadowEffect(new ShadowPathEffect(1.0f));
        }

        TextSettings settings = new TextSettings(text, font, textColor, effects,
                AbstractLayoutPainter.HorizontalAlignment.CENTER,
                AbstractLayoutPainter.VerticalAlignment.CENTER, false, 0);

        textFilter.setSettings(settings);
        textFilter.startOn(layer, FILTER_WITHOUT_DIALOG);
        layer.setTranslation(0, translationY);

        layer.enlargeImage(layer.getComp().getCanvasImBounds());

        layer.setOpacity(opacity, true, true, true);
        layer.setBlendingMode(blendingMode, true, true, true);

        return layer;
    }

    public static void addRadialBWGradientToActiveDrawable(Drawable dr, boolean radial) {
        Canvas canvas = dr.getComp().getCanvas();
        int canvasWidth = canvas.getImWidth();
        int canvasHeight = canvas.getImHeight();

        int startX = canvasWidth / 2;
        int startY = canvasHeight / 2;

        int endX = 0;
        int endY = 0;
        if (canvasWidth > canvasHeight) {
            endX = startX;
        } else {
            endY = startY;
        }

        GradientType gradientType;

        if (radial) {
            gradientType = GradientType.RADIAL;
        } else {
            gradientType = GradientType.SPIRAL_CW;
        }

        GradientTool.drawGradient(dr,
                gradientType,
                BLACK_TO_WHITE,
                REFLECT,
                AlphaComposite.SrcOver,
                new ImDrag(startX, startY, endX, endY),
                false);
    }
}
