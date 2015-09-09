/*
 * Copyright 2015 Laszlo Balazs-Csiki
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

package pixelitor.tools;

import org.jdesktop.swingx.geom.Star2D;
import pixelitor.tools.shapes.Bat;
import pixelitor.tools.shapes.Cat;
import pixelitor.tools.shapes.Heart;
import pixelitor.tools.shapes.Kiwi;
import pixelitor.tools.shapes.Rabbit;
import pixelitor.tools.shapes.RandomStar;
import pixelitor.utils.Utils;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * The shapes types in the shapes tool
 */
public enum ShapeType {
    RECTANGLE("Rectangle", true) {
        @Override
        public Shape getShape(UserDrag userDrag) {
            updateCoordinatesPositive(userDrag);
            return new Rectangle2D.Double(x, y, width, height);
        }

        @Override
        public Shape getShape(double x, double y, int diameter) {
            return new Rectangle2D.Double(x, y, diameter, diameter);
        }
    }, ELLIPSE("Ellipse", true) {
        @Override
        public Shape getShape(UserDrag userDrag) {
            updateCoordinatesPositive(userDrag);
            return new Ellipse2D.Double(x, y, width, height);
        }

        @Override
        public Shape getShape(double x, double y, int diameter) {
            return new Ellipse2D.Double(x, y, diameter, diameter);
        }
    }, DIAMOND("Diamond", true) {
        @Override
        public Shape getShape(UserDrag userDrag) {
            updateCoordinates(userDrag);
            return createDiamond(x, y, width, height);
        }

        private Shape createDiamond(double x, double y, double width, double height) {
            Path2D.Double path = new Path2D.Double();

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
        public Shape getShape(double x, double y, int diameter) {
            return createDiamond(x, y, diameter, diameter);
        }
    }, LINE("Line", false) {
        @Override
        public Shape getShape(UserDrag userDrag) {
//            updateCoordinatesPositive(start, end);
            return userDrag.asLine();
        }

        @Override
        public Shape getShape(double x, double y, int diameter) {
            return new Rectangle2D.Double(x, y, diameter / 5.0, diameter);
        }
    }, HEART("Heart", true) {
        @Override
        public Shape getShape(UserDrag userDrag) {
            updateCoordinates(userDrag);
            return new Heart(x, y, width, height);
        }

        @Override
        public Shape getShape(double x, double y, int diameter) {
            return new Heart(x, y, diameter, diameter);
        }
    }, STAR("Star", true) {
        @Override
        public Shape getShape(UserDrag userDrag) {
            updateCoordinates(userDrag);
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
        public Shape getShape(double x, double y, int diameter) {
            return createStar(x, y, diameter, diameter / 3.0 + 1);
        }
    }, RANDOM_STAR("Random Star", true) {
        private UserDrag lastUserDrag;

        @Override
        public Shape getShape(UserDrag userDrag) {
            if(userDrag != lastUserDrag) {
                RandomStar.randomize();
            } else {
                // do not generate a completely new shape, only scale it
            }
            lastUserDrag = userDrag;

            updateCoordinates(userDrag);
            return new RandomStar(x, y, width, height);
        }

        @Override
        public Shape getShape(double x, double y, int diameter) {
            RandomStar.randomize();
            return new RandomStar(x, y, diameter, diameter);
        }
    }, ARROW("Arrow", true) {
        GeneralPath unitArrow = null;

        @Override
        public Shape getShape(UserDrag userDrag) {
            if (unitArrow == null) {
                unitArrow = Utils.createUnitArrow();
            }

            updateCoordinates(userDrag);

            double distance = userDrag.getDistance();
            if (userDrag.isStartFromCenter()) {
                distance *= 2;
            }

            AffineTransform transform = AffineTransform.getTranslateInstance(x, y);
            transform.scale(distance, distance); // originally it had a length of 1.0
            double angleInRadians = userDrag.getDrawAngle();
            double angle = Utils.transformAtan2AngleToIntuitive(angleInRadians);
            angle += Math.PI / 2;
            transform.rotate(angle);
            return transform.createTransformedShape(unitArrow);
        }

        @Override
        public Shape getShape(double x, double y, int diameter) {
            // TODO USerDrag should be able to accept double parameters as well?
            int middleY = (int) (y + diameter / 2.0);
            UserDrag userDrag = new UserDrag(
                    (int) x,
                    middleY,
                    (int) x + diameter,
                    middleY);
            return getShape(userDrag);
        }
    }, CAT("Cat", true) {
        @Override
        public Shape getShape(UserDrag userDrag) {
            updateCoordinates(userDrag);
            return new Cat(x, y, width, height);
        }

        @Override
        public Shape getShape(double x, double y, int diameter) {
            return new Cat(x, y, diameter, diameter);
        }
    }, KIWI("Kiwi", true) {
        @Override
        public Shape getShape(UserDrag userDrag) {
            updateCoordinates(userDrag);
            return new Kiwi(x, y, width, height);
        }

        @Override
        public Shape getShape(double x, double y, int diameter) {
            return new Kiwi(x, y, diameter, diameter);
        }
    }, BAT("Bat", true) {
        @Override
        public Shape getShape(UserDrag userDrag) {
            updateCoordinates(userDrag);
            return new Bat(x, y, width, height);
        }

        @Override
        public Shape getShape(double x, double y, int diameter) {
            return new Bat(x, y, diameter, diameter);
        }
    }, RABBIT("Rabbit", true) {
        @Override
        public Shape getShape(UserDrag userDrag) {
            updateCoordinates(userDrag);
            return new Rabbit(x, y, width, height);
        }

        @Override
        public Shape getShape(double x, double y, int diameter) {
            return new Rabbit(x, y, diameter, diameter);
        }
        //    }, SKULL("Skull", true) {
//        @Override
//        public Shape getShape(UserDrag userDrag) {
//            updateCoordinates(userDrag);
//            return new Skull(x, y, width, height);
//        }
    };

    /**
     * Update the x, y, width, height coordinates so that width and height are positive
     */
    protected void updateCoordinatesPositive(UserDrag userDrag) {
        Rectangle2D r = userDrag.createPositiveRect();
        x = r.getX();
        y = r.getY();
        width = r.getWidth();
        height = r.getHeight();
    }

    /**
     * Update the x, y, width, height coordinates
     */
    protected void updateCoordinates(UserDrag userDrag) {
        Rectangle2D r = userDrag.createPossiblyEmptyRect();

        x = r.getX();
        y = r.getY();
        width = r.getWidth();
        height = r.getHeight();
    }

    private final String guiName;
    private final boolean closed;

    ShapeType(String guiName, boolean closed) {
        this.guiName = guiName;
        this.closed = closed;
    }

    protected double x, y, width, height;

    public abstract Shape getShape(UserDrag userDrag);

    public boolean isClosed() {
        return closed;
    }

    @Override
    public String toString() {
        return guiName;
    }

    public abstract Shape getShape(double x, double y, int diameter);
}
