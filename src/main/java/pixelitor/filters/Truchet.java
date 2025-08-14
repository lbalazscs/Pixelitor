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

import pixelitor.compactions.QuadrantAngle;
import pixelitor.filters.gui.*;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.utils.ImageUtils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static pixelitor.compactions.QuadrantAngle.ANGLE_180;
import static pixelitor.compactions.QuadrantAngle.ANGLE_270;
import static pixelitor.compactions.QuadrantAngle.ANGLE_90;
import static pixelitor.filters.gui.RandomizeMode.IGNORE_RANDOMIZE;
import static pixelitor.filters.gui.TransparencyMode.MANUAL_ALPHA_ONLY;

/**
 * Render Truchet tiles filter
 */
public class Truchet extends ParametrizedFilter {
    @Serial
    private static final long serialVersionUID = -3274808280051693533L;

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
    private static final int PATTERN_22 = 22;
    private static final int PATTERN_RANDOM_EXTENDED = 23;
    private static final int PATTERN_24 = 24;
    private static final int PATTERN_25 = 25;
    private static final int PATTERN_26 = 26;
    private static final int PATTERN_27 = 27;
    private static final int PATTERN_28 = 28;


    public static final Map<String, String> migration_helper = Map.ofEntries(
        Map.entry("Un", "Aquamarine"),
        Map.entry("Deux", "Baryte"),
        Map.entry("Trois", "Citrine"),
        Map.entry("Quatre", "Diamond"),
        Map.entry("Cinq", "Emerald"),
        Map.entry("Six", "Friedelite"),
        Map.entry("Sept", "Garnet"),
        Map.entry("Huit", "Hambergite"),
        Map.entry("Neuf", "Iolite"),
        Map.entry("Dix", "Jade"),
        Map.entry("Onze", "Kyanite"),
        Map.entry("Douze", "Lapis"),
        Map.entry("Treize", "Moonstone"),
        Map.entry("Quatorze", "Neptunite"),
        Map.entry("Quinze", "Opal"),
        Map.entry("Seize", "Pearl"),
        Map.entry("Dix-sept", "Quartz"),
        Map.entry("Dix-huit", "Ruby"),
        Map.entry("Dix-neuf", "Sapphire"),
        Map.entry("Vingt", "Turquoise"),
        Map.entry("Vingt et un", "Uvite")
    );

    private final EnumParam<TileType> typeParam = new EnumParam<>("Type", TileType.class);
    private final IntChoiceParam patternParam = new IntChoiceParam("Pattern", new Item[]{
        new Item("Random", PATTERN_RANDOM),
        new Item("Ex Random", PATTERN_RANDOM_EXTENDED),
        new Item("Aquamarine", PATTERN_1),
        new Item("Baryte", PATTERN_2),
        new Item("Citrine", PATTERN_3),
        new Item("Diamond", PATTERN_4),
        new Item("Emerald", PATTERN_5),
        new Item("Friedelite", PATTERN_6),
        new Item("Garnet", PATTERN_7),
        new Item("Hambergite", PATTERN_8),
        new Item("Iolite", PATTERN_9),
        new Item("Jade", PATTERN_10),
        new Item("Kyanite", PATTERN_11),
        new Item("Lapis", PATTERN_12),
        new Item("Moonstone", PATTERN_13),
        new Item("Neptunite", PATTERN_14),
        new Item("Opal", PATTERN_15),
        new Item("Pearl", PATTERN_16),
        new Item("Quartz", PATTERN_17),
        new Item("Ruby", PATTERN_18),
        new Item("Sapphire", PATTERN_19),
        new Item("Turquoise", PATTERN_20),
        new Item("Uvite", PATTERN_21),
        new Item("Vivianite", PATTERN_22),
        new Item("Wavellite", PATTERN_24),
        new Item("Xonotlite", PATTERN_25),
        new Item("Yugawaralite", PATTERN_26),
        new Item("Zincite", PATTERN_27),
        new Item("Amethyst", PATTERN_28),
    });
    private final RangeParam sizeParam = new RangeParam("Tile Size", 2, 20, 100);
    private final RangeParam widthParam = new RangeParam("Line Width", 1, 3, 20);
    private final ColorParam bgColor = new ColorParam("Background Color", WHITE, MANUAL_ALPHA_ONLY);
    private final ColorParam fgColor = new ColorParam("Foreground Color", BLACK, MANUAL_ALPHA_ONLY);
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

    private final int[][] ARRAY_22 = {
        {0, 2, 4, 4},
        {2, 0, 4, 4},
        {5, 5, 2, 0},
        {5, 5, 0, 2}
    };

    private final int[][] ARRAY_26 = {
        {4, 3},
        {1, 5},
    };

