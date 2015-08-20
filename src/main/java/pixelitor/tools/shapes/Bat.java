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
 * Bat shape based on http://en.wikipedia.org/wiki/File:Bat_shadow_black.svg
 */
public class Bat extends GeneralShape {
    public Bat(double x, double y, double width, double height) {
        double cp1X; // x of control point 1
        double cp1Y; // y of control point 1
        double cp2X; // x of control point 2
        double cp2Y; // y of control point 2
        double epX;  // x of end point
        double epY;  // y of end point

        epX = x + 0.48396146f * width;
        epY = y + 0.8711912f * height;
        path.moveTo(epX, epY);

        epX = x + 0.48396146f * width;
        epY = y + 0.8711912f * height;
        path.lineTo(epX, epY);

        cp1X = x + 0.46530184f * width;
        cp1Y = y + 0.8309665f * height;
        cp2X = x + 0.38263357f * width;
        cp2Y = y + 0.8160471f * height;
        epX = x + 0.3731808f * width;
        epY = y + 0.84056836f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.35756963f * width;
        cp1Y = y + 0.88106513f * height;
        cp2X = x + 0.34080854f * width;
        cp2Y = y + 0.9193152f * height;
        epX = x + 0.36195248f * width;
        epY = y + 0.83724654f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.3386589f * width;
        cp1Y = y + 0.8935222f * height;
        cp2X = x + 0.3420269f * width;
        cp2Y = y + 0.8584848f * height;
        epX = x + 0.36583346f * width;
        epY = y + 0.79526377f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.36583346f * width;
        cp1Y = y + 0.79526377f * height;
        cp2X = x + 0.39358535f * width;
        cp2Y = y + 0.7799532f * height;
        epX = x + 0.38822776f * width;
        epY = y + 0.7415569f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.38251382f * width;
        cp1Y = y + 0.7006066f * height;
        cp2X = x + 0.30032519f * width;
        cp2Y = y + 0.59342885f * height;
        epX = x + 0.23370443f * width;
        epY = y + 0.61983544f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.15583336f * width;
        cp1Y = y + 0.6507012f * height;
        cp2X = x + 0.18591423f * width;
        cp2Y = y + 0.6526183f * height;
        epX = x + 0.16175953f * width;
        epY = y + 0.58064216f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.1528414f * width;
        cp1Y = y + 0.55406797f * height;
        cp2X = x + 0.14705019f * width;
        cp2Y = y + 0.5273279f * height;
        epX = x + 0.07419801f * width;
        epY = y + 0.54101586f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.06698244f * width;
        cp1Y = y + 0.44892874f * height;
        cp2X = x + 0.058711186f * width;
        cp2Y = y + 0.38432577f * height;
        epX = x + 0.0079061575f * width;
        epY = y + 0.35636365f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.15172054f * width;
        cp1Y = y + 0.14081612f * height;
        cp2X = x + 0.2041543f * width;
        cp2Y = y + 0.14397591f * height;
        epX = x + 0.20349151f * width;
        epY = y + 0.13025562f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.20031318f * width;
        cp1Y = y + 0.0644616f * height;
        cp2X = x + 0.19046022f * width;
        cp2Y = y + -0.01011441f * height;
        epX = x + 0.21284555f * width;
        epY = y + 0.12160087f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.21233447f * width;
        cp1Y = y + 0.007110184f * height;
        cp2X = x + 0.20867635f * width;
        cp2Y = y + 0.04029033f * height;
        epX = x + 0.22205064f * width;
        epY = y + 0.12669845f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.22513902f * width;
        cp1Y = y + 0.1466518f * height;
        cp2X = x + 0.28788444f * width;
        cp2Y = y + 0.21163544f * height;
        epX = x + 0.33955792f * width;
        epY = y + 0.23622294f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.41069368f * width;
        cp1Y = y + 0.27007103f * height;
        cp2X = x + 0.43251768f * width;
        cp2Y = y + 0.30622476f * height;
        epX = x + 0.43251768f * width;
        epY = y + 0.28272754f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.43251768f * width;
        cp1Y = y + 0.2655886f * height;
        cp2X = x + 0.42556417f * width;
        cp2Y = y + 0.2189889f * height;
        epX = x + 0.41960958f * width;
        epY = y + 0.21069744f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.40620282f * width;
        cp1Y = y + 0.19202933f * height;
        cp2X = x + 0.39478126f * width;
        cp2Y = y + 0.056988757f * height;
        epX = x + 0.41154054f * width;
        epY = y + 0.028444579f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.41581085f * width;
        cp1Y = y + 0.018658942f * height;
        cp2X = x + 0.50143397f * width;
        cp2Y = y + 0.16836204f * height;
        epX = x + 0.50143397f * width;
        epY = y + 0.16836204f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.50143397f * width;
        cp1Y = y + 0.16836204f * height;
        cp2X = x + 0.5785536f * width;
        cp2Y = y + 0.0273761f * height;
        epX = x + 0.5868916f * width;
        epY = y + 0.019599283f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.5989375f * width;
        cp1Y = y + 0.04062414f * height;
        cp2X = x + 0.60303134f * width;
        cp2Y = y + 0.12733895f * height;
        epX = x + 0.5784455f * width;
        epY = y + 0.20690775f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.5733677f * width;
        cp1Y = y + 0.22334124f * height;
        cp2X = x + 0.5701417f * width;
        cp2Y = y + 0.25919494f * height;
        epX = x + 0.5701417f * width;
        epY = y + 0.27444258f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.5701417f * width;
        cp1Y = y + 0.3071336f * height;
        cp2X = x + 0.6937292f * width;
        cp2Y = y + 0.24839245f * height;
        epX = x + 0.7691606f * width;
        epY = y + 0.14076737f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.7923608f * width;
        cp1Y = y + 0.107665494f * height;
        cp2X = x + 0.77971613f * width;
        cp2Y = y + -0.0058511556f * height;
        epX = x + 0.7865298f * width;
        epY = y + 0.114005655f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.8027102f * width;
        cp1Y = y + 0.0111046815f * height;
        cp2X = x + 0.79922926f * width;
        cp2Y = y + 0.052270394f * height;
        epX = x + 0.7920877f * width;
        epY = y + 0.12626775f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.79057056f * width;
        cp1Y = y + 0.14198744f * height;
        cp2X = x + 0.899306f * width;
        cp2Y = y + 0.17557377f * height;
        epX = x + 0.9920938f * width;
        epY = y + 0.3866074f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.9691352f * width;
        cp1Y = y + 0.3866074f * height;
        cp2X = x + 0.9349961f * width;
        cp2Y = y + 0.3966509f * height;
        epX = x + 0.9280588f * width;
        epY = y + 0.5432177f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.8615748f * width;
        cp1Y = y + 0.5425939f * height;
        cp2X = x + 0.8545972f * width;
        cp2Y = y + 0.5403216f * height;
        epX = x + 0.82650656f * width;
        epY = y + 0.63398916f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.7509983f * width;
        cp1Y = y + 0.6200947f * height;
        cp2X = x + 0.67899865f * width;
        cp2Y = y + 0.61476916f * height;
        epX = x + 0.64566225f * width;
        epY = y + 0.6818779f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        epX = x + 0.609466f * width;
        epY = y + 0.7547435f * height;
        path.lineTo(epX, epY);

        epX = x + 0.6363375f * width;
        epY = y + 0.80627334f * height;
        path.lineTo(epX, epY);

        cp1X = x + 0.6519878f * width;
        cp1Y = y + 0.8362852f * height;
        cp2X = x + 0.6647533f * width;
        cp2Y = y + 0.88090324f * height;
        epX = x + 0.63656145f * width;
        epY = y + 0.83025086f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.6401184f * width;
        cp1Y = y + 0.9251861f * height;
        cp2X = x + 0.64008677f * width;
        cp2Y = y + 0.87504876f * height;
        epX = x + 0.62064874f * width;
        epY = y + 0.8233091f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.59059566f * width;
        cp1Y = y + 0.823376f * height;
        cp2X = x + 0.5248581f * width;
        cp2Y = y + 0.834002f * height;
        epX = x + 0.5188337f * width;
        epY = y + 0.874816f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.49758786f * width;
        cp1Y = y + 1.0187525f * height;
        cp2X = x + 0.49775344f * width;
        cp2Y = y + 1.0135795f * height;
        epX = x + 0.48396146f * width;
        epY = y + 0.8711912f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

    }

}