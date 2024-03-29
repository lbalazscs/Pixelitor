/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.tools.shapes;

import pixelitor.filters.gui.EnumParam;
import pixelitor.tools.shapes.custom.RandomStarShape;
import pixelitor.tools.util.Drag;
import pixelitor.tools.util.DragDisplayType;
import pixelitor.utils.Geometry;
import pixelitor.utils.Shapes;

import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.*;

/**
 * The shapes types in the shapes tool
 */
public enum ShapeType {
    RECTANGLE("Rectangle", false, true, true) {
        @Override
        public Shape createShape(Drag drag, ShapeTypeSettings settings) {
            var rs = (RectangleSettings) settings;
            double radius = rs == null ? 0 : rs.getRadius();
            Rectangle2D r = drag.createPositiveImRect();
            if (radius == 0) {
                return r;
            } else {
                return new RoundRectangle2D.Double(r.getX(), r.getY(),
                    r.getWidth(), r.getHeight(), radius, radius);
            }
        }

        @Override
        public Shape createShape(double x, double y, double width, double height) {
            return new Rectangle2D.Double(x, y, width, height);
        }

        @Override
        public DragDisplayType getDragDisplayType() {
            return DragDisplayType.WIDTH_HEIGHT;
        }

        @Override
        public RectangleSettings createSettings() {
            return new RectangleSettings();
        }
    }, ELLIPSE("Ellipse", false, false, false) {
        @Override
        public Shape createShape(Drag drag, ShapeTypeSettings settings) {
            Rectangle2D r = drag.createPositiveImRect();
            return new Ellipse2D.Double(r.getX(), r.getY(), r.getWidth(), r.getHeight());
        }

        @Override
        public Shape createShape(double x, double y, double width, double height) {
            return new Ellipse2D.Double(x, y, width, height);
        }

        @Override
        public DragDisplayType getDragDisplayType() {
            return DragDisplayType.WIDTH_HEIGHT;
        }
    }, DIAMOND("Diamond", false, false, false) {
        @Override
        public Shape createShape(Drag drag, ShapeTypeSettings settings) {
            Rectangle2D r = drag.createPossiblyEmptyImRect();
            return Shapes.createDiamond(r.getX(), r.getY(), r.getWidth(), r.getHeight());
        }

        @Override
        public Shape createShape(double x, double y, double width, double height) {
            return Shapes.createDiamond(x, y, width, height);
        }

        @Override
        public DragDisplayType getDragDisplayType() {
            return DragDisplayType.WIDTH_HEIGHT;
        }
    }, LINE("Line", true, true, true) {
        @Override
        public Shape createShape(Drag drag, ShapeTypeSettings settings) {
            var lineSettings = (LineSettings) settings;
            Stroke stroke;
            if (lineSettings != null) {
                stroke = lineSettings.getStroke();
            } else {
                stroke = new BasicStroke(5);
            }
            return stroke.createStrokedShape(drag.asLine());
        }

        @Override
        public Shape createShape(double x, double y, double width, double height) {
            return new Rectangle2D.Double(x, y, width / 5.0, height);
        }

        @Override
        public DragDisplayType getDragDisplayType() {
            return DragDisplayType.ANGLE_DIST;
        }

        @Override
        public LineSettings createSettings() {
            return new LineSettings();
        }
    }, HEART("Heart", false, false, false) {
        @Override
        public Shape createShape(Drag drag, ShapeTypeSettings settings) {
            Rectangle2D r = drag.createPossiblyEmptyImRect();
            return Shapes.createHeart(r.getX(), r.getY(), r.getWidth(), r.getHeight());
        }

        @Override
        public Shape createShape(double x, double y, double width, double height) {
            return Shapes.createHeart(x, y, width, height);
        }
    }, STAR("Star", false, true, false) {
        @Override
        public Shape createShape(Drag drag, ShapeTypeSettings settings) {
            StarSettings starSettings = (StarSettings) settings;
            int numBranches;
            double radiusRatio;
            if (starSettings != null) {
                numBranches = starSettings.getNumBranches();
                radiusRatio = starSettings.getRadiusRatio();
            } else {
                numBranches = StarSettings.DEFAULT_NUM_BRANCHES;
                radiusRatio = StarSettings.DEFAULT_RADIUS_RATIO;
            }

            Rectangle2D r = drag.createPositiveImRect();
            return Shapes.createStar(numBranches, r.getX(), r.getY(),
                r.getWidth(), r.getHeight(), radiusRatio);
        }

        @Override
        public Shape createShape(double x, double y, double width, double height) {
            return Shapes.createStar(StarSettings.DEFAULT_NUM_BRANCHES,
                x, y, width, height, StarSettings.DEFAULT_RADIUS_RATIO);
        }

        @Override
        public DragDisplayType getDragDisplayType() {
            return DragDisplayType.WIDTH_HEIGHT;
        }

        @Override
        public StarSettings createSettings() {
            return new StarSettings();
        }
    }, RANDOM_STAR("Random Star", false, false, false) {
        private Drag lastDrag;

        @Override
        public Shape createShape(Drag drag, ShapeTypeSettings settings) {
            if (drag != lastDrag) {
                RandomStarShape.randomize();
            } else {
                // do not generate a completely new shape, only scale it
            }
            lastDrag = drag;

            Rectangle2D r = drag.createPossiblyEmptyImRect();
            return new RandomStarShape(r.getX(), r.getY(), r.getWidth(), r.getHeight());
        }

        @Override
        public Shape createShape(double x, double y, double width, double height) {
            RandomStarShape.randomize();
            return new RandomStarShape(x, y, width, height);
        }

        @Override
        public DragDisplayType getDragDisplayType() {
            return DragDisplayType.WIDTH_HEIGHT;
        }
    }, ARROW("Arrow", true, false, false) {
        GeneralPath unitArrow = null;

        @Override
        public Shape createShape(Drag drag, ShapeTypeSettings settings) {
            if (unitArrow == null) {
                unitArrow = Shapes.createUnitArrow();
            }

            Rectangle2D r = drag.createPossiblyEmptyImRect();

            double distance = drag.calcImDist();
            var transform = AffineTransform.getTranslateInstance(r.getX(), r.getY());
            transform.scale(distance, distance); // originally it had a length of 1.0

            // rotate the arrow into the direction of the drag
            double angleInRadians = drag.getDrawAngle();
            double angle = Geometry.atan2ToIntuitive(angleInRadians);
            angle += Math.PI / 2;
            transform.rotate(angle);

            return transform.createTransformedShape(unitArrow);
        }

        @Override
        public Shape createShape(double x, double y, double width, double height) {
            double middleY = y + height / 2.0;
            Drag drag = new Drag(
                x,
                middleY,
                x + width,
                middleY);
            return createShape(drag, null);
        }

        @Override
        public DragDisplayType getDragDisplayType() {
            return DragDisplayType.ANGLE_DIST;
        }
    }, CAT("Cat", false, false, true) {
        @Override
        public Shape createShape(Drag drag, ShapeTypeSettings settings) {
            Rectangle2D r = drag.createPossiblyEmptyImRect();
            return Shapes.createCat(r.getX(), r.getY(), r.getWidth(), r.getHeight());
        }

        @Override
        public Shape createShape(double x, double y, double width, double height) {
            return Shapes.createCat(x, y, width, height);
        }
    }, KIWI("Kiwi", false, false, false) {
        @Override
        public Shape createShape(Drag drag, ShapeTypeSettings settings) {
            Rectangle2D r = drag.createPossiblyEmptyImRect();
            return Shapes.createKiwi(r.getX(), r.getY(), r.getWidth(), r.getHeight());
        }

        @Override
        public Shape createShape(double x, double y, double width, double height) {
            return Shapes.createKiwi(x, y, width, height);
        }
    }, BAT("Bat", false, false, true) {
        @Override
        public Shape createShape(Drag drag, ShapeTypeSettings settings) {
            Rectangle2D r = drag.createPossiblyEmptyImRect();
            return Shapes.createBat(r.getX(), r.getY(), r.getWidth(), r.getHeight());
        }

        @Override
        public Shape createShape(double x, double y, double width, double height) {
            return Shapes.createBat(x, y, width, height);
        }
    }, RABBIT("Rabbit", false, false, false) {
        @Override
        public Shape createShape(Drag drag, ShapeTypeSettings settings) {
            Rectangle2D r = drag.createPossiblyEmptyImRect();
            return Shapes.createRabbit(r.getX(), r.getY(), r.getWidth(), r.getHeight());
        }

        @Override
        public Shape createShape(double x, double y, double width, double height) {
            return Shapes.createRabbit(x, y, width, height);
        }
    };

