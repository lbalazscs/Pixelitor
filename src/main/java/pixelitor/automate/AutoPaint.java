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

package pixelitor.automate;

import pixelitor.Composition;
import pixelitor.colors.Colors;
import pixelitor.filters.gui.DialogMenuBar;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.history.History;
import pixelitor.history.ImageEdit;
import pixelitor.layers.Drawable;
import pixelitor.tools.AbstractBrushTool;
import pixelitor.tools.Tool;
import pixelitor.tools.util.PPoint;
import pixelitor.utils.Lazy;
import pixelitor.utils.Messages;
import pixelitor.utils.ProgressHandler;
import pixelitor.utils.Rnd;

import java.awt.Color;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.SplittableRandom;

import static pixelitor.colors.FgBgColors.getBGColor;
import static pixelitor.colors.FgBgColors.getFGColor;
import static pixelitor.colors.FgBgColors.randomizeColors;
import static pixelitor.colors.FgBgColors.setBGColor;
import static pixelitor.colors.FgBgColors.setFGColor;
import static pixelitor.tools.Tools.BRUSH;
import static pixelitor.tools.Tools.CLONE;
import static pixelitor.tools.Tools.ERASER;
import static pixelitor.tools.Tools.SMUDGE;
import static pixelitor.utils.Texts.i18n;
import static pixelitor.utils.Threads.calledOnEDT;
import static pixelitor.utils.Threads.threadInfo;

/**
 * The "Auto Paint" functionality.
 */
public class AutoPaint {
    public static final Tool[] SUPPORTED_TOOLS = {SMUDGE, BRUSH, CLONE, ERASER};
    private static Color origFg;
    private static Color origBg;
    private static final Lazy<AutoPaintPanel> CONFIG_PANEL_FACTORY = Lazy.of(AutoPaintPanel::new);

    private AutoPaint() {
        // Utility class, no instantiation
    }

    public static void showDialog(Drawable dr) {
        var configPanel = CONFIG_PANEL_FACTORY.get();
        new DialogBuilder()
            .validatedContent(configPanel)
            .title(i18n("auto_paint"))
            .menuBar(new DialogMenuBar(configPanel))
            .okAction(() -> autoPaint(dr, configPanel.getSettings()))
            .show();
    }

    private static void autoPaint(Drawable dr, AutoPaintSettings settings) {
        assert calledOnEDT() : threadInfo();

        BufferedImage backupImage = dr.getSelectedSubImage(true);
        String statusBarMessage = "Auto Paint with " + settings.getTool().getName();
        ProgressHandler progressHandler = Messages.startProgress(statusBarMessage, settings.getNumStrokes());

        try {
            rememberOriginalColors();
            History.setIgnoreEdits(true);

            generateStrokes(settings, dr, progressHandler);
        } catch (Exception e) {
            Messages.showException(e);
        } finally {
            History.setIgnoreEdits(false);
            History.add(new ImageEdit("Auto Paint", dr.getComp(),
                dr, backupImage, false));

            progressHandler.stopProgress();
            Messages.showPlainStatusMessage(statusBarMessage + " finished.");

            restoreOriginalColors(settings);
        }
    }

    private static void generateStrokes(AutoPaintSettings settings,
                                        Drawable dr,
                                        ProgressHandler progressHandler) {
        assert calledOnEDT() : threadInfo();

        var random = new SplittableRandom();
        var comp = dr.getComp();
        int strokeCount = settings.getNumStrokes();

        for (int i = 0; i < strokeCount; i++) {
            progressHandler.updateProgress(i);

            generateSingleStroke(dr, settings, comp, random);
            comp.getView().paintImmediately();
        }
    }

    private static void generateSingleStroke(Drawable dr,
                                             AutoPaintSettings settings,
                                             Composition comp,
                                             SplittableRandom rand) {
        assert calledOnEDT() : threadInfo();

        setColors(settings, rand);
        PPoint start = comp.genRandomPointInCanvas();
        PPoint end = settings.genRandomEndPoint(start, comp, rand);

        Tool tool = settings.getTool();
        if (tool instanceof AbstractBrushTool abt) {
            Path2D strokePath = createStrokePath(start, end, settings);
            abt.trace(dr, strokePath);
        } else {
            throw new IllegalStateException("tool = " + tool.getClass().getName());
        }
    }

    private static void setColors(AutoPaintSettings settings, SplittableRandom rand) {
        if (settings.useRandomColors()) {
            randomizeColors();
        } else if (settings.useInterpolatedColors()) {
            setFGColor(Colors.interpolateRGB(origFg, origBg, rand.nextDouble()));
        }
    }

    private static Path2D createStrokePath(PPoint start, PPoint end, AutoPaintSettings settings) {
        Path2D path = new Path2D.Double();
        path.moveTo(start.getImX(), start.getImY());

        double controlX = (start.getImX() + end.getImX()) / 2.0;
        double controlY = (start.getImY() + end.getImY()) / 2.0;

        double curvature = settings.getMaxCurvature();
        if (curvature > 0) {
            double maxShift = start.imDist(end) * curvature;
            controlX += (Rnd.nextDouble() - 0.5) * maxShift;
            controlY += (Rnd.nextDouble() - 0.5) * maxShift;
        }

        path.quadTo(controlX, controlY, end.getImX(), end.getImY());
        return path;
    }

    private static void rememberOriginalColors() {
        origFg = getFGColor();
        origBg = getBGColor();
    }

    private static void restoreOriginalColors(AutoPaintSettings settings) {
        // if colors were changed, restore the original
        if (settings.changeColors()) {
            setFGColor(origFg);
            setBGColor(origBg);
        }
    }
}
