/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.filters.gui.*;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.QuadrantAngle;
import pixelitor.utils.ReseedSupport;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Random;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static pixelitor.filters.gui.ColorParam.TransparencyPolicy.USER_ONLY_TRANSPARENCY;
import static pixelitor.filters.gui.RandomizePolicy.IGNORE_RANDOMIZE;
import static pixelitor.utils.QuadrantAngle.*;

/**
 * Render Truchet tiles filter
 */
public class Truchet extends ParametrizedFilter {
    public static final String NAME = "Truchet Tiles";

    private static final int PATTERN_RANDOM = 0;
    private static final int PATTERN_1 = 1;
    private static final int PATTERN_2 = 2;
    private static final int PATTERN_3 = 3;
    private static final int PATTERN_4 = 4;
    private static final int PATTERN_5 = 5;
    private static final int PATTERN_6 = 6;
    private static final int PATTERN_7 = 7;
    private static final int PATTERN_8 = 8;
    private static final int PATTERN_9 = 9;
    private static final int PATTERN_10 = 10;
    private static final int PATTERN_11 = 11;
    private static final int PATTERN_12 = 12;
    private static final int PATTERN_13 = 13;
    private static final int PATTERN_14 = 14;
    private static final int PATTERN_15 = 15;
    private static final int PATTERN_16 = 16;
    private static final int PATTERN_17 = 17;
    private static final int PATTERN_18 = 18;
    private static final int PATTERN_19 = 19;
    private static final int PATTERN_20 = 20;
    private static final int PATTERN_21 = 21;

    private final EnumParam<TileType> typeParam = new EnumParam<>("Type", TileType.class);
    private final IntChoiceParam patternParam = new IntChoiceParam("Pattern", new Item[]{
        new Item("Random", PATTERN_RANDOM),
        new Item("Un", PATTERN_1),
        new Item("Deux", PATTERN_2),
        new Item("Trois", PATTERN_3),
        new Item("Quatre", PATTERN_4),
        new Item("Cinq", PATTERN_5),
        new Item("Six", PATTERN_6),
        new Item("Sept", PATTERN_7),
        new Item("Huit", PATTERN_8),
        new Item("Neuf", PATTERN_9),
        new Item("Dix", PATTERN_10),
        new Item("Onze", PATTERN_11),
        new Item("Douze", PATTERN_12),
        new Item("Treize", PATTERN_13),
        new Item("Quatorze", PATTERN_14),
        new Item("Quinze", PATTERN_15),
        new Item("Seize", PATTERN_16),
        new Item("Dix-sept", PATTERN_17),
        new Item("Dix-huit", PATTERN_18),
        new Item("Dix-neuf", PATTERN_19),
        new Item("Vingt", PATTERN_20),
        new Item("Vingt et un", PATTERN_21),
    });
    private final RangeParam sizeParam = new RangeParam("Tile Size", 2, 20, 100);
    private final RangeParam widthParam = new RangeParam("Line Width", 1, 3, 20);
    private final ColorParam bgColor = new ColorParam("Background Color", WHITE, USER_ONLY_TRANSPARENCY);
    private final ColorParam fgColor = new ColorParam("Foreground Color", BLACK, USER_ONLY_TRANSPARENCY);
    private final BooleanParam showBoundary = new BooleanParam("Show Tile Boundary", false, IGNORE_RANDOMIZE);

    // the arrays are from https://openprocessing.org/sketch/162169
    private final int[][] ARRAY_3 = {
        {2, 2, 1, 1, 2, 2, 3, 3, 0, 0, 3, 3},
        {0, 0, 3, 3, 0, 0, 1, 1, 2, 2, 1, 1}};

    private final int[][] ARRAY_4 = {
        {0, 1, 2, 3},
        {1, 0, 3, 2},
        {2, 3, 0, 1},
        {3, 2, 1, 0}};

    private final int[][] ARRAY_5 = {
        {0, 0, 1, 1, 2, 2, 3, 3},
        {0, 0, 1, 1, 2, 2, 3, 3},
        {1, 1, 0, 0, 3, 3, 2, 2},
        {1, 1, 0, 0, 3, 3, 2, 2}};

    private final int[][] ARRAY_6 = {
        {2, 3, 2, 3, 0, 1, 0, 1},
        {1, 3, 2, 0, 3, 3, 2, 2},
        {2, 0, 1, 3, 0, 0, 1, 1},
        {1, 0, 1, 0, 3, 2, 3, 2},
        {0, 1, 0, 1, 2, 3, 2, 3},
        {3, 3, 2, 2, 1, 3, 2, 0},
        {0, 0, 1, 1, 2, 0, 1, 3},
        {3, 2, 3, 2, 1, 0, 1, 0}};

