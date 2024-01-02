/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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
 * Utility class for creating ANSI-colored strings.
 * Used only for debugging.
 */
public class Ansi {
    private static final String RESET = "\u001B[0m";
    private static final String BLACK = "\u001B[30m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String PURPLE = "\u001B[35m";
    private static final String CYAN = "\u001B[36m";
    private static final String WHITE = "\u001B[37m";

    private Ansi() {
        // prevent instantiation
    }

    public static String red(Object o) {
        return RED + o + RESET;
    }

    public static String green(Object o) {
        return GREEN + o + RESET;
    }

    public static String blue(Object o) {
        return BLUE + o + RESET;
    }

    public static String yellow(Object o) {
        return YELLOW + o + RESET;
    }

    public static String purple(Object o) {
        return PURPLE + o + RESET;
    }

    public static String cyan(Object o) {
        return CYAN + o + RESET;
    }
}
