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

import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.GroupedRangeParam;
import pixelitor.filters.gui.ParamAdjustmentListener;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

class AddGridGuidesPanel extends JPanel {
    private final GroupedRangeParam divisions = new GroupedRangeParam(
            "Divisions", 1, 4, 50, false);
    private final Guides.Builder builder;

    public AddGridGuidesPanel(Guides.Builder builder) {
        this.builder = builder;
        setLayout(new BorderLayout());
        add(divisions.createGUI(), BorderLayout.CENTER);

        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        BooleanParam clearExisting = builder.getClearExisting();
        southPanel.add(new JLabel(clearExisting.getName() + ": "));
        southPanel.add(clearExisting.createGUI());
        add(southPanel, BorderLayout.SOUTH);

        ParamAdjustmentListener updatePreview = () -> createGuides(true);
        divisions.setAdjustmentListener(updatePreview);
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
        guides.setName(String.format("horDivisions = %d, verDivisions = %d%n", horDivisions, verDivisions));
    }

    private int getNumHorDivisions() {
        return divisions.getValue(0);
    }

    private int getNumVerDivisions() {
        return divisions.getValue(1);
    }
}
