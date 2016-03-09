/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

package pixelitor;

/**
 * A non-GUI message handler for tests
 */
public class TestMessageHandler implements MessageHandler {
    @Override
    public void showStatusMessage(String msg) {
    }

    @Override
    public void startProgress(String msg, int max) {
    }

    @Override
    public void updateProgress(int value) {
    }

    @Override
    public void stopProgress() {
    }

    @Override
    public void showInfo(String title, String msg) {
    }

    @Override
    public void showError(String title, String msg) {
        throw new AssertionError("error");
    }

    @Override
    public void showNotImageLayerError() {
        throw new AssertionError("not image layer");
    }

    @Override
    public void showNotImageLayerOrMaskError() {
        throw new AssertionError("not image layer or mask");
    }

    @Override
    public void showException(Throwable e) {
        throw new AssertionError(e);
    }

    @Override
    public void showException(Throwable e, Thread t) {
        throw new AssertionError(e);
    }
}
