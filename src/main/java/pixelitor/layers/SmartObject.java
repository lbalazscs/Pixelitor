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
import pixelitor.gui.utils.PAction;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Utils;
import pixelitor.utils.debug.CompositionNode;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
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

    public SmartObject(Layer layer) {
        super(layer.getComp(), NAME_PREFIX + layer.getName());

        content = Composition.createEmpty(comp.getCanvasWidth(), comp.getCanvasHeight(), comp.getMode());
        Layer contentLayer = layer.duplicate(true);
        contentLayer.setName("original content", false);
        contentLayer.setComp(content);
        content.addLayerInInitMode(contentLayer);
        content.setName(getName());
        content.setOwner(this);
        copyBlendingFrom(layer);

        recalculateImage();
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
    }

    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        recalculateImage();
        lastFilterOutput = null;
        lastFilterState = null;
    }

    @Override
    boolean serializeImage() {
        return false;
    }

    private void recalculateImage() {
        resetImageFromContent();
        for (Filter filter : smartFilters) {
            image = filter.transformImage(image);
        }
    }

    private void resetImageFromContent() {
        image = content.getCompositeImage();
        if (ImageUtils.isSubImage(image)) {
            image = ImageUtils.copySubImage(image);
        }
    }

    public void contentClosed(Composition content) {
        // the reference might have been changed during editing
        this.content = content;

        recalculateImage();
        comp.update();
        updateIconImage();
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
            for (Filter smartFilter : smartFilters) {
                PAction editAction = new PAction("Edit " + smartFilter.getName()) {
                    @Override
                    public void onClick() {
                        editSmartFilter(smartFilter);
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
        OpenImages.addAsNewComp(content);
    }

    public boolean hasSmartFilters() {
        return !smartFilters.isEmpty();
    }

    public Filter getSmartFilter(int index) {
        return smartFilters.get(index);
    }

    public void addSmartFilter(Filter filter) {
        smartFilters.add(filter);
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

    public void editSmartFilter(Filter filter) {
        indexOfLastSmartFilter = smartFilters.indexOf(filter);
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
    }

    private void deleteSmartFilter() {
        smartFilters.clear();
        resetImageFromContent();
        comp.update();
        updateIconImage();
    }

    @Override
    public BufferedImage getCanvasSizedSubImage() {
        // workaround for moved layers
        BufferedImage img = ImageUtils.createSysCompatibleImage(comp.getCanvas());
        Graphics2D g = img.createGraphics();
        applyLayer(g, null, true);
        g.dispose();
        return img;
    }

    @Override
    public String getTypeStringLC() {
        return "smart object";
    }

    @Override
    public String getTypeStringUC() {
        return "Smart Object";
    }

    @Override
    public DebugNode createDebugNode(String descr) {
        DebugNode node = super.createDebugNode(descr);

        node.add(new CompositionNode("content", content));

        return node;
    }
}
