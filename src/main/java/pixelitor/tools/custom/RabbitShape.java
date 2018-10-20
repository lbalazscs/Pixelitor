/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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
package pixelitor.tools.custom;

/**
 * Rabbit shape based on http://commons.wikimedia.org/wiki/File:Lapin01.svg
 */
public class RabbitShape extends AbstractShape {
    public RabbitShape(double x, double y, double width, double height) {
        double cp1X; // x of control point 1
        double cp1Y; // y of control point 1
        double cp2X; // x of control point 2
        double cp2Y; // y of control point 2
        double epX;  // x of end point
        double epY;  // y of end point

        epX = x + 0.0225290f * width;
        epY = y + 0.9892685f * height;
        path.moveTo(epX, epY);

        cp1X = x + -0.024759f * width;
        cp1Y = y + 0.9614873f * height;
        cp2X = x + 0.0071945f * width;
        cp2Y = y + 0.9306156f * height;
        epX = x + 0.0738482f * width;
        epY = y + 0.9396878f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.1331521f * width;
        cp1Y = y + 0.9477595f * height;
        cp2X = x + 0.1338864f * width;
        cp2Y = y + 0.9364654f * height;
        epX = x + 0.0784516f * width;
        epY = y + 0.8688863f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + -0.036686f * width;
        cp1Y = y + 0.7285250f * height;
        cp2X = x + 0.0345920f * width;
        cp2Y = y + 0.5265495f * height;
        epX = x + 0.2341483f * width;
        epY = y + 0.4276998f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.3047678f * width;
        cp1Y = y + 0.3927187f * height;
        cp2X = x + 0.4253724f * width;
        cp2Y = y + 0.3631989f * height;
        epX = x + 0.4995234f * width;
        epY = y + 0.3627453f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.5905108f * width;
        cp1Y = y + 0.3621887f * height;
        cp2X = x + 0.6123865f * width;
        cp2Y = y + 0.3519972f * height;
        epX = x + 0.6186130f * width;
        epY = y + 0.3072637f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.6233143f * width;
        cp1Y = y + 0.2734866f * height;
        cp2X = x + 0.6171384f * width;
        cp2Y = y + 0.2616981f * height;
        epX = x + 0.5839637f * width;
        epY = y + 0.2411260f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.5366514f * width;
        cp1Y = y + 0.2117872f * height;
        cp2X = x + 0.5059068f * width;
        cp2Y = y + 0.1660547f * height;
        epX = x + 0.4921512f * width;
        epY = y + 0.1045552f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.4837125f * width;
        cp1Y = y + 0.0668264f * height;
        cp2X = x + 0.4866014f * width;
        cp2Y = y + 0.0593085f * height;
        epX = x + 0.5121517f * width;
        epY = y + 0.0525067f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.5285768f * width;
        cp1Y = y + 0.0481343f * height;
        cp2X = x + 0.5554013f * width;
        cp2Y = y + 0.0505655f * height;
        epX = x + 0.5717618f * width;
        epY = y + 0.0579093f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.5971791f * width;
        cp1Y = y + 0.0693187f * height;
        cp2X = x + 0.6034718f * width;
        cp2Y = y + 0.0669290f * height;
        epX = x + 0.6150042f * width;
        epY = y + 0.0414879f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.6341550f * width;
        cp1Y = y + -0.000759f * height;
        cp2X = x + 0.6568651f * width;
        cp2Y = y + -0.008506f * height;
        epX = x + 0.6893284f * width;
        epY = y + 0.0161347f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.7124922f * width;
        cp1Y = y + 0.0337173f * height;
        cp2X = x + 0.7174728f * width;
        cp2Y = y + 0.0539854f * height;
        epX = x + 0.7174728f * width;
        epY = y + 0.1306661f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.7174728f * width;
        cp1Y = y + 0.1306661f * height;
        cp2X = x + 0.7068683f * width;
        cp2Y = y + 0.2006619f * height;
        epX = x + 0.7269300f * width;
        epY = y + 0.2174885f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.7420615f * width;
        cp1Y = y + 0.2301800f * height;
        cp2X = x + 0.8046360f * width;
        cp2Y = y + 0.2389345f * height;
        epX = x + 0.8046360f * width;
        epY = y + 0.2389345f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.9100748f * width;
        cp1Y = y + 0.2572006f * height;
        cp2X = x + 1.0125896f * width;
        cp2Y = y + 0.3273645f * height;
        epX = x + 0.9987402f * width;
        epY = y + 0.3717845f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.9888083f * width;
        cp1Y = y + 0.4036398f * height;
        cp2X = x + 0.9524266f * width;
        cp2Y = y + 0.4318752f * height;
        epX = x + 0.9136272f * width;
        epY = y + 0.4378396f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.8906142f * width;
        cp1Y = y + 0.4413773f * height;
        cp2X = x + 0.8713374f * width;
        cp2Y = y + 0.4552008f * height;
        epX = x + 0.8641774f * width;
        epY = y + 0.5042721f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.8490333f * width;
        cp1Y = y + 0.6080650f * height;
        cp2X = x + 0.8333525f * width;
        cp2Y = y + 0.6571297f * height;
        epX = x + 0.7569181f * width;
        epY = y + 0.7195737f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.7002070f * width;
        cp1Y = y + 0.7659046f * height;
        cp2X = x + 0.6829472f * width;
        cp2Y = y + 0.7891283f * height;
        epX = x + 0.6778798f * width;
        epY = y + 0.8259222f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.6717813f * width;
        cp1Y = y + 0.8702050f * height;
        cp2X = x + 0.6750328f * width;
        cp2Y = y + 0.8749209f * height;
        epX = x + 0.7327265f * width;
        epY = y + 0.9054682f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.7327265f * width;
        cp1Y = y + 0.9054682f * height;
        cp2X = x + 0.7940558f * width;
        cp2Y = y + 0.9208544f * height;
        epX = x + 0.7940558f * width;
        epY = y + 0.9379408f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.7940558f * width;
        cp1Y = y + 0.9601508f * height;
        cp2X = x + 0.7312392f * width;
        cp2Y = y + 0.9757761f * height;
        epX = x + 0.7312392f * width;
        epY = y + 0.9757761f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.6497790f * width;
        cp1Y = y + 1.0042619f * height;
        cp2X = x + 0.6109745f * width;
        cp2Y = y + 0.9975729f * height;
        epX = x + 0.3687538f * width;
        epY = y + 0.9940703f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.2289030f * width;
        cp1Y = y + 0.9920482f * height;
        cp2X = x + 0.1012021f * width;
        cp2Y = y + 0.9944944f * height;
        epX = x + 0.0849741f * width;
        epY = y + 0.9995067f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.0661758f * width;
        cp1Y = y + 1.0053127f * height;
        cp2X = x + 0.0435150f * width;
        cp2Y = y + 1.0015974f * height;
        epX = x + 0.0225290f * width;
        epY = y + 0.9892685f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        epX = x + 0.6012331f * width;
        epY = y + 0.9044799f * height;
        path.moveTo(epX, epY);

        cp1X = x + 0.6012331f * width;
        cp1Y = y + 0.8895969f * height;
        cp2X = x + 0.5561252f * width;
        cp2Y = y + 0.8258701f * height;
        epX = x + 0.5455904f * width;
        epY = y + 0.8258701f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.5393719f * width;
        cp1Y = y + 0.8258701f * height;
        cp2X = x + 0.5121032f * width;
        cp2Y = y + 0.8450645f * height;
        epX = x + 0.4849935f * width;
        epY = y + 0.8685243f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        epX = x + 0.4357029f * width;
        epY = y + 0.9111785f * height;
        path.lineTo(epX, epY);

        epX = x + 0.5184681f * width;
        epY = y + 0.9111785f * height;
        path.lineTo(epX, epY);

        cp1X = x + 0.5639889f * width;
        cp1Y = y + 0.9111785f * height;
        cp2X = x + 0.6012331f * width;
        cp2Y = y + 0.9081641f * height;
        epX = x + 0.6012331f * width;
        epY = y + 0.9044799f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);
    }
}