/*
 * Copyright 2016 Laszlo Balazs-Csiki
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
import pixelitor.Composition;
import pixelitor.FgBgColors;
import pixelitor.FillType;
import pixelitor.MessageHandler;
import pixelitor.NewImage;
import pixelitor.automate.SingleDirChooserPanel;
import pixelitor.filters.ColorWheel;
import pixelitor.filters.ValueNoise;
import pixelitor.filters.jhlabsproxies.JHDropShadow;
import pixelitor.filters.painters.AreaEffects;
import pixelitor.filters.painters.TextFilter;
import pixelitor.filters.painters.TextSettings;
import pixelitor.gui.ImageComponent;
import pixelitor.gui.ImageComponents;
import pixelitor.gui.PixelitorWindow;
import pixelitor.history.AddToHistory;
import pixelitor.io.FileChoosers;
import pixelitor.io.OutputFormat;
import pixelitor.layers.AddNewLayerAction;
import pixelitor.layers.BlendingMode;
import pixelitor.layers.ImageLayer;
import pixelitor.tools.GradientTool;
import pixelitor.tools.GradientType;
import pixelitor.tools.UserDrag;
import pixelitor.utils.Messages;
import pixelitor.utils.UpdateGUI;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.File;

import static java.awt.Color.WHITE;
import static java.awt.MultipleGradientPaint.CycleMethod.REFLECT;
import static pixelitor.ChangeReason.OP_WITHOUT_DIALOG;
import static pixelitor.tools.GradientColorType.BLACK_TO_WHITE;

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

        MessageHandler messageHandler = Messages.getMessageHandler();
        String msg = String.format("Save %d Splash Images: ", numCreatedImages);
        messageHandler.startProgress(msg, numCreatedImages);
        File lastSaveDir = FileChoosers.getLastSaveDir();

        for (int i = 0; i < numCreatedImages; i++) {
            OutputFormat outputFormat = OutputFormat.getLastOutputFormat();

            String fileName = String.format("splash%04d.%s", i, outputFormat.toString());

            messageHandler.updateProgress(i);

            createSplashImage();
            ImageComponent ic = ImageComponents.getActiveIC();
            ic.paintImmediately(ic.getBounds());

            File f = new File(lastSaveDir, fileName);

            Composition comp = ic.getComp();
            outputFormat.saveComposition(comp, f, false);

            ic.close();
            ValueNoise.reseed();
        }
        messageHandler.stopProgress();
        messageHandler.showStatusMessage(String.format("Finished saving splash images to %s", lastSaveDir));
    }

    public static void createSplashImage() {
        assert EventQueue.isDispatchThread();

        NewImage.addNewImage(FillType.WHITE, 400, 247, "Splash");

        Composition ic = ImageComponents.getActiveComp().get();
        ic.getActiveLayer().setName("Color Wheel", AddToHistory.YES);
        new ColorWheel().execute(OP_WITHOUT_DIALOG);

        addNewLayer("Value Noise");
        ValueNoise valueNoise = new ValueNoise();
        valueNoise.setDetails(7);
        valueNoise.execute(OP_WITHOUT_DIALOG);
        ImageLayer layer = (ImageLayer) ic.getActiveLayer();
        layer.setOpacity(0.3f, UpdateGUI.YES, AddToHistory.YES, true);
        layer.setBlendingMode(BlendingMode.SCREEN, UpdateGUI.YES, AddToHistory.YES, true);

        addNewLayer("Gradient");
        addRadialBWGradientToActiveLayer(ic, true);
        layer = (ImageLayer) ic.getActiveLayer();
        layer.setOpacity(0.4f, UpdateGUI.YES, AddToHistory.YES, true);
        layer.setBlendingMode(BlendingMode.LUMINOSITY, UpdateGUI.YES, AddToHistory.YES, true);

        FgBgColors.setFG(WHITE);
        Font font = new Font(SPLASH_SCREEN_FONT, Font.BOLD, 48);
        addRasterizedTextLayer(ic, "Pixelitor", WHITE, font, -17, BlendingMode.NORMAL, 0.9f, false);
        addDropShadow();

        font = new Font(SPLASH_SCREEN_FONT, Font.BOLD, 22);
        addRasterizedTextLayer(ic, "Loading...", WHITE, font, -70, BlendingMode.NORMAL, 0.9f, false);
        addDropShadow();

        font = new Font(SPLASH_SCREEN_FONT, Font.PLAIN, 20);
        addRasterizedTextLayer(ic, "version " + Build.VERSION_NUMBER, WHITE, font, 50, BlendingMode.NORMAL, 0.9f, false);
        addDropShadow();

//        font = new Font(Font.SANS_SERIF, Font.PLAIN, 10);
//        addRasterizedTextLayer(ic, new Date().toString(), font, 0.8f, 100, false);

    }

    private static void addDropShadow() {
        JHDropShadow dropShadow = new JHDropShadow();
        dropShadow.setDistance(5);
        dropShadow.setSoftness(5);
        dropShadow.setOpacity(0.7f);
        dropShadow.execute(OP_WITHOUT_DIALOG);
    }

    private static void addNewLayer(String name) {
        AddNewLayerAction.INSTANCE.actionPerformed(new ActionEvent(PixelitorWindow.getInstance(), 0, name));
        ImageComponents.getActiveLayer().get().setName(name, AddToHistory.YES);
    }

    private static void addRasterizedTextLayer(Composition ic, String text, int translationY) {
        Font font = new Font(Font.SANS_SERIF, Font.BOLD, 20);
        addRasterizedTextLayer(ic, text, WHITE, font, translationY, BlendingMode.NORMAL, 1.0f, false);
    }

    private static void addRasterizedTextLayer(Composition ic, String text, Color textColor, Font font, int translationY, BlendingMode blendingMode, float opacity, boolean dropShadow) {
        addNewLayer(text);
        TextFilter textFilter = TextFilter.getInstance();

        AreaEffects effects = null;
        if (dropShadow) {
            effects = new AreaEffects();
            effects.setDropShadowEffect(new ShadowPathEffect(1.0f));
        }

        TextSettings settings = new TextSettings(text, font, textColor, effects,
                AbstractLayoutPainter.HorizontalAlignment.CENTER,
                AbstractLayoutPainter.VerticalAlignment.CENTER, false);

        textFilter.setSettings(settings);
        textFilter.execute(OP_WITHOUT_DIALOG);
        ImageLayer layer = (ImageLayer) ic.getActiveLayer();
        layer.setTranslation(0, translationY);

        layer.enlargeImage(layer.getComp().getCanvasBounds());

        layer.setOpacity(opacity, UpdateGUI.YES, AddToHistory.YES, true);
        layer.setBlendingMode(blendingMode, UpdateGUI.YES, AddToHistory.YES, true);
    }

    public static void addRadialBWGradientToActiveLayer(Composition comp, boolean radial) {
        int canvasWidth = comp.getCanvasWidth();
        int canvasHeight = comp.getCanvasHeight();

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

        GradientTool.drawGradient(comp.getActiveMaskOrImageLayer(),
                gradientType,
                BLACK_TO_WHITE,
                REFLECT,
                AlphaComposite.SrcOver,
                new UserDrag(startX, startY, endX, endY),
                false);
    }
}
