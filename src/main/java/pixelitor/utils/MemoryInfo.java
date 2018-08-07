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

package pixelitor.utils;

import static java.lang.String.format;

public class MemoryInfo {
    public static final int ONE_MEGABYTE = 1024*1024;

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

        freeMemoryMB = freeMemory / ONE_MEGABYTE;
        totalMemoryMB = totalMemory / ONE_MEGABYTE;
        usedMemoryMB = usedMemory / ONE_MEGABYTE;
        maxMemoryMB = maxMemory / ONE_MEGABYTE;
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
        return (maxMemoryMB - usedMemoryMB);
    }

    @Override
    public String toString() {
        return format("allocated = %d, used = %d, free = %d, max = %d",
                totalMemoryMB, usedMemoryMB, freeMemoryMB, maxMemoryMB);
    }
}
