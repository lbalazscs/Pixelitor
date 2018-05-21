/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.tools.shapes.BatShape;
import pixelitor.tools.shapes.CatShape;
import pixelitor.tools.shapes.HeartShape;
import pixelitor.tools.shapes.KiwiShape;
import pixelitor.tools.shapes.RabbitShape;
import pixelitor.tools.shapes.RandomStarShape;
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
            setPositiveCoordinates(userDrag);
            return new Rectangle2D.Double(x, y, width, height);
        }

        @Override
        public Shape getShape(double x, double y, int diameter) {
            return new Rectangle2D.Double(x, y, diameter, diameter);
        }
    }, ELLIPSE("Ellipse", true) {
        @Override
        public Shape getShape(UserDrag userDrag) {
            setPositiveCoordinates(userDrag);
            return new Ellipse2D.Double(x, y, width, height);
        }

        @Override
        public Shape getShape(double x, double y, int diameter) {
            return new Ellipse2D.Double(x, y, diameter, diameter);
        }
    }, DIAMOND("Diamond", true) {
        @Override
        public Shape getShape(UserDrag userDrag) {
            setCoordinates(userDrag);
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
            setCoordinates(userDrag);
            return new HeartShape(x, y, width, height);
        }

        @Override
        public Shape getShape(double x, double y, int diameter) {
            return new HeartShape(x, y, diameter, diameter);
        }
    }, STAR("Star", true) {
        @Override
        public Shape getShape(UserDrag userDrag) {
            setCoordinates(userDrag);
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
                RandomStarShape.randomize();
            } else {
                // do not generate a completely new shape, only scale it
            }
            lastUserDrag = userDrag;

            setCoordinates(userDrag);
            return new RandomStarShape(x, y, width, height);
        }

        @Override
        public Shape getShape(double x, double y, int diameter) {
            RandomStarShape.randomize();
            return new RandomStarShape(x, y, diameter, diameter);
        }
    }, ARROW("Arrow", true) {
        GeneralPath unitArrow = null;

        @Override
        public Shape getShape(UserDrag userDrag) {
            if (unitArrow == null) {
                unitArrow = Utils.createUnitArrow();
            }

            setCoordinates(userDrag);

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
            setCoordinates(userDrag);
            return new CatShape(x, y, width, height);
        }

        @Override
        public Shape getShape(double x, double y, int diameter) {
            return new CatShape(x, y, diameter, diameter);
        }
    }, KIWI("Kiwi", true) {
        @Override
        public Shape getShape(UserDrag userDrag) {
            setCoordinates(userDrag);
            return new KiwiShape(x, y, width, height);
        }

        @Override
        public Shape getShape(double x, double y, int diameter) {
            return new KiwiShape(x, y, diameter, diameter);
        }
    }, BAT("Bat", true) {
        @Override
        public Shape getShape(UserDrag userDrag) {
            setCoordinates(userDrag);
            return new BatShape(x, y, width, height);
        }

        @Override
        public Shape getShape(double x, double y, int diameter) {
            return new BatShape(x, y, diameter, diameter);
        }
    }, RABBIT("Rabbit", true) {
        @Override
        public Shape getShape(UserDrag userDrag) {
            setCoordinates(userDrag);
            return new RabbitShape(x, y, width, height);
        }

        @Override
        public Shape getShape(double x, double y, int diameter) {
            return new RabbitShape(x, y, diameter, diameter);
        }
        //    }, SKULL("Skull", true) {
//        @Override
//        public Shape getShape(UserDrag userDrag) {
//            updateCoordinates(userDrag);
//            return new Skull(x, y, width, height);
//        }
    };

    private final String guiName;
    private final boolean closed;

    protected double x, y, width, height;

    ShapeType(String guiName, boolean closed) {
        this.guiName = guiName;
        this.closed = closed;
    }

    /**
     * Set the x, y, width, height coordinates so that width and height are positive
     */
    protected void setPositiveCoordinates(UserDrag userDrag) {
        Rectangle2D r = userDrag.createPositiveRect();
        x = r.getX();
        y = r.getY();
        width = r.getWidth();
        height = r.getHeight();
    }

    /**
     * Set the x, y, width, height coordinates
     */
    protected void setCoordinates(UserDrag userDrag) {
        Rectangle2D r = userDrag.createPossiblyEmptyRect();

        x = r.getX();
        y = r.getY();
        width = r.getWidth();
        height = r.getHeight();
    }

    public abstract Shape getShape(UserDrag userDrag);

    public boolean isClosed() {
        return closed;
    }

    public abstract Shape getShape(double x, double y, int diameter);

    @Override
    public String toString() {
        return guiName;
    }
}
