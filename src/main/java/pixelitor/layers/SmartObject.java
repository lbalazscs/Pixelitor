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
import pixelitor.filters.Filter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.FilterState;
import pixelitor.gui.utils.PAction;
import pixelitor.history.History;
import pixelitor.history.ReplaceLayerEdit;
import pixelitor.utils.Messages;
import pixelitor.utils.debug.DebugNode;
import pixelitor.utils.debug.LayerNode;
import pixelitor.utils.debug.SmartObjectNode;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * A "smart object" that contains an embedded composition and allows "smart filters".
 * The cached result image behaves like the image of a regular {@link ImageLayer}.
 */
public class SmartObject extends ImageLayer {
    private final Composition content;

    /**
     * Smart filters allow non-destructive editing, but unlike adjustment layers,
     * they don't have to run every time some other layer is edited.
     */
    private final List<Filter> smartFilters = new ArrayList<>();

    // the image shown before a smart filter was edited
    private transient BufferedImage prevImage;

    // the state of the edited smart filter, saved before editing
    private transient FilterState lastFilterState;

    private int indexOfLastSmartFilter = -1;

    public SmartObject(ImageLayer imageLayer) {
        super(imageLayer.getComp(), imageLayer.getName());

        content = Composition.fromImage(imageLayer.getImage(), null, "smart object");
        recalculateImage();

        copyBlendingFrom(imageLayer);
    }

    // copy constructor
    private SmartObject(SmartObject orig, String name) {
        super(orig.comp, name);
        content = orig.content.copy(false, true);
        image = orig.image;
        if (!orig.smartFilters.isEmpty()) {
            // TODO copy the filters
            smartFilters.add(orig.smartFilters.get(0));
        }
        lastFilterState = orig.lastFilterState;
        prevImage = orig.prevImage;
        indexOfLastSmartFilter = orig.indexOfLastSmartFilter;
    }

    private void recalculateImage() {
        image = content.getCompositeImage();
        for (Filter filter : smartFilters) {
            image = filter.transformImage(image);
        }
    }

    public ImageLayer replaceWithRasterized() {
        ImageLayer rasterized = new ImageLayer(comp, image, name);
        rasterized.copyBlendingFrom(this);
        History.add(new ReplaceLayerEdit(comp, this, rasterized, "Rasterize Smart Object"));
        comp.replaceLayer(this, rasterized);
        Messages.showInStatusBar(String.format(
            "The smart object <b>\"%s\"</b> was rasterized.", getName()));
        return rasterized;
    }

    @Override
    protected Layer createTypeSpecificDuplicate(String duplicateName) {
        return new SmartObject(this, duplicateName);
    }

    void addSmartObjectSpecificItems(JPopupMenu popup) {
        popup.add(new PAction("Rasterize") {
            @Override
            public void onClick() {
                replaceWithRasterized();
            }
        });
        if (!smartFilters.isEmpty()) {
            JMenu filtersMenu = new JMenu("Smart Filters");
            for (Filter smartFilter : smartFilters) {
                filtersMenu.add(new PAction("Edit " + smartFilter.getName()) {
                    @Override
                    public void onClick() {
                        editSmartFilter(smartFilter);
                    }
                });
            }
            popup.add(filtersMenu);
        }
    }

    public int getNumSmartFilters() {
        return smartFilters.size();
    }

    public Filter getSmartFilter(int index) {
        return smartFilters.get(index);
    }

    public void addSmartFilter(Filter filter) {
        smartFilters.add(filter);
    }

    private void editSmartFilter(Filter filter) {
        indexOfLastSmartFilter = smartFilters.indexOf(filter);
        prevImage = image;
        if (filter instanceof ParametrizedFilter pf) {
            lastFilterState = pf.getParamSet().copyState(false);
        }
        image = content.getCompositeImage();
        if (filter.startOn(this, false)) {
            // the filter dialog was accepted, these are no longer needed
            prevImage.flush();
            prevImage = null;
            lastFilterState = null;
        } else {
            // filter cancelled: restore to the result of the previous run
            image = prevImage;
            prevImage = null;
            // restore the previous filter state
            Filter sf = smartFilters.get(indexOfLastSmartFilter);
            if (sf instanceof ParametrizedFilter pf) {
                pf.getParamSet().setState(lastFilterState, false);
                lastFilterState = null;
            }
        }
    }

    @Override
    public DebugNode createDebugNode(String description) {
        return new SmartObjectNode(LayerNode.descrToName(description, this), this);
    }
}
