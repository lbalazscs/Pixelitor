/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.guitest;

import pixelitor.gui.OpenComps;
import pixelitor.layers.MaskViewMode;

import static pixelitor.layers.MaskViewMode.EDIT_MASK;
import static pixelitor.layers.MaskViewMode.NORMAL;
import static pixelitor.layers.MaskViewMode.RUBYLITH;
import static pixelitor.layers.MaskViewMode.SHOW_MASK;

/**
 * What mask state is tested in an {@link AssertJSwingTest}
 */
enum MaskMode {
    /**
     * A layer with no mask is tested
     */
    NO_MASK(NORMAL) {
        @Override
        public void check() {
            if (EDT.activeLayerHasMask()) {
                throw new AssertionError("Mask found in " + EDT.activeLayerName());
            }
        }

        @Override
        public void set(AssertJSwingTest tester) {
            if(EDT.activeLayerHasMask()) {
                tester.deleteLayerMask();
            }
            
            check();
            tester.checkConsistency();
        }
    },
    /**
     * A layer with a mask is tested
     */
    WITH_MASK(NORMAL) {
        @Override
        public void check() {
            if (EDT.activeLayerHasMask()) {
                if (EDT.activeLayerIsMaskEditing()) {
                    throw new AssertionError("Mask editing in " + EDT.activeLayerName());
                }
            } else {
                throw new AssertionError("No mask found in " + EDT.activeLayerName());
            }
        }

        @Override
        public void set(AssertJSwingTest tester) {
            // existing masks are allowed because even if they result
            // from a layer duplication, a correct mask must be set up
            tester.addLayerMask(true);
            tester.keyboard().pressCtrlOne();

            tester.checkConsistency();
        }
    },
    /**
     * A mask is tested, while viewing the layer
     */
    ON_MASK_VIEW_LAYER(EDIT_MASK) {
        @Override
        public void check() {
            if (EDT.activeLayerHasMask()) {
                if (EDT.activeLayerIsMaskEditing()) {
                    assertMaskViewModeIs(EDIT_MASK);
                } else {
                    throw new AssertionError("Not mask editing in " + EDT.activeLayerName());
                }
            } else {
                throw new AssertionError("No mask found in " + EDT.activeLayerName());
            }
        }

        @Override
        public void set(AssertJSwingTest tester) {
            tester.addLayerMask(true);
            tester.keyboard().pressCtrlThree();

            tester.checkConsistency();
        }
    },
    /**
     * A mask is tested, while viewing the mask
     */
    ON_MASK_VIEW_MASK(SHOW_MASK) {
        @Override
        public void check() {
            if (EDT.activeLayerHasMask()) {
                if (EDT.activeLayerIsMaskEditing()) {
                    assertMaskViewModeIs(SHOW_MASK);
                } else {
                    throw new AssertionError("Not mask editing in " + EDT.activeLayerName());
                }
            } else {
                throw new AssertionError("No mask found in " + EDT.activeLayerName());
            }
        }

        @Override
        public void set(AssertJSwingTest tester) {
            tester.addLayerMask(true);
            tester.keyboard().pressCtrlTwo();

            tester.checkConsistency();
        }
    },
    /**
     * A mask is tested, while viewing the layer and a rubylith mask
     */
    RUBY(RUBYLITH) {
        @Override
        public void check() {
            if (EDT.activeLayerHasMask()) {
                if (EDT.activeLayerIsMaskEditing()) {
                    assertMaskViewModeIs(RUBYLITH);
                } else {
                    throw new AssertionError("Not mask editing in " + EDT.activeLayerName());
                }
            } else {
                throw new AssertionError("No mask found in " + EDT.activeLayerName());
            }
        }

        @Override
        public void set(AssertJSwingTest tester) {
            tester.addLayerMask(true);
            tester.keyboard().pressCtrlFour();

            tester.checkConsistency();
        }
    };

    private final MaskViewMode viewMode;

    MaskMode(MaskViewMode viewMode) {
        this.viewMode = viewMode;
    }

    public boolean isMaskEditing() {
        return viewMode.editMask();
    }

    protected static void assertMaskViewModeIs(MaskViewMode expected) {
        MaskViewMode actual = OpenComps.getActiveView().getMaskViewMode();
        if (actual != expected) {
            throw new AssertionError("expected mask view mode " + expected
                    + ", found " + actual);
        }
    }

    /**
     * Checks that the testing mode is set on the active layer
     */
    public abstract void check();

    /**
     * Make sure that the testing more is set on the active layer
     */
    public abstract void set(AssertJSwingTest tester);
}
