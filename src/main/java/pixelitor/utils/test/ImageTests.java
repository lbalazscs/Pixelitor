/*
 * Copyright 2015 Laszlo Balazs-Csiki
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
import pixelitor.ImageComponents;
import pixelitor.NewImage;
import pixelitor.PixelitorWindow;
import pixelitor.automate.Automate;
import pixelitor.automate.SingleDirChooserPanel;
import pixelitor.filters.ColorWheel;
import pixelitor.filters.ValueNoise;
import pixelitor.filters.comp.CompAction;
import pixelitor.filters.jhlabsproxies.JHDropShadow;
import pixelitor.filters.jhlabsproxies.JHGaussianBlur;
import pixelitor.filters.painters.AreaEffects;
import pixelitor.filters.painters.TextFilter;
import pixelitor.filters.painters.TextSettings;
import pixelitor.history.AddToHistory;
import pixelitor.io.FileChoosers;
import pixelitor.io.OutputFormat;
import pixelitor.layers.AddNewLayerAction;
import pixelitor.layers.BlendingMode;
import pixelitor.layers.DeleteActiveLayerAction;
import pixelitor.layers.ImageLayer;
import pixelitor.utils.Messages;
import pixelitor.utils.UpdateGUI;
import pixelitor.utils.Utils;

import javax.swing.*;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.File;
import java.lang.reflect.InvocationTargetException;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static pixelitor.ChangeReason.OP_WITHOUT_DIALOG;
import static pixelitor.Composition.ImageChangeActions.FULL;

/**
 *
 */
public class ImageTests {

//    public static final String SPLASH_SCREEN_FONT = "Comic Sans MS";
private static final String SPLASH_SCREEN_FONT = "DejaVu Sans Light";

    /**
     * Utility class with static methods
     */
    private ImageTests() {
    }

    public static void saveManySplashImages() {
        boolean okPressed = SingleDirChooserPanel.selectOutputDir(true);
        if (!okPressed) {
            return;
        }

        ProgressMonitor progressMonitor = Utils.createPercentageProgressMonitor("Save Many Splash Images", "Stop");

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            public Void doInBackground() {
                int nurOfSplashImages = 32;

                for (int i = 0; i < nurOfSplashImages; i++) {
                    OutputFormat outputFormat = OutputFormat.getLastOutputFormat();

                    String fileName = String.format("splash%04d.%s", i, outputFormat.toString());

                    progressMonitor.setProgress((int) ((float) i * 100 / nurOfSplashImages));
                    progressMonitor.setNote("Creating " + fileName);
                    if (progressMonitor.isCanceled()) {
                        break;
                    }

                    Runnable guiTask = () -> {
                        createSplashImage();

                        File lastSaveDir = FileChoosers.getLastSaveDir();
                        File f = new File(lastSaveDir, fileName);

                        Composition comp = ImageComponents.getActiveComp().get();

                        outputFormat.saveComposition(comp, f, false);

                        ImageComponents.getActiveIC().close();
                        ValueNoise.reseed();
                    };
                    try {
                        EventQueue.invokeAndWait(guiTask);
                        Thread.sleep(1000L);
                    } catch (InterruptedException | InvocationTargetException e) {
                        Messages.showException(e);
                    }
                } // end of for loop
                progressMonitor.close();
                return null;
            } // end of doInBackground()
        };
        worker.execute();

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
        ToolTests.addRadialBWGradientToActiveLayer(ic, true);
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

    public static void ioOverlayBlur() {
        boolean selected = Automate.selectInputAndOutputDir(false, "Overlay Blur - select the input and output folders");
        if (!selected) {
            return;
        }

        CompAction ca = comp -> {
            comp.addNewLayerFromComposite("Overlay Blur");
            comp.getActiveLayer().setBlendingMode(BlendingMode.OVERLAY, UpdateGUI.YES, AddToHistory.YES, true);
            JHGaussianBlur blur = new JHGaussianBlur();
            blur.setRadius(5);
            blur.execute(OP_WITHOUT_DIALOG);
        };
        Automate.processEachFile(ca, false, "Overlay Blur Progress");
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

    public static void testLayers() {
        FgBgColors.setBG(WHITE);
        FgBgColors.setFG(BLACK);
        NewImage.addNewImage(FillType.TRANSPARENT, 400, 400, "Layer Test");
        Composition comp = ImageComponents.getActiveComp().get();

        addRasterizedTextLayer(comp, "this should be deleted", 0);

        addRasterizedTextLayer(comp, "this should at the bottom", 100);
        comp.moveActiveLayerToBottom();

        comp.moveLayerSelectionUp();
        comp.moveLayerSelectionUp();
        DeleteActiveLayerAction.INSTANCE.actionPerformed(null);

        addRasterizedTextLayer(comp, "this should at the top", -100);
        addRasterizedTextLayer(comp, "this should be selected", 50);
        comp.moveActiveLayerDown();

//        ic.moveActiveLayerDown();
//        ic.flattenImage();


        // merge down
        // HueSat
        // ColorBalance
        // Channel mixer

        comp.imageChanged(FULL);
    }

    private static void addRasterizedTextLayer(Composition ic, String text, int translationY) {
        Font font = new Font(Font.SANS_SERIF, Font.BOLD, 20);
        addRasterizedTextLayer(ic, text, WHITE, font, translationY, BlendingMode.NORMAL, 1.0f, false);
    }

    private static void addRasterizedTextLayer(Composition ic, String text, Color textColor, Font font, int translationY, BlendingMode blendingMode, float opacity, boolean dropShadow) {
        addNewLayer(text);
        TextFilter textFilter = TextFilter.INSTANCE;

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
}
