package pixelitor.filters;

import com.jhlabs.image.AbstractBufferedImageOp;
import pixelitor.history.History;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.function.Supplier;

/**
 * An action that invokes a filter
 */
public class FilterAction extends AbstractAction {
    private final Supplier<Filter> filterSupplier;
    private Filter filter;
    private String listNamePrefix = null;
    private boolean noGUI = false;

    private String name;
    private String menuName;

    public FilterAction(String name, Supplier<Filter> filterSupplier) {
        assert name != null;
        assert filterSupplier != null;

        this.name = name;
        this.filterSupplier = filterSupplier;

        menuName = name + "...";
        putValue(Action.NAME, menuName);

        FilterUtils.addFilter(this);
    }

    public FilterAction(String name, AbstractBufferedImageOp op) {
        this(name, () -> new SimpleForwardingFilter(op));
    }

    public String getMenuName() {
        return menuName;
    }

    public String getName() {
        return name;
    }

    /**
     * This name appears when all filters are listed in a combo box
     * For example "Fill with Transparent" is better than "Transparent" in this case
     */
    public String getListName() {
        if (listNamePrefix == null) {
            return name;
        } else {
            return listNamePrefix + name;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        createFilter();

        filter.execute();
    }

    private void createFilter() {
        if (filter == null) {
            filter = filterSupplier.get();
            filter.setFilterAction(this);
        }
    }

    public Filter getFilter() {
        createFilter();
        return filter;
    }

    public FilterAction withListNamePrefix(String listNamePrefix) {
        this.listNamePrefix = listNamePrefix;
        return this;
    }

    public FilterAction withFillListName() {
        return withListNamePrefix("Fill with ");
    }

    public FilterAction withExtractChannelListName() {
        return withListNamePrefix("Extract Channel: ");
    }

    public FilterAction withoutGUI() {
        noGUI = true;
        menuName = name; // without the "..."
        putValue(Action.NAME, menuName);

        return this;
    }

    public boolean isAnimationFilter() {
        if (noGUI) {
            return false;
        }

        createFilter();
        if (!(filter instanceof FilterWithParametrizedGUI)) {
            return false;
        }
        FilterWithParametrizedGUI fpg = (FilterWithParametrizedGUI) filter;
        if (fpg.excludeFromAnimation()) {
            return false;
        }
        if (!fpg.getParamSet().canBeAnimated()) {
            return false;
        }
        if (filter instanceof Fade) {
            if (!History.canFade()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FilterAction filterAction = (FilterAction) o;

        if (!getName().equals(filterAction.getName())) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }


    @Override
    public String toString() {
        return getListName();
    }
}