    private final int[][] ARRAY_7 = {
        {0, 1, 1, 0, 0, 1},
        {3, 3, 2, 3, 2, 2},
        {0, 0, 1, 0, 1, 1},
        {3, 2, 2, 3, 3, 2}};

    private final int[][] ARRAY_8 = {
        {0, 2, 3},
        {3, 2, 0},
        {2, 0, 1},
        {1, 0, 2}
    };

    private final int[][] ARRAY_9 = {
        {0, 2, 3, 1},
        {1, 0, 2, 3},
        {3, 1, 0, 2},
        {2, 3, 1, 0}
    };

    private final int[][] ARRAY_10 = {
        {2, 1, 3, 1, 0, 1, 3, 1},
        {1, 3, 1, 0, 1, 3, 1, 2},
        {3, 1, 0, 1, 3, 1, 2, 1},
        {1, 0, 1, 3, 1, 2, 1, 3},
        {0, 1, 3, 1, 2, 1, 3, 1},
        {1, 3, 1, 2, 1, 3, 1, 0},
        {3, 1, 2, 1, 3, 1, 0, 1},
        {1, 2, 1, 3, 1, 0, 1, 3}
    };

    private final int[][] ARRAY_11 = {
        {0, 2, 2, 3, 2, 0},
        {2, 2, 0, 0, 1, 0},
        {2, 0, 0, 2, 2, 3},
        {1, 0, 2, 2, 0, 0},
        {2, 3, 2, 0, 0, 2},
        {0, 0, 1, 0, 2, 2}
    };

    private final int[][] ARRAY_12 = {
        {3, 2, 3, 2, 1, 0, 1, 0},
        {0, 2, 3, 1, 2, 0, 1, 3},
        {3, 1, 0, 2, 1, 3, 2, 0},
        {0, 1, 0, 1, 2, 3, 2, 3},
        {1, 0, 1, 0, 3, 2, 3, 2},
        {2, 0, 1, 3, 0, 2, 3, 1},
        {1, 3, 2, 0, 3, 1, 0, 2},
        {2, 3, 2, 3, 0, 1, 0, 1}
    };

    private final int[][] ARRAY_13 = {
        {0, 2, 3, 1, 2, 0, 1, 3},
        {0, 1, 0, 1, 2, 3, 2, 3},
        {3, 2, 3, 2, 1, 0, 1, 0},
        {0, 2, 3, 1, 2, 0, 1, 3},
        {0, 1, 0, 1, 2, 3, 2, 3},
        {3, 2, 3, 2, 1, 0, 1, 0}
    };

    private final int[][] ARRAY_14 = {
        {2, 2, 3, 3, 0, 0, 1, 1},
        {1, 1, 0, 0, 3, 3, 2, 2},
        {2, 0, 1, 3, 0, 2, 3, 1},
        {1, 3, 2, 0, 3, 1, 0, 2}
    };

    private final int[][] ARRAY_15 = {
        {0, 2, 1, 0, 3, 1, 2, 0, 3, 2, 1, 3},
        {2, 0, 3, 2, 1, 3, 0, 2, 1, 0, 3, 1},
        {3, 1, 0, 1, 0, 2, 1, 3, 2, 3, 2, 0},
        {0, 2, 3, 2, 3, 1, 2, 0, 1, 0, 1, 3},
        {1, 3, 0, 1, 2, 0, 3, 1, 2, 3, 0, 2},
        {3, 1, 2, 3, 0, 2, 1, 3, 0, 1, 2, 0},
        {2, 0, 3, 2, 1, 3, 0, 2, 1, 0, 3, 1},
        {0, 2, 1, 0, 3, 1, 2, 0, 3, 2, 1, 3},
        {1, 3, 2, 3, 2, 0, 3, 1, 0, 1, 0, 2},
        {2, 0, 1, 0, 1, 3, 0, 2, 3, 2, 3, 1},
        {3, 1, 2, 3, 0, 2, 1, 3, 0, 1, 2, 0},
        {1, 3, 0, 1, 2, 0, 3, 1, 2, 3, 0, 2}
    };

    private final int[][] ARRAY_16 = {
        {0, 1, 3, 1, 3, 2, 0, 2},
        {3, 2, 0, 2, 0, 1, 3, 1},
        {1, 0, 2, 0, 2, 3, 1, 3},
        {3, 2, 0, 2, 0, 1, 3, 1},
        {1, 0, 2, 0, 2, 3, 1, 3},
        {2, 3, 1, 3, 1, 0, 2, 0},
        {0, 1, 3, 1, 3, 2, 0, 2},
        {2, 3, 1, 3, 1, 0, 2, 0}
    };

