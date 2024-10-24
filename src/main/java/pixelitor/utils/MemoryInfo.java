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

package pixelitor.utils;

import static java.lang.String.format;

public class MemoryInfo {
    public static final int NUM_BYTES_IN_MEGABYTE = 1_048_576;
    private static final int NUM_BYTES_IN_KILOBYTE = 1_024;
    private final long freeMemoryMB;
    private final long totalMemoryMB;
    private final long usedMemoryMB;
    private final long maxMemoryMB;

    public MemoryInfo() {
        Runtime runtime = Runtime.getRuntime();

        long freeMemory = runtime.freeMemory();
        long totalMemory = runtime.totalMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();

        freeMemoryMB = freeMemory / NUM_BYTES_IN_MEGABYTE;
        totalMemoryMB = totalMemory / NUM_BYTES_IN_MEGABYTE;
        usedMemoryMB = usedMemory / NUM_BYTES_IN_MEGABYTE;
        maxMemoryMB = maxMemory / NUM_BYTES_IN_MEGABYTE;
    }

    public static String bytesToString(int bytes) {
        if (bytes < NUM_BYTES_IN_KILOBYTE) {
            return bytes + " bytes";
        } else if (bytes < NUM_BYTES_IN_MEGABYTE) {
            float kiloBytes = ((float) bytes) / NUM_BYTES_IN_KILOBYTE;
            return format("%.2f kilobytes", kiloBytes);
        } else {
            float megaBytes = ((float) bytes) / NUM_BYTES_IN_MEGABYTE;
            return format("%.2f megabytes", megaBytes);
        }
    }

    public static int getMaxHeapMb() {
        long heapMaxSize = Runtime.getRuntime().maxMemory();
        return (int) (heapMaxSize / NUM_BYTES_IN_MEGABYTE);
    }

    public static int getTotalMemoryMb() {
        long totalMemory = Runtime.getRuntime().totalMemory();
        return (int) (totalMemory / NUM_BYTES_IN_MEGABYTE);
    }

    public String getFreeMemory() {
        return freeMemoryMB + " megabytes";
    }

    public String getTotalMemory() {
        return totalMemoryMB + " megabytes";
    }

    public String getUsedMemory() {
        return usedMemoryMB + " megabytes";
    }

    public String getMaxMemory() {
        return maxMemoryMB + " megabytes";
    }

    public long getAvailableMemoryMB() {
        return maxMemoryMB - usedMemoryMB;
    }

    @Override
    public String toString() {
        return format("allocated = %d, used = %d, free = %d, max = %d",
            totalMemoryMB, usedMemoryMB, freeMemoryMB, maxMemoryMB);
    }
}
