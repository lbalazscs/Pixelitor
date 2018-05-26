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

package pixelitor;

import pixelitor.gui.ImageComponents;
import pixelitor.layers.Layer;
import pixelitor.layers.MaskViewMode;

import static pixelitor.layers.MaskViewMode.EDIT_MASK;
import static pixelitor.layers.MaskViewMode.NORMAL;
import static pixelitor.layers.MaskViewMode.RUBYLITH;
import static pixelitor.layers.MaskViewMode.SHOW_MASK;

/**
 * What is tested in AssertJ Swing test
 */
enum TestingMode {
    /**
     * A layer with no mask is tested
     */
    NO_MASK(NORMAL) {
        @Override
        public boolean isSet(Layer layer) {
            return !layer.hasMask();
        }

        @Override
        public void set(AssertJSwingTest tester) {
            assert isSet(ImageComponents.getActiveLayerOrNull());
            // do nothing as initially the layers have no masks

            assert tester.checkConsistency();
        }
    },
    /**
     * A layer with a mask is tested
     */
    WITH_MASK(NORMAL) {
        @Override
        public boolean isSet(Layer layer) {
            if (layer.hasMask()) {
                return !layer.isMaskEditing();
            } else {
                return false;
            }
        }

        @Override
        public void set(AssertJSwingTest tester) {
            // existing masks are allowed because even if they result
            // from a layer duplication, a correct mask must be set up
            tester.addLayerMask(true);
            tester.pressCtrlOne();

            assert tester.checkConsistency();
        }
    },
    /**
     * A mask is tested, while viewing the layer
     */
    ON_MASK_VIEW_LAYER(EDIT_MASK) {
        @Override
        public boolean isSet(Layer layer) {
            if (layer.hasMask()) {
                if (layer.isMaskEditing()) {
                    return isActualMaskViewMode(EDIT_MASK);
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }

        @Override
        public void set(AssertJSwingTest tester) {
            tester.addLayerMask(true);
            tester.pressCtrlThree();

            assert tester.checkConsistency();
        }
    },
    /**
     * A mask is tested, while viewing the mask
     */
    ON_MASK_VIEW_MASK(SHOW_MASK) {
        @Override
        public boolean isSet(Layer layer) {
            if (layer.hasMask()) {
                if (layer.isMaskEditing()) {
                    return isActualMaskViewMode(SHOW_MASK);
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }

        @Override
        public void set(AssertJSwingTest tester) {
            tester.addLayerMask(true);
            tester.pressCtrlTwo();

            assert tester.checkConsistency();
        }
    },
    /**
     * A mask is tested, while viewing the layer and a rubylith mask
     */
    RUBY(RUBYLITH) {
        @Override
        public boolean isSet(Layer layer) {
            if (layer.hasMask()) {
                if (layer.isMaskEditing()) {
                    return isActualMaskViewMode(RUBYLITH);
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }

        @Override
        public void set(AssertJSwingTest tester) {
            tester.addLayerMask(true);
            tester.pressCtrlFour();

            assert tester.checkConsistency();
        }
    };

    private final MaskViewMode viewMode;

    TestingMode(MaskViewMode viewMode) {
        this.viewMode = viewMode;
    }

    public boolean isMaskEditing() {
        return viewMode.editMask();
    }

    protected static boolean isActualMaskViewMode(MaskViewMode expected) {
        return ImageComponents.getActiveIC().getMaskViewMode() == expected;
    }

    /**
     * Return true if the testing mode is set
     */
    public abstract boolean isSet(Layer layer);

    /**
     * Make sure that the testing more is set
     */
    public abstract void set(AssertJSwingTest tester);
}
