/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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
package pixelitor.tools.shapes.custom;

/**
 * Kiwi shape based on http://en.wikipedia.org/wiki/File:Kiwi_silhouette-by-flomar.svg
 */
public class KiwiShape extends AbstractShape {
    public KiwiShape(double x, double y, double width, double height) {
        double cp1X; // x of control point 1
        double cp1Y; // y of control point 1
        double cp2X; // x of control point 2
        double cp2Y; // y of control point 2
        double epX;  // x of end point
        double epY;  // y of end point

        epX = x + 0.3201992f * width;
        epY = y + 0.4113421f * height;
        path.moveTo(epX, epY);

        cp1X = x + 0.3201992f * width;
        cp1Y = y + 0.4113421f * height;
        cp2X = x + 0.3886100f * width;
        cp2Y = y + 0.6210994f * height;
        epX = x + 0.6166460f * width;
        epY = y + 0.6810300f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        epX = x + 0.5979885f * width;
        epY = y + 0.8349427f * height;
        path.lineTo(epX, epY);

        cp1X = x + 0.5555098f * width;
        cp1Y = y + 0.8410811f * height;
        cp2X = x + 0.4960748f * width;
        cp2Y = y + 0.8598133f * height;
        epX = x + 0.5013717f * width;
        epY = y + 0.9341682f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.5153917f * width;
        cp1Y = y + 0.9033809f * height;
        cp2X = x + 0.5378418f * width;
        cp2Y = y + 0.8967044f * height;
        epX = x + 0.5627466f * width;
        epY = y + 0.8894251f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.5616206f * width;
        cp1Y = y + 0.9079770f * height;
        cp2X = x + 0.5613357f * width;
        cp2Y = y + 0.9263459f * height;
        epX = x + 0.5623692f * width;
        epY = y + 0.9448706f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.5757225f * width;
        cp1Y = y + 0.9125036f * height;
        cp2X = x + 0.5948789f * width;
        cp2Y = y + 0.8921492f * height;
        epX = x + 0.6166460f * width;
        epY = y + 0.8880631f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.6298102f * width;
        cp1Y = y + 0.8855920f * height;
        cp2X = x + 0.6456055f * width;
        cp2Y = y + 0.8902638f * height;
        epX = x + 0.6493103f * width;
        epY = y + 0.8818314f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.6532299f * width;
        cp1Y = y + 0.8729106f * height;
        cp2X = x + 0.6404861f * width;
        cp2Y = y + 0.8699734f * height;
        epX = x + 0.6404861f * width;
        epY = y + 0.8608218f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.6404861f * width;
        cp1Y = y + 0.8376669f * height;
        cp2X = x + 0.6467052f * width;
        cp2Y = y + 0.7477709f * height;
        epX = x + 0.6767646f * width;
        epY = y + 0.7055470f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.6767646f * width;
        cp1Y = y + 0.7055470f * height;
        cp2X = x + 0.7462119f * width;
        cp2Y = y + 0.6960126f * height;
        epX = x + 0.7555406f * width;
        epY = y + 0.7859087f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.7648693f * width;
        cp1Y = y + 0.8758046f * height;
        cp2X = x + 0.7451753f * width;
        cp2Y = y + 0.8867011f * height;
        epX = x + 0.7358466f * width;
        epY = y + 0.8921492f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.7031188f * width;
        cp1Y = y + 0.9122557f * height;
        cp2X = x + 0.6401039f * width;
        cp2Y = y + 0.9133864f * height;
        epX = x + 0.6230900f * width;
        epY = y + 0.9638219f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.6498552f * width;
        cp1Y = y + 0.9548539f * height;
        cp2X = x + 0.6771716f * width;
        cp2Y = y + 0.9520250f * height;
        epX = x + 0.7047508f * width;
        epY = y + 0.9507179f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.7014105f * width;
        cp1Y = y + 0.9670282f * height;
        cp2X = x + 0.6977268f * width;
        cp2Y = y + 0.9831436f * height;
        epX = x + 0.6964999f * width;
        epY = y + 1.0f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.7110112f * width;
        cp1Y = y + 0.9904656f * height;
        cp2X = x + 0.7431022f * width;
        cp2Y = y + 0.9602523f * height;
        epX = x + 0.7752346f * width;
        epY = y + 0.9561661f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.7899039f * width;
        cp1Y = y + 0.9543007f * height;
        cp2X = x + 0.8067132f * width;
        cp2Y = y + 0.9595316f * height;
        epX = x + 0.8107810f * width;
        epY = y + 0.9502732f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.8138921f * width;
        cp1Y = y + 0.9431922f * height;
        cp2X = x + 0.7940724f * width;
        cp2Y = y + 0.9360016f * height;
        epX = x + 0.7918191f * width;
        epY = y + 0.9071320f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.7876730f * width;
        cp1Y = y + 0.8540115f * height;
        cp2X = x + 0.8001113f * width;
        cp2Y = y + 0.8008913f * height;
        epX = x + 0.8498646f * width;
        epY = y + 0.7368744f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.8996180f * width;
        cp1Y = y + 0.6728576f * height;
        cp2X = x + 0.9721922f * width;
        cp2Y = y + 0.6497447f * height;
        epX = x + 0.9918688f * width;
        epY = y + 0.4868325f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 1.0136793f * width;
        cp1Y = y + 0.3062516f * height;
        cp2X = x + 0.9873011f * width;
        cp2Y = y + 0.2081411f * height;
        epX = x + 0.9253203f * width;
        epY = y + 0.1288647f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.8632650f * width;
        cp1Y = y + 0.0494930f * height;
        cp2X = x + 0.7348101f * width;
        cp2Y = y + 0.0027241f * height;
        epX = x + 0.6601802f * width;
        epY = y + 0.0f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.5855501f * width;
        cp1Y = y + -0.002724f * height;
        cp2X = x + 0.5357969f * width;
        cp2Y = y + 0.0081724f * height;
        epX = x + 0.4590939f * width;
        epY = y + 0.0572065f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.3823908f * width;
        cp1Y = y + 0.1062407f * height;
        cp2X = x + 0.3740986f * width;
        cp2Y = y + 0.1661713f * height;
        epX = x + 0.3139800f * width;
        epY = y + 0.1579989f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.1685614f * width;
        cp1Y = y + 0.1297290f * height;
        cp2X = x + 0.1173945f * width;
        cp2Y = y + 0.1811683f * height;
        epX = x + 0.1066745f * width;
        epY = y + 0.3663941f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.1077111f * width;
        cp1Y = y + 0.4508418f * height;
        cp2X = x + 0.1077111f * width;
        cp2Y = y + 0.4508418f * height;
        epX = x + 0.1077111f * width;
        epY = y + 0.4508418f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.1077111f * width;
        cp1Y = y + 0.4508418f * height;
        cp2X = x + 0.0163014f * width;
        cp2Y = y + 0.5921931f * height;
        epX = x + 0.0030218f * width;
        epY = y + 0.7886327f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.0030218f * width;
        cp1Y = y + 0.7886327f * height;
        cp2X = x + -0.004829f * width;
        cp2Y = y + 0.8463501f * height;
        epX = x + 0.0075935f * width;
        epY = y + 0.8707875f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.0290599f * width;
        cp1Y = y + 0.7727283f * height;
        cp2X = x + 0.0542405f * width;
        cp2Y = y + 0.6017497f * height;
        epX = x + 0.1451244f * width;
        epY = y + 0.4803964f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.1451244f * width;
        cp1Y = y + 0.4803964f * height;
        cp2X = x + 0.2389402f * width;
        cp2Y = y + 0.4977325f * height;
        epX = x + 0.3201992f * width;
        epY = y + 0.4113421f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);
    }
}
