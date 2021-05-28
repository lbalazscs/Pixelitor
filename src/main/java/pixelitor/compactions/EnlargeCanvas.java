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
import pixelitor.layers.Drawable;
import pixelitor.utils.ImageUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import static javax.swing.BorderFactory.createTitledBorder;
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
        var p = new EnlargeCanvasGUI();
        new DialogBuilder()
                .title(NAME)
                .content(p)
                .okAction(() -> {
                    var comp = OpenImages.getActiveComp();
                    p.getCompAction(comp.getCanvas())
                            .process(comp);
                })
                .show();
    }

    /**
     * The GUI which a the four sliders and a view {@link CanvasEditsViewer} in center.
     */
    static class EnlargeCanvasGUI extends JPanel {

        // Note, earlier they represented "<value> pixels to be added extra in the given direction"
        // Now they represent "<value> * [width/height] pixels to be added extra in the given direction"
        // So, now, getNorth and so on gives a float [0, 1];
        final RangeParam northRange = new RangeParam("North", 0, 0, 100);
        final RangeParam eastRange = new RangeParam("East", 0, 0, 100);
        final RangeParam southRange = new RangeParam("South", 0, 0, 100);
        final RangeParam westRange = new RangeParam("West", 0, 0, 100);

        final CanvasEditsViewer canvasEditsViewer = new CanvasEditsViewer();

        private EnlargeCanvasGUI() {
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
            s.addChangeListener(e -> canvasEditsViewer.repaint());

            GridBagConstraints c = new GridBagConstraints();
            c.gridx = layout_x;
            c.gridy = layout_y;

            add(s, c);
        }

        private void addCanvasEditor() {
            GridBagConstraints c = new GridBagConstraints();
            c.weightx = c.weighty = c.gridx = c.gridy = 1;
//            c.insets = new Insets(20, 20, 20, 20);
            c.fill = GridBagConstraints.BOTH;

            add(new JPanel(new BorderLayout()) {{
                setBorder(createTitledBorder("Preview"));
                add(canvasEditsViewer);
            }}, c);
        }

        public float getNorth() {
            return northRange.getValueAsFloat() / 100;
        }

        public float getSouth() {
            return southRange.getValueAsFloat() / 100;
        }

        public float getWest() {
            return westRange.getValueAsFloat() / 100;
        }

        public float getEast() {
            return eastRange.getValueAsFloat() / 100;
        }

        public EnlargeCanvas getCompAction(Canvas canvas) {
            return new EnlargeCanvas(
                    (int) (canvas.getHeight() * getNorth()),
                    (int) (canvas.getWidth() * getEast()),
                    (int) (canvas.getHeight() * getSouth()),
                    (int) (canvas.getWidth() * getWest())
            );
        }

        /**
         * This is a JPanel, in charge of showing the relative size of new canvas to the current canvas.
         */
        private class CanvasEditsViewer extends JPanel {

            private static final Color newCanvasColor = new Color(136, 139, 146);
            private BufferedImage thumb;

            public CanvasEditsViewer() {
                setBackground(new Color(214, 217, 223));
                addComponentListener(new EventAdaptor());
            }

            private class EventAdaptor extends ComponentAdapter {
                @Override
                public void componentResized(ComponentEvent e) {
                    Drawable dr = OpenImages.getActiveDrawable();
                    if (dr != null) { // in unit tests it might be null
                        BufferedImage actualImage = dr.getImageForFilterDialogs();
                        thumb = ImageUtils.createThumbnail(actualImage, getWidth() / 2, null);
                    }
                }

                @Override
                public void componentShown(ComponentEvent e) {
                    componentResized(e);
                    repaint();
                }
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                Canvas canvas = OpenImages.getActiveComp().getCanvas();

                // If canvas is null, what are you resizing?
                if (canvas == null) return;

                // Actual canvas Width and Height.
                float canvasW = canvas.getWidth(), canvasH = canvas.getHeight();

                // As how I imagined the tool, I wanted the canvas be always drawn
                // in the center! So when the new canvas size is set, I take only
                // the maximum of the axial size.
                // Just another note: So the two boxes, ie the newCanvas and the
                // original canvas, are drawn as if being part of a big box (called
                // allocated space) enclosing them to fit the original canvas in
                // center.
                // Say the original canvas is 20x50, and the extension [N/E/W/S]
                // are [5, 10, 15, 20] respectively. So the new box will be of size
                // WxH = canvasW + 2 * Math.max(E, W) x canvasH + 2 * Math.max(N, S)
                // = 20 + 2 * Math.max(10, 15) x 50 + 2 * Math.max(5, 20)
                // = 20 + 2 * 15 x 50 + 2 * 20
                // = 50x90
                int N = (int) (getNorth() * canvasH), E = (int) (getEast() * canvasW), W = (int) (getWest() * canvasW), S = (int) (getSouth() * canvasH);
                float allocatedW = canvasW + 2 * Math.max(E, W);
                float allocatedH = canvasH + 2 * Math.max(N, S);

                // These represent the total space in which we can draw!
                int drawW = getWidth(), drawH = getHeight();
                int ox = drawW / 2, oy = drawH / 2; // Origin

                // So we need to scale down every value to fit draw space.
                // (canvas's, newCanvas*'s, allocated space's  W and H)
                // * by newCanvas, I meant [N/E/W/S]
                // We scale down everything to fit 75% of the draw space...
                // well that gives a nice spacing/padding...
                float factor;
                if (allocatedW / drawW > allocatedH / drawH)
                    factor = drawW * 0.75f / allocatedW;
                else factor = drawH * 0.75f / allocatedH;

                canvasH *= factor;
                canvasW *= factor;
                N *= factor;
                E *= factor;
                W *= factor;
                S *= factor;

                // Drawing the newCanvas
                g.setColor(newCanvasColor);
                g.fillRect(
                        (int) (ox - canvasW / 2 - W),
                        (int) (oy - canvasH / 2 - N),
                        (int) (E + W + canvasW),
                        (int) (N + S + canvasH)
                );

                // Drawing the thumb, which also represents the old canvas!
                g.drawImage(thumb, (int) (ox - canvasW / 2), (int) (oy - canvasH / 2), (int) canvasW, (int) canvasH, null);

            }

        }

    }

}
