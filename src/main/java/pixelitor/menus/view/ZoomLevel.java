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
 * The available zoom levels
 */
public enum ZoomLevel {
    Z12 {
        @Override
        public double getPercentValue() {
            return 12.5;
        }

        @Override
        public ZoomLevel zoomIn() {
            return Z12plus;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z12;
        }

        @Override
        public String toString() {
            return "12.5 %";
        }
    }, Z12plus { // 12.5 * sqrt(2)

        @Override
        public double getPercentValue() {
            return 17.677669529663688110021109052621;
        }

        @Override
        public ZoomLevel zoomIn() {
            return Z25;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z12;
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
        public ZoomLevel zoomIn() {
            return Z25plus;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z12plus;
        }

        @Override
        public String toString() {
            return "25 %";
        }
    }, Z25plus {
        @Override
        public double getPercentValue() {
            return 35.355339059327376220042218105242;
        }

        @Override
        public ZoomLevel zoomIn() {
            return Z50;
        }

        @Override
        public ZoomLevel zoomOut() {
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
        public ZoomLevel zoomIn() {
            return Z50plus;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z25plus;
        }

        @Override
        public String toString() {
            return "50 %";
        }
    }, Z50plus {
        @Override
        public double getPercentValue() {
            return 70.710678118654752440084436210485;
        }

        @Override
        public ZoomLevel zoomIn() {
            return Z100;
        }

        @Override
        public ZoomLevel zoomOut() {
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
        public ZoomLevel zoomIn() {
            return Z100plus;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z50plus;
        }

        @Override
        public String toString() {
            return "100 %";
        }
    }, Z100plus {
        @Override
        public double getPercentValue() {
            return 141.42135623730950488016887242097;
        }

        @Override
        public ZoomLevel zoomIn() {
            return Z200;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z100;
        }

        @Override
        public String toString() {
            return "141.4 %";
        }
    }, Z200 {
        @Override
        public double getPercentValue() {
            return 200;
        }

        @Override
        public ZoomLevel zoomIn() {
            return Z200plus;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z100plus;
        }

        @Override
        public String toString() {
            return "200 %";
        }
    }, Z200plus {
        @Override
        public double getPercentValue() {
            return 282.84271247461900976033774484194;
        }

        @Override
        public ZoomLevel zoomIn() {
            return Z400;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z200;
        }

        @Override
        public String toString() {
            return "282.8 %";
        }
    }, Z400 {
        @Override
        public double getPercentValue() {
            return 400;
        }

        @Override
        public ZoomLevel zoomIn() {
            return Z400plus;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z200plus;
        }

        @Override
        public String toString() {
            return "400 %";
        }
    }, Z400plus {
        @Override
        public double getPercentValue() {
            return 565.68542494923801952067548968388;
        }

        @Override
        public ZoomLevel zoomIn() {
            return Z800;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z400;
        }

        @Override
        public String toString() {
            return "565.7 %";
        }
    }, Z800 {
        @Override
        public double getPercentValue() {
            return 800;
        }

        @Override
        public ZoomLevel zoomIn() {
            return Z800plus;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z400plus;
        }

        @Override
        public String toString() {
            return "800 %";
        }
    }, Z800plus {
        @Override
        public double getPercentValue() {
            return 1131.3708498984760390413509793678;
        }

        @Override
        public ZoomLevel zoomIn() {
            return Z1600;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z800;
        }

        @Override
        public String toString() {
            return "1131.4 %";
        }
    }, Z1600 {
        @Override
        public double getPercentValue() {
            return 1600;
        }

        @Override
        public ZoomLevel zoomIn() {
            return Z1600plus;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z800plus;
        }

        @Override
        public String toString() {
            return "1600 %";
        }
    }, Z1600plus {
        @Override
        public double getPercentValue() {
            return 2262.7416997969520780827019587355;
        }

        @Override
        public ZoomLevel zoomIn() {
            return Z3200;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z1600;
        }

        @Override
        public String toString() {
            return "2262.7 %";
        }
    }, Z3200 {
        @Override
        public double getPercentValue() {
            return 3200;
        }

        @Override
        public ZoomLevel zoomIn() {
            return Z3200plus;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z1600plus;
        }

        @Override
        public String toString() {
            return "3200 %";
        }
    }, Z3200plus {
        @Override
        public double getPercentValue() {
            return 4525.483399593904156165403917471;
        }

        @Override
        public ZoomLevel zoomIn() {
            return Z6400;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z3200;
        }

        @Override
        public String toString() {
            return "4525.5 %";
        }
    }, Z6400 {
        @Override
        public double getPercentValue() {
            return 6400;
        }

        @Override
        public ZoomLevel zoomIn() {
            return Z6400;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z3200plus;
        }

        @Override
        public String toString() {
            return "6400 %";
        }
    };

    private BasicStroke outerGeometryStroke;
    private BasicStroke innerGeometryStroke;

    public BasicStroke getOuterGeometryStroke() {
        if (outerGeometryStroke == null) {
            outerGeometryStroke = new BasicStroke((float) (300.0f / getPercentValue()));
        }
        return outerGeometryStroke;
    }

    public BasicStroke getInnerGeometryStroke() {
        if (innerGeometryStroke == null) {
            innerGeometryStroke = new BasicStroke((float) (100.0f / getPercentValue()));
        }
        return innerGeometryStroke;
    }

    public abstract double getPercentValue();

    public abstract ZoomLevel zoomIn();

    public abstract ZoomLevel zoomOut();

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
