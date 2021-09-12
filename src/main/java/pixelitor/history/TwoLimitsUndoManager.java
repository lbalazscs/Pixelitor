package pixelitor.history;

import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;
import java.util.Vector;


public class TwoLimitsUndoManager extends UndoManager {

    private int heavyEditLimit;
    private int lightEditLimit;

    public TwoLimitsUndoManager() {
        this(64, 256);
    }

    public TwoLimitsUndoManager(int heavyEditLimit, int lightEditLimit) {
        setHeavyEditLimit(heavyEditLimit);
        setLightEditLimit(lightEditLimit);
        int limit = getLimit();
        edits.ensureCapacity(limit);
        super.setLimit(limit);
    }

    @Override
    public synchronized int getLimit() {
        return getHeavyEditLimit() + getLightEditLimit();
    }

    public synchronized int getHeavyEditLimit() {
        return heavyEditLimit;
    }

    public synchronized int getLightEditLimit() {
        return lightEditLimit;
    }

    @Override
    public synchronized void setLimit(int lightEditLimit) {
        setHeavyEditLimit(lightEditLimit);
        super.setLimit(getLimit());
    }

    public synchronized void setHeavyEditLimit(int heavyEditLimit) {
        this.heavyEditLimit = heavyEditLimit;
    }

    public synchronized void setLightEditLimit(int lightEditLimit) {
        this.lightEditLimit = lightEditLimit;
    }

    @Override
    protected void trimForLimit() {

        super.trimForLimit();

        int extraHeavyEdits = getHeavyEditCount() - heavyEditLimit;
        if (extraHeavyEdits > 0) {
            for (int i = 0, c = 0; i < edits.size(); i++) {
                PixelitorEdit edit = (PixelitorEdit) edits.get(i);
                if (edit.isHeavy()) {
                    c++;
                }
                if (c == extraHeavyEdits) {
                    trimEdits(0, i);
                    break;
                }
            }
        }

        int extraLightEdits = getLightEditCount() - lightEditLimit;
        if (extraLightEdits > 0) {
            for (int i = 0, c = 0; i < edits.size(); i++) {
                PixelitorEdit edit = (PixelitorEdit) edits.get(i);
                if (!edit.isHeavy()) {
                    c++;
                }
                if (c == extraLightEdits) {
                    trimEdits(0, i);
                    break;
                }
            }
        }
    }

    public int getHeavyEditCount() {
        int count = 0;
        for (UndoableEdit edit : edits) {
            if (((PixelitorEdit) edit).isHeavy()) {
                count++;
            }
        }
        return count;
    }

    public int getLightEditCount() {
        int count = 0;
        for (UndoableEdit edit : edits) {
            if (!((PixelitorEdit) edit).isHeavy()) {
                count++;
            }
        }
        return count;
    }

    public Vector<UndoableEdit> cookAMeal() {
        return edits;
    }

    public int getSize() {
        return edits.size();
    }
}
