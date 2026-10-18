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

package pixelitor.autopaint;

import com.jhlabs.math.Noise;
import pixelitor.Canvas;
import pixelitor.colors.FgBgColors;
import pixelitor.filters.gui.DialogMenuBar;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.history.History;
import pixelitor.history.ImageEdit;
import pixelitor.layers.Drawable;
import pixelitor.progress.ProgressHandler;
import pixelitor.tools.AbstractBrushTool;
import pixelitor.utils.Lazy;
import pixelitor.utils.Messages;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;

import static pixelitor.colors.FgBgColors.getBgColor;
import static pixelitor.colors.FgBgColors.getFgColor;
import static pixelitor.tools.Tools.*;
import static pixelitor.utils.Texts.i18n;
import static pixelitor.utils.Threads.callInfo;
import static pixelitor.utils.Threads.calledOnEDT;

/**
 * The "Auto Paint" feature: generates random brush strokes across the canvas.
 */
public class AutoPaint {
    public static final AbstractBrushTool[] SUPPORTED_TOOLS = {SMUDGE, BRUSH, CLONE, ERASER};
    private static final Lazy<AutoPaintPanel> CONFIG_PANEL = Lazy.of(AutoPaintPanel::new);
    private static final long MIN_REPAINT_INTERVAL_NANOS = 30_000_000L; // ~30 fps
    public static final String NAME = i18n("auto_paint");

    private AutoPaint() {
        // utility class, no instantiation
    }

    public static void showDialog(Drawable dr) {
        var configPanel = CONFIG_PANEL.get();
        new DialogBuilder()
            .validatedContent(configPanel)
            .title(NAME)
            .menuBar(new DialogMenuBar(configPanel))
            .okAction(() -> autoPaint(dr, configPanel.getSettings()))
            .show();
    }

    private static void autoPaint(Drawable dr, AutoPaintSettings settings) {
        assert calledOnEDT() : callInfo();

        // a run only ever changes the foreground color
        Color origFg = getFgColor();
        Color origBg = getBgColor();
        BufferedImage backupImage = dr.getSelectedSubImage(true);
        String statusBarMessage = "Auto Paint with " + settings.tool().getName();
        ProgressHandler progressHandler = Messages.startProgress(statusBarMessage, settings.numStrokes());
        boolean completed = false;

        try {
            History.setIgnoreEdits(true);
            generateStrokes(settings, dr, progressHandler, origFg, origBg);
            completed = true;
        } catch (Exception e) {
            Messages.showException(e);
        } finally {
            History.setIgnoreEdits(false);
            History.add(new ImageEdit(NAME, dr.getComp(),
                dr, backupImage, false));
            setFgIfChanged(origFg);
            progressHandler.stopProgress();
        }
        if (completed) {
            Messages.showPlainStatusMessage(statusBarMessage + " finished.");
        }
    }

    private static void generateStrokes(AutoPaintSettings settings, Drawable dr,
                                        ProgressHandler progressHandler,
                                        Color origFg, Color origBg) {
        var rng = new SplittableRandom();
        var comp = dr.getComp();
        var canvas = comp.getCanvas();
        var view = comp.getView();
        int strokeCount = settings.numStrokes();

        boolean dynamicColors = settings.colorMode() != ColorMode.FOREGROUND;

        if (settings.strokeDirection() == StrokeDirection.NOISE) {
            Noise.reseed(System.nanoTime());
        }

        long lastPaint = System.nanoTime();
        for (int i = 0; i < strokeCount; i++) {
            if (dynamicColors) {
                setFgIfChanged(settings.colorMode().pickStrokeColor(origFg, origBg, rng));
            }
            generateSingleStroke(dr, settings, canvas, rng);

            long now = System.nanoTime();
            if (now - lastPaint >= MIN_REPAINT_INTERVAL_NANOS) {
                progressHandler.updateProgress(i + 1);
                view.paintImmediately();
                lastPaint = System.nanoTime();
            }
        }
        view.paintImmediately();
    }

    private static void generateSingleStroke(Drawable dr,
                                             AutoPaintSettings settings,
                                             Canvas canvas,
                                             RandomGenerator rng) {
        settings.tool().trace(settings.createRandomStrokePath(canvas, rng), dr);
    }

    private static void setFgIfChanged(Color color) {
        if (!color.equals(getFgColor())) {
            // changes the foreground color without affecting the color history
            FgBgColors.setFgColorNoHistory(color);
        }
    }

    // whether the given tool uses the global foreground color
    public static boolean toolUsesColors(AbstractBrushTool selectedTool) {
        return selectedTool == BRUSH;
    }
}
