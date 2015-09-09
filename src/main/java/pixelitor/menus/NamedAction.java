package pixelitor.menus;

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
}
