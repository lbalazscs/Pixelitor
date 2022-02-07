/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

import org.jdesktop.swingx.painter.CheckerboardPainter;
import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.Views;
import pixelitor.filters.gui.DialogMenuOwner;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.UserPreset;
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
 * The GUI which has the four sliders and a {@link PreviewPanel} in center.
 */
class EnlargeCanvasPanel extends JPanel implements DialogMenuOwner {
    // Percentage Based
    private final RangeParam northPercent = new RangeParam("North", 0, 0, 100);
    private final RangeParam eastPercent = new RangeParam("East", 0, 0, 100);
    private final RangeParam southPercent = new RangeParam("South", 0, 0, 100);
    private final RangeParam westPercent = new RangeParam("West", 0, 0, 100);

    // Actual Pixel Based
    private final RangeParam northPixels;
    private final RangeParam eastPixels;
    private final RangeParam southPixels;
    private final RangeParam westPixels;

    private final JRadioButton btnUsePixels = new JRadioButton("Pixels");
    private final JRadioButton btnUsePercentage = new JRadioButton("Percentage");

    private final PreviewPanel previewPanel = new PreviewPanel();

    EnlargeCanvasPanel() {
        setLayout(new GridBagLayout());

        Canvas c = Views.getActiveComp().getCanvas();
        northPixels = new RangeParam("North", 0, 0, c.getHeight());
        eastPixels = new RangeParam("East", 0, 0, c.getWidth());
        southPixels = new RangeParam("South", 0, 0, c.getHeight());
        westPixels = new RangeParam("West", 0, 0, c.getWidth());

        addRadioButtons();

        addSliderSpinner(northPercent, northPixels, "north", 1, 0, SliderSpinner.HORIZONTAL);
        addSliderSpinner(eastPercent, eastPixels, "east", 2, 1, SliderSpinner.VERTICAL);
        addSliderSpinner(southPercent, southPixels, "south", 1, 2, SliderSpinner.HORIZONTAL);
        addSliderSpinner(westPercent, westPixels, "west", 0, 1, SliderSpinner.VERTICAL);

        btnUsePixels.doClick();

        addCanvasEditor();
    }

    private void addRadioButtons() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = c.gridy = 2;
        c.anchor = GridBagConstraints.WEST;

        btnUsePixels.addActionListener(e -> syncToPixels());
        btnUsePercentage.addActionListener(e -> syncToPercentage());

        new ButtonGroup() {{
            add(btnUsePixels);
            add(btnUsePercentage);
        }};

