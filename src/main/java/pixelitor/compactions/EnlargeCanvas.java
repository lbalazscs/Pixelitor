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

package pixelitor.compactions;

import pixelitor.Canvas;
import pixelitor.OpenImages;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.View;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.OpenImageEnabledAction;
import pixelitor.gui.utils.SliderSpinner;
import pixelitor.guides.Guides;
import pixelitor.layers.ContentLayer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.AffineTransform;

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

    public void setupToFitContentOf(ContentLayer contentLayer) {
        Rectangle contentBounds = contentLayer.getContentBounds();
        Canvas canvas = contentLayer.getComp().getCanvas();

        if (contentBounds.x < -west) {
            west = -contentBounds.x;
        }

        if (contentBounds.y < -north) {
            north = -contentBounds.y;
        }

        int contentMaxX = contentBounds.x + contentBounds.width;
        if (contentMaxX > canvas.getWidth() + east) {
            east = contentMaxX - canvas.getWidth();
        }

        int contentMaxY = contentBounds.y + contentBounds.height;
        if (contentMaxY > canvas.getHeight() + south) {
            south = contentMaxY - canvas.getHeight();
        }
    }

    public boolean doesNothing() {
        return north == 0 && east == 0 && south == 0 && west == 0;
    }

    @Override
    protected void changeCanvasSize(Canvas newCanvas, View view) {
        newCanvasWidth = newCanvas.getWidth() + east + west;
        newCanvasHeight = newCanvas.getHeight() + north + south;
        newCanvas.changeSize(newCanvasWidth, newCanvasHeight, view, false);
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
        return new OpenImageEnabledAction("Enlarge Canvas...") {
            @Override
            public void onClick() {
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

        final CanvasEditor canvasEditor = new CanvasEditor();

        private EnlargeCanvasPanel() {
            setLayout(new GridBagLayout());

            addSliderSpinner(northRange, "north", 1, 0, SliderSpinner.HORIZONTAL);
            addSliderSpinner(eastRange, "east", 2, 1, SliderSpinner.VERTICAL);
            addSliderSpinner(southRange, "south", 1, 2, SliderSpinner.HORIZONTAL);
            addSliderSpinner(westRange, "west", 0, 1, SliderSpinner.VERTICAL);

            addCanvasEditor();
        }

        private void addSliderSpinner(RangeParam range, String sliderName, int layout_x, int layout_y, int orientation) {
            var s = new SliderSpinner(range, BORDER, false, orientation);
            s.setName(sliderName);
            s.addChangeListener(e -> canvasEditor.repaint());

            GridBagConstraints c = new GridBagConstraints();
            c.gridx = layout_x;
            c.gridy = layout_y;

            add(s, c);
        }

        private void addCanvasEditor() {
            GridBagConstraints c = new GridBagConstraints();
            c.weightx = c.weighty = c.gridx = c.gridy = 1;
            c.insets = new Insets(20, 20, 20, 20);
            c.fill = GridBagConstraints.BOTH;

            add(canvasEditor, c);
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


        //


        //


        private class CanvasEditor extends JPanel {

            private boolean initialised = false;

            //            private Composition composition;
            private Canvas canvas;


            public CanvasEditor() {
                setBackground(Color.WHITE);
                addComponentListener(new ResizeListener());
            }

            private class ResizeListener extends ComponentAdapter {
                @Override
                public void componentResized(ComponentEvent e) {
                    initialised = true;
//                    composition = OpenImages.getActiveComp();
                    canvas = OpenImages.getActiveComp().getCanvas();
                    repaint();
                }
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                if (!initialised) return;
                if (canvas == null) return;

                g.setColor(Color.RED);

                float canvasW = canvas.getWidth(), canvasH = canvas.getHeight();

                float allocatedW = canvasW + 2 * Math.max(getEast(), getWest());
                float allocatedH = canvasH + 2 * Math.max(getNorth(), getSouth());

                int drawW = getWidth(), drawH = getHeight();
                int ox = drawW / 2, oy = drawH / 2; // Origin

                float factor;

                if (allocatedW / drawW > allocatedH / drawH)
                    factor = drawW * 0.75f / allocatedW;
                else factor = drawH * 0.75f / allocatedH;

                canvasH *= factor;
                canvasW *= factor;

                int N = (int) (getNorth() * factor);
                int E = (int) (getEast() * factor);
                int W = (int) (getWest() * factor);
                int S = (int) (getSouth() * factor);

                g.setColor(Color.RED);
                g.fillRect(
                        ox - (int) (canvasW / 2) - W,
                        oy - (int) (canvasH / 2) - N,
                        E + W + (int) canvasW,
                        N + S + (int) canvasH
                );

                g.setColor(Color.BLUE);
                g.fillRect((int) (ox - canvasW / 2), (int) (oy - canvasH / 2), (int) canvasW, (int) canvasH);

            }

        }

    }

}
