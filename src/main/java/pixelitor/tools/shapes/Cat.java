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
 * Cat based on http://commons.wikimedia.org/wiki/File:Cat_silhouette.svg
 */
public class Cat extends GeneralShape {
    public Cat(double x, double y, double width, double height) {

        double cp1X; // x of control point 1
        double cp1Y; // y of control point 1
        double cp2X; // x of control point 2
        double cp2Y; // y of control point 2
        double epX;  // x of end point
        double epY;  // y of end point

        epX = x + 0.3783726f * width;
        epY = y + 0.80843306f * height;
        path.moveTo(epX, epY);

        epX = x + 0.3783726f * width;
        epY = y + 0.80843306f * height;
        path.lineTo(epX, epY);

        epX = x + 0.6608726f * width;
        epY = y + 0.80843306f * height;
        path.lineTo(epX, epY);

        cp1X = x + 0.6608726f * width;
        cp1Y = y + 0.7661604f * height;
        cp2X = x + 0.65800524f * width;
        cp2Y = y + 0.7577751f * height;
        epX = x + 0.5914976f * width;
        epY = y + 0.7577751f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.6021226f * width;
        cp1Y = y + 0.72422254f * height;
        cp2X = x + 0.6453106f * width;
        cp2Y = y + 0.6430476f * height;
        epX = x + 0.6693101f * width;
        epY = y + 0.6430476f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.6905427f * width;
        cp1Y = y + 0.6430476f * height;
        cp2X = x + 0.7158726f * width;
        cp2Y = y + 0.6442787f * height;
        epX = x + 0.7158726f * width;
        epY = y + 0.6952751f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.7158726f * width;
        cp1Y = y + 0.7531699f * height;
        cp2X = x + 0.80842924f * width;
        cp2Y = y + 0.8465144f * height;
        epX = x + 0.8308726f * width;
        epY = y + 0.80843306f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.8643192f * width;
        cp1Y = y + 0.7516815f * height;
        cp2X = x + 0.7733726f * width;
        cp2Y = y + 0.76898724f * height;
        epX = x + 0.7733726f * width;
        epY = y + 0.6321172f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.7733726f * width;
        cp1Y = y + 0.44487196f * height;
        cp2X = x + 0.87718546f * width;
        cp2Y = y + 0.47152817f * height;
        epX = x + 0.87718546f * width;
        epY = y + 0.36632773f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.87718546f * width;
        cp1Y = y + 0.31369618f * height;
        cp2X = x + 0.86337256f * width;
        cp2Y = y + 0.3065888f * height;
        epX = x + 0.86337256f * width;
        epY = y + 0.26895934f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.86337256f * width;
        cp1Y = y + 0.21922882f * height;
        cp2X = x + 0.9049929f * width;
        cp2Y = y + 0.22211468f * height;
        epX = x + 0.8968952f * width;
        epY = y + 0.18088126f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.8913015f * width;
        cp1Y = y + 0.1523991f * height;
        cp2X = x + 0.886924f * width;
        cp2Y = y + 0.12955788f * height;
        epX = x + 0.8836629f * width;
        epY = y + 0.0951155f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.88134897f * width;
        cp1Y = y + 0.07067712f * height;
        cp2X = x + 0.88061124f * width;
        cp2Y = y + 0.044629995f * height;
        epX = x + 0.85649633f * width;
        epY = y + 0.04567732f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.8281626f * width;
        cp1Y = y + 0.046907872f * height;
        cp2X = x + 0.8174137f * width;
        cp2Y = y + 0.09940576f * height;
        epX = x + 0.7733726f * width;
        epY = y + 0.103169866f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.7294213f * width;
        cp1Y = y + 0.10692629f * height;
        cp2X = x + 0.69194585f * width;
        cp2Y = y + 0.06378429f * height;
        epX = x + 0.6749351f * width;
        epY = y + 0.069946185f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.6580677f * width;
        cp1Y = y + 0.076056145f * height;
        cp2X = x + 0.6633726f * width;
        cp2Y = y + 0.124222495f * height;
        epX = x + 0.67337257f * width;
        epY = y + 0.15843302f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.6890897f * width;
        cp1Y = y + 0.21220203f * height;
        cp2X = x + 0.7233726f * width;
        cp2Y = y + 0.28211725f * height;
        epX = x + 0.6608726f * width;
        epY = y + 0.29264355f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.59837264f * width;
        cp1Y = y + 0.30316988f * height;
        cp2X = x + 0.49087262f * width;
        cp2Y = y + 0.31369618f * height;
        epX = x + 0.4133726f * width;
        epY = y + 0.4215909f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.33587262f * width;
        cp1Y = y + 0.52948564f * height;
        cp2X = x + 0.33873355f * width;
        cp2Y = y + 0.65610844f * height;
        epX = x + 0.3083726f * width;
        epY = y + 0.6952751f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.20647675f * width;
        cp1Y = y + 0.82672447f * height;
        cp2X = x + 0.1020598f * width;
        cp2Y = y + 0.77275324f * height;
        epX = x + 0.1020598f * width;
        epY = y + 0.8821173f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.1020598f * width;
        cp1Y = y + 0.93107945f * height;
        cp2X = x + 0.18087262f * width;
        cp2Y = y + 0.9663278f * height;
        epX = x + 0.1933726f * width;
        epY = y + 0.95053834f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.20587261f * width;
        cp1Y = y + 0.9347488f * height;
        cp2X = x + 0.08715942f * width;
        cp2Y = y + 0.88769966f * height;
        epX = x + 0.21998873f * width;
        epY = y + 0.83211726f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.33346778f * width;
        cp1Y = y + 0.7846319f * height;
        cp2X = x + 0.34359783f * width;
        cp2Y = y + 0.77493846f * height;
        epX = x + 0.3783726f * width;
        epY = y + 0.80843306f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);
    }
}
