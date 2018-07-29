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

package pixelitor.utils.debug;

/**
 * Helper class with ANSI colors for debugging
 */
public class Ansi {
    public static final String RESET = "\u001B[0m";
    public static final String BLACK = "\u001B[30m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String PURPLE = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";

    private Ansi() {
    }

    public static String red(String s) {
        return RED + s + RESET;
    }

    public static String green(String s) {
        return GREEN + s + RESET;
    }

    public static String blue(String s) {
        return BLUE + s + RESET;
    }

    public static String yellow(String s) {
        return YELLOW + s + RESET;
    }

    public static String purple(String s) {
        return PURPLE + s + RESET;
    }

    public static String cyan(String s) {
        return CYAN + s + RESET;
    }
}
