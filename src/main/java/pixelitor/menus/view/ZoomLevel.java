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
package pixelitor.menus.view;

import java.awt.BasicStroke;
import java.util.Random;

/**
 *
 */
public enum ZoomLevel {
    Z12d5 {
        @Override
        public double getPercentValue() {
            return 12.5;
        }

        @Override
        public ZoomLevel getNext() {
            return Z17d7;
        }

        @Override
        public ZoomLevel getPrevious() {
            return Z12d5;
        }

        @Override
        public String toString() {
            return "12.5 %";
        }
    }, Z17d7 { // 12.5 * sqrt(2)
        @Override
        public double getPercentValue() {
            return 17.677669529663688110021109052621;
        }

        @Override
        public ZoomLevel getNext() {
            return Z25;
        }

        @Override
        public ZoomLevel getPrevious() {
            return Z12d5;
        }

        @Override
        public String toString() {
            return "17.7 %";
        }

    }, Z25 {
        @Override
        public double getPercentValue() {
            return 25;
        }

        @Override
        public ZoomLevel getNext() {
            return Z35d3;
        }

        @Override
        public ZoomLevel getPrevious() {
            return Z17d7;
        }

        @Override
        public String toString() {
            return "25 %";
        }
    }, Z35d3 {
        @Override
        public double getPercentValue() {
            return 35.355339059327376220042218105242;
        }

        @Override
        public ZoomLevel getNext() {
            return Z50;
        }

        @Override
        public ZoomLevel getPrevious() {
            return Z25;
        }

        @Override
        public String toString() {
            return "35.3 %";
        }
    }, Z50 {
        @Override
        public double getPercentValue() {
            return 50;
        }

        @Override
        public ZoomLevel getNext() {
            return Z70d7;
        }

        @Override
        public ZoomLevel getPrevious() {
            return Z35d3;
        }

        @Override
        public String toString() {
            return "50 %";
        }
    }, Z70d7 {
        @Override
        public double getPercentValue() {
            return 70.710678118654752440084436210485;
        }

        @Override
        public ZoomLevel getNext() {
            return Z100;
        }

        @Override
        public ZoomLevel getPrevious() {
            return Z50;
        }

        @Override
        public String toString() {
            return "70.7 %";
        }
    }, Z100 {
        @Override
        public double getPercentValue() {
            return 100;
        }

        @Override
        public ZoomLevel getNext() {
            return Z200;
        }

        @Override
        public ZoomLevel getPrevious() {
            return Z70d7;
        }

        @Override
        public String toString() {
            return "100 %";
        }

//        @Override
//        public Rectangle fromComponentSpaceToImage(Rectangle input) {
//            return input;
//        }

//        @Override
//        public Rectangle fromImageSpaceToComponent(Rectangle input) {
//            return input;
//        }
    }, Z200 {
        @Override
        public double getPercentValue() {
            return 200;
        }

        @Override
        public ZoomLevel getNext() {
            return Z400;
        }

        @Override
        public ZoomLevel getPrevious() {
            return Z100;
        }

        @Override
        public String toString() {
            return "200 %";
        }
    }, Z400 {
        @Override
        public double getPercentValue() {
            return 400;
        }

        @Override
        public ZoomLevel getNext() {
            return Z800;
        }

        @Override
        public ZoomLevel getPrevious() {
            return Z200;
        }

        @Override
        public String toString() {
            return "400 %";
        }
    }, Z800 {
        @Override
        public double getPercentValue() {
            return 800;
        }

        @Override
        public ZoomLevel getNext() {
            return Z1600;
        }

        @Override
        public ZoomLevel getPrevious() {
            return Z400;
        }

        @Override
        public String toString() {
            return "800 %";
        }
    }, Z1600 {
        @Override
        public double getPercentValue() {
            return 1600;
        }

        @Override
        public ZoomLevel getNext() {
            return Z3200;
        }

        @Override
        public ZoomLevel getPrevious() {
            return Z800;
        }

        @Override
        public String toString() {
            return "1600 %";
        }
    }, Z3200 {
        @Override
        public double getPercentValue() {
            return 3200;
        }

        @Override
        public ZoomLevel getNext() {
            return Z6400;
        }

        @Override
        public ZoomLevel getPrevious() {
            return Z1600;
        }

        @Override
        public String toString() {
            return "3200 %";
        }
    }, Z6400 {
        @Override
        public double getPercentValue() {
            return 6400;
        }

        @Override
        public ZoomLevel getNext() {
            return Z6400;
        }

        @Override
        public ZoomLevel getPrevious() {
            return Z3200;
        }

        @Override
        public String toString() {
            return "6400 %";
        }
    };

    private BasicStroke outerGeometryStroke;
    private BasicStroke innerGeometryStroke;

    public BasicStroke getOuterGeometryStroke() {
        if(outerGeometryStroke == null) {
            outerGeometryStroke = new BasicStroke((float) (300.0f/ getPercentValue()));
        }
        return outerGeometryStroke;
    }

    public BasicStroke getInnerGeometryStroke() {
        if(innerGeometryStroke == null) {
            innerGeometryStroke = new BasicStroke((float) (100.0f/ getPercentValue()));
        }
        return innerGeometryStroke;
    }

    public abstract double getPercentValue();

    public abstract ZoomLevel getNext();

    public abstract ZoomLevel getPrevious();

//    public Point fromComponentSpaceToImage(Point input) {
//        double zoom = 100.0 / getPercentValue();
//        return new Point((int)(input.x*zoom), (int)(input.y*zoom));
//    }
//
//    public Rectangle fromComponentSpaceToImage(Rectangle input) {
//        double zoom = 100.0 / getPercentValue();
//        return new Rectangle((int)(input.x*zoom), (int)(input.y*zoom), (int)(input.width*zoom), (int)(input.height*zoom));
//    }
//
//    public Point fromImageSpaceToComponent(Point input) {
//        double zoom = getPercentValue() / 100.0;
//        return new Point((int)(input.x*zoom), (int)(input.y*zoom));
//    }

//    public Rectangle fromImageSpaceToComponent(Rectangle input) {
//        double zoom = getPercentValue() / 100.0;
//        return new Rectangle((int)(input.x*zoom), (int)(input.y*zoom), (int)(input.width*zoom), (int)(input.height*zoom));
//    }

    private final ZoomMenuItem menuItem = new ZoomMenuItem(this);

    public ZoomMenuItem getMenuItem() {
        return menuItem;
    }

    public static ZoomLevel getRandomZoomLevel(Random rand) {
        int index = rand.nextInt(values().length);
        return values()[index];
    }

    public double getViewScale() {
        return getPercentValue() / 100.0;
    }
}
