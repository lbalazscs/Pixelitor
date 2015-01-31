/*
 * Copyright 2015 Laszlo Balazs-Csiki
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
package pixelitor.tools;

enum CropToolState {
    INITIAL {
        @Override
        public CropToolState getNextAfterMousePressed() {
            return USER_DRAG;
        }

        @Override
        public void cancelPressed(CropTool cropTool) {
            throw new IllegalStateException();
        }
    }, USER_DRAG {
        // from the first mouse press until the first mouse release
        // in this state the transform handles are not shown

        @Override
        public CropToolState getNextAfterMousePressed() {
            throw new IllegalStateException();
        }

        @Override
        public void cancelPressed(CropTool cropTool) {
            cropTool.resetStateToInitial();
        }
    }, TRANSFORM {
        // from the first mouse release until crop or cancel is pressed
        // the handles are shown

        @Override
        public CropToolState getNextAfterMousePressed() {
            return TRANSFORM;
        }

        @Override
        public void cancelPressed(CropTool cropTool) {
            cropTool.resetStateToInitial();
        }
    };

    public abstract CropToolState getNextAfterMousePressed();

    public abstract void cancelPressed(CropTool cropTool);

}
