/*
 * Copyright 2009-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.tryout;

import net.jafama.DoubleWrapper;
import net.jafama.FastMath;

import java.util.Random;

public class TestFastMathHypot {
    public static void main(String[] args) {

        Random r = new Random(0);
        int ARRAY_SIZE = 10000000;
        double[] inputs = new double[ARRAY_SIZE];

        double res = 0;
        for (int i = 0; i < ARRAY_SIZE; i++) {
            inputs[i] = r.nextDouble() * 5 - 2;
            res += twoCalls(inputs[i]);
            res += combinedCalls(inputs[i]);
        }
        System.out.println(String.format("TestFastMathHypot::main: res = %.2f", res));


        long startTime = System.nanoTime();

        for (int j = 0; j < 100; j++) {
            for (int i = 0; i < ARRAY_SIZE; i++) {
                res += combinedCalls(inputs[i]);
            }
        }

        double estimatedSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0;

        System.out.println(String.format("TestFastMathHypot::main: estimatedSeconds = '%.2f'", estimatedSeconds));
        System.out.println(String.format("TestFastMathHypot::main: res = %.2f", res));
    }

    public static double twoCalls(double a) {
        double r = FastMath.sin(a);
        r += FastMath.cos(a);
        return r;
    }



    public static double combinedCalls(double a) {
        DoubleWrapper dw = new DoubleWrapper();
        double r = FastMath.sinAndCos(a, dw);
        r += dw.value;
        return r;
    }

}

