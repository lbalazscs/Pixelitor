package pixelitor.history;

import pixelitor.Composition;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * A PixelitorEdit that represents two edits
 * that need to be undone/redone together
 */
public class CompoundEdit extends PixelitorEdit {
    private final PixelitorEdit first;
    private final PixelitorEdit second;

    public CompoundEdit(Composition comp, String name, PixelitorEdit first, PixelitorEdit second) {
        super(comp, name);
        this.first = first;
        this.second = second;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        second.undo();
        first.undo();

        History.notifyMenus(this);
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        first.redo();
        second.redo();

        History.notifyMenus(this);
    }

    @Override
    public void die() {
        super.die();

        comp = null;
        first.die();
        second.die();
    }

    @Override
    public boolean canRepeat() {
        return first.canRepeat() && second.canRepeat();
    }
}