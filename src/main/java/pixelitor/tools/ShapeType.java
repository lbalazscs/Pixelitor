/*
 * Copyright 2010-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
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

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;

/**
 *
 */
public enum ShapeType {
    RECTANGLE(true) {
        @Override
        public Shape getShape(UserDrag userDrag) {
            updateCoordinatesPositive(userDrag);
            return new Rectangle(x, y, width, height);
        }

        @Override
        public String toString() {
            return "Rectangle";
        }
    }, ELLIPSE(true) {
        @Override
        public Shape getShape(UserDrag userDrag) {
            updateCoordinatesPositive(userDrag);
            return new Ellipse2D.Float(x, y, width, height);
        }

        @Override
        public String toString() {
            return "Ellipse";
        }
    }, DIAMOND(true) {
        @Override
        public Shape getShape(UserDrag userDrag) {
            updateCoordinates(userDrag);
            Path2D.Float path = new Path2D.Float();

            int cx = x + width / 2;
            int cy = y + height / 2;

            path.moveTo(cx, y);
            path.lineTo(x + width, cy);
            path.lineTo(cx, y + height);
            path.lineTo(x, cy);
            path.closePath();

            return path;
        }

        @Override
        public String toString() {
            return "Diamond";
        }
    }, LINE(false) {
        @Override
        public Shape getShape(UserDrag userDrag) {
//            updateCoordinatesPositive(start, end);
            return userDrag.asLine();
        }

        @Override
        public String toString() {
            return "Line";
        }
    }, HEART(true) {
        @Override
        public Shape getShape(UserDrag userDrag) {
            updateCoordinates(userDrag);
            return new Heart(x, y, width, height);
        }

        @Override
        public String toString() {
            return "Heart";
        }
    }, STAR(true) {
        @Override
        public Shape getShape(UserDrag userDrag) {
            updateCoordinates(userDrag);
            double halfWidth = ((double) width) / 2;
            double halfHeight = ((double) height) / 2;
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
            } else { // TODO
                innerRadius = halfWidth;
                outerRadius = innerRadius + 0.01;
            }

            return new Star2D(cx, cy, innerRadius, outerRadius, 7);
        }

        @Override
        public String toString() {
            return "Star";
        }
    }, RANDOM_STAR(true) {
        private UserDrag lastUserDrag;
        private RandomStar lastStar;

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
        public String toString() {
            return "Random Star";
        }

    }, ARROW(true) {
        GeneralPath unitArrow = null;

        @Override
        public Shape getShape(UserDrag userDrag) {
            if (unitArrow == null) {
                unitArrow = Utils.createUnitArrow();
            }

            updateCoordinates(userDrag);

            float distance = userDrag.getDistance();
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
        public String toString() {
            return "Arrow";
        }
    }, CAT(true) {
        @Override
        public Shape getShape(UserDrag userDrag) {
            updateCoordinates(userDrag);
            return new Cat(x, y, width, height);
        }

        @Override
        public String toString() {
            return "Cat";
        }
    }, KIWI(true) {
        @Override
        public Shape getShape(UserDrag userDrag) {
            updateCoordinates(userDrag);
            return new Kiwi(x, y, width, height);
        }

        @Override
        public String toString() {
            return "Kiwi";
        }
    }, BAT(true) {
        @Override
        public Shape getShape(UserDrag userDrag) {
            updateCoordinates(userDrag);
            return new Bat(x, y, width, height);
        }

        @Override
        public String toString() {
            return "Bat";
        }
    }, RABBIT(true) {
        @Override
        public Shape getShape(UserDrag userDrag) {
            updateCoordinates(userDrag);
            return new Rabbit(x, y, width, height);
        }

        @Override
        public String toString() {
            return "Rabbit";
        }
//    }, SKULL(true) {
//        @Override
//        public Shape getShape(UserDrag userDrag) {
//            updateCoordinates(userDrag);
//            return new Skull(x, y, width, height);
//        }
//        @Override
//        public String toString() {
//            return "Skull";
//        }

    };

    /**
     * Update the x, y, width, height coordinates so that width and height are positive
     */
    protected void updateCoordinatesPositive(UserDrag userDrag) {
        Rectangle r = userDrag.createPositiveRectangle();
        x = r.x;
        y = r.y;
        width = r.width;
        height = r.height;
    }

    /**
     * Update the x, y, width, height coordinates
     */
    protected void updateCoordinates(UserDrag userDrag) {
        Rectangle r = userDrag.createPossiblyEmptyRectangle();

        x = r.x;
        y = r.y;
        width = r.width;
        height = r.height;
    }

    private final boolean closed;

    ShapeType(boolean closed) {
        this.closed = closed;
    }

    protected int x, y, width, height;

    public abstract Shape getShape(UserDrag userDrag);

    public boolean isClosed() {
        return closed;
    }
}
