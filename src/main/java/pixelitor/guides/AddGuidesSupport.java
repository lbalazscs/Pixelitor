/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.guides;

import pixelitor.Composition;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.ParamAdjustmentListener;
import pixelitor.history.History;

import javax.swing.*;

/**
 * Code that can be reused for both single-line and grid guides.
 */
public class AddGuidesSupport {
    private final BooleanParam clearExisting;
    private final Composition comp;
    private final Guides oldGuides;

    public AddGuidesSupport(Composition comp, boolean clearByDefault) {
        this.comp = comp;
        oldGuides = comp.getGuides();
        clearExisting = new BooleanParam("Clear Existing Guides", clearByDefault);
    }

    public void set(Guides guides, boolean preview) {
        comp.setGuides(guides);
        comp.repaint();

        if (!preview) {
            History.addEdit(new GuidesChangeEdit(comp, oldGuides, guides));
        }
    }

    public void resetOldGuides() {
        comp.setGuides(oldGuides);
        comp.repaint();
    }

    public String getClearText() {
        return clearExisting.getName() + ": ";
    }

    public JComponent createClearCB() {
        return clearExisting.createGUI();
    }

    public BooleanParam getClearExisting() {
        return clearExisting;
    }

    public Guides createEmptyGuides() {
        Guides guides = new Guides(comp);
        boolean clearBefore = clearExisting.isChecked();
        if (!clearBefore && oldGuides != null) {
            guides.copyValuesFrom(oldGuides);
        }

        return guides;
    }

    public void setAdjustmentListener(ParamAdjustmentListener updatePreview) {
        clearExisting.setAdjustmentListener(updatePreview);
    }

    public Composition getComp() {
        return comp;
    }
}