    private final int[][] ARRAY_27 = {
        {4, 0, 4, 5, 3, 5},
        {3, 5, 1, 2, 4, 0},
        {4, 2, 4, 5, 1, 5},
        {5, 3, 5, 4, 0, 4},
        {2, 4, 0, 3, 5, 1},
        {5, 1, 5, 4, 2, 4},
    };

    private final int[][] ARRAY_28 = {
        {5, 4, 3},
        {3, 5, 4},
        {4, 3, 5},
    };

    private final int[][] ARRAY_29 = {
        {2, 4, 3},
        {0, 1, 4},
        {3, 2, 4},
        {1, 4, 0},
    };

    private final int[][] ARRAY_30 = {
        {5, 4},
        {3, 1},
        {4, 5},
        {0, 2},
    };


    public Truchet() {
        super(false);

        help = Help.fromWikiURL("https://en.wikipedia.org/wiki/Truchet_tiles");

        typeParam.setupEnableOtherIf(widthParam, type -> type != TileType.TRIANGLES);
        FilterButtonModel reseedAction = paramSet.createReseedAction();
        List<Integer> randomPatterns = List.of(PATTERN_RANDOM, PATTERN_RANDOM_EXTENDED);
        patternParam.setupDisableOtherIf(reseedAction, item -> !randomPatterns.contains(item.value()));

        initParams(
            typeParam,
            patternParam.withSideButton(reseedAction),
            sizeParam,
            widthParam,
            bgColor,
            fgColor,
            showBoundary
        );
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        Random rand = paramSet.getLastSeedRandom();
        int size = sizeParam.getValue();

        // generates 4 images, although with the current tiles only 2 will be different
        BufferedImage[] tiles = {
            createTile(null),
            createTile(ANGLE_90),
            createTile(ANGLE_180),
            createTile(ANGLE_270),
            createPlainTile(true),
            createPlainTile(false)
        };

        dest = ImageUtils.copyImage(src);
        Graphics2D g = dest.createGraphics();

        g.setColor(bgColor.getColor());
        g.fillRect(0, 0, dest.getWidth(), dest.getHeight());

        int numTilesHor = dest.getWidth() / size + 1;
        int numTilesVer = dest.getHeight() / size + 1;
        int pattern = patternParam.getValue();

        for (int j = 0; j < numTilesVer; j++) {
            for (int i = 0; i < numTilesHor; i++) {
                int tileIndex = switch (pattern) {
                    case PATTERN_RANDOM -> rand.nextInt(4);
                    case PATTERN_RANDOM_EXTENDED -> rand.nextInt(6);
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
                    case PATTERN_20 ->
                        (i < numTilesHor / 2) ? ((j < numTilesVer / 2) ? 0 : 1) : ((j < numTilesVer / 2) ? 3 : 2);
                    case PATTERN_21 ->
                        (i < numTilesHor / 2) ? ((j < numTilesVer / 2) ? 1 : 0) : ((j < numTilesVer / 2) ? 2 : 3);
                    case PATTERN_22 -> indexFromArray(ARRAY_22, i, j);
                    case PATTERN_24 -> indexFromArray(ARRAY_26, i, j);
                    case PATTERN_25 -> indexFromArray(ARRAY_27, i, j);
                    case PATTERN_26 -> indexFromArray(ARRAY_28, i, j);
                    case PATTERN_27 -> indexFromArray(ARRAY_29, i, j);
                    case PATTERN_28 -> indexFromArray(ARRAY_30, i, j);
                    default -> throw new IllegalStateException("Unexpected value: " + pattern);
                };
                g.drawImage(tiles[tileIndex], i * size - size / 2, j * size - size / 2, null);
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

        BufferedImage tile = new BufferedImage(2 * tileSize, 2 * tileSize, TYPE_INT_ARGB);
        Graphics2D tg = tile.createGraphics();

        if (angle != null) {
            tg.transform(angle.createTransform(2 * tileSize, 2 * tileSize));
        }

        tg.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        tg.setColor(fgColor.getColor());

        Shape shape = type.createFilledArea(tileSize, widthParam.getValue());
        tg.fill(shape);
        tg.dispose();
        return tile;
    }

    private BufferedImage createPlainTile(boolean isBlank) {
        int tileSize = sizeParam.getValue();
        TileType type = typeParam.getSelected();

        BufferedImage tile = new BufferedImage(2 * tileSize, 2 * tileSize, TYPE_INT_ARGB);
        Graphics2D tg = tile.createGraphics();

        tg.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        tg.setColor(fgColor.getColor());

        Shape shape = type.createBlankArea(tileSize, widthParam.getValue(), isBlank);
        if (shape != null) {
            tg.fill(shape);
        }
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
                double firsts = Math.floor(tileSize / 2.0);
                double thirds = Math.ceil(3 * tileSize / 2.0);
                triangle.moveTo(thirds, firsts);
                triangle.lineTo(thirds, thirds);
                triangle.lineTo(firsts, thirds);
                triangle.closePath();
                return triangle;
            }

            @Override
            public Shape createBlankArea(int tileSize, int lineWidth, boolean isBlank) {
                if (isBlank) {
                    return null;
                }
                return new Rectangle2D.Double(Math.floor(tileSize / 2.0), Math.floor(tileSize / 2.0),
                    tileSize, tileSize);
            }

        }, QUARTER_CIRCLES("Quarter Circles") {
            @Override
            public Shape createFilledArea(int tileSize, int lineWidth) {
                Path2D path = new Path2D.Double();
                BasicStroke stroke = new BasicStroke(lineWidth);

                Rectangle2D bounds1 = new Rectangle2D.Double(
                    tileSize, 0, tileSize, tileSize);
                Arc2D arc1 = new Arc2D.Double(bounds1, 180, 90, Arc2D.OPEN);
                path.append(stroke.createStrokedShape(arc1), false);

                Rectangle2D bounds2 = new Rectangle2D.Double(
                    0, tileSize, tileSize, tileSize);
                Arc2D arc2 = new Arc2D.Double(bounds2, 0, 90, Arc2D.OPEN);
                path.append(stroke.createStrokedShape(arc2), false);

                return path;
            }

            @Override
            public Shape createBlankArea(int tileSize, int lineWidth, boolean isBlank) {
                Shape shape;
                if (isBlank) {
                    Path2D lines = new Path2D.Double();
                    lines.moveTo(tileSize, tileSize / 2.0);
                    lines.lineTo(tileSize, 3 * tileSize / 2.0);
                    lines.moveTo(tileSize / 2.0, tileSize);
                    lines.lineTo(3 * tileSize / 2.0, tileSize);
                    shape = lines;
                } else {
                    Path2D path = new Path2D.Double();

                    Rectangle2D bounds1 = new Rectangle2D.Double(
                        tileSize, 0, tileSize, tileSize);
                    Arc2D arc1 = new Arc2D.Double(bounds1, 180, 90, Arc2D.OPEN);
                    path.append((arc1), false);

                    Rectangle2D bounds2 = new Rectangle2D.Double(
                        0, tileSize, tileSize, tileSize);
                    Arc2D arc2 = new Arc2D.Double(bounds2, 0, 90, Arc2D.OPEN);
                    path.append((arc2), false);

                    Rectangle2D bounds3 = new Rectangle2D.Double(
                        0, 0, tileSize, tileSize);
                    Arc2D arc3 = new Arc2D.Double(bounds3, 270, 90, Arc2D.OPEN);
                    path.append((arc3), false);

                    Rectangle2D bounds4 = new Rectangle2D.Double(
                        tileSize, tileSize, tileSize, tileSize);
                    Arc2D arc4 = new Arc2D.Double(bounds4, 90, 90, Arc2D.OPEN);
                    path.append((arc4), false);

                    shape = path;
                }
                BasicStroke stroke = new BasicStroke(lineWidth);
                return stroke.createStrokedShape(shape);
            }
        }, DIAGONALS("Diagonals") {
            @Override
            public Shape createFilledArea(int tileSize, int lineWidth) {
                Line2D line = new Line2D.Double(tileSize / 2.0, tileSize / 2.0, 3 * tileSize / 2.0, 3 * tileSize / 2.0);
                BasicStroke stroke = new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
                return stroke.createStrokedShape(line);
            }

            @Override
            public Shape createBlankArea(int tileSize, int lineWidth, boolean isBlank) {
                double firsts = Math.floor(tileSize / 2.0);
                double thirds = Math.ceil(3 * tileSize / 2.0);
                double offset = isBlank ? firsts : thirds;
                double onset = isBlank ? thirds : firsts;
                Path2D lines = new Path2D.Double();
                lines.moveTo(firsts, firsts);
                lines.lineTo(offset, onset);
                lines.moveTo(onset, offset);
                lines.lineTo(thirds, thirds);
                BasicStroke stroke = new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
                return stroke.createStrokedShape(lines);
            }
        };

        private final String displayName;

        TileType(String displayName) {
            this.displayName = displayName;
        }

        public abstract Shape createFilledArea(int tileSize, int lineWidth);

        public abstract Shape createBlankArea(int tileSize, int lineWidth, boolean isBlank);

        @Override
        public String toString() {
            return displayName;
        }
    }
}