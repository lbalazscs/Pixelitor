/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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

import org.junit.jupiter.api.*;
import pixelitor.TestHelper;

import javax.swing.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("PixelitorUndoManager tests")
@TestMethodOrder(MethodOrderer.Random.class)
class PixelitorUndoManagerTest {
    private PixelitorUndoManager undoManager;
    private PixelitorEdit edit0;
    private PixelitorEdit edit1;
    private PixelitorEdit edit2;

    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    @BeforeEach
    void beforeEachTest() {
        undoManager = new PixelitorUndoManager();

        edit0 = createMockEdit("edit 0");
        edit1 = createMockEdit("edit 1");
        edit2 = createMockEdit("edit 2");

        undoManager.addEdit(edit0);
        undoManager.addEdit(edit1);
        undoManager.addEdit(edit2);

        assertThat(undoManager.getSize()).isEqualTo(3);
        assertThat(undoManager.getSelectedIndex()).isEqualTo(2);
    }

    @Test
    void jumpingOneStepBySelection() {
        selectIndex(1);

        // expect that only the last edit is undone
        verify(edit0, never()).undo();
        verify(edit1, never()).undo();
        verify(edit2, times(1)).undo();

        selectIndex(2);

        // expect that only the last edit is redone
        verify(edit0, never()).redo();
        verify(edit1, never()).redo();
        verify(edit2, times(1)).redo();
    }

    @Test
    void jumpingTwoStepsBySelection() {
        selectIndex(0);

        // expect that only the last two edits are undone
        verify(edit0, never()).undo();
        verify(edit1, times(1)).undo();
        verify(edit2, times(1)).undo();

        selectIndex(2);

        // expect that only the last two edits are redone
        verify(edit0, never()).redo();
        verify(edit1, times(1)).redo();
        verify(edit2, times(1)).redo();
    }

    @Test
    void undoingAndRedoingEverythingAdjustsSelection() {
        assertThat(undoManager.getSelectedIndex()).isEqualTo(2);

        // first undo
        assertThat(undoManager.canUndo()).isTrue();
        undoManager.undo();
        assertThat(undoManager.getSelectedIndex()).isEqualTo(1);

        // second undo
        assertThat(undoManager.canUndo()).isTrue();
        undoManager.undo();
        assertThat(undoManager.getSelectedIndex()).isEqualTo(0);

        // last undo
        assertThat(undoManager.canUndo()).isTrue();
        undoManager.undo();
        // expect no selection
        assertThat(undoManager.getSelectedIndex()).isEqualTo(-1);

        // check that no more undo is possible
        assertThat(undoManager.canUndo()).isFalse();

        // first redo
        assertThat(undoManager.canRedo()).isTrue();
        undoManager.redo();
        assertThat(undoManager.getSelectedIndex()).isEqualTo(0);

        // second redo
        assertThat(undoManager.canRedo()).isTrue();
        undoManager.redo();
        assertThat(undoManager.getSelectedIndex()).isEqualTo(1);

        // third redo
        assertThat(undoManager.canRedo()).isTrue();
        undoManager.redo();
        assertThat(undoManager.getSelectedIndex()).isEqualTo(2);

        // check that no more redo is possible
        assertThat(undoManager.canRedo()).isFalse();
    }

    @Test
    void undoEverythingThenSelect() {
        undoManager.undo();
        undoManager.undo();
        undoManager.undo();
        // expect no selection
        assertThat(undoManager.getSelectedIndex()).isEqualTo(-1);

        // now that we have no selection, select the middle one,
        // and expect redos to happen on the first two edits
        selectIndex(1);
        verify(edit0, times(1)).redo();
        verify(edit1, times(1)).redo();
        verify(edit2, never()).redo();
    }

    private void selectIndex(int index) {
        ListSelectionModel selectionModel = undoManager.getSelectionModel();
        selectionModel.setSelectionInterval(index, index);
    }

    private static PixelitorEdit createMockEdit(String name) {
        PixelitorEdit edit = mock(PixelitorEdit.class);

        when(edit.replaceEdit(any())).thenReturn(false);
        when(edit.addEdit(any())).thenReturn(false);
        when(edit.getName()).thenReturn(name);
        when(edit.isSignificant()).thenReturn(true);
        when(edit.canUndo()).thenReturn(true);
        when(edit.canRedo()).thenReturn(true);

        return edit;
    }

//    /**
//     * Newly added test case
//     */
//    @Test
//    void testAddEditAndEventFiring() {
//        PixelitorEdit edit = createMockEdit("new edit");
//        int originalSize = undoManager.getSize();
//
//        boolean result = undoManager.addEdit(edit);
//
//        assertThat(result).isTrue();
//        assertThat(undoManager.getSize()).isEqualTo(originalSize + 1);
//        assertThat(undoManager.getSelectedIndex()).isEqualTo(originalSize);
//        // Verify that the appropriate events were fired
//    }
//
//    @Test
//    void testUndoAndRedo() {
//        // Assuming undoManager is in a state where undo and redo are possible
//        int originalSize = undoManager.getSize();
//        int originalIndex = undoManager.getSelectedIndex();
//
//        // Test undo
//        undoManager.undo();
//        assertThat(undoManager.getSelectedIndex()).isEqualTo(originalIndex - 1);
//        // Verify that the last edit was undone and a status message was shown
//
//        // Test redo
//        undoManager.redo();
//        assertThat(undoManager.getSelectedIndex()).isEqualTo(originalIndex);
//        // Verify that the last edit was redone and a status message was shown
//    }


}