    private final int[][] ARRAY_17 = {
        {0, 0, 0, 2, 0, 1, 3, 1, 1, 1},
        {0, 0, 2, 0, 2, 3, 1, 3, 1, 1},
        {0, 2, 0, 2, 0, 1, 3, 1, 3, 1},
        {2, 0, 2, 0, 0, 1, 1, 3, 1, 3},
        {0, 2, 0, 0, 2, 3, 1, 1, 3, 1},
        {3, 1, 3, 3, 1, 0, 2, 2, 0, 2},
        {1, 3, 1, 3, 3, 2, 2, 0, 2, 0},
        {3, 1, 3, 1, 3, 2, 0, 2, 0, 2},
        {3, 3, 1, 3, 1, 0, 2, 0, 2, 2},
        {3, 3, 3, 1, 3, 2, 0, 2, 2, 2}
    };

    private final int[][] ARRAY_18 = {
        {0, 1, 0, 1, 0, 1, 2, 3, 2, 3, 2, 3},
        {3, 2, 2, 3, 3, 2, 1, 0, 0, 1, 1, 0},
        {0, 2, 0, 1, 3, 1, 2, 0, 2, 3, 1, 3},
        {3, 1, 3, 2, 0, 2, 1, 3, 1, 0, 2, 0},
        {0, 1, 1, 0, 0, 1, 2, 3, 3, 2, 2, 3},
        {3, 2, 3, 2, 3, 2, 1, 0, 1, 0, 1, 0},
        {2, 3, 2, 3, 2, 3, 0, 1, 0, 1, 0, 1},
        {1, 0, 0, 1, 1, 0, 3, 2, 2, 3, 3, 2},
        {2, 0, 2, 3, 1, 3, 0, 2, 0, 1, 3, 1},
        {1, 3, 1, 0, 2, 0, 3, 1, 3, 2, 0, 2},
        {2, 3, 3, 2, 2, 3, 0, 1, 1, 0, 0, 1},
        {1, 0, 1, 0, 1, 0, 3, 2, 3, 2, 3, 2}
    };

    private final int[][] ARRAY_19 = {
        {0, 2, 0, 2, 0, 1, 3, 1},
        {0, 1, 3, 1, 0, 2, 0, 2},
        {2, 3, 1, 3, 2, 0, 2, 0},
        {2, 0, 2, 0, 2, 3, 1, 3}
    };


    public Truchet() {
        super(false);

        helpURL = "https://en.wikipedia.org/wiki/Truchet_tiles";

        typeParam.setupEnableOtherIf(widthParam, type -> type != TileType.TRIANGLES);
        FilterButtonModel reseedAction = ReseedSupport.createAction();
        patternParam.setupDisableOtherIf(reseedAction, item -> item.getValue() != PATTERN_RANDOM);

        setParams(
            typeParam,
            patternParam.withAction(reseedAction),
            sizeParam,
            widthParam,
            bgColor,
            fgColor,
            showBoundary
        );
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        Random rand = ReseedSupport.getLastSeedRandom();
        int size = sizeParam.getValue();

        // generates 4 images, although with the current tiles only 2 will be different
        BufferedImage[] tiles = {
            createTile(null),
            createTile(ANGLE_90),
            createTile(ANGLE_180),
            createTile(ANGLE_270)};

        dest = ImageUtils.copyImage(src);
        Graphics2D g = dest.createGraphics();

        int numTilesHor = dest.getWidth() / size + 1;
        int numTilesVer = dest.getHeight() / size + 1;
        int pattern = patternParam.getValue();

        for (int j = 0; j < numTilesVer; j++) {
            for (int i = 0; i < numTilesHor; i++) {
                int tileIndex = switch (pattern) {
                    case PATTERN_RANDOM -> rand.nextInt(4);
                    case PATTERN_1 -> (i % 2 == 0) ? ((j % 2 == 0) ? 0 : 1) : ((j % 2 == 0) ? 2 : 3);
                    case PATTERN_2 -> (i % 2 == 0) ? ((j % 2 == 0) ? 0 : 2) : ((j % 2 == 0) ? 1 : 3);
                    case PATTERN_3 -> indexFromArray(ARRAY_3, i, j);
                    case PATTERN_4 -> indexFromArray(ARRAY_4, i, j);
                    case PATTERN_5 -> indexFromArray(ARRAY_5, i, j);
                    case PATTERN_6 -> indexFromArray(ARRAY_6, i, j);
                    case PATTERN_7 -> indexFromArray(ARRAY_7, i, j);
                    case PATTERN_8 -> indexFromArray(ARRAY_8, i, j);
                    case PATTERN_9 -> indexFromArray(ARRAY_9, i, j);
                    case PATTERN_10 -> indexFromArray(ARRAY_10, i, j);
                    case PATTERN_11 -> indexFromArray(ARRAY_11, i, j);
                    case PATTERN_12 -> indexFromArray(ARRAY_12, i, j);
                    case PATTERN_13 -> indexFromArray(ARRAY_13, i, j);
                    case PATTERN_14 -> indexFromArray(ARRAY_14, i, j);
                    case PATTERN_15 -> indexFromArray(ARRAY_15, i, j);
                    case PATTERN_16 -> indexFromArray(ARRAY_16, i, j);
                    case PATTERN_17 -> indexFromArray(ARRAY_17, i, j);
                    case PATTERN_18 -> indexFromArray(ARRAY_18, i, j);
                    case PATTERN_19 -> indexFromArray(ARRAY_19, i, j);
                    case PATTERN_20 -> (i < numTilesHor / 2) ? ((j < numTilesVer / 2) ? 0 : 1) : ((j < numTilesVer / 2) ? 3 : 2);
                    case PATTERN_21 -> (i < numTilesHor / 2) ? ((j < numTilesVer / 2) ? 1 : 0) : ((j < numTilesVer / 2) ? 2 : 3);
                    default -> throw new IllegalStateException("Unexpected value: " + pattern);
                };
                g.drawImage(tiles[tileIndex], i * size, j * size, null);
            }
        }

        if (showBoundary.isChecked()) {
            g.setColor(Color.RED);
            for (int i = 1; i < numTilesHor; i++) {
                g.drawLine(i * size, 0, i * size, dest.getHeight());
            }
            for (int j = 1; j < numTilesVer; j++) {
                g.drawLine(0, j * size, dest.getWidth(), j * size);
            }
        }

        g.dispose();
        return dest;
    }

