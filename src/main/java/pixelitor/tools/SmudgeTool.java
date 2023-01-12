/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.tools;

import org.jdesktop.swingx.combobox.EnumComboBoxModel;
import pixelitor.colors.FgBgColors;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.UserPreset;
import pixelitor.gui.utils.SliderSpinner;
import pixelitor.gui.utils.VectorIcon;
import pixelitor.layers.Drawable;
import pixelitor.tools.brushes.*;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.tools.util.PPoint;
import pixelitor.utils.Cursors;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

import static java.awt.BasicStroke.CAP_ROUND;
import static java.awt.BasicStroke.JOIN_ROUND;
import static pixelitor.gui.utils.SliderSpinner.TextPosition.WEST;

/**
 * The smudge tool.
 */
public class SmudgeTool extends AbstractBrushTool {

    private EnumComboBoxModel<CopyBrushType> brushModel;

    public SmudgeTool() {
        super("Smudge", 'K',
            "<b>click and drag</b> to smudge. " +
            "<b>Click</b> and <b>Shift-click</b> to smudge along a line.",
            Cursors.HAND, false);

        drawDestination = DrawDestination.DIRECT;
    }

    private final RangeParam strengthParam = new RangeParam("Strength", 1, 60, 100);
    private SmudgeBrush smudgeBrush;
    private JCheckBox fingerPaintingCB;

    @Override
    protected void initBrushVariables() {
        smudgeBrush = new SmudgeBrush(getRadius(), CopyBrushType.HARD);
        affectedArea = new AffectedArea();
        brush = new AffectedAreaTracker(smudgeBrush, affectedArea);
    }

    @Override
    protected void updateLazyBrushEnabledState() {
        if (lazyMouseEnabled.isChecked()) {
            lazyMouseBrush = new LazyMouseBrush(smudgeBrush);
            brush = new AffectedAreaTracker(lazyMouseBrush, affectedArea);
            lazyMouse = true;
        } else {
            brush = new AffectedAreaTracker(smudgeBrush, affectedArea);
            lazyMouseBrush = null;
            lazyMouse = false;
        }
    }

    @Override
    public void initSettingsPanel() {
        brushModel = settingsPanel.addCopyBrushTypeSelector(
            CopyBrushType.HARD, smudgeBrush::typeChanged);

        addSizeSelector();
        addStrengthSelector();
        addFingerPaintingSelector();

        settingsPanel.addSeparator();

        addLazyMouseDialogButton();
    }

    private void addStrengthSelector() {
        SliderSpinner strengthSelector = new SliderSpinner(
            strengthParam, WEST, false);
        settingsPanel.add(strengthSelector);
    }

    private void addFingerPaintingSelector() {
        fingerPaintingCB = new JCheckBox();
        settingsPanel.addWithLabel("Finger Painting:", fingerPaintingCB, "fingerPaintingCB");
        fingerPaintingCB.setName("fingerPaintingCB");
        fingerPaintingCB.addActionListener(
            e -> smudgeBrush.setFingerPainting(fingerPaintingCB.isSelected()));
    }

    @Override
    public void mousePressed(PMouseEvent e) {
        Drawable dr = e.getComp().getActiveDrawableOrThrow();

        // We could also pass the full image and the translation
        // and the smudge brush could always adjust the last sampling point
        // with the translation.
        BufferedImage sourceImage = dr.getCanvasSizedSubImage();

        if (e.isShiftDown()) {
            // if we *start* with a shift-click, then we must initialize,
            // but otherwise don't re-initialize for shift-clicks
            if (!smudgeBrush.firstPointWasInitialized()) {
                initStroke(sourceImage, e);
            }
        } else {
            initStroke(sourceImage, e);
        }

        super.mousePressed(e);
    }

    private void initStroke(BufferedImage sourceImage, PPoint p) {
        smudgeBrush.setupFirstPoint(sourceImage, p, (float) strengthParam.getPercentage());
    }

