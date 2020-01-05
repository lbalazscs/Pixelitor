/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

import org.jdesktop.swingx.geom.Star2D;
import pixelitor.filters.gui.EnumParam;
import pixelitor.tools.shapes.custom.RandomStarShape;
import pixelitor.tools.util.DragDisplayType;
import pixelitor.tools.util.ImDrag;
import pixelitor.utils.Shapes;
import pixelitor.utils.Utils;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * The shapes types in the shapes tool
 */
public enum ShapeType {
    RECTANGLE("Rectangle", true, false) {
        @Override
        public Shape createShape(ImDrag imDrag) {
            setPositiveCoordinates(imDrag);
            return new Rectangle2D.Double(x, y, width, height);
        }

        @Override
        public Shape createShape(double x, double y, double diameter) {
            return new Rectangle2D.Double(x, y, diameter, diameter);
        }

        @Override
        public DragDisplayType getDragDisplayType() {
            return DragDisplayType.WIDTH_HEIGHT;
        }
    }, ELLIPSE("Ellipse", true, false) {
        @Override
        public Shape createShape(ImDrag imDrag) {
            setPositiveCoordinates(imDrag);
            return new Ellipse2D.Double(x, y, width, height);
        }

        @Override
        public Shape createShape(double x, double y, double diameter) {
            return new Ellipse2D.Double(x, y, diameter, diameter);
        }

        @Override
        public DragDisplayType getDragDisplayType() {
            return DragDisplayType.WIDTH_HEIGHT;
        }
    }, DIAMOND("Diamond", true, false) {
        @Override
        public Shape createShape(ImDrag imDrag) {
            setCoordinates(imDrag);
            return createDiamond(x, y, width, height);
        }

        private Shape createDiamond(double x, double y, double width, double height) {
            Path2D path = new Path2D.Double();

            double cx = x + width / 2.0;
            double cy = y + height / 2.0;

            path.moveTo(cx, y);
            path.lineTo(x + width, cy);
            path.lineTo(cx, y + height);
            path.lineTo(x, cy);
            path.closePath();

            return path;
        }

        @Override
        public Shape createShape(double x, double y, double diameter) {
            return createDiamond(x, y, diameter, diameter);
        }

        @Override
        public DragDisplayType getDragDisplayType() {
            return DragDisplayType.WIDTH_HEIGHT;
        }
    }, LINE("Line", false, true) {
        @Override
        public Shape createShape(ImDrag imDrag) {
//            updateCoordinatesPositive(start, end);
            return imDrag.asLine();
        }

        @Override
        public Shape createShape(double x, double y, double diameter) {
            return new Rectangle2D.Double(x, y, diameter / 5.0, diameter);
        }

        @Override
        public Shape createHorizontalShape(ImDrag imDrag) {
            return new Line2D.Double(imDrag.getStartX(), imDrag.getStartY(),
                    imDrag.getStartX() + imDrag.getDistance(), imDrag.getStartY());
        }

        @Override
        public DragDisplayType getDragDisplayType() {
            return DragDisplayType.ANGLE_DIST;
        }
    }, HEART("Heart", true, false) {
        @Override
        public Shape createShape(ImDrag imDrag) {
            setCoordinates(imDrag);
            return Shapes.createHeartShape(x, y, width, height);
        }

        @Override
        public Shape createShape(double x, double y, double diameter) {
            return Shapes.createHeartShape(x, y, diameter, diameter);
        }
    }, STAR("Star", true, false) {
        @Override
        public Shape createShape(ImDrag imDrag) {
            setCoordinates(imDrag);
            return createStar(x, y, width, height);
        }

        protected Shape createStar(double x, double y, double width, double height) {
            double halfWidth = width / 2;
            double halfHeight = height / 2;
            double cx = x + halfWidth;
            double cy = y + halfHeight;
            double innerRadius;
            double outerRadius;
            if (width > height) {
                innerRadius = halfHeight;
                outerRadius = halfWidth;
            } else if (height > width) {
                innerRadius = halfWidth;
                outerRadius = halfHeight;
            } else {
                // the Star2D constructor insists that the outer radius
                // must be greater than the inner radius
                innerRadius = halfWidth;
                outerRadius = innerRadius + 0.01;
            }

            return new Star2D(cx, cy, innerRadius, outerRadius, 7);
        }

        @Override
        public Shape createShape(double x, double y, double diameter) {
            return createStar(x, y, diameter, diameter / 3.0 + 1);
        }

        @Override
        public DragDisplayType getDragDisplayType() {
            return DragDisplayType.WIDTH_HEIGHT;
        }
    }, RANDOM_STAR("Random Star", true, false) {
        private ImDrag lastUserDrag;

        @Override
        public Shape createShape(ImDrag imDrag) {
            if (imDrag != lastUserDrag) {
                RandomStarShape.randomize();
            } else {
                // do not generate a completely new shape, only scale it
            }
            lastUserDrag = imDrag;

            setCoordinates(imDrag);
            return new RandomStarShape(x, y, width, height);
        }

        @Override
        public Shape createShape(double x, double y, double diameter) {
            RandomStarShape.randomize();
            return new RandomStarShape(x, y, diameter, diameter);
        }

        @Override
        public DragDisplayType getDragDisplayType() {
            return DragDisplayType.WIDTH_HEIGHT;
        }
    }, ARROW("Arrow", true, true) {
        GeneralPath unitArrow = null;

        @Override
        public Shape createShape(ImDrag imDrag) {
            return createArrowShape(imDrag, true);
        }

        @Override
        public Shape createHorizontalShape(ImDrag imDrag) {
            return createArrowShape(imDrag, false);
        }

        private Shape createArrowShape(ImDrag imDrag, boolean rotate) {
            if (unitArrow == null) {
                unitArrow = Shapes.createUnitArrow();
            }

            setCoordinates(imDrag);

            double distance = imDrag.getDistance();
            if (imDrag.isStartFromCenter()) {
                distance *= 2;
            }

            var transform = AffineTransform.getTranslateInstance(x, y);
            transform.scale(distance, distance); // originally it had a length of 1.0
            if (rotate) {
                double angleInRadians = imDrag.getDrawAngle();
                double angle = Utils.atan2AngleToIntuitive(angleInRadians);
                angle += Math.PI / 2;
                transform.rotate(angle);
            }
            return transform.createTransformedShape(unitArrow);
        }

        @Override
        public Shape createShape(double x, double y, double diameter) {
            double middleY = y + diameter / 2.0;
            ImDrag imDrag = new ImDrag(
                    x,
                    middleY,
                    x + diameter,
                    middleY);
            return createShape(imDrag);
        }

        @Override
        public DragDisplayType getDragDisplayType() {
            return DragDisplayType.ANGLE_DIST;
        }
    }, CAT("Cat", true, false) {
        @Override
        public Shape createShape(ImDrag imDrag) {
            setCoordinates(imDrag);
            return Shapes.createCatShape(x, y, width, height);
        }

        @Override
        public Shape createShape(double x, double y, double diameter) {
            return Shapes.createCatShape(x, y, diameter, diameter);
        }
    }, KIWI("Kiwi", true, false) {
        @Override
        public Shape createShape(ImDrag imDrag) {
            setCoordinates(imDrag);
            return Shapes.createKiwiShape(x, y, width, height);
        }

        @Override
        public Shape createShape(double x, double y, double diameter) {
            return Shapes.createKiwiShape(x, y, diameter, diameter);
        }
    }, BAT("Bat", true, false) {
        @Override
        public Shape createShape(ImDrag imDrag) {
            setCoordinates(imDrag);
            return Shapes.createBatShape(x, y, width, height);
        }

        @Override
        public Shape createShape(double x, double y, double diameter) {
            return Shapes.createBatShape(x, y, diameter, diameter);
        }
    }, RABBIT("Rabbit", true, false) {
        @Override
        public Shape createShape(ImDrag imDrag) {
            setCoordinates(imDrag);
            return Shapes.createRabbitShape(x, y, width, height);
        }

        @Override
        public Shape createShape(double x, double y, double diameter) {
            return Shapes.createRabbitShape(x, y, diameter, diameter);
        }
//    }, RND_ANIMAL_FACE("Random Animal Face", true, false) {
//        final int[] codePoints = {
//                0x1F428, // koala
//                0x1F42D, // mouse
//                0x1F42E, // cow
//                0x1F42F, // tiger
//                0x1F430, // rabbit
//                0x1F431, // cat
//              //  0x1F432, // dragon
//                0x1F434, // horse
//                0x1F435, // monkey
//                0x1F436, // dog
//                0x1F437, // pig
//                0x1F438, // frog
//                0x1F439, // hamster
//                0x1F43A, // wolf
//                0x1F43B, // bear
//                0x1F43C, // panda
//
//                // from here not even on windows
//                0x1F981, // lion
//                0x1F984, // unicorn
//                0x1F985, // eagle
//                0x1F98A, // fox
//                0x1F98C, // deer
//                0x1F98D, // gorilla
//                0x1F98F, // rhinoceros
//                0x1F993, // zebra
//                0x1F994, // hedgehog
//                0x1F99D, // raccoon
//        };
//        // it seems that on Linux not all code points are supported
//        final int[] supportedCodePoints = IntStream.of(codePoints)
//                .filter(UnicodeShapes::isSupported)
//                .toArray();
//
//        @Override
//        public Shape createShape(ImDrag imDrag) {
//            int codePoint = supportedCodePoints[Rnd.nextInt(supportedCodePoints.length)];
//            System.out.printf("ShapeType::createShape: codePoint = 0x%x%n", codePoint);
//            return UnicodeShapes.extract(codePoint,
//                    imDrag.getStartX(), imDrag.getStartY(), imDrag.getDX(), imDrag.getDY());
//        }
//
//        @Override
//        public Shape createShape(double x, double y, double diameter) {
//            int codePoint = supportedCodePoints[Rnd.nextInt(supportedCodePoints.length)];
//            return UnicodeShapes.extract(codePoint,
//                    x, y, diameter, diameter);
//        }
//    }, RND_CHESS("Random Chess Piece", true, false) {
//        final String[] chars = {"\u2654", "\u2655", "\u2656", "\u2657", "\u2658", "\u2659",
//                "\u265A", "\u265B", "\u265C", "\u265D", "\u265E", "\u265F"};
//
//        @Override
//        public Shape createShape(ImDrag imDrag) {
//            String uChar = Rnd.chooseFrom(chars);
//            return UnicodeShapes.extract(uChar,
//                    imDrag.getStartX(), imDrag.getStartY(), imDrag.getDX(), imDrag.getDY());
//        }
//
//        @Override
//        public Shape createShape(double x, double y, double diameter) {
//            String uChar = Rnd.chooseFrom(chars);
//            return UnicodeShapes.extract(uChar,
//                    x, y, diameter, diameter);
//        }
//    }, RND_ZODIAC("Random Zodiac Sign", true, false) {
//        final String[] chars = {"\u2648", "\u2649", "\u264A", "\u264B", "\u264C", "\u264D",
//                "\u264E", "\u264F", "\u2650", "\u2651", "\u2652", "\u2653"};
//
//        @Override
//        public Shape createShape(ImDrag imDrag) {
//            String uChar = Rnd.chooseFrom(chars);
//            return UnicodeShapes.extract(uChar,
//                    imDrag.getStartX(), imDrag.getStartY(), imDrag.getDX(), imDrag.getDY());
//        }
//
//        @Override
//        public Shape createShape(double x, double y, double diameter) {
//            String uChar = Rnd.chooseFrom(chars);
//            return UnicodeShapes.extract(uChar,
//                    x, y, diameter, diameter);
//        }
    };

