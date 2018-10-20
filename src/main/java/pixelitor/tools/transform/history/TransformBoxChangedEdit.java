package pixelitor.tools.transform.history;

import pixelitor.Composition;
import pixelitor.history.PixelitorEdit;
import pixelitor.tools.transform.TransformBox;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import static pixelitor.Composition.ImageChangeActions.REPAINT;

public class TransformBoxChangedEdit extends PixelitorEdit {
    private final TransformBox box;
    private final TransformBox.Memento before;
    private final TransformBox.Memento after;
    private final boolean simpleRepaint;

    public TransformBoxChangedEdit(Composition comp, TransformBox box,
                                   TransformBox.Memento before,
                                   TransformBox.Memento after,
                                   boolean simpleRepaint) {
        super("Transform Box Changed", comp);
        this.box = box;
        this.before = before;
        this.after = after;
        this.simpleRepaint = simpleRepaint;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        box.restoreFrom(before);

        updateGUI();
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        box.restoreFrom(after);

        updateGUI();
    }

    private void updateGUI() {
        if(simpleRepaint) {
            comp.repaint();
        } else {
            comp.imageChanged(REPAINT);
        }
    }
}
