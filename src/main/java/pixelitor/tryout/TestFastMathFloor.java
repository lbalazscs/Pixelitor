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

import net.jafama.FastMath;

import java.util.Random;

public class TestFastMathFloor {
    static final int NUM_LOOPS = 100;

    public static void main(String[] args) {
        float[] arrayF = new float[10_000_000];
        double[] arrayD = new double[10_000_000];
        Random r = new Random();
        for (int i = 0; i < arrayF.length; i++) {
            arrayF[i] = r.nextFloat() * r.nextInt(1000);
            arrayD[i] = r.nextDouble() * r.nextInt(1000);
        }
        double v = testMathFloorF(arrayF);
        System.out.println(String.format("TestFastMathFloor::main: F v = %.2f", v));
        v = testFastMathFloorF(arrayF);
        System.out.println(String.format("TestFastMathFloor::main: F v = %.2f", v));
        v = testMathFloorD(arrayD);
        System.out.println(String.format("TestFastMathFloor::main: D v = %.2f", v));
        v = testFastMathFloorD(arrayD);
        System.out.println(String.format("TestFastMathFloor::main: D v = %.2f", v));



        long startTime = System.nanoTime();

        v = testFastMathFloorF(arrayF);
        System.out.println(String.format("TestFastMathFloor::main F: v = %.2f", v));

        double estimatedSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0;
        System.out.println(String.format("TestFastMathFloor::main F: estimatedSeconds for FastMath.floor = '%.2f'", estimatedSeconds));


        startTime = System.nanoTime();

        v = testMathFloorF(arrayF);
        System.out.println(String.format("TestFastMathFloor::main F: v = %.2f", v));


        estimatedSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0;
        System.out.println(String.format("TestFastMathFloor::main F: estimatedSeconds for Math.floor = '%.2f'", estimatedSeconds));

//// double

        startTime = System.nanoTime();

        v = testFastMathFloorD(arrayD);
        System.out.println(String.format("TestFastMathFloor::main D: v = %.2f", v));

        estimatedSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0;
        System.out.println(String.format("TestFastMathFloor::main: estimatedSeconds for FastMath.floor D = '%.2f'", estimatedSeconds));


        startTime = System.nanoTime();

        v = testMathFloorD(arrayD);
        System.out.println(String.format("TestFastMathFloor::main D: v = %.2f", v));


        estimatedSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0;
        System.out.println(String.format("TestFastMathFloor::main: estimatedSeconds for Math.floor D = '%.2f'", estimatedSeconds));


    }

    static double testMathFloorF(float[] array) {
        double retVal = 0;
        for (int i = 0; i < NUM_LOOPS; i++) {
            for (float v : array) {
                double floor = Math.floor(v);
                retVal += floor;
            }
        }
        return retVal;
    }

    static double testFastMathFloorF(float[] array) {
        double retVal = 0;
        for (int i = 0; i < NUM_LOOPS; i++) {
            for (float v : array) {
                double floor = FastMath.floor(v);
                retVal += floor;
            }
        }
        return retVal;
    }

    // doubles

    static double testMathFloorD(double[] array) {
        double retVal = 0;
        for (int i = 0; i < NUM_LOOPS; i++) {
            for (double v : array) {
                double floor = Math.floor(v);
                retVal += floor;
            }
        }
        return retVal;
    }

    static double testFastMathFloorD(double[] array) {
        double retVal = 0;
        for (int i = 0; i < NUM_LOOPS; i++) {
            for (double v : array) {
                double floor = FastMath.floor(v);
                retVal += floor;
            }
        }
        return retVal;
    }

}
