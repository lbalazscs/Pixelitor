/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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
import java.util.function.Supplier;

/**
 * The shape types in the shapes tool.
 */
public enum ShapeType {
    RECTANGLE("Rectangle", false, true, OverlayType.WIDTH_HEIGHT,
        Rectangle2D.Double::new, RectangleSettings::new) {
        @Override
        public Shape createShape(Drag drag, ShapeTypeSettings settings) {
            var rs = (RectangleSettings) settings;
            double radius = rs == null ? 0 : rs.getRadius();
            Rectangle2D r = drag.toPosImRect();
            return (radius == 0) ? r : new RoundRectangle2D.Double(
                r.getX(), r.getY(), r.getWidth(), r.getHeight(), radius, radius);
        }
    }, ELLIPSE("Ellipse", false, false, OverlayType.WIDTH_HEIGHT, Ellipse2D.Double::new) {
        @Override
        protected Rectangle2D getShapeBounds(Drag drag) {
            return drag.toPosImRect();
        }
    }, DIAMOND("Diamond", false, false, OverlayType.WIDTH_HEIGHT, CustomShapes::createDiamond),
    LINE("Line", true, true, OverlayType.ANGLE_DIST, null, LineSettings::new) {
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
    }, HEART("Heart", false, false, CustomShapes::createHeart),
    STAR("Star", false, false, OverlayType.WIDTH_HEIGHT,
        (x, y, width, height) -> CustomShapes.createStar(
            StarSettings.DEFAULT_NUM_BRANCHES, x, y, width, height, StarSettings.DEFAULT_RADIUS_RATIO),
        StarSettings::new) {
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

            Rectangle2D r = drag.toPosImRect();
            return CustomShapes.createStar(numBranches, r.getX(), r.getY(),
                r.getWidth(), r.getHeight(), radiusRatio);
        }
    }, RANDOM_STAR("Random Star", false, false, OverlayType.WIDTH_HEIGHT) {
        private Drag lastDrag;

        @Override
        public Shape createShape(Drag drag, ShapeTypeSettings settings) {
            if (drag != lastDrag) {
                RandomStarShape.randomizeStarParameters();
            } else {
                // do not generate a completely new shape, only scale it
            }
            lastDrag = drag;

            Rectangle2D r = drag.toSignedImRect();
            return new RandomStarShape(r.getX(), r.getY(), r.getWidth(), r.getHeight());
        }

        @Override
        public Shape createShape(double x, double y, double width, double height) {
            RandomStarShape.randomizeStarParameters();
            return new RandomStarShape(x, y, width, height);
        }
    }, ARROW("Arrow", true, false, OverlayType.ANGLE_DIST) {
        Path2D unitArrow = null;

        @Override
        public Shape createShape(Drag drag, ShapeTypeSettings settings) {
            if (unitArrow == null) {
                unitArrow = CustomShapes.createUnitArrow();
            }

            Rectangle2D r = drag.toSignedImRect();

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
    }, CAT("Cat", false, true, CustomShapes::createCat),
    KIWI("Kiwi", false, false, CustomShapes::createKiwi),
    BAT("Bat", false, true, CustomShapes::createBat),
    RABBIT("Rabbit", false, false, CustomShapes::createRabbit);

    // the key can't be simply "Shape", because
    // that key is used by the stroke settings
    public static final String PRESET_KEY = "ShapeType";

    private static final String NAME = "Shape";
    private final String displayName;

    private final boolean hasAreaBug;

    // for the directional shapes the transform box
    // is initialized at the angle of the shape
    private final boolean directional;

    private final OverlayType overlayType;

    // factory for simple shapes that can be created from a rectangle
    private final ShapeFactory shapeFactory;

    private final Supplier<? extends ShapeTypeSettings> settingsSupplier;

    @FunctionalInterface
    private interface ShapeFactory {
        Shape create(double x, double y, double width, double height);
    }

    // Constructor for shapes without a factory or settings (complex shapes)
    ShapeType(String displayName, boolean directional, boolean hasAreaBug,
              OverlayType overlayType) {
        this(displayName, directional, hasAreaBug, overlayType, null, null);
    }

    // Constructor for simple shapes with a factory (they all have OverlayType.NONE)
    ShapeType(String displayName, boolean directional, boolean hasAreaBug,
              ShapeFactory factory) {
        this(displayName, directional, hasAreaBug, OverlayType.NONE, factory, null);
    }

    ShapeType(String displayName, boolean directional, boolean hasAreaBug,
              OverlayType overlayType, ShapeFactory factory) {
        this(displayName, directional, hasAreaBug, overlayType, factory, null);
    }

    ShapeType(String displayName, boolean directional, boolean hasAreaBug,
              OverlayType overlayType, ShapeFactory factory,
              Supplier<? extends ShapeTypeSettings> settingsSupplier) {
        this.displayName = displayName;
        this.directional = directional;
        this.hasAreaBug = hasAreaBug;
        this.overlayType = overlayType;
        this.shapeFactory = factory;
        this.settingsSupplier = settingsSupplier;
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
     * Most simple shapes use a signed rectangle for symmetry during drag.
     */
    protected Rectangle2D getShapeBounds(Drag drag) {
        return drag.toSignedImRect();
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
        return overlayType;
    }

    public boolean isDirectional() {
        return directional;
    }

    public boolean hasSettings() {
        return settingsSupplier != null;
    }

    /**
     * Return true if the shape could trigger https://bugs.openjdk.java.net/browse/JDK-6357341
     */
    public boolean hasAreaBug() {
        return hasAreaBug;
    }

    public ShapeTypeSettings createSettings() {
        if (settingsSupplier == null) {
            throw new UnsupportedOperationException("no settings for " + this);
        }
        return settingsSupplier.get();
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