    @Override
    protected Symmetry getSymmetry() {
        throw new UnsupportedOperationException("no symmetry");
    }

    @Override
    protected void prepareProgrammaticBrushStroke(Drawable dr, PPoint start) {
        super.prepareProgrammaticBrushStroke(dr, start);

        BufferedImage sourceImg = dr.getCanvasSizedSubImage();
        initStroke(sourceImg, start);
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        super.saveStateTo(preset);

        preset.put("Brush Type", brushModel.getSelectedItem().toString());
        strengthParam.saveStateTo(preset);
        preset.putBoolean("Finger Painting", fingerPaintingCB.isSelected());
        FgBgColors.saveStateTo(preset);
    }

    @Override
    public void loadUserPreset(UserPreset preset) {
        super.loadUserPreset(preset);

        brushModel.setSelectedItem(preset.getEnum("Brush Type", CopyBrushType.class));
        strengthParam.loadStateFrom(preset);
        fingerPaintingCB.setSelected(preset.getBoolean("Finger Painting"));
        FgBgColors.loadStateFrom(preset);
    }

    @Override
    public VectorIcon createIcon() {
        return new SmudgeToolIcon();
    }

    private static class SmudgeToolIcon extends Tool.ToolIcon {
        @Override
        public void paintIcon(Graphics2D g) {
            // based on smudge_tool.svg

            Path2D shape = new Path2D.Float();
            shape.moveTo(22.147558, 2.4032667);
            shape.curveTo(24.571192, 8.312117, 24.440912, 17.305197, 23.313412, 17.941687);
            shape.curveTo(21.999336, 18.683477, 19.62295, 19.883547, 18.796078, 20.238096);
            shape.curveTo(17.900263, 20.622177, 17.936176, 20.847305, 13.613189, 20.898235);
            shape.curveTo(11.353691, 20.926235, 13.273945, 18.784855, 13.844296, 18.054346);
            shape.curveTo(14.628173, 17.050365, 16.27149, 18.165026, 17.198284, 17.238327);
            shape.curveTo(18.125061, 16.311558, 18.14161, 11.826557, 17.214813, 12.753326);
            shape.curveTo(16.28802, 13.680137, 10.402279, 23.827217, 7.847664, 25.839296);
            shape.curveTo(5.9332438, 27.347157, 4.7676945, 27.763706, 3.3247247, 27.160036);
            shape.curveTo(2.032953, 26.619627, 2.6445398, 25.255596, 3.4592745, 24.813887);
            shape.curveTo(6.5001836, 23.165287, 7.438078, 20.790596, 9.101483, 18.542076);
            shape.curveTo(9.880999, 17.488396, 12.282015, 13.668896, 13.881689, 11.873806);
            shape.curveTo(11.842229, 13.5384865, 9.519419, 18.817917, 8.774448, 18.702995);
            shape.curveTo(8.342622, 18.636395, 7.3511715, 17.627275, 7.3511715, 16.700466);
            shape.curveTo(7.3511715, 15.773696, 10.068112, 12.470516, 11.375208, 10.529186);
            shape.curveTo(9.828844, 12.499756, 8.350622, 14.858656, 7.6307526, 15.349896);
            shape.curveTo(6.6277995, 16.034336, 5.596125, 14.886256, 6.0640936, 13.575437);
            shape.curveTo(6.297478, 12.921747, 8.215015, 10.376117, 9.820339, 8.357027);
            shape.curveTo(8.008162, 10.500257, 7.4930644, 11.035727, 6.7325964, 11.983277);
            shape.curveTo(6.1860065, 12.664357, 4.3971505, 12.153507, 4.756982, 10.893757);
            shape.curveTo(5.0504823, 9.866137, 13.408393, 1.0940566, 13.408393, 1.0940566);
            shape.lineTo(13.408393, 1.0940566);

            g.setStroke(new BasicStroke(1.1584814f, CAP_ROUND, JOIN_ROUND, 4));
            g.draw(shape);
        }
    }
}
