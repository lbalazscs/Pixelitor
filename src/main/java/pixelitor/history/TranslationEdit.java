package pixelitor.history;

import pixelitor.Composition;
import pixelitor.layers.ContentLayer;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import static pixelitor.Composition.ImageChangeActions.FULL;

/**
 * Saves and restores the translation of a ContentLayer.
 */
public class TranslationEdit extends PixelitorEdit {
    private ContentLayer layer;
    private int backupTX = 0;
    private int backupTY = 0;

    /**
     * This constructor must be called before the change
     */
    public TranslationEdit(Composition comp, ContentLayer layer) {
        this(comp, layer, layer.getTX(), layer.getTY());
    }

    /**
     * This constructor can be called after the change
     */
    public TranslationEdit(Composition comp, ContentLayer layer, int oldTX, int oldTY) {
        super(comp, "");
        this.layer = layer;

        this.backupTX = oldTX;
        this.backupTY = oldTY;

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
        int tmpTX = layer.getTX();
        int tmpTY = layer.getTY();

        layer.setTranslation(backupTX, backupTY);
        backupTX = tmpTX;
        backupTY = tmpTY;

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
