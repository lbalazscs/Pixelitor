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

package pixelitor.history;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pixelitor.TestHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TwoLimitsUndoManagerTest {

    private TwoLimitsUndoManager undoManager;

    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    @BeforeEach
    void beforeEachTest() {
        undoManager = new TwoLimitsUndoManager(3, 8);

        assertThat(undoManager.getHeavyEditLimit()).isEqualTo(3);
        assertThat(undoManager.getLightEditLimit()).isEqualTo(8);
        assertThat(undoManager.getLimit()).isEqualTo(3 + 8);
    }

    @Test
    void fillingUpUndoManager() {
        fillUpUM(3, 8);
        assertThat(undoManager.getSize()).isEqualTo(3 + 8);
        assertThat(undoManager.getHeavyEditLimit()).isEqualTo(3);
        assertThat(undoManager.getLightEditLimit()).isEqualTo(8);
    }

    @Test
    void overFillingHeavyEditsUndoManager() {
        fillUpUM(3, 5);

        undoManager.addEdit(createMockEdit(true));
        assertThat(undoManager.getSize()).isEqualTo(8);
        assertThat(undoManager.getHeavyEditsCount()).isEqualTo(3);
        assertThat(undoManager.getLightEditsCount()).isEqualTo(5);

        undoManager.addEdit(createMockEdit(true));
        assertThat(undoManager.getSize()).isEqualTo(8);
        assertThat(undoManager.getHeavyEditsCount()).isEqualTo(3);
        assertThat(undoManager.getLightEditsCount()).isEqualTo(5);

        undoManager.addEdit(createMockEdit(true));
        assertThat(undoManager.getSize()).isEqualTo(8);
        assertThat(undoManager.getHeavyEditsCount()).isEqualTo(3);
        assertThat(undoManager.getLightEditsCount()).isEqualTo(5);

        undoManager.addEdit(createMockEdit(true));
        assertThat(undoManager.getSize()).isEqualTo(3);
        assertThat(undoManager.getHeavyEditsCount()).isEqualTo(3);
        assertThat(undoManager.getLightEditsCount()).isEqualTo(0); // Important - this time all light edits in between were removed!

    }

    @Test
    void overFillingLightEditsUndoManager() {
        fillUpUM(0, 8);
        fillUpUM(2, 0);

        // adding in 8 new light edits will only cause the first 8 edits in undo manager to be removed
        for (int i = 0; i < 8; i++) {
            undoManager.addEdit(createMockEdit(false));
            assertThat(undoManager.getSize()).isEqualTo(10);
            assertThat(undoManager.getHeavyEditsCount()).isEqualTo(2);
            assertThat(undoManager.getLightEditsCount()).isEqualTo(8);
        }

        // now, the first 2 edits in the vector are heavy edits. Adding in one more light edit will cause those 2 and a light next to be removed

        undoManager.addEdit(createMockEdit(false));
        assertThat(undoManager.getSize()).isEqualTo(8);
        assertThat(undoManager.getHeavyEditsCount()).isEqualTo(0);
        assertThat(undoManager.getLightEditsCount()).isEqualTo(8);

    }

    private void fillUpUM(int he, int le) {
        for (int i = 0; i < he; i++) {
            undoManager.addEdit(createMockEdit(true));
        }

        for (int i = 0; i < le; i++) {
            undoManager.addEdit(createMockEdit(false));
        }
    }

    static int counter = 1;
    static int counterH = 1;
    static int counterL = 1;

    private static PixelitorEdit createMockEdit(boolean isHeavy) {
        PixelitorEdit edit = mock(PixelitorEdit.class);
        String name = (counter++) + (isHeavy ? (" Heavy " + (counterH++)) : (" Light " + (counterL++)));

        when(edit.replaceEdit(any())).thenReturn(false);
        when(edit.addEdit(any())).thenReturn(false);
        when(edit.getName()).thenReturn(name);
        when(edit.isSignificant()).thenReturn(true);
        when(edit.isHeavy()).thenReturn(isHeavy);
        when(edit.canUndo()).thenReturn(true);
        when(edit.canRedo()).thenReturn(true);

        return edit;
    }

}