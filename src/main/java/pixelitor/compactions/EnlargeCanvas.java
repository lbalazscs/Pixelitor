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
import pixelitor.Composition;
import pixelitor.OpenImages;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.View;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.OpenImageEnabledAction;
import pixelitor.gui.utils.SliderSpinner;
import pixelitor.guides.Guides;
import pixelitor.layers.ContentLayer;
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

        // Percentage Based
        final RangeParam northRangePercent = new RangeParam("North", 0, 0, 100);
        final RangeParam eastRangePercent = new RangeParam("East", 0, 0, 100);
        final RangeParam southRangePercent = new RangeParam("South", 0, 0, 100);
        final RangeParam westRangePercent = new RangeParam("West", 0, 0, 100);

        // Actual Pixel Based
        final RangeParam northRangePixels = new RangeParam("North", 0, 0, 500);
        final RangeParam eastRangePixels = new RangeParam("East", 0, 0, 500);
        final RangeParam southRangePixels = new RangeParam("South", 0, 0, 500);
        final RangeParam westRangePixels = new RangeParam("West", 0, 0, 500);

        final JRadioButton btnUsePixels = new JRadioButton("Pixels");
        final JRadioButton btnUsePercentage = new JRadioButton("Percentage");

        final CanvasEditsViewer canvasEditsViewer = new CanvasEditsViewer();

        private EnlargeCanvasGUI() {
            setLayout(new GridBagLayout());

            addRadioButtons();

            addSliderSpinner(northRangePercent, "north", 1, 0, SliderSpinner.HORIZONTAL, false);
            addSliderSpinner(eastRangePercent, "east", 2, 1, SliderSpinner.VERTICAL, false);
            addSliderSpinner(southRangePercent, "south", 1, 2, SliderSpinner.HORIZONTAL, false);
            addSliderSpinner(westRangePercent, "west", 0, 1, SliderSpinner.VERTICAL, false);

            addSliderSpinner(northRangePixels, "north", 1, 0, SliderSpinner.HORIZONTAL, true);
            addSliderSpinner(eastRangePixels, "east", 2, 1, SliderSpinner.VERTICAL, true);
            addSliderSpinner(southRangePixels, "south", 1, 2, SliderSpinner.HORIZONTAL, true);
            addSliderSpinner(westRangePixels, "west", 0, 1, SliderSpinner.VERTICAL, true);

            btnUsePixels.doClick();

            addCanvasEditor();
        }

        private void addRadioButtons() {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = c.gridy = 2;
            c.anchor = GridBagConstraints.WEST;

            new ButtonGroup() {{
                add(btnUsePixels);
                add(btnUsePercentage);
            }};

            add(new Box(BoxLayout.Y_AXIS) {{
                add(btnUsePixels);
                add(btnUsePercentage);
            }}, c);
        }

        private void addSliderSpinner(RangeParam range, String sliderName, int layout_x, int layout_y, int orientation, boolean isPixel) {
            var s = new SliderSpinner(range, BORDER, false, orientation);
            s.setName(sliderName);
            s.addChangeListener(e -> canvasEditsViewer.repaint());
            s.setVisible(false);

            btnUsePixels.addActionListener(e -> s.setVisible(isPixel));
            btnUsePercentage.addActionListener(e -> s.setVisible(!isPixel));

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

        public int getNorth(int height) {
            if (btnUsePixels.isSelected())
                return northRangePixels.getValue();
            else
                return (int) (height * northRangePercent.getValueAsFloat() / 100);
        }

        public int getSouth(int height) {
            if (btnUsePixels.isSelected())
                return southRangePixels.getValue();
            else
                return (int) (height * southRangePercent.getValueAsFloat() / 100);
        }

        public int getWest(int width) {
            if (btnUsePixels.isSelected())
                return westRangePixels.getValue();
            else
                return (int) (width * westRangePercent.getValueAsFloat() / 100);
        }

        public int getEast(int width) {
            if (btnUsePixels.isSelected())
                return eastRangePixels.getValue() ;
            else
                return (int) (width * eastRangePercent.getValueAsFloat() / 100);
        }

        public EnlargeCanvas getCompAction(Canvas canvas) {
            int w = canvas.getWidth(), h = canvas.getHeight();
            return new EnlargeCanvas(
                    getNorth(h),
                    getEast(w),
                    getSouth(h),
                    getWest(w)
            );
        }

        /**
         * This is a JPanel, in charge of showing the relative size of new canvas to the current canvas.
         */
        private class CanvasEditsViewer extends JPanel {

            private static final Color newCanvasColor = new Color(136, 139, 146);
            private BufferedImage board;
            private BufferedImage thumb;

            public CanvasEditsViewer() {
                setBackground(new Color(214, 217, 223));
                addComponentListener(new EventAdaptor());
            }

            private class EventAdaptor extends ComponentAdapter {
                @Override
                public void componentResized(ComponentEvent e) {
                    createTheThumb();
                }

                @Override
                public void componentShown(ComponentEvent e) {
                    componentResized(e);
                    repaint();
                }
            }

            public void createTheThumb() {
                Composition comp = OpenImages.getActiveComp();
                if (comp != null) {
                    BufferedImage actualImage = comp.getCompositeImage();
                    thumb = ImageUtils.createThumbnail(actualImage, getWidth() / 2, null);
                    board = new BufferedImage(thumb.getWidth(), thumb.getHeight(), thumb.getType());
                    ImageUtils.createCheckerboardPainter().paint(board.createGraphics(), null, board.getWidth(), board.getHeight());
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
                float N = getNorth(canvas.getHeight()) , E = getEast(canvas.getWidth()), W = getWest(canvas.getWidth()), S = getSouth(canvas.getHeight());
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


                if (thumb == null) createTheThumb();
                // Drawing the thumb, which also represents the old canvas!
                g.drawImage(board, (int) (ox - canvasW / 2), (int) (oy - canvasH / 2), (int) canvasW, (int) canvasH, null);
                g.drawImage(thumb, (int) (ox - canvasW / 2), (int) (oy - canvasH / 2), (int) canvasW, (int) canvasH, null);

            }

        }

    }

}
