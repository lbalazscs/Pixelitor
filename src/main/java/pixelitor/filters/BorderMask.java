/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.filters;

import pixelitor.colors.Colors;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.GroupedRangeParam;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.Serial;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;

/**
 * Border Mask filter
 */
public class BorderMask extends ParametrizedFilter {
    public static final String NAME = "Border Mask";

    @Serial
    private static final long serialVersionUID = 1L;

    private final GroupedRangeParam distanceParam = new GroupedRangeParam("Distance",
        new String[] {"North", "East", "South", "West"}, 0, 10, 500, true);
    private final GroupedRangeParam roundnessParam = new GroupedRangeParam("Roundness", 0, 20, 500);
    private final BooleanParam invertParam = new BooleanParam("Invert", false);
    private final BooleanParam transparencyParam = new BooleanParam("Render Transparency", false);

    public BorderMask() {
        super(false);

        setParams(
            distanceParam,
            roundnessParam,
            invertParam,
            transparencyParam
        );
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        int width = dest.getWidth();
        int height = dest.getHeight();

        Graphics2D g = dest.createGraphics();

        boolean invert = invertParam.isChecked();
        boolean renderTransparency = transparencyParam.isChecked();

        if (renderTransparency) {
            g.drawImage(src, 0, 0, null);
        } else {
            Colors.fillWith(invert ? WHITE : BLACK, g, width, height);
        }

        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

        double distN = distanceParam.getValueAsDouble(0);
        double distE = distanceParam.getValueAsDouble(1);
        double distS = distanceParam.getValueAsDouble(2);
        double distW = distanceParam.getValueAsDouble(3);
        double roundnessH = roundnessParam.getValueAsDouble(0);
        double roundnessV = roundnessParam.getValueAsDouble(1);

        Shape rect = new RoundRectangle2D.Double(distW, distN,
            width - distW - distE, height - distN - distS, roundnessH, roundnessV);

        if (renderTransparency) {
            g.setComposite(AlphaComposite.DstOut);
            if (invert) {
                g.fill(rect);
            } else {
                Area filledArea = new Area(new Rectangle(0, 0, width, height));
                filledArea.subtract(new Area(rect));
                g.fill(filledArea);
            }
        } else {
            g.setColor(invert ? BLACK : WHITE);
            g.fill(rect);
        }

        g.dispose();
        return dest;
    }
}