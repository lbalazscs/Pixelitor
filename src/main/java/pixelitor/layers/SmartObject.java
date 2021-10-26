/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.layers;

import pixelitor.Composition;
import pixelitor.OpenImages;
import pixelitor.filters.Filter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.FilterState;
import pixelitor.gui.View;
import pixelitor.gui.utils.PAction;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Utils;
import pixelitor.utils.debug.CompositionNode;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import static pixelitor.utils.Keys.CTRL_SHIFT_E;

/**
 * A "smart object" that contains an embedded composition and allows "smart filters".
 * The cached result image behaves like the image of a regular {@link ImageLayer}.
 */
public class SmartObject extends ImageLayer {
    private static final String NAME_PREFIX = "smart ";
    private Composition content;
    private boolean smartFilterIsVisible = true;

    // only used for deserialization
    private boolean newVersion = true;

    @Serial
    private static final long serialVersionUID = 8594248957749192719L;

    /**
     * Smart filters allow non-destructive editing, but unlike adjustment layers,
     * they don't have to run every time some other layer is edited.
     */
    private final List<Filter> smartFilters = new ArrayList<>();

    // The following two fields are used only during editing of smart filters
    // to restore the image and filter state if the user cancels the dialog.
    private transient BufferedImage lastFilterOutput;
    private transient FilterState lastFilterState;

    private int indexOfLastSmartFilter = -1;

    private transient boolean imageNeedsRefresh = false;

    public SmartObject(Layer layer) {
        super(layer.getComp(), NAME_PREFIX + layer.getName());

        content = Composition.createEmpty(comp.getCanvasWidth(), comp.getCanvasHeight(), comp.getMode());
        // the mask stays outside the content, and will become the mask of the smart object
        Layer contentLayer = layer.duplicate(true, false);
        contentLayer.setName("original content", false);
        contentLayer.setComp(content);
        content.addLayerInInitMode(contentLayer);
        content.setName(getName());
        content.setOwner(this);
        copyBlendingFrom(layer);

        recalculateImage(false);
    }

