/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

import org.jdesktop.swingx.painter.AbstractLayoutPainter.HorizontalAlignment;
import org.jdesktop.swingx.painter.AbstractLayoutPainter.VerticalAlignment;
import org.jdesktop.swingx.painter.effects.ShadowPathEffect;
import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.NewImage;
import pixelitor.Pixelitor;
import pixelitor.automate.DirectoryChooser;
import pixelitor.colors.FgBgColors;
import pixelitor.colors.FillType;
import pixelitor.filters.AbstractLights;
import pixelitor.filters.painters.AreaEffects;
import pixelitor.filters.painters.TextSettings;
import pixelitor.gui.utils.AlignmentSelector;
import pixelitor.io.Dirs;
import pixelitor.io.FileFormat;
import pixelitor.io.FileUtils;
import pixelitor.io.SaveSettings;
import pixelitor.layers.*;
import pixelitor.tools.gradient.Gradient;
import pixelitor.tools.gradient.GradientType;
import pixelitor.tools.util.Drag;
import pixelitor.utils.Geometry;
import pixelitor.utils.Messages;
import pixelitor.utils.ProgressHandler;
import pixelitor.utils.Rnd;

import java.awt.Font;
import java.awt.font.TextAttribute;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.awt.Color.WHITE;
import static java.awt.MultipleGradientPaint.CycleMethod.REFLECT;
import static java.awt.font.TextAttribute.KERNING;
import static java.awt.font.TextAttribute.KERNING_ON;
import static java.awt.font.TextAttribute.LIGATURES;
import static java.awt.font.TextAttribute.LIGATURES_ON;
import static java.lang.String.format;
import static pixelitor.layers.BlendingMode.NORMAL;
import static pixelitor.tools.gradient.GradientColorType.BLACK_TO_WHITE;
import static pixelitor.utils.Threads.callInfo;
import static pixelitor.utils.Threads.calledOnEDT;
import static pixelitor.utils.Threads.onEDT;

/**
 * Static methods for creating and saving splash images.
 */
public class SplashImageCreator {
    private static final String SPLASH_SMALL_FONT = "Mochiy Pop One";
    private static final String SPLASH_MAIN_FONT = "Mochiy Pop One";
    private static final int MAIN_FONT_SIZE = 62;

    private static final int SPLASH_WIDTH = 400;
    private static final int SPLASH_HEIGHT = 247;

    private SplashImageCreator() {
        // prevent instantiation
    }

    public static void saveSplashImages(int numImages) {
        assert calledOnEDT() : callInfo();

        if (!DirectoryChooser.selectOutputDir(FileFormat.PNG)) {
            return;
        }

        String msg = format("Save %d Splash Images", numImages);
        var progressHandler = Messages.startProgress(msg, numImages);

        FileFormat format = FileFormat.getLastSaved();
        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);