        add(new Box(BoxLayout.Y_AXIS) {{
            add(btnUsePixels);
            add(btnUsePercentage);
        }}, c);
    }

    private void syncToPixels() {
        syncToPixels(northPixels, northPercent);
        syncToPixels(eastPixels, eastPercent);
        syncToPixels(westPixels, westPercent);
        syncToPixels(southPixels, southPercent);
    }

    private static void syncToPixels(RangeParam pixels, RangeParam percent) {
        pixels.setValue(pixels.getMaximum() * percent.getValueAsDouble() / 100, false);
    }

    private void syncToPercentage() {
        syncToPercentage(northPercent, northPixels);
        syncToPercentage(eastPercent, eastPixels);
        syncToPercentage(westPercent, westPixels);
        syncToPercentage(southPercent, southPixels);
    }

    private static void syncToPercentage(RangeParam percent, RangeParam pixels) {
        percent.setValue(100 * pixels.getValueAsDouble() / pixels.getMaximum(), false);
    }

    private void addSliderSpinner(RangeParam percent, RangeParam pixels, String sliderName, int gridX, int gridY, int orientation) {
        var percentGUI = new SliderSpinner(percent, BORDER, false, orientation);
        percentGUI.setName(sliderName);
        percentGUI.addChangeListener(e -> previewPanel.repaint());

        var pixelGUI = new SliderSpinner(pixels, BORDER, false, orientation);
        pixelGUI.setName(sliderName);
        pixelGUI.addChangeListener(e -> previewPanel.repaint());

        CardLayout cardLayout = new CardLayout();
        JPanel card = new JPanel(cardLayout);
        card.add(pixelGUI, "pixel");
        card.add(percentGUI, "percent");
        cardLayout.show(card, "pixel");

        btnUsePixels.addActionListener(e -> cardLayout.first(card));
        btnUsePercentage.addActionListener(e -> cardLayout.last(card));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = gridX;
        c.gridy = gridY;

        add(card, c);
    }

    private void addCanvasEditor() {
        GridBagConstraints c = new GridBagConstraints();
        c.weightx = c.weighty = c.gridx = c.gridy = 1;
        c.fill = GridBagConstraints.BOTH;

        add(previewPanel, c);
    }

    private int getNorth(int height) {
        if (btnUsePixels.isSelected()) {
            return northPixels.getValue();
        } else {
            return (int) (height * northPercent.getValueAsFloat() / 100);
        }
    }

    private int getSouth(int height) {
        if (btnUsePixels.isSelected()) {
            return southPixels.getValue();
        } else {
            return (int) (height * southPercent.getValueAsFloat() / 100);
        }
    }

    private int getWest(int width) {
        if (btnUsePixels.isSelected()) {
            return westPixels.getValue();
        } else {
            return (int) (width * westPercent.getValueAsFloat() / 100);
        }
    }

    private int getEast(int width) {
        if (btnUsePixels.isSelected()) {
            return eastPixels.getValue();
        } else {
            return (int) (width * eastPercent.getValueAsFloat() / 100);
        }
    }

    public EnlargeCanvas getCompAction(Canvas canvas) {
        int w = canvas.getWidth(), h = canvas.getHeight();
        return new EnlargeCanvas(getNorth(h), getEast(w), getSouth(h), getWest(w));
    }

    @Override
    public boolean canHaveUserPresets() {
        return true;
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        boolean usePixels = btnUsePixels.isSelected();
        preset.putBoolean("Pixels", usePixels);
        if (usePixels) {
            northPixels.saveStateTo(preset);
            eastPixels.saveStateTo(preset);
            southPixels.saveStateTo(preset);
            westPixels.saveStateTo(preset);
        } else {
            northPercent.saveStateTo(preset);
            eastPercent.saveStateTo(preset);
            southPercent.saveStateTo(preset);
            westPercent.saveStateTo(preset);
        }
    }

    @Override
    public void loadUserPreset(UserPreset preset) {
        boolean usePixels = preset.getBoolean("Pixels");
        if (usePixels) {
            btnUsePixels.doClick();

            northPixels.loadStateFrom(preset);
            eastPixels.loadStateFrom(preset);
            southPixels.loadStateFrom(preset);
            westPixels.loadStateFrom(preset);
        } else {
            btnUsePercentage.doClick();

            northPercent.loadStateFrom(preset);
            eastPercent.loadStateFrom(preset);
            southPercent.loadStateFrom(preset);
            westPercent.loadStateFrom(preset);
        }
    }

    @Override
    public String getPresetDirName() {
        return "Enlarge Canvas";
    }

    /**
     * Shows the relative size of new canvas to the current canvas.
     */
    private class PreviewPanel extends JPanel {
        private static final Color newCanvasColor = new Color(136, 139, 146);
        private static final CheckerboardPainter painter = ImageUtils.createCheckerboardPainter();
        private BufferedImage thumb;

        public PreviewPanel() {
            setBackground(new Color(214, 217, 223));
            addComponentListener(new PreviewPanel.EventAdaptor());
            setBorder(createTitledBorder("Preview"));
        }

        private class EventAdaptor extends ComponentAdapter {
            @Override
            public void componentResized(ComponentEvent e) {
                createThumb();
            }

            @Override
            public void componentShown(ComponentEvent e) {
                componentResized(e);
                repaint();
            }
        }

        public void createThumb() {
            Composition comp = Views.getActiveComp();
            if (comp != null) {
                BufferedImage actualImage = comp.getCompositeImage();
                thumb = ImageUtils.createThumbnail(actualImage, getWidth() / 2, null);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Canvas canvas = Views.getActiveComp().getCanvas();

            // Actual canvas Width and Height.
            float canvasW = canvas.getWidth(), canvasH = canvas.getHeight();

            float N = getNorth(canvas.getHeight());
            float W = getWest(canvas.getWidth());
            float newCanvasW = W + canvasW + getEast(canvas.getWidth());
            float newCanvasH = N + canvasH + getSouth(canvas.getHeight());

            // These represent the total space in which we can draw!
            int drawW = getWidth(), drawH = getHeight();
            int ox = drawW / 2, oy = drawH / 2; // Origin

            float factor;
            if (newCanvasW / drawW > newCanvasH / drawH) {
                factor = drawW * 0.75f / newCanvasW;
            } else {
                factor = drawH * 0.75f / newCanvasH;
            }

            newCanvasH *= factor;
            newCanvasW *= factor;
            canvasH *= factor;
            canvasW *= factor;
            N *= factor;
            W *= factor;

            // Drawing the newCanvas
            g.setColor(newCanvasColor);
            g.fillRect(
                (int) (ox - newCanvasW / 2),
                (int) (oy - newCanvasH / 2),
                (int) (newCanvasW),
                (int) (newCanvasH)
            );

            if (thumb == null) {
                createThumb();
            }

            BufferedImage board = new BufferedImage((int) canvasW, (int) canvasH, thumb.getType());
            Graphics2D boardGraphics = board.createGraphics();
            painter.paint(boardGraphics, null, board.getWidth(), board.getHeight());
            boardGraphics.dispose();

            // Drawing the thumb, which represents the old canvas
            g.drawImage(board, (int) (ox - newCanvasW / 2 + W), (int) (oy - newCanvasH / 2 + N), (int) canvasW, (int) canvasH, null);
            g.drawImage(thumb, (int) (ox - newCanvasW / 2 + W), (int) (oy - newCanvasH / 2 + N), (int) canvasW, (int) canvasH, null);
        }
    }
}