    // copy constructor
    private SmartObject(SmartObject orig, String name) {
        super(orig.comp, name);
        content = orig.content.copy(false, true);
        content.setOwner(this);
        image = orig.image;
        if (!orig.smartFilters.isEmpty()) {
            smartFilters.add(orig.smartFilters.get(0).copy());
        }
        lastFilterState = orig.lastFilterState;
        lastFilterOutput = orig.lastFilterOutput;
        indexOfLastSmartFilter = orig.indexOfLastSmartFilter;
        smartFilterIsVisible = orig.smartFilterIsVisible;
    }

    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        if (!newVersion) {
            // if the pxc was saved with an old version,
            // then assume smart filter visibility
            smartFilterIsVisible = true;
        }
        imageNeedsRefresh = true;
        recalculateImage(false);
        lastFilterOutput = null;
        lastFilterState = null;
    }

    @Override
    boolean serializeImage() {
        return false;
    }

    private void recalculateImage(boolean updateIcon) {
        resetImageFromContent();
        if (smartFilterIsVisible) {
            for (Filter filter : smartFilters) {
                image = filter.transformImage(image);
            }
        }
        if (updateIcon) {
            updateIconImage();
        }
        imageNeedsRefresh = false;
    }

    private void resetImageFromContent() {
        image = content.getCompositeImage();
        if (ImageUtils.isSubImage(image)) {
            image = ImageUtils.copySubImage(image);
        }
    }

    public void invalidateImageCache() {
        imageNeedsRefresh = true;
    }

    public void contentDeactivated(Composition content) {
        // the reference might have been changed during editing
        this.content = content;

        if (!content.isDirty()) {
            return;
        }

        invalidateImageCache();
        comp.smartObjectChanged();

        // as long as there is no undo, it's necessary to manually set this
        comp.setDirty(true);
    }

    @Override
    public BufferedImage getVisibleImage() {
        if (imageNeedsRefresh) {
            recalculateImage(true);
        }
        return super.getVisibleImage();
    }

    @Override
    protected boolean isSmartObject() {
        return true;
    }

    @Override
    public boolean isRasterizable() {
        return true;
    }

    @Override
    protected String getRasterizedName() {
        return Utils.removePrefix(name, NAME_PREFIX);
    }

    @Override
    protected Layer createTypeSpecificDuplicate(String duplicateName) {
        return new SmartObject(this, duplicateName);
    }

    void addSmartObjectSpecificItems(JPopupMenu popup) {
        popup.add(new PAction("Edit Contents") {
            @Override
            public void onClick() {
                edit();
            }
        });
        if (!smartFilters.isEmpty()) {
            JMenu filtersMenu = new JMenu("Smart Filters");
            for (int i = 0; i < smartFilters.size(); i++) {
                int finalI = i;
                Filter smartFilter = smartFilters.get(i);
                PAction editAction = new PAction("Edit " + smartFilter.getName()) {
                    @Override
                    public void onClick() {
                        editSmartFilter(finalI);
                    }
                };

                var editMenuItem = new JMenuItem(editAction);
                editMenuItem.setAccelerator(CTRL_SHIFT_E);
                filtersMenu.add(editMenuItem);

                filtersMenu.add(new PAction("Delete " + smartFilter.getName()) {
                    @Override
                    public void onClick() {
                        deleteSmartFilter();
                    }
                });
            }
            popup.add(filtersMenu);
        }
    }

    @Override
    public void edit() {
        View contentView = content.getView();
        if (contentView == null) {
            OpenImages.addAsNewComp(content);
            content.setDirty(false);
        } else {
            OpenImages.setActiveView(contentView, true);
        }
    }

    public boolean hasSmartFilters() {
        return !smartFilters.isEmpty();
    }

    public boolean smartFilterIsVisible() {
        return smartFilterIsVisible;
    }

    public void setSmartFilterVisibility(boolean b) {
        boolean changed = smartFilterIsVisible != b;
        smartFilterIsVisible = b;
        if (changed) {
            recalculateImage(true);
            comp.update();
        }
    }

    public Filter getSmartFilter(int index) {
        return smartFilters.get(index);
    }

    public void addSmartFilter(Filter filter) {
        smartFilters.add(filter);
        smartFilterIsVisible = true;
        updateSmartFilterUI();
    }

    public void replaceSmartFilter(Filter newFilter) {
        Filter prevFilter = smartFilters.get(0);
        lastFilterOutput = image;
        smartFilters.clear();
        resetImageFromContent();
        boolean filterDialogAccepted = newFilter.startOn(this, false);
        if (filterDialogAccepted) {
            lastFilterOutput.flush();
            lastFilterOutput = null;
            addSmartFilter(newFilter);
        } else {
            image = lastFilterOutput;
            addSmartFilter(prevFilter);
        }
    }

    public void editSmartFilter(int index) {
        indexOfLastSmartFilter = index;
        Filter filter = smartFilters.get(index);
        lastFilterOutput = image;
        if (filter instanceof ParametrizedFilter pf) {
            lastFilterState = pf.getParamSet().copyState(false);
        }
        resetImageFromContent();
        boolean filterDialogAccepted = filter.startOn(this, false);
        if (filterDialogAccepted) {
            // these are no longer needed
            lastFilterOutput.flush();
            lastFilterOutput = null;
            lastFilterState = null;
        } else {
            // restore to the result of the previous run
            image = lastFilterOutput;
            lastFilterOutput = null;
            // restore the previous filter state
            Filter sf = smartFilters.get(indexOfLastSmartFilter);
            if (sf instanceof ParametrizedFilter pf) {
                pf.getParamSet().setState(lastFilterState, false);
                lastFilterState = null;
            }
        }
        updateSmartFilterUI();
    }

    private void deleteSmartFilter() {
        smartFilters.clear();
        resetImageFromContent();
        comp.update();
        updateIconImage();
        updateSmartFilterUI();
    }

    private void updateSmartFilterUI() {
        ui.updateSmartFilterPanel();
        EventQueue.invokeLater(() ->
            comp.getView().getLayersPanel().revalidate());
    }

    @Override
    public BufferedImage getCanvasSizedSubImage() {
        // workaround for moved layers
        BufferedImage img = ImageUtils.createSysCompatibleImage(comp.getCanvas());
        Graphics2D g = img.createGraphics();

        // don't call applyLayer, because the mask should NOT be considered
        setupDrawingComposite(g, true);
        paintLayerOnGraphics(g, true);

        g.dispose();
        return img;
    }

    @Override
    public String getTypeString() {
        return "Smart Object";
    }

    @Override
    public DebugNode createDebugNode(String descr) {
        DebugNode node = super.createDebugNode(descr);

        node.add(new CompositionNode("content", content));

        return node;
    }
}
