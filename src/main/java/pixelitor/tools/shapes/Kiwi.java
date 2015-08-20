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
package pixelitor.tools.shapes;

/**
 * Kiwi shape based on http://en.wikipedia.org/wiki/File:Kiwi_silhouette-by-flomar.svg
 */
public class Kiwi extends GeneralShape {
    public Kiwi(double x, double y, double width, double height) {
        double cp1X; // x of control point 1
        double cp1Y; // y of control point 1
        double cp2X; // x of control point 2
        double cp2Y; // y of control point 2
        double epX;  // x of end point
        double epY;  // y of end point

        epX = x + 0.35196587f * width;
        epY = y + 0.45308727f * height;
        path.moveTo(epX, epY);

        epX = x + 0.35196587f * width;
        epY = y + 0.45308727f * height;
        path.lineTo(epX, epY);

        cp1X = x + 0.35196587f * width;
        cp1Y = y + 0.45308727f * height;
        cp2X = x + 0.42045602f * width;
        cp2Y = y + 0.66276336f * height;
        epX = x + 0.64875656f * width;
        epY = y + 0.72267085f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        epX = x + 0.6300773f * width;
        epY = y + 0.87652403f * height;
        path.lineTo(epX, epY);

        cp1X = x + 0.5875494f * width;
        cp1Y = y + 0.88266f * height;
        cp2X = x + 0.52804554f * width;
        cp2Y = y + 0.901385f * height;
        epX = x + 0.5333485f * width;
        epY = y + 0.97571117f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.5473848f * width;
        cp1Y = y + 0.9449358f * height;
        cp2X = x + 0.56986094f * width;
        cp2Y = y + 0.93826187f * height;
        epX = x + 0.5947946f * width;
        epY = y + 0.9309854f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.5936673f * width;
        cp1Y = y + 0.94953007f * height;
        cp2X = x + 0.59338206f * width;
        cp2Y = y + 0.9678919f * height;
        epX = x + 0.59441686f * width;
        epY = y + 0.9864094f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.60778564f * width;
        cp1Y = y + 0.95405495f * height;
        cp2X = x + 0.6269642f * width;
        cp2Y = y + 0.93370837f * height;
        epX = x + 0.64875656f * width;
        epY = y + 0.9296239f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.661936f * width;
        cp1Y = y + 0.9271537f * height;
        cp2X = x + 0.67774963f * width;
        cp2Y = y + 0.93182373f * height;
        epX = x + 0.6814588f * width;
        epY = y + 0.9233946f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.68538284f * width;
        cp1Y = y + 0.9144773f * height;
        cp2X = x + 0.6726243f * width;
        cp2Y = y + 0.91154116f * height;
        epX = x + 0.6726243f * width;
        epY = y + 0.90239316f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.6726243f * width;
        cp1Y = y + 0.8792472f * height;
        cp2X = x + 0.67885065f * width;
        cp2Y = y + 0.789386f * height;
        epX = x + 0.7089448f * width;
        epY = y + 0.7471784f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.7089448f * width;
        cp1Y = y + 0.7471784f * height;
        cp2X = x + 0.7784727f * width;
        cp2Y = y + 0.7376477f * height;
        epX = x + 0.7878123f * width;
        epY = y + 0.8275089f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.7971518f * width;
        cp1Y = y + 0.91737014f * height;
        cp2X = x + 0.77743495f * width;
        cp2Y = y + 0.92826235f * height;
        epX = x + 0.7680954f * width;
        epY = y + 0.93370837f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.7353296f * width;
        cp1Y = y + 0.95380706f * height;
        cp2X = x + 0.6722417f * width;
        cp2Y = y + 0.9549374f * height;
        epX = x + 0.655208f * width;
        epY = y + 1.0053535f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.6820042f * width;
        cp1Y = y + 0.9963889f * height;
        cp2X = x + 0.70935225f * width;
        cp2Y = y + 0.993561f * height;
        epX = x + 0.7369635f * width;
        epY = y + 0.9922544f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.73361933f * width;
        cp1Y = y + 1.0085584f * height;
        cp2X = x + 0.7299314f * width;
        cp2Y = y + 1.0246676f * height;
        epX = x + 0.728703f * width;
        epY = y + 1.0415175f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.7432312f * width;
        cp1Y = y + 1.0319868f * height;
        cp2X = x + 0.77535945f * width;
        cp2Y = y + 1.0017852f * height;
        epX = x + 0.8075291f * width;
        epY = y + 0.9977005f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.8222154f * width;
        cp1Y = y + 0.99583584f * height;
        cp2X = x + 0.8390442f * width;
        cp2Y = y + 1.0010648f * height;
        epX = x + 0.8431168f * width;
        epY = y + 0.9918099f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.8462314f * width;
        cp1Y = y + 0.9847317f * height;
        cp2X = x + 0.82638866f * width;
        cp2Y = y + 0.97754383f * height;
        epX = x + 0.8241328f * width;
        epY = y + 0.94868535f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.8199819f * width;
        cp1Y = y + 0.8955855f * height;
        cp2X = x + 0.8324347f * width;
        cp2Y = y + 0.8424858f * height;
        epX = x + 0.88224566f * width;
        epY = y + 0.77849364f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.9320568f * width;
        cp1Y = y + 0.7145016f * height;
        cp2X = x + 1.0047152f * width;
        cp2Y = y + 0.6913977f * height;
        epX = x + 1.0244145f * width;
        epY = y + 0.5285484f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 1.0462503f * width;
        cp1Y = y + 0.3480374f * height;
        cp2X = x + 1.0198417f * width;
        cp2Y = y + 0.24996486f * height;
        epX = x + 0.9577889f * width;
        epY = y + 0.17071912f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.89566165f * width;
        cp1Y = y + 0.09137808f * height;
        cp2X = x + 0.76705766f * width;
        cp2Y = y + 0.044627253f * height;
        epX = x + 0.69234115f * width;
        epY = y + 0.041904192f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.6176246f * width;
        cp1Y = y + 0.039181136f * height;
        cp2X = x + 0.56781363f * width;
        cp2Y = y + 0.050073527f * height;
        epX = x + 0.49102166f * width;
        epY = y + 0.099088594f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.4142296f * width;
        cp1Y = y + 0.1481039f * height;
        cp2X = x + 0.40592784f * width;
        cp2Y = y + 0.20801127f * height;
        epX = x + 0.3457395f * width;
        epY = y + 0.199842f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.20015223f * width;
        cp1Y = y + 0.17158309f * height;
        cp2X = x + 0.14892596f * width;
        cp2Y = y + 0.22300243f * height;
        epX = x + 0.13819353f * width;
        epY = y + 0.40815666f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.13923131f * width;
        cp1Y = y + 0.49257162f * height;
        cp2X = x + 0.13923131f * width;
        cp2Y = y + 0.49257162f * height;
        epX = x + 0.13923131f * width;
        epY = y + 0.49257162f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.13923131f * width;
        cp1Y = y + 0.49257162f * height;
        cp2X = x + 0.047715656f * width;
        cp2Y = y + 0.63386834f * height;
        epX = x + 0.03442062f * width;
        epY = y + 0.8302319f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.03442062f * width;
        cp1Y = y + 0.8302319f * height;
        cp2X = x + 0.02656054f * width;
        cp2Y = y + 0.887927f * height;
        epX = x + 0.038997572f * width;
        epY = y + 0.91235495f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.060488936f * width;
        cp1Y = y + 0.8143337f * height;
        cp2X = x + 0.08569869f * width;
        cp2Y = y + 0.64342123f * height;
        epX = x + 0.17668806f * width;
        epY = y + 0.5221148f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.17668806f * width;
        cp1Y = y + 0.5221148f * height;
        cp2X = x + 0.2706126f * width;
        cp2Y = y + 0.53944427f * height;
        epX = x + 0.35196587f * width;
        epY = y + 0.45308727f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);
    }

}
