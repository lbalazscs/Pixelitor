package pixelitor.menus;

import com.jhlabs.image.AbstractBufferedImageOp;
import pixelitor.filters.Filter;
import pixelitor.filters.FilterAction;

import javax.swing.*;
import java.util.function.Supplier;

/**
 * A JMenu with some utility methods
 */
public class PMenu extends JMenu {
    public PMenu(String s) {
        super(s);
    }

    public PMenu(String s, char c) {
        super(s);
        setMnemonic(c);
    }

    /**
     * Simple add for non-filter actions, no builder is needed in the simplest case
     */
    public void addAction(Action action) {
        JMenuItem menuItem = EnabledIf.THERE_IS_OPEN_IMAGE.getMenuItem(action);
        add(menuItem);
    }

    /**
     * Maybe it is not worth creating a builder for this frequent case...
     */
    public void addActionWithKey(Action action, KeyStroke keyStroke) {
        JMenuItem menuItem = EnabledIf.THERE_IS_OPEN_IMAGE.getMenuItem(action);
        menuItem.setAccelerator(keyStroke);
        add(menuItem);
    }

    /**
     * Returns an action builder for non-filter actions
     */
    public ActionBuilder buildAction(Action action) {
        ActionBuilder builder = new ActionBuilder(this, action);
        return builder;
    }

    /**
     * Simple add for filter actions, no builder is needed in the simplest case
     */
    public void addFA(String name, Supplier<Filter> supplier) {
        FilterAction fa = new FilterAction(name, supplier);
        addFA(fa);
    }

    /**
     * Simple add for simple filters
     */
    public void addFA(String name, AbstractBufferedImageOp op) {
        FilterAction fa = new FilterAction(name, op);
        addFA(fa);
    }

    public void addFA(FilterAction fa) {
        JMenuItem menuItem = EnabledIf.THERE_IS_OPEN_IMAGE.getMenuItem(fa);
        add(menuItem);
    }

    /**
     * Returns a FilterAction builder
     */
    public FilterActionBuilder buildFA(String name, Supplier<Filter> supplier) {
        FilterAction fa = new FilterAction(name, supplier);
        return buildFA(fa);
    }

    public FilterActionBuilder buildFA(FilterAction fa) {
        FilterActionBuilder builder = new FilterActionBuilder(this, fa);
        return builder;
    }

    /**
     * Action builder for non-filter actions
     */
    public static class ActionBuilder {
        private final PMenu menu;
        protected final Action action;
        private KeyStroke keyStroke;
        private EnabledIf whenToEnable;

        public ActionBuilder(PMenu menu, Action action) {
            this.action = action;
            this.menu = menu;
        }

        public void add() {
            if (whenToEnable == null) {
                whenToEnable = EnabledIf.THERE_IS_OPEN_IMAGE;
            }
            JMenuItem menuItem = whenToEnable.getMenuItem(action);
            menu.add(menuItem);
            if (keyStroke != null) {
                menuItem.setAccelerator(keyStroke);
            }
        }

        public ActionBuilder withKey(KeyStroke keyStroke) {
            this.keyStroke = keyStroke;
            return this;
        }

        public ActionBuilder enableIf(EnabledIf whenToEnable) {
            this.whenToEnable = whenToEnable;
            return this;
        }
    }

    /**
     * Filter action builder
     */
    public static class FilterActionBuilder extends ActionBuilder {
        public FilterActionBuilder(PMenu menu, FilterAction action) {
            super(menu, action);
        }

        public FilterActionBuilder noGUI() {
            ((FilterAction) action).withoutGUI();
            return this;
        }

        public FilterActionBuilder withFillListName() {
            ((FilterAction) action).withFillListName();
            return this;
        }

        public FilterActionBuilder extract() {
            ((FilterAction) action).withExtractChannelListName();
            return this;
        }
    }
}
