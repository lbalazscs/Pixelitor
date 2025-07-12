/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.AppMode;
import pixelitor.colors.Colors;
import pixelitor.filters.gui.*;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.layers.BlendingMode;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.test.RandomGUITest;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import static java.awt.Color.BLACK;
import static java.awt.Color.BLUE;
import static java.awt.Color.RED;
import static java.awt.Color.WHITE;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.KEY_FRACTIONALMETRICS;
import static java.awt.RenderingHints.KEY_RENDERING;
import static java.awt.RenderingHints.KEY_TEXT_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.awt.RenderingHints.VALUE_FRACTIONALMETRICS_ON;
import static java.awt.RenderingHints.VALUE_RENDER_QUALITY;
import static java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB;
import static pixelitor.filters.gui.TransparencyMode.ALPHA_ENABLED;

/**
 * A test {@link ParametrizedFilter} with all {@link FilterParam} objects
 */
public class ParamTestFilter extends ParametrizedFilter {
    public static final String NAME = "Param Test";

    @Serial
    private static final long serialVersionUID = 7920135228910788174L;

    public ParamTestFilter() {
        super(true);

        initParams(getTestParams());
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        if (AppMode.isDevelopment() && !RandomGUITest.isRunning()) {
            System.out.println("ParamTest.transform CALLED");
        }

        List<FilterParam> testParams = paramSet.getParams();
        List<String> lines = new ArrayList<>();
        for (FilterParam param : testParams) {
            String name = param.getName();
            String value = param.getParamValue();
            String line = name + " = " + value;
            lines.add(line);
        }

        dest = ImageUtils.copyImage(src);
        Graphics2D g = dest.createGraphics();

        g.setRenderingHint(KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        g.setRenderingHint(KEY_RENDERING, VALUE_RENDER_QUALITY);
        g.setRenderingHint(KEY_FRACTIONALMETRICS, VALUE_FRACTIONALMETRICS_ON);

        int width = dest.getWidth();
        int height = dest.getHeight();
        Colors.fillWith(BLACK, g, width, height);
        g.setColor(WHITE);

        Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 24);
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics(font);
        int maxLineWidth = 0;
        for (String line : lines) {
            maxLineWidth = Math.max(maxLineWidth, fm.stringWidth(line));
        }
        int blockHeight = lines.size() * fm.getHeight();

        // draw the strings, centering each line horizontally
        int startY = (height - blockHeight) / 2 + fm.getAscent();
        int currentY = startY;
        for (String line : lines) {
            int lineX = (width - fm.stringWidth(line)) / 2;
            g.drawString(line, lineX, currentY);
            currentY += fm.getHeight();
        }

        g.dispose();
        return dest;
    }

    public static FilterParam[] getTestParams() {
        float[] defaultThumbPositions = {0.0f, 0.5f, 1.0f};
        Color[] defaultValues = {BLACK, BLUE, WHITE};

        RangeParam a = new RangeParam("A", 0, 50, 100);
        RangeParam b = new RangeParam("B", 0, 25, 100);
        RangeParam c = new RangeParam("C", 0, 25, 100);

        return new FilterParam[]{
            new GradientParam("Colors", defaultThumbPositions, defaultValues),
            new RangeParam("RangeParam", 0, 50, 100),
            new GroupedRangeParam("Normalized Group", new RangeParam[]{
                a, b, c}, false).autoNormalized(),
            new RangeWithColorsParam(RED, BLUE, "RangeWithColorsParam", 0, 50, 100),
            new ImagePositionParam("ImagePositionParam"),
            new IntChoiceParam("IntChoiceParam", new Item[]{
                new Item("value 1", 1),
                new Item("value 2", 2),
            }),
            new ColorParam("ColorParam", WHITE, ALPHA_ENABLED),
            new GroupedColorsParam("GroupedColorsParam", "A", WHITE, "B", Color.YELLOW, ALPHA_ENABLED, true, false),
            new AngleParam("AngleParam", 0),
            new ElevationAngleParam("ElevationAngleParam", 0),
            new BlendingModeParam(BlendingMode.values()),
            new BooleanParam("BooleanParam"),
            new TextParam("TextParam", "default value", true),
            new LogZoomParam("Zoom", 200, 200, 1000),
        };
    }
}