        for (int i = 0; i < numImages; i++) {
            int sequenceNumber = i;
            chain = chain.thenCompose(v ->
                makeSplashAsync(progressHandler, sequenceNumber, format));
        }
        chain.thenRunAsync(() ->
            finalizeAfterSplashesBatch(numImages, progressHandler), onEDT);
    }

    private static CompletableFuture<Void> makeSplashAsync(ProgressHandler ph,
                                                           int sequenceNumber,
                                                           FileFormat format) {
        return CompletableFuture
            .supplyAsync(() -> genSplashComp(ph, sequenceNumber, format), onEDT)
            .thenCompose(comp -> saveAndCloseCompAsync(comp, format));
    }

    private static Composition genSplashComp(ProgressHandler ph,
                                             int sequenceNumber,
                                             FileFormat format) {
        ph.updateProgress(sequenceNumber);

        Composition comp = createSplashComp();
        comp.getView().paintImmediately();

        String fileName = format("splash%04d.%s", sequenceNumber, format);
        comp.setFile(new File(Dirs.getLastSave(), fileName));

        return comp;
    }

    public static Composition createSplashComp() {
        assert calledOnEDT() : callInfo();

        FgBgColors.setFGColor(WHITE);
        FgBgColors.setBGColor(Rnd.createRandomColor().darker().darker().darker());

        Composition comp = NewImage.addNewImage(FillType.WHITE,
            SPLASH_WIDTH, SPLASH_HEIGHT, "Splash");
        ImageLayer baseLayer = (ImageLayer) comp.getLayer(0);

        baseLayer.replaceWithSmartObject();
        SmartObject so = (SmartObject) comp.getLayer(0);

        AbstractLights lights = new AbstractLights();
        lights.randomize();

        SmartFilter lightsSF = new SmartFilter(lights, so.getContent(), so);
        so.addSmartFilter(lightsSF, true, true);
        lightsSF.evaluateNow();

        addTextLayers(comp);

        return comp;
    }

    private static void addTextLayers(Composition comp) {
        FgBgColors.setFGColor(WHITE);

        Font smallFont = createSplashFont(SPLASH_SMALL_FONT, 20);
        addTextLayer(comp, "version " + Pixelitor.VERSION, smallFont, 50);

        Font mainFont = createSplashFont(SPLASH_MAIN_FONT, MAIN_FONT_SIZE);
        addTextLayer(comp, "Pixelitor", mainFont, -17);

        Font loadingFont = createSplashFont(SPLASH_SMALL_FONT, 22);
        addTextLayer(comp, "Loading...", loadingFont, -70);
    }

    private static Font createSplashFont(String name, int size) {
        Font font = new Font(name, Font.PLAIN, size);

        // check that the font exists
        assert font.getName().equals(name) : font.getName();

        Map<TextAttribute, Object> attr = new HashMap<>();
        attr.put(KERNING, KERNING_ON);
        attr.put(LIGATURES, LIGATURES_ON);

        return font.deriveFont(attr);
    }

    private static void addNewTextLayer(Composition comp, String name,
                                        TextSettings settings, int translationY) {
        var layer = new TextLayer(comp, name, settings);

        layer.setTranslation(0, translationY);

        comp.addWithHistory(layer, "add " + name);
    }

    private static void addTextLayer(Composition comp, String text,
                                     Font font, int translationY) {
        AreaEffects effects = createDropShadowEffect();
        TextSettings settings = new TextSettings(text, font, WHITE, effects,
            HorizontalAlignment.CENTER, VerticalAlignment.CENTER, AlignmentSelector.CENTER,
            false, 0, 1.0, 1.0, 1.0, 0.0, 0.0, null);
        addNewTextLayer(comp, text, settings, translationY);
    }

    private static AreaEffects createDropShadowEffect() {
        var effects = new AreaEffects();
        var dropShadowEffect = new ShadowPathEffect(0.6f);
        dropShadowEffect.setEffectWidth(3);
        dropShadowEffect.setOffset(Geometry.polarToCartesian(4, 0.7));
        effects.setDropShadow(dropShadowEffect);
        return effects;
    }

    private static void addBWGradientTo(Drawable dr) {
        Canvas canvas = dr.getComp().getCanvas();
        int centerX = canvas.getWidth() / 2;
        int centerY = canvas.getHeight() / 2;

        // ensure that the radial gradient fully fits inside the canvas
        int endX;
        int endY;
        if (canvas.isLandscape()) {
            endX = centerX;
            endY = 0;
        } else {
            endX = 0;
            endY = centerY;
        }

        Gradient gradient = new Gradient(new Drag(centerX, centerY, endX, endY),
            GradientType.RADIAL, REFLECT, BLACK_TO_WHITE, false, NORMAL, 1.0f);
        gradient.paintOn(dr);
    }

    private static CompletableFuture<Void> saveAndCloseCompAsync(Composition comp, FileFormat format) {
        var flatSettings = new SaveSettings.Simple(format, comp.getFile());

        var pxcFile = new File(comp.getFile().getParent(),
            FileUtils.replaceExtension(comp.getFile().getName(), "pxc"));
        var pxcSettings = new SaveSettings.Simple(FileFormat.PXC, pxcFile);

        return comp.saveAsync(flatSettings, false)
            .thenCompose(v -> comp.saveAsync(pxcSettings, false))
            .thenAcceptAsync(v -> comp.getView().close(), onEDT);
    }

    private static void finalizeAfterSplashesBatch(int numCreatedImages,
                                                   ProgressHandler progressHandler) {
        progressHandler.stopProgress();
        Messages.showPlainStatusMessage(format("Finished saving %d splash images to %s",
            numCreatedImages, Dirs.getLastSave()));
    }
}
