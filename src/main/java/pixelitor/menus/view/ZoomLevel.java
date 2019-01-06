/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.menus.view;

import pixelitor.utils.Lazy;
import pixelitor.utils.Rnd;

/**
 * The available zoom levels
 */
public enum ZoomLevel {
    Z12("12.5%") {
        @Override
        public double getPercentValue() {
            return 12.5;
        }

        @Override
        public ZoomLevel zoomIn() {
            return Z18;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z12;
        }
    }, Z18("17.7%") { // 12.5 * sqrt(2)
        @Override
        public double getPercentValue() {
            return 17.67766952966369;
        }

        @Override
        public ZoomLevel zoomIn() {
            return Z25;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z12;
        }
    }, Z25("25%") {
        @Override
        public double getPercentValue() {
            return 25;
        }

        @Override
        public ZoomLevel zoomIn() {
            return Z35;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z18;
        }
    }, Z35("35.3%") {
        @Override
        public double getPercentValue() {
            return 35.35533905932738;
        }

        @Override
        public ZoomLevel zoomIn() {
            return Z50;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z25;
        }
    }, Z50("50%") {
        @Override
        public double getPercentValue() {
            return 50;
        }

        @Override
        public ZoomLevel zoomIn() {
            return Z71;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z35;
        }
    }, Z71("70.7%") {
        @Override
        public double getPercentValue() {
            return 70.71067811865476;
        }

        @Override
        public ZoomLevel zoomIn() {
            return Z100;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z50;
        }
    }, Z100("100%") {
        @Override
        public double getPercentValue() {
            return 100;
        }

        @Override
        public ZoomLevel zoomIn() {
            return Z141;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z71;
        }
    }, Z141("141.4%") {
        @Override
        public double getPercentValue() {
            return 141.4213562373095;
        }

        @Override
        public ZoomLevel zoomIn() {
            return Z200;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z100;
        }
    }, Z200("200%") {
        @Override
        public double getPercentValue() {
            return 200;
        }

        @Override
        public ZoomLevel zoomIn() {
            return Z283;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z141;
        }
    }, Z283("282.8%") {
        @Override
        public double getPercentValue() {
            return 282.842712474619;
        }

        @Override
        public ZoomLevel zoomIn() {
            return Z400;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z200;
        }
    }, Z400("400%") {
        @Override
        public double getPercentValue() {
            return 400;
        }

        @Override
        public ZoomLevel zoomIn() {
            return Z566;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z283;
        }
    }, Z566("565.7%") {
        @Override
        public double getPercentValue() {
            return 565.685424949238;
        }

        @Override
        public ZoomLevel zoomIn() {
            return Z800;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z400;
        }
    }, Z800("800%") {
        @Override
        public double getPercentValue() {
            return 800;
        }

        @Override
        public ZoomLevel zoomIn() {
            return Z1131;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z566;
        }
    }, Z1131("1131.4%") {
        @Override
        public double getPercentValue() {
            return 1131.370849898476;
        }

        @Override
        public ZoomLevel zoomIn() {
            return Z1600;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z800;
        }
    }, Z1600("1600%") {
        @Override
        public double getPercentValue() {
            return 1600;
        }

        @Override
        public ZoomLevel zoomIn() {
            return Z2263;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z1131;
        }
    }, Z2263("2262.7%") {
        @Override
        public double getPercentValue() {
            return 2262.741699796952;
        }

        @Override
        public ZoomLevel zoomIn() {
            return Z3200;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z1600;
        }
    }, Z3200("3200%") {
        @Override
        public double getPercentValue() {
            return 3200;
        }

        @Override
        public ZoomLevel zoomIn() {
            return Z4525;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z2263;
        }
    }, Z4525("4525.5%") {
        @Override
        public double getPercentValue() {
            return 4525.483399593904;
        }

        @Override
        public ZoomLevel zoomIn() {
            return Z6400;
        }

        @Override
        public ZoomLevel zoomOut() {
            return Z3200;
        }
    }, Z6400("6400%") {
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
            return Z4525;
        }
    };

    private final String guiName;

    ZoomLevel(String guiName) {
        this.guiName = guiName;
    }

    // The menuItem must be initialized only after the enum constructor
    // in order to make sure that it has a name
    private final Lazy<ZoomMenuItem> menuItem = Lazy.of(
            () -> new ZoomMenuItem(this));

    @Override
    public String toString() {
        return guiName;
    }

    public ZoomMenuItem getMenuItem() {
        return menuItem.get();
    }

    public abstract double getPercentValue();

    public abstract ZoomLevel zoomIn();

    public abstract ZoomLevel zoomOut();

    public static ZoomLevel getRandomZoomLevel() {
        return Rnd.chooseFrom(values());
    }

    public double getViewScale() {
        return getPercentValue() / 100.0;
    }

    public boolean allowPixelGrid() {
        return getPercentValue() > 1500;
    }
}
