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

package pixelitor.filters;

import net.jafama.FastMath;
import pixelitor.ThreadPool;
import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.utils.ImageUtils;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.concurrent.Future;

public class ColorWheel extends FilterWithParametrizedGUI {
    private final ImagePositionParam center = new ImagePositionParam("Center");
    private final AngleParam hueShiftParam = new AngleParam("Rotate (Degrees)", 0);
    private final RangeParam brightnessParam = new RangeParam("Brightness (%)", 0, 100, 75);
    private final RangeParam satParam = new RangeParam("Saturation (%)", 0, 100, 90);

    public ColorWheel() {
        super("Color Wheel", false, false);
        setParamSet(new ParamSet(center, hueShiftParam, brightnessParam, satParam));
        listNamePrefix = "Fill with ";
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        int[] destData = ImageUtils.getPixelsAsArray(dest);

        int width = dest.getWidth();
        int height = dest.getHeight();

        int cx = (int) (width * center.getRelativeX());
        int cy = (int) (height * center.getRelativeY());

        float hueShift = (float) hueShiftParam.getValueInRadians();
        float saturation = satParam.getValueAsPercentage();
        float brightness = brightnessParam.getValueAsPercentage();

        boolean multiThreaded = ThreadPool.runMultiThreaded();
        if (multiThreaded) {
            Future<?>[] futures = new Future[height];
            for (int y = 0; y < height; y++) {
                int finalY = y;
                Runnable lineTask = () -> calculateLine(destData, width, cx, cy, hueShift, saturation, brightness, finalY);
                futures[y] = ThreadPool.executorService.submit(lineTask);
            }
            ThreadPool.waitForFutures(futures);
        } else {
            for (int y = 0; y < height; y++) {
                calculateLine(destData, width, cx, cy, hueShift, saturation, brightness, y);
            }
        }

        return dest;
    }

    private static void calculateLine(int[] destData, int width, int cx, int cy, float hueShift, float saturation, float brightness, int y) {
        for (int x = 0; x < width; x++) {
            double yDiff = (double) (cy - y);
            double xDiff = (double) x - cx;
            float angle = (float) (FastMath.atan2(yDiff, xDiff)) + hueShift;
            float hue = (float) (angle / (2 * Math.PI));

            destData[x + y * width] = Color.HSBtoRGB(hue, saturation, brightness);
        }
    }
}
