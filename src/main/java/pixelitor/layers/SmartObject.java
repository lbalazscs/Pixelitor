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
import pixelitor.history.History;
import pixelitor.history.ReplaceLayerEdit;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Messages;
import pixelitor.utils.Utils;
import pixelitor.utils.debug.DebugNode;
import pixelitor.utils.debug.LayerNode;
import pixelitor.utils.debug.SmartObjectNode;

import javax.swing.*;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
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

    // the image shown before a smart filter was edited
    private transient BufferedImage lastFilterOutput;

    // the state of the edited smart filter, saved before editing
    private transient FilterState lastFilterState;

    private int indexOfLastSmartFilter = -1;

    public SmartObject(ImageLayer imageLayer) {
        super(imageLayer.getComp(), NAME_PREFIX + imageLayer.getName());

        content = Composition.createEmpty(comp.getCanvasWidth(), comp.getCanvasHeight(), comp.getMode());
        Layer contentLayer = imageLayer.duplicate(true);
        contentLayer.setName("original content", false);
        contentLayer.setComp(content);
        content.addLayerInInitMode(contentLayer);
        content.setName(getName());
        content.setOwner(this);
        recalculateImage();

        copyBlendingFrom(imageLayer);
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

    private void recalculateImage() {
        image = content.getCompositeImage();
        for (Filter filter : smartFilters) {
            image = filter.transformImage(image);
        }
    }

    public void contentClosed(Composition content) {
        // the reference might have been changed during editing
        this.content = content;

        recalculateImage();
        comp.update();
        updateIconImage();
    }

    public ImageLayer replaceWithRasterized() {
        ImageLayer rasterized = new ImageLayer(comp, image, Utils.removePrefix(name, NAME_PREFIX));
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
        popup.add(new PAction("Edit Contents") {
            @Override
            public void onClick() {
                OpenImages.addAsNewComp(content);
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
        image = content.getCompositeImage();
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
        image = content.getCompositeImage();
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
        image = content.getCompositeImage();
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
    public DebugNode createDebugNode(String description) {
        return new SmartObjectNode(LayerNode.descrToName(description, this), this);
    }
}
