/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.manual;

import com.jhlabs.image.KaleidoscopeFilter;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.ProgressTracker;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.File;
import java.io.IOException;

public class TestFilterPerformance {
    private static final int SLEEP_SECONDS_BETWEEN_TESTS = 2;

    private TestFilterPerformance() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            throw new IllegalStateException("missing argument");
        }
        File imgFile = new File(args[0]);
        BufferedImage bigImage = ImageIO.read(imgFile);
        bigImage = ImageUtils.toSysCompatibleImage(bigImage);

        double minSeconds = Double.MAX_VALUE;
        double sumSeconds = 0;
        int NUM_TESTS_PER_FILTER = 20;
        for (int i = 0; i < NUM_TESTS_PER_FILTER; i++) {
            double seconds = measurePerformance(getFilter(), bigImage);
            System.out.print(String.format("%.2f ", seconds));
            if (seconds < minSeconds) {
                minSeconds = seconds;
            }
            sumSeconds += seconds;
        }
        System.out.println(String.format("   min = %.2f, average = %.2f",
                minSeconds, sumSeconds / NUM_TESTS_PER_FILTER));
        System.exit(0);
    }

    // a place to configure the tested filter
    private static BufferedImageOp getFilter() {
        var f = new KaleidoscopeFilter("Kaleidoscope Test");
        f.setZoom(1.0f);

        f.setProgressTracker(ProgressTracker.NULL_TRACKER);
        return f;
    }

    private static double measurePerformance(BufferedImageOp op, BufferedImage img) {
        long startTime = System.nanoTime();

        op.filter(img, img);

        double estimatedSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0;

        try {
            Thread.sleep(SLEEP_SECONDS_BETWEEN_TESTS * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return estimatedSeconds;
    }
}
