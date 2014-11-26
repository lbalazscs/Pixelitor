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

import java.util.Random;

public class TestDoubleFloat {
    static int numLoops = 100;

    public static void main(String[] args) {
        int ARRAY_SIZE = 1_000_000;
        double[] dArray = new double[ARRAY_SIZE];
        float[] fArray = new float[ARRAY_SIZE];
        Random r = new Random();
        for (int i = 0; i < ARRAY_SIZE; i++) {
            dArray[i] = r.nextDouble();
            fArray[i] = r.nextFloat();
        }

        int i = testDouble(dArray);
        int j = testFloat(fArray);
        int k = testDoubleCasted(dArray);
        System.out.println("TestDoubleFloat::main: j = " + j + ", j = " + j + ", k = " + k);

        numLoops = 200;


        long startTime = System.nanoTime();

        int ii = testDouble(dArray);

        double estimatedSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0;
        System.out.println(String.format("TestDoubleFloat::main: estimatedSeconds D = '%.2f'", estimatedSeconds));


        startTime = System.nanoTime();

        int jj = testFloat(fArray);


        estimatedSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0;
        System.out.println(String.format("TestDoubleFloat::main: estimatedSeconds F = '%.2f'", estimatedSeconds));


        startTime = System.nanoTime();

        int kk = testDoubleCasted(dArray);


        estimatedSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0;
        System.out.println(String.format("TestDoubleFloat::main: estimatedSeconds DC = '%.2f'", estimatedSeconds));


        System.out.println("TestDoubleFloat::main: ii = " + ii + ", jj = " + jj + ", kk = " + kk);
    }

    static int testFloat(float[] array) {
        int retVal = 0;
        for (int i = 0; i < numLoops; i++) {
            for (float v : array) {
                int interp = bilinearInterpolateF(v, v, 34, 43, 76, 97);
                retVal += interp;
            }
        }
        return retVal;
    }

    static int testDouble(double[] array) {
        int retVal = 0;
        for (int i = 0; i < numLoops; i++) {
            for (double v : array) {
                int interp = bilinearInterpolateD(v, v, 34, 43, 76, 97);
                retVal += interp;
            }
        }
        return retVal;
    }

    static int testDoubleCasted(double[] array) {
        int retVal = 0;
        for (int i = 0; i < numLoops; i++) {
            for (double v : array) {
                int interp = bilinearInterpolateF((float)v, (float)v, 34, 43, 76, 97);
                retVal += interp;
            }
        }
        return retVal;
    }


    public static int bilinearInterpolateF(float x, float y, int nw, int ne, int sw, int se) {
        float m0, m1;
        int a0 = (nw >> 24) & 0xff;
        int r0 = (nw >> 16) & 0xff;
        int g0 = (nw >> 8) & 0xff;
        int b0 = nw & 0xff;
        int a1 = (ne >> 24) & 0xff;
        int r1 = (ne >> 16) & 0xff;
        int g1 = (ne >> 8) & 0xff;
        int b1 = ne & 0xff;
        int a2 = (sw >> 24) & 0xff;
        int r2 = (sw >> 16) & 0xff;
        int g2 = (sw >> 8) & 0xff;
        int b2 = sw & 0xff;
        int a3 = (se >> 24) & 0xff;
        int r3 = (se >> 16) & 0xff;
        int g3 = (se >> 8) & 0xff;
        int b3 = se & 0xff;

        float cx = 1.0f - x;
        float cy = 1.0f - y;

        m0 = cx * a0 + x * a1;
        m1 = cx * a2 + x * a3;
        int a = (int) (cy * m0 + y * m1);

        m0 = cx * r0 + x * r1;
        m1 = cx * r2 + x * r3;
        int r = (int) (cy * m0 + y * m1);

        m0 = cx * g0 + x * g1;
        m1 = cx * g2 + x * g3;
        int g = (int) (cy * m0 + y * m1);

        m0 = cx * b0 + x * b1;
        m1 = cx * b2 + x * b3;
        int b = (int) (cy * m0 + y * m1);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int bilinearInterpolateD(double x, double y, int nw, int ne, int sw, int se) {
        double m0, m1;
        int a0 = (nw >> 24) & 0xff;
        int r0 = (nw >> 16) & 0xff;
        int g0 = (nw >> 8) & 0xff;
        int b0 = nw & 0xff;
        int a1 = (ne >> 24) & 0xff;
        int r1 = (ne >> 16) & 0xff;
        int g1 = (ne >> 8) & 0xff;
        int b1 = ne & 0xff;
        int a2 = (sw >> 24) & 0xff;
        int r2 = (sw >> 16) & 0xff;
        int g2 = (sw >> 8) & 0xff;
        int b2 = sw & 0xff;
        int a3 = (se >> 24) & 0xff;
        int r3 = (se >> 16) & 0xff;
        int g3 = (se >> 8) & 0xff;
        int b3 = se & 0xff;

        double cx = 1.0f - x;
        double cy = 1.0f - y;

        m0 = cx * a0 + x * a1;
        m1 = cx * a2 + x * a3;
        int a = (int) (cy * m0 + y * m1);

        m0 = cx * r0 + x * r1;
        m1 = cx * r2 + x * r3;
        int r = (int) (cy * m0 + y * m1);

        m0 = cx * g0 + x * g1;
        m1 = cx * g2 + x * g3;
        int g = (int) (cy * m0 + y * m1);

        m0 = cx * b0 + x * b1;
        m1 = cx * b2 + x * b3;
        int b = (int) (cy * m0 + y * m1);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

}
