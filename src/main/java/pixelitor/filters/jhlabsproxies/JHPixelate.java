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

package pixelitor.filters.jhlabsproxies;

import com.jhlabs.image.BlockFilter;
import pixelitor.colors.Colors;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.impl.BrickBlockFilter;
import pixelitor.filters.impl.TriangleBlockFilter;
import pixelitor.gui.GUIText;
import pixelitor.utils.ImageUtils;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.Serial;

import static java.awt.Color.GRAY;
import static java.awt.Color.WHITE;

/**
 * Pixelate filter based on the JHLabs {@link BlockFilter}
 * (or alternatively on {@link BrickBlockFilter})
 */
public class JHPixelate extends ParametrizedFilter {
    public static final String NAME = "Pixelate";

    @Serial
    private static final long serialVersionUID = 152174018715974359L;

    private static final int STYLE_FLAT = 0;
    private static final int STYLE_3D = 1;
    private static final int STYLE_EMBEDDED = 2;

    private static final int TYPE_SQUARE = 0;
    private static final int TYPE_BRICK = 1;
    private static final int TYPE_TRIANGLE = 2;

    private final IntChoiceParam typeParam = new IntChoiceParam(GUIText.TYPE, new Item[]{
        new Item("Squares", TYPE_SQUARE),
        new Item("Brick Wall", TYPE_BRICK),
        new Item("Triangles", TYPE_TRIANGLE),
    });

    private final IntChoiceParam styleParam = new IntChoiceParam("Style", new Item[]{
        new Item("Flat", STYLE_FLAT),
        new Item("3D", STYLE_3D),
        new Item("Embedded", STYLE_EMBEDDED),
    });

    private final RangeParam cellSizeParam = new RangeParam("Cell Size", 3, 20, 200);

    private BlockFilter blockFilter;
    private BrickBlockFilter brickBlockFilter;
    private TriangleBlockFilter triangleBlockFilter;

    public JHPixelate() {
        super(true);

        typeParam.setPresetKey("Type");

        setParams(
            cellSizeParam.withAdjustedRange(0.2),
            styleParam,
            typeParam
        );
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        int style = styleParam.getValue();
        int type = typeParam.getValue();

        int cellSize = cellSizeParam.getValue();

        if (style == STYLE_FLAT || style == STYLE_3D || style == STYLE_EMBEDDED) {
            switch (type) {
                case TYPE_SQUARE -> {
                    if (blockFilter == null) {
                        blockFilter = new BlockFilter(NAME);
                    }
                    blockFilter.setBlockSize(cellSize);
                    dest = blockFilter.filter(src, dest);
                }
                case TYPE_BRICK -> {
                    if (brickBlockFilter == null) {
                        brickBlockFilter = new BrickBlockFilter(NAME);
                    }
                    brickBlockFilter.setHorBlockSize(cellSize * 2);
                    brickBlockFilter.setVerBlockSize(cellSize);
                    dest = brickBlockFilter.filter(src, dest);
                }
                case TYPE_TRIANGLE -> {
                    if (triangleBlockFilter == null) {
                        triangleBlockFilter = new TriangleBlockFilter(NAME);
                    }
                    triangleBlockFilter.setSize(cellSize);
                    dest = triangleBlockFilter.filter(src, dest);
                }
            }
        }

        if (style == STYLE_3D || style == STYLE_EMBEDDED) {
            int width = dest.getWidth();
            int height = dest.getHeight();

            BufferedImage bumpSource = (style == STYLE_EMBEDDED)
                ? dest
                : createBumpSource(type, cellSize, width, height, src);
            dest = ImageUtils.bumpMap(dest, bumpSource, NAME);
        }

        return dest;
    }

    private static BufferedImage createBumpSource(int type, int cellSize, int width, int height, BufferedImage src) {
        BufferedImage bumpSource = ImageUtils.createImageWithSameCM(src);

        Graphics2D g = bumpSource.createGraphics();
        Colors.fillWith(WHITE, g, width, height);

        int gapWidth = (cellSize < 15) ? 1 : 2;
        switch (type) {
            case TYPE_SQUARE -> ImageUtils.renderGrid(g, GRAY, gapWidth, cellSize, width, height);
            case TYPE_BRICK -> ImageUtils.renderBrickGrid(g, GRAY, cellSize, width, height);
            case TYPE_TRIANGLE -> ImageUtils.renderTriangleGrid(g, GRAY, gapWidth, cellSize, width, height);
            default -> throw new IllegalStateException("Unexpected value: " + type);
        }

        g.dispose();
        return bumpSource;
    }

    @Override
    public boolean supportsGray() {
        return false;
    }
}