    // the key can't be simpy "Shape", because
    // that key is used by the stroke settings
    public static final String PRESET_KEY = "ShapeType";

    private static final String NAME = "Shape";
    private final String guiName;

    private final boolean hasSettings;
    private final boolean areaProblem;

    // directional shapes are the arrow and line, where the
    // transform box is initialized at the angle of the shape
    private final boolean directional;

    ShapeType(String guiName, boolean directional, boolean hasSettings, boolean areaProblem) {
        this.guiName = guiName;
        this.directional = directional;
        this.hasSettings = hasSettings;
        this.areaProblem = areaProblem;
    }

    /**
     * The returned shapes must always be closed, so that they can be filled.
     */
    public abstract Shape createShape(Drag drag, ShapeTypeSettings settings);

    public final Shape createShape(double x, double y, double size) {
        return createShape(x, y, size, size);
    }

    public abstract Shape createShape(double x, double y, double width, double height);

    public DragDisplayType getDragDisplayType() {
        // overridden if necessary
        return DragDisplayType.NONE;
    }

    public boolean isDirectional() {
        return directional;
    }

    public boolean hasSettings() {
        return hasSettings;
    }

    /**
     * Return true if the shape could trigger https://bugs.openjdk.java.net/browse/JDK-6357341
     */
    public boolean hasAreaProblem() {
        return areaProblem;
    }

    public ShapeTypeSettings createSettings() {
        assert hasSettings() : "no settings for " + this;
        // should be overridden, if it has settings
        throw new UnsupportedOperationException("no settings for " + this);
    }

    public static EnumParam<ShapeType> asParam(ShapeType defaultType) {
        return asParam().withDefault(defaultType);
    }

    public static EnumParam<ShapeType> asParam() {
        return new EnumParam<>(NAME, ShapeType.class);
    }

    @Override
    public String toString() {
        return guiName;
    }
}
