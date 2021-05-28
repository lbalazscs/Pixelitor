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
import pixelitor.gui.utils.SliderSpinner;
import pixelitor.utils.ImageUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;

import static javax.swing.BorderFactory.createTitledBorder;
import static pixelitor.gui.utils.SliderSpinner.TextPosition.BORDER;

/**
 * The GUI which has the four sliders and a {@link CanvasEditsViewer} in center.
 */
class EnlargeCanvasGUI extends JPanel {
    // Percentage Based
    private final RangeParam northRangePercent = new RangeParam("North", 0, 0, 100);
    private final RangeParam eastRangePercent = new RangeParam("East", 0, 0, 100);
    private final RangeParam southRangePercent = new RangeParam("South", 0, 0, 100);
    private final RangeParam westRangePercent = new RangeParam("West", 0, 0, 100);

    // Actual Pixel Based
    private final RangeParam northRangePixels;
    private final RangeParam eastRangePixels;
    private final RangeParam southRangePixels;
    private final RangeParam westRangePixels;

    private final JRadioButton btnUsePixels = new JRadioButton("Pixels");
    private final JRadioButton btnUsePercentage = new JRadioButton("Percentage");

    private final CanvasEditsViewer canvasEditsViewer = new CanvasEditsViewer();

    EnlargeCanvasGUI() {
        setLayout(new GridBagLayout());

        Canvas c = OpenImages.getActiveComp().getCanvas();
        northRangePixels = new RangeParam("North", 0, 0, c.getHeight());
        eastRangePixels = new RangeParam("East", 0, 0, c.getWidth());
        southRangePixels = new RangeParam("South", 0, 0, c.getHeight());
        westRangePixels = new RangeParam("West", 0, 0, c.getWidth());

        addRadioButtons();

        addSliderSpinner(northRangePercent, northRangePixels, "north", 1, 0, SliderSpinner.HORIZONTAL);
        addSliderSpinner(eastRangePercent, eastRangePixels, "east", 2, 1, SliderSpinner.VERTICAL);
        addSliderSpinner(southRangePercent, southRangePixels, "south", 1, 2, SliderSpinner.HORIZONTAL);
        addSliderSpinner(westRangePercent, westRangePixels, "west", 0, 1, SliderSpinner.VERTICAL);

        btnUsePixels.doClick();

        addCanvasEditor();
    }

    private void addRadioButtons() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = c.gridy = 2;
        c.anchor = GridBagConstraints.WEST;

        btnUsePixels.addActionListener(e -> {
            syncValueToPixels(northRangePixels, northRangePercent);
            syncValueToPixels(eastRangePixels, eastRangePercent);
            syncValueToPixels(westRangePixels, westRangePercent);
            syncValueToPixels(southRangePixels, southRangePercent);
        });

        btnUsePercentage.addActionListener(e -> {
            syncValueToPercentage(northRangePercent, northRangePixels);
            syncValueToPercentage(eastRangePercent, eastRangePixels);
            syncValueToPercentage(westRangePercent, westRangePixels);
            syncValueToPercentage(southRangePercent, southRangePixels);
        });

        new ButtonGroup() {{
            add(btnUsePixels);
            add(btnUsePercentage);
        }};

        add(new Box(BoxLayout.Y_AXIS) {{
            add(btnUsePixels);
            add(btnUsePercentage);
        }}, c);
    }

    private static void syncValueToPixels(RangeParam pixels, RangeParam percent) {
        pixels.setValue(pixels.getMaximum() * percent.getValueAsDouble() / 100, false);
    }

    private static void syncValueToPercentage(RangeParam percent, RangeParam pixels) {
        percent.setValue(100 * pixels.getValueAsDouble() / pixels.getMaximum(), false);
    }

    private void addSliderSpinner(RangeParam percent, RangeParam pixels, String sliderName, int layout_x, int layout_y, int orientation) {
        var percentGUI = new SliderSpinner(percent, BORDER, false, orientation);
        percentGUI.setName(sliderName);
        percentGUI.addChangeListener(e -> canvasEditsViewer.repaint());

        var pixelGUI = new SliderSpinner(pixels, BORDER, false, orientation);
        pixelGUI.setName(sliderName);
        pixelGUI.addChangeListener(e -> canvasEditsViewer.repaint());

        CardLayout cardLayout = new CardLayout();
        JPanel card = new JPanel(cardLayout);
        card.add(pixelGUI, "pixel");
        card.add(percentGUI, "percent");
        cardLayout.show(card, "pixel");

        btnUsePixels.addActionListener(e -> cardLayout.first(card));
        btnUsePercentage.addActionListener(e -> cardLayout.last(card));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = layout_x;
        c.gridy = layout_y;

        add(card, c);
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

    private int getNorth(int height) {
        if (btnUsePixels.isSelected()) {
            return northRangePixels.getValue();
        } else {
            return (int) (height * northRangePercent.getValueAsFloat() / 100);
        }
    }

    private int getSouth(int height) {
        if (btnUsePixels.isSelected()) {
            return southRangePixels.getValue();
        } else {
            return (int) (height * southRangePercent.getValueAsFloat() / 100);
        }
    }

    private int getWest(int width) {
        if (btnUsePixels.isSelected()) {
            return westRangePixels.getValue();
        } else {
            return (int) (width * westRangePercent.getValueAsFloat() / 100);
        }
    }

    private int getEast(int width) {
        if (btnUsePixels.isSelected()) {
            return eastRangePixels.getValue();
        } else {
            return (int) (width * eastRangePercent.getValueAsFloat() / 100);
        }
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
            addComponentListener(new CanvasEditsViewer.EventAdaptor());
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

                Graphics2D g = board.createGraphics();
                ImageUtils.createCheckerboardPainter().paint(g, null, board.getWidth(), board.getHeight());
                g.dispose();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Canvas canvas = OpenImages.getActiveComp().getCanvas();

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
            float N = getNorth(canvas.getHeight());
            float E = getEast(canvas.getWidth());
            float W = getWest(canvas.getWidth());
            float S = getSouth(canvas.getHeight());
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
            if (allocatedW / drawW > allocatedH / drawH) {
                factor = drawW * 0.75f / allocatedW;
            } else {
                factor = drawH * 0.75f / allocatedH;
            }

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

            if (thumb == null) {
                createTheThumb();
            }
            // Drawing the thumb, which also represents the old canvas!
            g.drawImage(board, (int) (ox - canvasW / 2), (int) (oy - canvasH / 2), (int) canvasW, (int) canvasH, null);
            g.drawImage(thumb, (int) (ox - canvasW / 2), (int) (oy - canvasH / 2), (int) canvasW, (int) canvasH, null);
        }
    }
}
