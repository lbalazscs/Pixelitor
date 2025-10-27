/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.layers.MaskViewMode;

import java.util.Arrays;
import java.util.Locale;

import static pixelitor.layers.MaskViewMode.EDIT_MASK;
import static pixelitor.layers.MaskViewMode.NORMAL;
import static pixelitor.layers.MaskViewMode.RUBYLITH;
import static pixelitor.layers.MaskViewMode.VIEW_MASK;

/**
 * The possible states of the active layer with regards to its mask.
 * Used to test all states with the same mouse and keyboard input
 * in {@link MainGuiTest}.
 */
enum MaskMode {
    /**
     * The active layer has no mask.
     */
    NO_MASK(null) {
        @Override
        public void check() {
            EDT.assertActiveLayerHasNoMask();
        }

        @Override
        public void configureActiveLayer(MainGuiTest tester) {
            if (EDT.activeLayerHasMask()) {
                tester.app().deleteLayerMask();
            }
        }

        @Override
        public void setMaskViewMode(Keyboard keyboard) {
            // do nothing
        }

        @Override
        public boolean isMaskEditing() {
            return false;
        }
    },
    /**
     * The active layer has a mask and it's in NORMAL mask view mode
     */
    WITH_MASK(NORMAL) {
        @Override
        public void check() {
            EDT.assertActiveLayerHasMask();
            EDT.assertActiveLayerIsNotMaskEditing();
        }

        @Override
        public void configureActiveLayer(MainGuiTest tester) {
            // existing masks are allowed because even if they result
            // from a layer duplication, a correct mask must be set up
            tester.addLayerMask(true);
        }

        @Override
        public void setMaskViewMode(Keyboard keyboard) {
            keyboard.pressCtrlOne();
        }
    },
    /**
     * A mask is tested, while viewing the layer
     */
    ON_MASK_VIEW_LAYER(EDIT_MASK) {
        @Override
        public void check() {
            checkMaskEditingInMode(EDIT_MASK);
        }

        @Override
        public void configureActiveLayer(MainGuiTest tester) {
            tester.addLayerMask(true);
        }

        @Override
        public void setMaskViewMode(Keyboard keyboard) {
            keyboard.pressCtrlThree();
        }
    },
    /**
     * A mask is tested, while viewing the mask
     */
    ON_MASK_VIEW_MASK(VIEW_MASK) {
        @Override
        public void check() {
            checkMaskEditingInMode(VIEW_MASK);
        }

        @Override
        public void configureActiveLayer(MainGuiTest tester) {
            tester.addLayerMask(true);
        }

        @Override
        public void setMaskViewMode(Keyboard keyboard) {
            keyboard.pressCtrlTwo();
        }
    },
    /**
     * A mask is tested, while viewing the layer and a rubylith mask
     */
    RUBY(RUBYLITH) {
        @Override
        public void check() {
            checkMaskEditingInMode(RUBYLITH);
        }

        @Override
        public void configureActiveLayer(MainGuiTest tester) {
            tester.addLayerMask(true);
        }

        @Override
        public void setMaskViewMode(Keyboard keyboard) {
            keyboard.pressCtrlFour();
        }
    };

    private static void checkMaskEditingInMode(MaskViewMode expectedMode) {
        EDT.assertActiveLayerHasMask();
        EDT.assertActiveLayerIsMaskEditing();
        EDT.assertMaskViewModeIs(expectedMode);
    }

    private final MaskViewMode viewMode;

    MaskMode(MaskViewMode viewMode) {
        this.viewMode = viewMode;
    }

    public boolean isMaskEditing() {
        return viewMode.editMask();
    }

    /**
     * Checks that the testing mode is set on the active layer
     */
    public abstract void check();

    /**
     * Make sure that the testing more is set on the active layer
     */
    public void apply(MainGuiTest tester) {
        configureActiveLayer(tester);
        setMaskViewMode(tester.keyboard());
        tester.checkConsistency();
    }

    abstract void configureActiveLayer(MainGuiTest tester);

    abstract void setMaskViewMode(Keyboard keyboard);

    public static MaskMode[] load() {
        String maskMode = System.getProperty("mask.mode");
        if (maskMode == null || maskMode.equalsIgnoreCase("all")) {
//            Collections.shuffle(Arrays.asList(usedMaskModes));
            return values();
        }

        maskMode = maskMode.toUpperCase(Locale.ENGLISH);
        MaskMode[] usedMaskModes;
        // if a specific test mode was configured, test only that
        MaskMode mode = null;
        try {
            mode = valueOf(maskMode);
        } catch (IllegalArgumentException e) {
            String msg = "Mask mode " + maskMode + " not found.\n" +
                "Available mask modes: " + Arrays.toString(values());
            System.err.println(msg);
            System.exit(1);
        }
        usedMaskModes = new MaskMode[]{mode};
        return usedMaskModes;
    }
}
