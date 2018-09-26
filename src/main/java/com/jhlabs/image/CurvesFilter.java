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

public class CurvesFilter extends TransferFilter {

    private Curve[] curves;

    public CurvesFilter(String filterName) {
        super(filterName);
        curves = new Curve[4];
        curves[0] = new Curve();
        curves[1] = new Curve();
        curves[2] = new Curve();
        curves[3] = new Curve();
    }
    
    protected void initialize() {
        initialized = true;
        if ( curves.length == 1 )
            rTable = gTable = bTable = curves[0].makeTable();
        else {
            rTable = new int[256];
            gTable = new int[256];
            bTable = new int[256];

            int[] r = curves[0].makeTable();
            int[] g = curves[1].makeTable();
            int[] b = curves[2].makeTable();
            int[] rgb = curves[3].makeTable();

            for (int x = 0; x <= 255; x++) {
                rTable[x] = ImageMath.clamp(r[rgb[x]], 0, 255);
                gTable[x] = ImageMath.clamp(g[rgb[x]], 0, 255);
                bTable[x] = ImageMath.clamp(b[rgb[x]], 0, 255);
            }
        }
    }

    public void setCurve( Curve curve ) {
        curves = new Curve[] { curve };
        initialized = false;
    }

    public void setCurves(Curve rgb, Curve r, Curve g, Curve b) {
        initialized = false;
        curves[0] = r;
        curves[1] = g;
        curves[2] = b;
        curves[3] = rgb;
    }

    public Curve[] getCurves() {
        return curves;
    }

    public String toString() {
        return "Colors/Curves...";
    }

}

