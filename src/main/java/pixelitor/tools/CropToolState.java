/*
 * Copyright 2017 Laszlo Balazs-Csiki
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
    }, USER_DRAG {
        // the transform handles are not shown
        // from the first mouse press until the first mouse release

        @Override
        public CropToolState getNextAfterMousePressed() {
            throw new IllegalStateException();
        }
    }, TRANSFORM {
        // the handles are shown
        // from the first mouse release until crop or cancel

        @Override
        public CropToolState getNextAfterMousePressed() {
            return TRANSFORM;
        }
    };

    public abstract CropToolState getNextAfterMousePressed();
}
