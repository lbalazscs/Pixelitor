/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.colors.Colors;
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
import java.util.Random;

import static java.lang.String.format;
import static pixelitor.colors.FgBgColors.*;
import static pixelitor.tools.Tools.*;
import static pixelitor.utils.Texts.i18n;
import static pixelitor.utils.Threads.calledOnEDT;
import static pixelitor.utils.Threads.threadInfo;

/**
 * The "Auto Paint" functionality
 */
public class AutoPaint {
    public static final Tool[] ALLOWED_TOOLS = {SMUDGE, BRUSH, CLONE, ERASER};
    private static Color origFg;
    private static Color origBg;
    private static final Lazy<AutoPaintPanel> panelFactory = Lazy.of(AutoPaintPanel::new);

    private AutoPaint() {
    }

    public static void showDialog(Drawable dr) {
        var configPanel = panelFactory.get();
        new DialogBuilder()
            .validatedContent(configPanel)
            .title(i18n("auto_paint"))
            .okAction(() -> paintStrokes(dr, configPanel.getSettings()))
            .show();
    }

    private static void paintStrokes(Drawable dr, AutoPaintSettings settings) {
        assert calledOnEDT() : threadInfo();

        saveOriginalFgBgColors();
        History.setIgnoreEdits(true);
        BufferedImage backupImage = dr.getSelectedSubImage(true);

        String msg = format("Auto Paint with %s Tool: ", settings.getTool());
        var progressHandler = Messages.startProgress(msg, settings.getNumStrokes());

        try {
            runStrokes(settings, dr, progressHandler);
        } catch (Exception e) {
            Messages.showException(e);
        } finally {
            History.setIgnoreEdits(false);
            History.add(new ImageEdit("Auto Paint", dr.getComp(),
                dr, backupImage, false));

            progressHandler.stopProgress();
            Messages.showPlainInStatusBar(msg + "finished.");

            restoreOriginalFgBgColors(settings);
        }
    }

    private static void runStrokes(AutoPaintSettings settings,
                                   Drawable dr,
                                   ProgressHandler progressHandler) {
        assert calledOnEDT() : threadInfo();

        var random = new Random();
        var comp = dr.getComp();

        int numStrokes = settings.getNumStrokes();
        for (int i = 0; i < numStrokes; i++) {
            progressHandler.updateProgress(i);

            paintSingleStroke(dr, settings, comp, random);
            comp.getView().paintImmediately();
        }
    }

    private static void paintSingleStroke(Drawable dr,
                                          AutoPaintSettings settings,
                                          Composition comp,
                                          Random rand) {
        assert calledOnEDT() : threadInfo();

        setFgBgColors(settings, rand);

        PPoint start = calcRandomStartPoint(comp, rand);
        PPoint end = settings.calcRandomEndPoint(start, comp, rand);

        drawBrushStroke(dr, start, end, settings);
    }

    private static void setFgBgColors(AutoPaintSettings settings, Random rand) {
        if (settings.useRandomColors()) {
            randomizeColors();
        } else if (settings.useInterpolatedColors()) {
            Color interpolated = Colors.rgbInterpolate(
                origFg, origBg, rand.nextFloat());
            setFGColor(interpolated);
        }
    }

    private static PPoint calcRandomStartPoint(Composition comp, Random rand) {
        Canvas canvas = comp.getCanvas();
        return PPoint.lazyFromIm(
            rand.nextInt(canvas.getWidth()),
            rand.nextInt(canvas.getHeight()),
            comp.getView()
        );
    }

    private static void drawBrushStroke(Drawable dr,
                                        PPoint start, PPoint end,
                                        AutoPaintSettings settings) {
        Tool tool = settings.getTool();
        // tool.randomize();
        if (tool instanceof AbstractBrushTool abt) {
            Path2D shape = new Path2D.Double();
            shape.moveTo(start.getImX(), start.getImY());
            double cp1X = (start.getImX() + end.getImX()) / 2.0;
            double cp1Y = (start.getImY() + end.getImY()) / 2.0;
            float curvature = settings.getMaxCurvature();
            if (curvature > 0) {
                double maxShift = start.imDist(end) * curvature;
                cp1X += (Rnd.nextDouble() - 0.5) * maxShift;
                cp1Y += (Rnd.nextDouble() - 0.5) * maxShift;
            }
            shape.quadTo(cp1X, cp1Y, end.getImX(), end.getImY());
            abt.trace(dr, shape);
        } else {
            throw new IllegalStateException("tool = " + tool.getClass().getName());
        }
    }

    private static void saveOriginalFgBgColors() {
        origFg = getFGColor();
        origBg = getBGColor();
    }

    private static void restoreOriginalFgBgColors(AutoPaintSettings settings) {
        // if colors were changed, restore the original
        if (settings.changeColors()) {
            setFGColor(origFg);
            setBGColor(origBg);
        }
    }
}
