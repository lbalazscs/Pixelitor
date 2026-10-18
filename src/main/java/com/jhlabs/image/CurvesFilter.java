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

/**
 * Applies a master RGB curve and per-channel
 * R, G, and B curves through lookup tables.
 */
public class CurvesFilter extends TransferFilter {
    private final Curve rCurve;
    private final Curve gCurve;
    private final Curve bCurve;
    private final Curve rgbCurve;

    public CurvesFilter(String filterName, Curve rCurve, Curve gCurve, Curve bCurve, Curve rgbCurve) {
        super(filterName);

        this.rCurve = rCurve;
        this.gCurve = gCurve;
        this.bCurve = bCurve;
        this.rgbCurve = rgbCurve;
        initialized = false;
    }

    @Override
    protected void initialize() {
        initialized = true;
        int[] r = rCurve.makeTable();
        int[] g = gCurve.makeTable();
        int[] b = bCurve.makeTable();
        int[] master = rgbCurve.makeTable();

        rTable = new int[256];
        gTable = new int[256];
        bTable = new int[256];
        for (int i = 0; i < 256; i++) {
            rTable[i] = r[master[i]];
            gTable[i] = g[master[i]];
            bTable[i] = b[master[i]];
        }
    }
}
