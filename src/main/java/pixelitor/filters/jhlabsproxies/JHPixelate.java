/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

package pixelitor.filters.jhlabsproxies;

import com.jhlabs.image.BlockFilter;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.filters.impl.BrickBlockFilter;
import pixelitor.utils.ImageUtils;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static java.awt.Color.GRAY;
import static java.awt.Color.WHITE;

/**
 * Pixelate based on the JHLabs BlockFilter
 */
public class JHPixelate extends FilterWithParametrizedGUI {
    public static final String NAME = "Pixelate";

    private static final int STYLE_FLAT = 0;
    private static final int STYLE_3D = 1;
    private static final int STYLE_EMBEDDED = 2;
//    private static final int STYLE_GRID_ONLY = 3;

    private static final int TYPE_SQUARE = 0;
    private static final int TYPE_BRICK = 1;

    private final IntChoiceParam typeParam = new IntChoiceParam("Type", new IntChoiceParam.Value[]{
            new IntChoiceParam.Value("Squares", TYPE_SQUARE),
            new IntChoiceParam.Value("Brick Wall", TYPE_BRICK),
    });

    private final IntChoiceParam styleParam = new IntChoiceParam("Style", new IntChoiceParam.Value[]{
            new IntChoiceParam.Value("Flat", STYLE_FLAT),
            new IntChoiceParam.Value("3D", STYLE_3D),
            new IntChoiceParam.Value("Embedded", STYLE_EMBEDDED),
//            new IntChoiceParam.Value("Grid", STYLE_GRID_ONLY)
    });

    private final RangeParam cellSizeParam = new RangeParam("Cell Size", 3, 20, 200);

    private BlockFilter blockFilter;
    private BrickBlockFilter brickBlockFilter;

    public JHPixelate() {
        super(ShowOriginal.YES);
        setParamSet(new ParamSet(
                cellSizeParam.adjustRangeToImageSize(0.2),
                styleParam,
                typeParam
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {

        int style = styleParam.getValue();
        int type = typeParam.getValue();

        int cellSize = cellSizeParam.getValue();

        if (style == STYLE_FLAT || style == STYLE_3D || style == STYLE_EMBEDDED) {
            if (type == TYPE_SQUARE) {
                if (blockFilter == null) {
                    blockFilter = new BlockFilter(NAME);
                }
                blockFilter.setBlockSize(cellSize);
                dest = blockFilter.filter(src, dest);
            } else if (type == TYPE_BRICK) {
                if (brickBlockFilter == null) {
                    brickBlockFilter = new BrickBlockFilter(NAME);
                }
                brickBlockFilter.setHorizontalBlockSize(cellSize * 2);
                brickBlockFilter.setVerticalBlockSize(cellSize);
                dest = brickBlockFilter.filter(src, dest);
            }
        }

//        if ((style == STYLE_3D) || (style == STYLE_GRID_ONLY || (style == STYLE_EMBEDDED))) {
        if ((style == STYLE_3D) || (style == STYLE_EMBEDDED)) {
            int width = dest.getWidth();
            int height = dest.getHeight();

            BufferedImage bumpSource;
            if (style == STYLE_EMBEDDED) {
                bumpSource = dest;
            } else {
                bumpSource = createBumpSource(type, cellSize, width, height, src);
            }

            if (style == STYLE_3D || style == STYLE_EMBEDDED) {
                dest = ImageUtils.bumpMap(dest, bumpSource, NAME);
//            } else if (style == STYLE_GRID_ONLY) {
//                dest = ImageUtils.bumpMap(src, bumpSource);
            } else {
                throw new IllegalStateException("style = " + style);
            }
        }

        return dest;
    }

    private static BufferedImage createBumpSource(int type, int cellSize, int width, int height, BufferedImage src) {
        BufferedImage bumpSource = ImageUtils.createImageWithSameColorModel(src);

        int gapWidth;
        if (cellSize < 15) {
            gapWidth = 1;
        } else {
            gapWidth = 2;
        }

        Graphics2D g = bumpSource.createGraphics();

        g.setColor(WHITE);
        g.fillRect(0, 0, width, height);

        if (type == TYPE_SQUARE) {
            ImageUtils.drawGrid(GRAY, g, width, height, gapWidth, cellSize, gapWidth, cellSize, false);
        } else if (type == TYPE_BRICK) {
            ImageUtils.drawBrickGrid(GRAY, g, cellSize, width, height);
        }

        g.dispose();
        return bumpSource;
    }

    @Override
    public boolean supportsGray() {
        return false;
    }
}