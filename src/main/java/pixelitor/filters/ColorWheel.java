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

package pixelitor.filters;

import net.jafama.FastMath;
import org.jdesktop.swingx.graphics.ColorUtilities;
import pixelitor.ThreadPool;
import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.EnumParam;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.StatusBarProgressTracker;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.concurrent.Future;

/**
 * Renders a color wheel
 */
public class ColorWheel extends ParametrizedFilter {
    public static final String NAME = "Color Wheel";

    @Serial
    private static final long serialVersionUID = -8398979151631397821L;

    private static final double SPIRAL_EFFECT_SCALE = 0.0005;

    public enum ColorSpaceType {
        HSB {
            @Override
            int toRGB(float hue, float sat, float bri) {
                return Color.HSBtoRGB(hue, sat, bri);
            }
        },
        HSL {
            @Override
            int toRGB(float hue, float sat, float bri) {
                int[] buffer = new int[3];
                ColorUtilities.HSLtoRGB((hue - (float) Math.floor(hue)), sat, bri, buffer);
                return 0xFF_00_00_00 | (buffer[0] << 16) | (buffer[1] << 8) | buffer[2];
            }
        };

        abstract int toRGB(float hue, float sat, float bri);
    }

    private final EnumParam<ColorSpaceType> type = new EnumParam<>("Color Space", ColorSpaceType.class);
    private final ImagePositionParam center = new ImagePositionParam("Center");
    private final AngleParam hueRotParam = new AngleParam("Rotate", 0);
    private final RangeParam brgLumParam = new RangeParam("Brightness (%)", 0, 75, 100);
    private final RangeParam satParam = new RangeParam("Saturation (%)", 0, 90, 100);
    private final RangeParam spiralParam = new RangeParam("Spiral", -100, 0, 100);

    public ColorWheel() {
        super(false);

        setParams(type, center,
            hueRotParam, brgLumParam, satParam, spiralParam);
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        int[] destPixels = ImageUtils.getPixels(dest);

        int width = dest.getWidth();
        int height = dest.getHeight();

        ColorSpaceType space = type.getSelected();

        double cx = width * center.getRelativeX();
        double cy = height * center.getRelativeY();

        double hueRot = hueRotParam.getValueInRadians();
        double sat = satParam.getPercentage();
        double brgLum = brgLumParam.getPercentage();

        double spiral = spiralParam.getValueAsDouble();

        var pt = new StatusBarProgressTracker(NAME, height);

        Future<?>[] rowFutures = new Future[height];
        for (int y = 0; y < height; y++) {
            int finalY = y;
            Runnable rowTask = () -> processRow(
                destPixels, width, finalY, cx, cy, hueRot, sat, brgLum, space, spiral);
            rowFutures[y] = ThreadPool.submit(rowTask);
        }
        ThreadPool.waitFor(rowFutures, pt);
        pt.finished();

        return dest;
    }

    private static void processRow(int[] destPixels, int width, int y,
                                   double cx, double cy, double hueRot,
                                   double saturation, double brightness,
                                   ColorSpaceType model, double spiral) {
        for (int x = 0; x < width; x++) {
            double yDiff = cy - y;
            double xDiff = x - cx;
            double baseAngle = FastMath.atan2(yDiff, xDiff);
            double angle = baseAngle + hueRot;
            if (spiral != 0.0) {
                double radius = FastMath.hypot(xDiff, yDiff);
                double spiralAngleOffset = spiral * radius * SPIRAL_EFFECT_SCALE;
                angle += spiralAngleOffset;
            }

            double hue = angle / (2 * Math.PI);

            destPixels[x + y * width] = model.toRGB((float) hue, (float) saturation, (float) brightness);
        }
    }

    @Override
    public boolean supportsGray() {
        return false;
    }
}
