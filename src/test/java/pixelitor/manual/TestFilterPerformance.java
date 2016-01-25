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

package pixelitor.manual;

import com.jhlabs.image.SparkleFilter;
import pixelitor.utils.ImageUtils;

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
        File imgFile = new File("C:\\Users\\Laci\\Desktop\\tmp\\nagy.jpg");
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
        System.out.println(String.format("   min = %.2f, average = %.2f", minSeconds, sumSeconds / NUM_TESTS_PER_FILTER));
        System.exit(0);
    }

    private static BufferedImageOp getFilter() {
////        SwirlFilter f = new SwirlFilter();
//        SwirlFilterD f = new SwirlFilterD();
//        f.setCenterX(0.5f);
//        f.setCenterY(0.5f);
//        f.setZoom(1.0f);
//        f.setAmount(1.5f);
//        f.setRadius(1555.0f);

//        KaleidoscopeFilterD f = new KaleidoscopeFilterD();
//        f.setZoom(1.0f);

        SparkleFilter f = new SparkleFilter("SparkleFilter test");
        f.setRadius(518);
        f.setRays(200);
        f.setAmount(55);
        f.setRandomness(25);

        return f;
    }

    private static double measurePerformance(BufferedImageOp op, BufferedImage img) {
        long startTime = System.nanoTime();

        op.filter(img, img);

//        try {
//            File output = new File("C:\\Users\\Laci\\Desktop\\tmp\\nagy_" + new Date().getTime() + ".png");
//            ImageIO.write(img, "png", output);
//            System.out.println("TestFilterPerformance::measurePerformance: output = " + output.getAbsolutePath() + (output.exists() ? " - exists" : " - does not exist!"));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        double estimatedSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0;

        try {
            Thread.sleep(SLEEP_SECONDS_BETWEEN_TESTS * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return estimatedSeconds;
    }
}
