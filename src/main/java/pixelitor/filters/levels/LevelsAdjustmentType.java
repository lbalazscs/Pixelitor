package pixelitor.filters.levels;

import java.awt.Color;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;

public enum LevelsAdjustmentType {
    RGB {
        @Override
        public String getName() {
            return "Red, Green, Blue";
        }

        @Override
        public Color getBackColor() {
            return BLACK;
        }

        @Override
        public Color getWhiteColor() {
            return WHITE;
        }
    }, R {
        @Override
        public String getName() {
            return "Red";
        }

        @Override
        public Color getBackColor() {
            return DARK_CYAN;
        }

        @Override
        public Color getWhiteColor() {
            return LIGHT_PINK;
        }
    }, G {
        @Override
        public String getName() {
            return "Green";
        }

        @Override
        public Color getBackColor() {
            return DARK_PURPLE;
        }

        @Override
        public Color getWhiteColor() {
            return LIGHT_GREEN;
        }
    }, B {
        @Override
        public String getName() {
            return "Blue";
        }

        @Override
        public Color getBackColor() {
            return DARK_YELLOW_GREEN;
        }

        @Override
        public Color getWhiteColor() {
            return LIGHT_BLUE;
        }
    }, RG {
        @Override
        public String getName() {
            return "Red, Green";
        }

        @Override
        public Color getBackColor() {
            return DARK_BLUE;
        }

        @Override
        public Color getWhiteColor() {
            return LIGHT_YELLOW;
        }
    }, RB {
        @Override
        public String getName() {
            return "Red, Blue";
        }

        @Override
        public Color getBackColor() {
            return DARK_GREEN;
        }

        @Override
        public Color getWhiteColor() {
            return LIGHT_PURPLE;
        }
    }, GB {
        @Override
        public String getName() {
            return "Green, Blue";
        }

        @Override
        public Color getBackColor() {
            return DARK_RED;
        }

        @Override
        public Color getWhiteColor() {
            return LIGHT_CYAN;
        }
    };

    private static final Color DARK_CYAN = new Color(0, 128, 128);
    private static final Color LIGHT_PINK = new Color(255, 128, 128);
    private static final Color DARK_PURPLE = new Color(128, 0, 128);
    private static final Color LIGHT_GREEN = new Color(128, 255, 128);
    private static final Color DARK_YELLOW_GREEN = new Color(128, 128, 0);
    private static final Color LIGHT_BLUE = new Color(128, 128, 255);
    private static final Color DARK_BLUE = new Color(0, 0, 128);
    private static final Color LIGHT_YELLOW = new Color(255, 255, 128);
    private static final Color DARK_GREEN = new Color(0, 128, 0);
    private static final Color LIGHT_PURPLE = new Color(255, 128, 255);
    private static final Color DARK_RED = new Color(128, 0, 0);
    private static final Color LIGHT_CYAN = new Color(128, 255, 128);

    abstract String getName();

    public abstract Color getBackColor();

    public abstract Color getWhiteColor();
}
