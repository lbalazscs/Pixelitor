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

package pixelitor.io;

import pixelitor.utils.ProgressTracker;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Tracks the progress of reading from an InputStream.
 * It is similar to {@link javax.swing.ProgressMonitorInputStream},
 * but works with {@link ProgressTracker} objects
 */
public class ProgressTrackingInputStream extends FilterInputStream {
    private final ProgressTracker pt;

    /**
     * The max value of the given {@link ProgressTracker}
     * must be initialized to the file size
     */
    public ProgressTrackingInputStream(InputStream in, ProgressTracker pt) {
        super(in);
        this.pt = pt;
    }

    @Override
    public int read() throws IOException {
        int readByte = in.read();
        pt.unitDone();
        return readByte;
    }

    @Override
    public int read(byte[] b) throws IOException {
        int numBytes = in.read(b);
        pt.unitsDone(numBytes);
        return numBytes;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int numBytes = in.read(b, off, len);
        pt.unitsDone(numBytes);
        return numBytes;
    }

    @Override
    public long skip(long n) throws IOException {
        long numBytes = in.skip(n);

        // by caring only about ints, we can handle files up to 2GBytes
        pt.unitsDone((int) numBytes);

        return numBytes;
    }
}
