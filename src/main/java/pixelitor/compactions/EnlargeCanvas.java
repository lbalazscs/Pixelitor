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

package pixelitor.compactions;

import pixelitor.Canvas;
import pixelitor.OpenImages;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.View;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.SliderSpinner;
import pixelitor.guides.Guides;
import pixelitor.layers.ContentLayer;

import javax.swing.*;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.geom.AffineTransform;

import static javax.swing.BoxLayout.Y_AXIS;
import static pixelitor.gui.utils.SliderSpinner.TextPosition.BORDER;

/**
 * Enlarges the canvas for all layers of a composition
 */
public class EnlargeCanvas extends SimpleCompAction {
    public static final String NAME = "Enlarge Canvas";
    private int north;
    private int east;
    private int south;
    private int west;
    private int newCanvasWidth;
    private int newCanvasHeight;

    public EnlargeCanvas(int north, int east, int south, int west) {
        super(NAME, true);
        this.north = north;
        this.east = east;
        this.south = south;
        this.west = west;
    }

    public void ensureCovering(Rectangle imageBounds, Canvas canvas) {
        if (imageBounds.x < -west) {
            west = -imageBounds.x;
        }
        if (imageBounds.y < -north) {
            north = -imageBounds.y;
        }
        int imageMaxX = imageBounds.x + imageBounds.width;
        if (imageMaxX > canvas.getWidth() + east) {
            east = imageMaxX - canvas.getWidth();
        }
        int imageMaxY = imageBounds.y + imageBounds.height;
        if (imageMaxY > canvas.getHeight() + south) {
            south = imageMaxY - canvas.getHeight();
        }
    }

    public boolean doesNothing() {
        return north == 0 && east == 0 && south == 0 && west == 0;
    }

    @Override
    protected void changeCanvasSize(Canvas newCanvas, View view) {
        newCanvasWidth = newCanvas.getWidth() + east + west;
        newCanvasHeight = newCanvas.getHeight() + north + south;
        newCanvas.changeSize(newCanvasWidth, newCanvasHeight, view);
    }

    @Override
    protected String getEditName() {
        return NAME;
    }

    @Override
    protected void transform(ContentLayer contentLayer) {
        contentLayer.enlargeCanvas(north, east, south, west);
    }

    @Override
    protected AffineTransform createCanvasTransform(Canvas canvas) {
        return AffineTransform.getTranslateInstance(west, north);
    }

    @Override
    protected Guides createGuidesCopy(Guides oldGuides, View view, Canvas oldCanvas) {
        return oldGuides.copyForEnlargedCanvas(north, east, south, west, view, oldCanvas);
    }

    @Override
    protected String getStatusBarMessage() {
        return "The canvas was enlarged to "
                + newCanvasWidth + " x " + newCanvasHeight + " pixels.";
    }

    public static Action getAction() {
        return new AbstractAction("Enlarge Canvas...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                showInDialog();
            }
        };
    }

    private static void showInDialog() {
        var p = new EnlargeCanvasPanel();
        new DialogBuilder()
            .title(NAME)
                .content(p)
                .okAction(() -> {
                    var comp = OpenImages.getActiveComp();
                    new EnlargeCanvas(p.getNorth(), p.getEast(), p.getSouth(), p.getWest())
                            .process(comp);
                })
                .show();
    }

    static class EnlargeCanvasPanel extends JPanel {
        final RangeParam northRange = new RangeParam("North", 0, 0, 500);
        final RangeParam eastRange = new RangeParam("East", 0, 0, 500);
        final RangeParam southRange = new RangeParam("South", 0, 0, 500);
        final RangeParam westRange = new RangeParam("West", 0, 0, 500);

        private EnlargeCanvasPanel() {
            setLayout(new BoxLayout(this, Y_AXIS));

            addSliderSpinner(northRange, "north");
            addSliderSpinner(eastRange, "east");
            addSliderSpinner(southRange, "south");
            addSliderSpinner(westRange, "west");
        }

        private void addSliderSpinner(RangeParam range, String sliderName) {
            var s = new SliderSpinner(range, BORDER, false);
            s.setName(sliderName);
            add(s);
        }

        public int getNorth() {
            return northRange.getValue();
        }

        public int getSouth() {
            return southRange.getValue();
        }

        public int getWest() {
            return westRange.getValue();
        }

        public int getEast() {
            return eastRange.getValue();
        }
    }
}
