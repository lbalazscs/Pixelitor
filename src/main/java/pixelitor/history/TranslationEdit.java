package pixelitor.history;

import pixelitor.Composition;
import pixelitor.layers.ContentLayer;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import static pixelitor.Composition.ImageChangeActions.FULL;

/**
 * Saves and restores the translation of an ImageLayer.
 */
public class TranslationEdit extends PixelitorEdit {
    private ContentLayer layer;
    private int backupTranslationX = 0;
    private int backupTranslationY = 0;

    public TranslationEdit(Composition comp, ContentLayer layer) {
        this(comp, layer, layer.getTranslationX(), layer.getTranslationY());
    }

    public TranslationEdit(Composition comp, ContentLayer layer, int oldTranslationX, int oldTranslationY) {
        super(comp, "");
        this.layer = layer;

        this.backupTranslationX = oldTranslationX;
        this.backupTranslationY = oldTranslationY;

        // currently always embedded
        embedded = true;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        swapTranslation();
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        swapTranslation();
    }

    private void swapTranslation() {
        int tmpX = layer.getTranslationX();
        int tmpY = layer.getTranslationY();

        layer.setTranslation(backupTranslationX, backupTranslationY);
        backupTranslationX = tmpX;
        backupTranslationY = tmpY;

        if (!embedded) {
            layer.getComp().imageChanged(FULL);
            History.notifyMenus(this);
        }
    }

    @Override
    public void die() {
        super.die();

        layer = null;
    }

    @Override
    public boolean canRepeat() {
        return false;
    }
}