    private static int indexFromArray(int[][] array, int i, int j) {
        int sizeX = array[0].length;
        int sizeY = array.length;
        return array[j % sizeY][i % sizeX];
    }

    private BufferedImage createTile(QuadrantAngle angle) {
        int tileSize = sizeParam.getValue();
        TileType type = typeParam.getSelected();

        BufferedImage tile = new BufferedImage(tileSize, tileSize, TYPE_INT_ARGB);
        Graphics2D tg = tile.createGraphics();
        Colors.fillWith(bgColor.getColor(), tg, tileSize, tileSize);

        if (angle != null) {
            tg.transform(angle.createTransform(tileSize, tileSize));
        }

        tg.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        tg.setColor(fgColor.getColor());

        Shape shape = type.createFilledArea(tileSize, widthParam.getValue());
        tg.fill(shape);
        tg.dispose();
        return tile;
    }

    @Override
    protected boolean createDefaultDestImg() {
        return false;
    }

    enum TileType {
        TRIANGLES("Triangles") {
            @Override
            public Shape createFilledArea(int tileSize, int lineWidth) {
                Path2D triangle = new Path2D.Double();
                triangle.moveTo(tileSize, 0);
                triangle.lineTo(tileSize, tileSize);
                triangle.lineTo(0, tileSize);
                triangle.closePath();
                return triangle;
            }
        }, QUARTER_CIRCLES("Quarter Circles") {
            @Override
            public Shape createFilledArea(int tileSize, int lineWidth) {
                Path2D path = new Path2D.Double();
                BasicStroke stroke = new BasicStroke(lineWidth);

                Rectangle2D bounds1 = new Rectangle2D.Double(
                    tileSize / 2.0, -tileSize / 2.0, tileSize, tileSize);
                Arc2D arc1 = new Arc2D.Double(bounds1, 180, 90, Arc2D.OPEN);
                path.append(stroke.createStrokedShape(arc1), false);

                Rectangle2D bounds2 = new Rectangle2D.Double(
                    -tileSize / 2.0, tileSize / 2.0, tileSize, tileSize);
                Arc2D arc2 = new Arc2D.Double(bounds2, 0, 90, Arc2D.OPEN);
                path.append(stroke.createStrokedShape(arc2), false);

                return path;
            }
        }, DIAGONALS("Diagonals") {
            @Override
            public Shape createFilledArea(int tileSize, int lineWidth) {
                Line2D line = new Line2D.Double(0, 0, tileSize, tileSize);
                BasicStroke stroke = new BasicStroke(lineWidth);
                return stroke.createStrokedShape(line);
            }
        };

        private final String guiName;

        TileType(String guiName) {
            this.guiName = guiName;
        }

        public abstract Shape createFilledArea(int tileSize, int lineWidth);

        @Override
        public String toString() {
            return guiName;
        }
    }
}