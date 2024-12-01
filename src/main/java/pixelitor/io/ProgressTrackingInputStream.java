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

package pixelitor.io;

import pixelitor.utils.ProgressTracker;
import pixelitor.utils.StatusBarProgressTracker;

import java.io.*;

/**
 * A wrapper for an InputStream that tracks the progress of reading.
 * It's similar to {@link javax.swing.ProgressMonitorInputStream},
 * but it uses a {@link ProgressTracker}.
 */
public class ProgressTrackingInputStream extends FilterInputStream {
    private final ProgressTracker progressTracker;
    private boolean closed = false;

    public ProgressTrackingInputStream(File file) throws FileNotFoundException {
        super(new FileInputStream(file));

        this.progressTracker = new StatusBarProgressTracker(
            "Reading " + file.getName(), (int) file.length());
    }

    @Override
    public int read() throws IOException {
        int byteRead = in.read();
        if (byteRead != -1) {
            progressTracker.unitDone();
        }
        return byteRead;
    }

    @Override
    public int read(byte[] b) throws IOException {
        int bytesRead = in.read(b);
        if (bytesRead > 0) {
            progressTracker.unitsDone(bytesRead);
        }
        return bytesRead;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int bytesRead = in.read(b, off, len);
        if (bytesRead > 0) {
            progressTracker.unitsDone(bytesRead);
        }
        return bytesRead;
    }

    @Override
    public long skip(long n) throws IOException {
        long bytesSkipped = in.skip(n);
        if (bytesSkipped > 0) {
            // by caring only about ints, we can handle files up to 2GBytes
            progressTracker.unitsDone((int) Math.min(bytesSkipped, Integer.MAX_VALUE));
        }
        return bytesSkipped;
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;

        super.close();
        progressTracker.finished();
    }
}
