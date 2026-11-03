/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.utils;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Static utility methods related to the flood fill algorithm.
 */
public class FloodFill {
    private FloodFill() {
    }

    /**
     * An interface for processing a horizontal line segment of pixels.
     */
    @FunctionalInterface
    public interface SegmentProcessor {
        /**
         * Processes a horizontal segment.
         *
         * @param y  The y-coordinate of the segment.
         * @param x1 The starting x-coordinate (inclusive).
         * @param x2 The ending x-coordinate (inclusive).
         */
        void process(int y, int x1, int x2);
    }

    /**
     * Handles the traversal of a color-bounded area.
     * The actual action performed on each segment of the area
     * is defined by the given {@link SegmentProcessor}.
     */
    public static void floodFill(int[] pixels, int width, int height,
                                 int startX, int startY, int tolerance,
                                 SegmentProcessor processor) {
        if (startX < 0 || startX >= width || startY < 0 || startY >= height) {
            return;
        }

        boolean[] visited = new boolean[width * height];
        // stack of horizontal line segments to process: {y, x1, x2}
        Deque<int[]> stack = new ArrayDeque<>();

        int startColor = pixels[startY * width + startX];

        // 1. find the initial horizontal line segment around the starting point
        int x1 = startX;
        int x2 = startX;

        // scan left
        while (x1 > 0 && isSimilar(startColor, pixels[startY * width + x1 - 1], tolerance)) {
            x1--;
        }
        // scan right
        while (x2 < width - 1 && isSimilar(startColor, pixels[startY * width + x2 + 1], tolerance)) {
            x2++;
        }

        // process the initial segment and push it onto the stack
        processor.process(startY, x1, x2);
        for (int x = x1; x <= x2; x++) {
            visited[startY * width + x] = true;
        }
        stack.push(new int[]{startY, x1, x2});

        // 2. process segments from the stack until it's empty
        while (!stack.isEmpty()) {
            int[] segment = stack.pop();
            int y = segment[0];
            int left = segment[1];
            int right = segment[2];

            // 3. scan the rows directly above (dy = -1) and below (dy = 1) the current segment
            for (int dy = -1; dy <= 1; dy += 2) {
                int nextY = y + dy;
                if (nextY < 0 || nextY >= height) {
                    continue;
                }

                for (int x = left; x <= right; x++) {
                    int index = nextY * width + x;
                    // if we find an unvisited, similar-colored pixel, start a new horizontal scan
                    if (!visited[index] && isSimilar(startColor, pixels[index], tolerance)) {
                        int newX1 = x;
                        int newX2 = x;
                        // scan left
                        while (newX1 > 0 && !visited[nextY * width + newX1 - 1] && isSimilar(startColor, pixels[nextY * width + newX1 - 1], tolerance)) {
                            newX1--;
                        }
                        // scan right
                        while (newX2 < width - 1 && !visited[nextY * width + newX2 + 1] && isSimilar(startColor, pixels[nextY * width + newX2 + 1], tolerance)) {
                            newX2++;
                        }

                        // process the new segment, mark its pixels as visited, and push it
                        processor.process(nextY, newX1, newX2);
                        for (int i = newX1; i <= newX2; i++) {
                            visited[nextY * width + i] = true;
                        }
                        stack.push(new int[]{nextY, newX1, newX2});

                        // advance x to the end of the new segment to avoid redundant checks
                        x = newX2;
                    }
                }
            }
        }
    }

    /**
     * Checks if two colors are within the given tolerance (per channel).
     */
    public static boolean isSimilar(int color1, int color2, int tolerance) {
        if (color1 == color2) {
            return true;
        }

        int a1 = color1 >>> 24;
        int r1 = (color1 >>> 16) & 0xFF;
        int g1 = (color1 >>> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int a2 = color2 >>> 24;
        int r2 = (color2 >>> 16) & 0xFF;
        int g2 = (color2 >>> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        return (r2 <= r1 + tolerance) && (r2 >= r1 - tolerance) &&
            (g2 <= g1 + tolerance) && (g2 >= g1 - tolerance) &&
            (b2 <= b1 + tolerance) && (b2 >= b1 - tolerance) &&
            (a2 <= a1 + tolerance) && (a2 >= a1 - tolerance);
    }
}