    private final String guiName;

    // if a shape is not closed, then it can't be filled directly
    private final boolean closed;

    // directional shapes are the arrow and line, where the
    // transform box is initialized at the angle of the shape
    private final boolean directional;

    protected double x, y, width, height;

    ShapeType(String guiName, boolean closed, boolean directional) {
        this.guiName = guiName;
        this.closed = closed;
        this.directional = directional;
    }

    /**
     * Set the x, y, width, height coordinates so that width and height are positive
     */
    protected void setPositiveCoordinates(ImDrag imDrag) {
        Rectangle2D r = imDrag.createPositiveRect();
        x = r.getX();
        y = r.getY();
        width = r.getWidth();
        height = r.getHeight();
    }

    /**
     * Set the x, y, width, height coordinates
     */
    protected void setCoordinates(ImDrag imDrag) {
        Rectangle2D r = imDrag.createPossiblyEmptyRect();

        x = r.getX();
        y = r.getY();
        width = r.getWidth();
        height = r.getHeight();
    }

    public abstract Shape createShape(ImDrag imDrag);

    /**
     * Return the directional shape that would result
     * from the given drag if it was horizontal
     */
    public Shape createHorizontalShape(ImDrag imDrag) {
        // overridden in directional types
        assert !directional;
        throw new UnsupportedOperationException("not directional");
    }

    public boolean isDirectional() {
        return directional;
    }

    public boolean isClosed() {
        return closed;
    }

    public abstract Shape createShape(double x, double y, double diameter);

    public static EnumParam<ShapeType> asParam(ShapeType defaultValue) {
        EnumParam<ShapeType> param = asParam("Shape");
        param.selectAndSetAsDefault(defaultValue);
        return param;
    }

    public static EnumParam<ShapeType> asParam(String name) {
        return new EnumParam<>(name, ShapeType.class);
    }

    @Override
    public String toString() {
        return guiName;
    }

    public DragDisplayType getDragDisplayType() {
        // overridden if necessary
        return DragDisplayType.NONE;
    }
}
