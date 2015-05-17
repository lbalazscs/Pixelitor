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

package pixelitor.filters;

import com.jhlabs.composite.AddComposite;
import org.jdesktop.swingx.image.FastBlurFilter;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.utils.ImageUtils;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;

/**
 * Mystic Rose
 */
public class MysticRose extends FilterWithParametrizedGUI {
    private final ImagePositionParam center = new ImagePositionParam("Center");
    private final RangeParam nrPoints = new RangeParam("Number of Points", 3, 42, 10);
    private final RangeParam lineWidth = new RangeParam("Line Width", 1, 10, 1);
    private final RangeParam radius = new RangeParam("Radius (Height %)", 1, 200, 45);
    private final RangeParam rotate = new RangeParam("Rotate", 0, 100, 0);
    private final BooleanParam glow = new BooleanParam("Glow", false);

    public MysticRose() {
        super("Mystic Rose", false, false);
        setParamSet(new ParamSet(
                nrPoints,
                rotate,
                center,
                radius,
                lineWidth,
                glow
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();

        dest = new BufferedImage(srcWidth, srcHeight, src.getType());
        Graphics2D g2 = dest.createGraphics();
        g2.setColor(BLACK);
        g2.fillRect(0, 0, srcWidth, srcHeight);
        g2.setColor(WHITE);
        g2.setStroke(new BasicStroke(lineWidth.getValueAsFloat()));
        g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

        int numPoints = nrPoints.getValue();
        Point[] points = new Point[numPoints];
        double r = radius.getValueAsPercentage() * srcHeight;
        double startAngle = 2 * Math.PI / numPoints * rotate.getValueAsPercentage();
        double cx = srcWidth * center.getRelativeX();
        double cy = srcHeight * center.getRelativeY();

        for (int i = 0; i < points.length; i++) {
            double theta = startAngle + 2 * Math.PI * i / numPoints;
            points[i] = new Point((int) (cx + r * Math.cos(theta)), (int) (cy + r * Math.sin(theta)));
        }
        for (int i = 0; i < points.length; i++) {
            for (int j = 0; j < points.length; j++) {
                if(i > j) { // draw only in one direction
                    g2.drawLine(points[i].x, points[i].y, points[j].x, points[j].y);
                }
            }
        }

        if (glow.isChecked()) {
            BufferedImage copy = ImageUtils.copyImage(dest);
            // TODO FastBlurFilter takes an int as constructor argument - not good for animation
            // BoxBlurFilter on the other hand might be OK
            FastBlurFilter fastBlur = new FastBlurFilter(lineWidth.getValue());
            fastBlur.filter(copy, copy);
            g2.setComposite(new AddComposite(1.0f));
            g2.drawImage(copy, 0, 0, null);

//            GlowFilter glowFilter = new GlowFilter();
//            glowFilter.setRadius(lineWidth.getValue());
//            glowFilter.filter(dest, dest);
        }
        g2.dispose();

        return dest;
    }
}