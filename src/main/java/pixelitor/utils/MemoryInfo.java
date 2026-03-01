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

import static java.lang.String.format;

/**
 * Information about the current JVM memory usage.
 */
public class MemoryInfo {
    public static final int BYTES_PER_MEGABYTE = 1_048_576;
    private static final int BYTES_PER_KILOBYTE = 1_024;

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

        freeMemoryMB = freeMemory / BYTES_PER_MEGABYTE;
        totalMemoryMB = totalMemory / BYTES_PER_MEGABYTE;
        usedMemoryMB = usedMemory / BYTES_PER_MEGABYTE;
        maxMemoryMB = maxMemory / BYTES_PER_MEGABYTE;
    }

    public static String formatBytes(int bytes) {
        if (bytes < BYTES_PER_KILOBYTE) {
            return bytes + " bytes";
        } else if (bytes < BYTES_PER_MEGABYTE) {
            float kiloBytes = ((float) bytes) / BYTES_PER_KILOBYTE;
            return format("%.2f KB", kiloBytes);
        } else {
            float megaBytes = ((float) bytes) / BYTES_PER_MEGABYTE;
            return format("%.2f MB", megaBytes);
        }
    }

    public static int getMaxHeapMB() {
        long heapMaxSize = Runtime.getRuntime().maxMemory();
        return (int) (heapMaxSize / BYTES_PER_MEGABYTE);
    }

    public static int getTotalMemoryMB() {
        long totalMemory = Runtime.getRuntime().totalMemory();
        return (int) (totalMemory / BYTES_PER_MEGABYTE);
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
        return format("allocated = %d MB, used = %d MB, free = %d MB, max = %d MB",
            totalMemoryMB, usedMemoryMB, freeMemoryMB, maxMemoryMB);
    }
}
