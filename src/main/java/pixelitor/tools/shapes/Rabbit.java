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
 * Rabbit shape based on http://commons.wikimedia.org/wiki/File:Lapin01.svg
 */
public class Rabbit extends GeneralShape {
    public Rabbit(double x, double y, double width, double height) {
        double cp1X; // x of control point 1
        double cp1Y; // y of control point 1
        double cp2X; // x of control point 2
        double cp2Y; // y of control point 2
        double epX;  // x of end point
        double epY;  // y of end point

        epX = x + 0.040013865f * width;
        epY = y + 1.008531f * height;
        path.moveTo(epX, epY);

        epX = x + 0.040013865f * width;
        epY = y + 1.008531f * height;
        path.lineTo(epX, epY);

        cp1X = x + -0.0072747176f * width;
        cp1Y = y + 0.98074985f * height;
        cp2X = x + 0.024679389f * width;
        cp2Y = y + 0.9498783f * height;
        epX = x + 0.09133304f * width;
        epY = y + 0.9589505f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.15063688f * width;
        cp1Y = y + 0.9670223f * height;
        cp2X = x + 0.15137121f * width;
        cp2Y = y + 0.9557281f * height;
        epX = x + 0.09593642f * width;
        epY = y + 0.88814896f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + -0.01920119f * width;
        cp1Y = y + 0.74778765f * height;
        cp2X = x + 0.052076936f * width;
        cp2Y = y + 0.5458122f * height;
        epX = x + 0.25163308f * width;
        epY = y + 0.44696248f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.32225257f * width;
        cp1Y = y + 0.4119814f * height;
        cp2X = x + 0.4428571f * width;
        cp2Y = y + 0.3824616f * height;
        epX = x + 0.51700807f * width;
        epY = y + 0.38200802f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.60799533f * width;
        cp1Y = y + 0.38145146f * height;
        cp2X = x + 0.629871f * width;
        cp2Y = y + 0.3712599f * height;
        epX = x + 0.63609755f * width;
        epY = y + 0.32652643f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.6407988f * width;
        cp1Y = y + 0.2927493f * height;
        cp2X = x + 0.6346229f * width;
        cp2Y = y + 0.28096077f * height;
        epX = x + 0.60144824f * width;
        epY = y + 0.26038873f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.554136f * width;
        cp1Y = y + 0.23104994f * height;
        cp2X = x + 0.52339137f * width;
        cp2Y = y + 0.18531744f * height;
        epX = x + 0.50963587f * width;
        epY = y + 0.12381795f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.50119716f * width;
        cp1Y = y + 0.086089045f * height;
        cp2X = x + 0.50408596f * width;
        cp2Y = y + 0.07857119f * height;
        epX = x + 0.5296363f * width;
        epY = y + 0.07176943f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.5460614f * width;
        cp1Y = y + 0.067397006f * height;
        cp2X = x + 0.5728859f * width;
        cp2Y = y + 0.069828175f * height;
        epX = x + 0.58924633f * width;
        epY = y + 0.077172026f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.61466366f * width;
        cp1Y = y + 0.08858141f * height;
        cp2X = x + 0.6209563f * width;
        cp2Y = y + 0.08619166f * height;
        epX = x + 0.6324887f * width;
        epY = y + 0.060750637f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.65163946f * width;
        cp1Y = y + 0.018502839f * height;
        cp2X = x + 0.6743496f * width;
        cp2Y = y + 0.010755983f * height;
        epX = x + 0.7068129f * width;
        epY = y + 0.03539744f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.72997665f * width;
        cp1Y = y + 0.052980013f * height;
        cp2X = x + 0.7349572f * width;
        cp2Y = y + 0.073248066f * height;
        epX = x + 0.7349572f * width;
        epY = y + 0.14992881f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.7349572f * width;
        cp1Y = y + 0.14992881f * height;
        cp2X = x + 0.7243528f * width;
        cp2Y = y + 0.21992458f * height;
        epX = x + 0.7444144f * width;
        epY = y + 0.23675124f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.7595459f * width;
        cp1Y = y + 0.24944268f * height;
        cp2X = x + 0.8221204f * width;
        cp2Y = y + 0.25819716f * height;
        epX = x + 0.8221204f * width;
        epY = y + 0.25819716f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.92755914f * width;
        cp1Y = y + 0.2764633f * height;
        cp2X = x + 1.0300739f * width;
        cp2Y = y + 0.34662718f * height;
        epX = x + 1.0162245f * width;
        epY = y + 0.39104718f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 1.0062926f * width;
        cp1Y = y + 0.42290244f * height;
        cp2X = x + 0.96991086f * width;
        cp2Y = y + 0.45113784f * height;
        epX = x + 0.9311116f * width;
        epY = y + 0.45710227f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.9080986f * width;
        cp1Y = y + 0.46063998f * height;
        cp2X = x + 0.8888217f * width;
        cp2Y = y + 0.47446352f * height;
        epX = x + 0.8816618f * width;
        epY = y + 0.5235348f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.86651766f * width;
        cp1Y = y + 0.62732774f * height;
        cp2X = x + 0.85083693f * width;
        cp2Y = y + 0.6763924f * height;
        epX = x + 0.77440256f * width;
        epY = y + 0.73883635f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.7176915f * width;
        cp1Y = y + 0.7851673f * height;
        cp2X = x + 0.70043164f * width;
        cp2Y = y + 0.80839103f * height;
        epX = x + 0.69536436f * width;
        epY = y + 0.8451849f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.6892658f * width;
        cp1Y = y + 0.8894677f * height;
        cp2X = x + 0.69251734f * width;
        cp2Y = y + 0.8941835f * height;
        epX = x + 0.75021094f * width;
        epY = y + 0.9247309f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.75021094f * width;
        cp1Y = y + 0.9247309f * height;
        cp2X = x + 0.81154025f * width;
        cp2Y = y + 0.94011706f * height;
        epX = x + 0.81154025f * width;
        epY = y + 0.9572035f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.81154025f * width;
        cp1Y = y + 0.97941357f * height;
        cp2X = x + 0.7487237f * width;
        cp2Y = y + 0.99503887f * height;
        epX = x + 0.7487237f * width;
        epY = y + 0.99503887f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.6672635f * width;
        cp1Y = y + 1.0235244f * height;
        cp2X = x + 0.628459f * width;
        cp2Y = y + 1.0168356f * height;
        epX = x + 0.38623852f * width;
        epY = y + 1.0133331f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.24638778f * width;
        cp1Y = y + 1.0113109f * height;
        cp2X = x + 0.11868698f * width;
        cp2Y = y + 1.0137571f * height;
        epX = x + 0.10245893f * width;
        epY = y + 1.0187693f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.083660685f * width;
        cp1Y = y + 1.0245752f * height;
        cp2X = x + 0.060999904f * width;
        cp2Y = y + 1.0208601f * height;
        epX = x + 0.040013865f * width;
        epY = y + 1.008531f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        epX = x + 0.6187177f * width;
        epY = y + 0.9237426f * height;
        path.moveTo(epX, epY);

        epX = x + 0.6187177f * width;
        epY = y + 0.9237426f * height;
        path.lineTo(epX, epY);

        cp1X = x + 0.6187177f * width;
        cp1Y = y + 0.90885955f * height;
        cp2X = x + 0.57360977f * width;
        cp2Y = y + 0.84513277f * height;
        epX = x + 0.56307495f * width;
        epY = y + 0.84513277f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.55685645f * width;
        cp1Y = y + 0.84513277f * height;
        cp2X = x + 0.5295878f * width;
        cp2Y = y + 0.8643272f * height;
        epX = x + 0.5024781f * width;
        epY = y + 0.88778704f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        epX = x + 0.45318758f * width;
        epY = y + 0.9304412f * height;
        path.lineTo(epX, epY);

        epX = x + 0.5359526f * width;
        epY = y + 0.9304412f * height;
        path.lineTo(epX, epY);

        cp1X = x + 0.58147347f * width;
        cp1Y = y + 0.9304412f * height;
        cp2X = x + 0.6187177f * width;
        cp2Y = y + 0.9274268f * height;
        epX = x + 0.6187177f * width;
        epY = y + 0.9237426f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);
    }

}