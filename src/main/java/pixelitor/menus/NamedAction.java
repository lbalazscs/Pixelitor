package pixelitor.menus;

import pixelitor.utils.VisibleForTesting;

import javax.swing.*;

/**
 * An action that can be simply renamed
 */
public abstract class NamedAction extends AbstractAction {
    protected NamedAction() {
    }

    protected NamedAction(String name) {
        super(name);
    }

    public void setName(String newName) {
        this.putValue(AbstractAction.NAME, newName);
    }

    @VisibleForTesting
    public String getName() {
        return (String) getValue(AbstractAction.NAME);
    }
}
