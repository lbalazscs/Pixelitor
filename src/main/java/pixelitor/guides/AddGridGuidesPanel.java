/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.GroupedRangeParam;
import pixelitor.filters.gui.ParamAdjustmentListener;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.SOUTH;
import static java.awt.FlowLayout.LEFT;
import static java.lang.String.format;

class AddGridGuidesPanel extends JPanel {
    private final GroupedRangeParam guidesParam = new GroupedRangeParam(
            "Guides", 0, 3, 50, false);
    private final Guides.Builder builder;

    public AddGridGuidesPanel(Guides.Builder builder) {
        this.builder = builder;
        setLayout(new BorderLayout());
        add(guidesParam.createGUI(), CENTER);

        var southPanel = new JPanel(new FlowLayout(LEFT));
        BooleanParam clearExisting = builder.getClearExisting();
        southPanel.add(new JLabel(clearExisting.getName() + ": "));
        southPanel.add(clearExisting.createGUI());
        add(southPanel, SOUTH);

        ParamAdjustmentListener updatePreview = () -> createGuides(true);
        guidesParam.setAdjustmentListener(updatePreview);
        builder.setAdjustmentListener(updatePreview);
        updatePreview.paramAdjusted(); // set initial preview
    }

    public void createGuides(boolean preview) {
        builder.build(preview, this::setup);
    }

    private void setup(Guides guides) {
        int horDivisions = getNumHorDivisions();
        int verDivisions = getNumVerDivisions();
        guides.addRelativeGrid(horDivisions, verDivisions);
        guides.setName(format("horDivisions = %d, verDivisions = %d%n", horDivisions, verDivisions));
    }

    private int getNumHorDivisions() {
        return guidesParam.getValue(0) + 1;
    }

    private int getNumVerDivisions() {
        return guidesParam.getValue(1) + 1;
    }
}
