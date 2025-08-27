/*
Copyright 2006 Jerry Huxtable

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.jhlabs.image;

public class Curve {
    public float[] x;
    public float[] y;

    public Curve() {
        x = new float[]{0, 1};
        y = new float[]{0, 1};
    }

    public Curve(Curve curve) {
        x = curve.x.clone();
        y = curve.y.clone();
    }

    public int addKnot(float kx, float ky) {
        int pos = -1;
        int numKnots = x.length;
        float[] nx = new float[numKnots + 1];
        float[] ny = new float[numKnots + 1];
        int j = 0;
        for (int i = 0; i < numKnots; i++) {
            if (pos == -1 && x[i] > kx) {
                pos = j;
                nx[j] = kx;
                ny[j] = ky;
                j++;
            }
            nx[j] = x[i];
            ny[j] = y[i];
            j++;
        }
        if (pos == -1) {
            pos = j;
            nx[j] = kx;
            ny[j] = ky;
        }
        x = nx;
        y = ny;
        return pos;
    }

    public int findKnotPos(float kx) {
        int numKnots = x.length;
        for (int i = 0; i < numKnots; i++) {
            if (x[i] > kx) {
                return i;
            }
        }

        return numKnots;
    }

    public void removeKnot(int n) {
        int numKnots = x.length;
        if (numKnots <= 2) {
            return;
        }
        float[] nx = new float[numKnots - 1];
        float[] ny = new float[numKnots - 1];
        int j = 0;
        for (int i = 0; i < numKnots - 1; i++) {
            if (i == n) {
                j++;
            }
            nx[i] = x[j];
            ny[i] = y[j];
            j++;
        }
        x = nx;
        y = ny;
    }

    public int[] makeTable() {
        int numKnots = x.length;
        float[] nx = new float[numKnots + 2];
        float[] ny = new float[numKnots + 2];
        System.arraycopy(x, 0, nx, 1, numKnots);
        System.arraycopy(y, 0, ny, 1, numKnots);
        nx[0] = nx[1];
        ny[0] = ny[1];
        nx[numKnots + 1] = nx[numKnots];
        ny[numKnots + 1] = ny[numKnots];

        int[] table = new int[256];

        // if first knot is > 0 fill the table with y position
        if (nx[0] > 0) {
            int nxStart = (int) (nx[0] * 255);
            int nyStart = (int) (ny[0] * 255);
            for (int i = 0; i <= nxStart; i++) {
                table[i] = nyStart;
            }
        }

        // if last knot is < 1 fill the table with y position
        if (nx[numKnots] < 1) {
            int nxStart = (int) (nx[numKnots] * 255);
            int nyStart = (int) (ny[numKnots] * 255);
            for (int i = nxStart; i <= 255; i++) {
                table[i] = nyStart;
            }
        }

        for (int i = 0; i < 2048; i++) {
            float f = i / 2048.0f;
            int x = (int) (255 * ImageMath.splineClamped(f, nx.length, nx) + 0.5f);
            int y = (int) (255 * ImageMath.spline(f, nx.length, ny) + 0.5f);
            x = ImageMath.clamp(x, 0, 255);
            y = ImageMath.clamp(y, 0, 255);
            table[x] = y;
        }
        return table;
    }
}
