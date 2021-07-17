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
import static pixelitor.utils.QuadrantAngle.*;

/**
 * Render Truchet tiles filter
 */
public class Truchet extends ParametrizedFilter {
    public static final String NAME = "Truchet Tiles";

    private static final int PATTERN_RANDOM = 0;
    private static final int PATTERN_A = 1;
    private static final int PATTERN_B = 2;
    private static final int PATTERN_C = 3;
    private static final int PATTERN_D = 4;

    private final EnumParam<TileType> typeParam = new EnumParam<>("Type", TileType.class);
    private final IntChoiceParam patternParam = new IntChoiceParam("Pattern", new Item[]{
        new Item("Random", PATTERN_RANDOM),
        new Item("A", PATTERN_A),
        new Item("B", PATTERN_B),
        new Item("C", PATTERN_C),
        new Item("D", PATTERN_D),
    });
    private final RangeParam sizeParam = new RangeParam("Tile Size", 2, 20, 100);
    private final RangeParam widthParam = new RangeParam("Line Width", 1, 3, 20);
    private final ColorParam bgColorParam = new ColorParam("Background Color", WHITE, USER_ONLY_TRANSPARENCY);
    private final ColorParam fgColorParam = new ColorParam("Foreground Color", BLACK, USER_ONLY_TRANSPARENCY);

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
            bgColorParam,
            fgColorParam
        );
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        Random rand = ReseedSupport.reInitialize();
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
                    case PATTERN_A -> (i % 2 == 0) ? ((j % 2 == 0) ? 0 : 1) : ((j % 2 == 0) ? 2 : 3);
                    case PATTERN_B -> (i % 2 == 0) ? ((j % 2 == 0) ? 0 : 2) : ((j % 2 == 0) ? 1 : 3);
                    case PATTERN_C -> (i < numTilesHor / 2) ? ((j < numTilesVer / 2) ? 0 : 1) : ((j < numTilesVer / 2) ? 3 : 2);
                    case PATTERN_D -> (i < numTilesHor / 2) ? ((j < numTilesVer / 2) ? 1 : 0) : ((j < numTilesVer / 2) ? 2 : 3);
                    default -> throw new IllegalStateException("Unexpected value: " + pattern);
                };
                g.drawImage(tiles[tileIndex], i * size, j * size, null);
            }
        }

        g.dispose();
        return dest;
    }

    private BufferedImage createTile(QuadrantAngle angle) {
        int tileSize = sizeParam.getValue();
        TileType type = typeParam.getSelected();

        BufferedImage tile = new BufferedImage(tileSize, tileSize, TYPE_INT_ARGB);
        Graphics2D tg = tile.createGraphics();
        Colors.fillWith(bgColorParam.getColor(), tg, tileSize, tileSize);

        if (angle != null) {
            tg.transform(angle.createTransform(tileSize, tileSize));
        }

        tg.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        tg.setColor(fgColorParam.getColor());

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