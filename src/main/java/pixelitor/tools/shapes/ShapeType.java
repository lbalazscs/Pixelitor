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

package pixelitor.tools.shapes;

import pixelitor.filters.gui.EnumParam;
import pixelitor.tools.shapes.custom.RandomStarShape;
import pixelitor.tools.util.Drag;
import pixelitor.tools.util.OverlayType;
import pixelitor.utils.CustomShapes;
import pixelitor.utils.Geometry;

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
            return (radius == 0) ? r : new RoundRectangle2D.Double(
                r.getX(), r.getY(), r.getWidth(), r.getHeight(), radius, radius);
        }

        @Override
        public Shape createShape(double x, double y, double width, double height) {
            return new Rectangle2D.Double(x, y, width, height);
        }

        @Override
        public OverlayType getOverlayType() {
            return OverlayType.WIDTH_HEIGHT;
        }

        @Override
        public RectangleSettings createSettings() {
            return new RectangleSettings();
        }
    }, ELLIPSE("Ellipse", false, false, false, Ellipse2D.Double::new) {
        @Override
        protected Rectangle2D getShapeBounds(Drag drag) {
            return drag.createPositiveImRect();
        }

        @Override
        public OverlayType getOverlayType() {
            return OverlayType.WIDTH_HEIGHT;
        }
    }, DIAMOND("Diamond", false, false, false, CustomShapes::createDiamond) {
        @Override
        public OverlayType getOverlayType() {
            return OverlayType.WIDTH_HEIGHT;
        }
    }, LINE("Line", true, true, true) {
        @Override
        public Shape createShape(Drag drag, ShapeTypeSettings settings) {
            var lineSettings = (LineSettings) settings;
            Stroke stroke = (lineSettings != null)
                ? lineSettings.getStroke()
                : new BasicStroke(5);
            return stroke.createStrokedShape(drag.asLine());
        }

        @Override
        public Shape createShape(double x, double y, double width, double height) {
            return new Rectangle2D.Double(x, y, width / 5.0, height);
        }

        @Override
        public OverlayType getOverlayType() {
            return OverlayType.ANGLE_DIST;
        }

        @Override
        public LineSettings createSettings() {
            return new LineSettings();
        }
    }, HEART("Heart", false, false, false, CustomShapes::createHeart),
    STAR("Star", false, true, false) {
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
            return CustomShapes.createStar(numBranches, r.getX(), r.getY(),
                r.getWidth(), r.getHeight(), radiusRatio);
        }

        @Override
        public Shape createShape(double x, double y, double width, double height) {
            return CustomShapes.createStar(StarSettings.DEFAULT_NUM_BRANCHES,
                x, y, width, height, StarSettings.DEFAULT_RADIUS_RATIO);
        }

        @Override
        public OverlayType getOverlayType() {
            return OverlayType.WIDTH_HEIGHT;
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
                RandomStarShape.randomizeStarParameters();
            } else {
                // do not generate a completely new shape, only scale it
            }
            lastDrag = drag;

            Rectangle2D r = drag.createSignedImRect();
            return new RandomStarShape(r.getX(), r.getY(), r.getWidth(), r.getHeight());
        }

        @Override
        public Shape createShape(double x, double y, double width, double height) {
            RandomStarShape.randomizeStarParameters();
            return new RandomStarShape(x, y, width, height);
        }

        @Override
        public OverlayType getOverlayType() {
            return OverlayType.WIDTH_HEIGHT;
        }
    }, ARROW("Arrow", true, false, false) {
        Path2D unitArrow = null;

        @Override
        public Shape createShape(Drag drag, ShapeTypeSettings settings) {
            if (unitArrow == null) {
                unitArrow = CustomShapes.createUnitArrow();
            }

            Rectangle2D r = drag.createSignedImRect();

            double length = drag.calcImLength();
            var transform = AffineTransform.getTranslateInstance(r.getX(), r.getY());
            transform.scale(length, length); // originally it had a length of 1.0

            // rotate the arrow into the direction of the drag
            double angle = Geometry.atan2ToIntuitive(drag.calcDrawAngle());
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
        public OverlayType getOverlayType() {
            return OverlayType.ANGLE_DIST;
        }
    }, CAT("Cat", false, false, true, CustomShapes::createCat),
    KIWI("Kiwi", false, false, false, CustomShapes::createKiwi),
    BAT("Bat", false, false, true, CustomShapes::createBat),
    RABBIT("Rabbit", false, false, false, CustomShapes::createRabbit);

    // the key can't be simpy "Shape", because
    // that key is used by the stroke settings
    public static final String PRESET_KEY = "ShapeType";

    private static final String NAME = "Shape";
    private final String displayName;

    private final boolean hasSettings;
    private final boolean hasAreaBug;

    // for the directional shapes the transform box
    // is initialized at the angle of the shape
    private final boolean directional;

    // factory for simple shapes that can be created from a rectangle
    private final ShapeFactory shapeFactory;

    /**
     * Functional interface for shape creation from a rectangle's components
     */
    @FunctionalInterface
    private interface ShapeFactory {
        Shape create(double x, double y, double width, double height);
    }

    // Constructor for shapes without a factory (complex shapes)
    ShapeType(String displayName, boolean directional, boolean hasSettings, boolean hasAreaBug) {
        this(displayName, directional, hasSettings, hasAreaBug, null);
    }

    // Constructor for simple shapes with a factory
    ShapeType(String displayName, boolean directional, boolean hasSettings, boolean hasAreaBug,
              ShapeFactory factory) {
        this.displayName = displayName;
        this.directional = directional;
        this.hasSettings = hasSettings;
        this.hasAreaBug = hasAreaBug;
        this.shapeFactory = factory;
    }

    /**
     * The returned shapes must always be closed, so that they can be filled.
     */
    public Shape createShape(Drag drag, ShapeTypeSettings settings) {
        if (shapeFactory == null) {
            throw new UnsupportedOperationException("Shape " + this + " must override createShape(Drag, ShapeTypeSettings)");
        }
        Rectangle2D r = getShapeBounds(drag);
        return shapeFactory.create(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    /**
     * Override this to use a different rectangle type (positive vs signed).
     * Most simple shapes use signed rect for symmetry during drag.
     */
    protected Rectangle2D getShapeBounds(Drag drag) {
        return drag.createSignedImRect();
    }

    public final Shape createShape(double x, double y, double size) {
        return createShape(x, y, size, size);
    }

    public Shape createShape(double x, double y, double width, double height) {
        if (shapeFactory == null) {
            throw new UnsupportedOperationException("Shape " + this + " must override createShape(double, double, double, double)");
        }
        return shapeFactory.create(x, y, width, height);
    }

    public OverlayType getOverlayType() {
        // overridden if necessary
        return OverlayType.NONE;
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
        return hasAreaBug;
    }

    public ShapeTypeSettings createSettings() {
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
        return displayName;
    }
}
