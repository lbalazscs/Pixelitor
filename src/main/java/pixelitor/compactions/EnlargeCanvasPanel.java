/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.utils.Icons;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.ResizeUnit;
import pixelitor.utils.Shapes;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static javax.swing.BorderFactory.createTitledBorder;
import static pixelitor.gui.utils.SliderSpinner.LabelPosition.BORDER;

/**
 * The GUI configuration panel for the "Enlarge Canvas" feature.
 */
class EnlargeCanvasPanel extends JPanel implements DialogMenuOwner {
    private static final String PIXEL_CARD = "pixel";
    private static final String PERCENT_CARD = "percent";

    // Percentage-based sliders (0-100%)
    private final RangeParam northPercentage = new RangeParam("North", 0, 0, 100);
    private final RangeParam eastPercentage = new RangeParam("East", 0, 0, 100);
    private final RangeParam southPercentage = new RangeParam("South", 0, 0, 100);
    private final RangeParam westPercentage = new RangeParam("West", 0, 0, 100);

    // Pixel-based sliders (0 to canvas dimension)
    private final RangeParam northPixels;
    private final RangeParam eastPixels;
    private final RangeParam southPixels;
    private final RangeParam westPixels;

    private final JRadioButton usePixelsRadio = new JRadioButton("Pixels");
    private final JRadioButton usePercentsRadio = new JRadioButton("Percentage");

    private final JButton resetButton = new JButton("Reset", Icons.getResetIcon());

    private final CanvasPreviewPanel previewPanel = new CanvasPreviewPanel();

    EnlargeCanvasPanel() {
        setLayout(new GridBagLayout());

        Canvas c = Views.getActiveComp().getCanvas();
        northPixels = new RangeParam("North", 0, 0, c.getHeight());
        eastPixels = new RangeParam("East", 0, 0, c.getWidth());
        southPixels = new RangeParam("South", 0, 0, c.getHeight());
        westPixels = new RangeParam("West", 0, 0, c.getWidth());

        setupRadioButtons();
        setupResetButton();

        addSliderSpinner(northPercentage, northPixels, "north", 1, 0, SliderSpinner.HORIZONTAL);
        addSliderSpinner(eastPercentage, eastPixels, "east", 2, 1, SliderSpinner.VERTICAL);
        addSliderSpinner(southPercentage, southPixels, "south", 1, 2, SliderSpinner.HORIZONTAL);
        addSliderSpinner(westPercentage, westPixels, "west", 0, 1, SliderSpinner.VERTICAL);

        addPreviewPanel();

        usePixelsRadio.doClick(); // Default to pixel mode
    }

    private void setupRadioButtons() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 2;
        c.anchor = GridBagConstraints.CENTER;

        usePixelsRadio.addActionListener(e -> syncToNewUnit(ResizeUnit.PIXELS));
        usePercentsRadio.addActionListener(e -> syncToNewUnit(ResizeUnit.PERCENT));

        ButtonGroup unitToggleGroup = new ButtonGroup();
        unitToggleGroup.add(usePixelsRadio);
        unitToggleGroup.add(usePercentsRadio);

        Box radioContainer = new Box(BoxLayout.Y_AXIS);
        radioContainer.add(usePixelsRadio);
        radioContainer.add(usePercentsRadio);
        add(radioContainer, c);
    }

    private void setupResetButton() {
        resetButton.setEnabled(false);
        resetButton.addActionListener(e -> reset());

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 2;
        c.anchor = GridBagConstraints.CENTER;
        add(resetButton, c);
    }

    private void reset() {
        if (this.usePixels()) {
            northPixels.setValue(0);
            eastPixels.setValue(0);
            southPixels.setValue(0);
            westPixels.setValue(0);
        } else {
            northPercentage.setValue(0);
            eastPercentage.setValue(0);
            southPercentage.setValue(0);
            westPercentage.setValue(0);
        }
        resetButton.setEnabled(false);
    }

    private void updateReserButtonState() {
        resetButton.setEnabled(hasNonZeroEnlargement());
    }

    private boolean hasNonZeroEnlargement() {
        if (usePixels()) {
            return !northPixels.isZero() ||
                !eastPixels.isZero() ||
                !southPixels.isZero() ||
                !westPixels.isZero();
        } else {
            return !northPercentage.isZero() ||
                !eastPercentage.isZero() ||
                !southPercentage.isZero() ||
                !westPercentage.isZero();
        }
    }

    private void addSliderSpinner(RangeParam percent, RangeParam pixels, String sliderName, int gridX, int gridY, int orientation) {
        var percentGUI = new SliderSpinner(percent, BORDER, false, orientation);
        percentGUI.setName(sliderName);
        percentGUI.addChangeListener(e -> sliderChanged());

        var pixelGUI = new SliderSpinner(pixels, BORDER, false, orientation);
        pixelGUI.setName(sliderName);
        pixelGUI.addChangeListener(e -> sliderChanged());

        CardLayout cardLayout = new CardLayout();
        JPanel card = new JPanel(cardLayout);
        card.add(pixelGUI, PIXEL_CARD);
        card.add(percentGUI, PERCENT_CARD);
        cardLayout.show(card, PIXEL_CARD);

        usePixelsRadio.addActionListener(e -> cardLayout.first(card));
        usePercentsRadio.addActionListener(e -> cardLayout.last(card));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = gridX;
        c.gridy = gridY;

        add(card, c);
    }

    private void sliderChanged() {
        previewPanel.repaint();
        updateReserButtonState();
    }

    private void addPreviewPanel() {
        GridBagConstraints c = new GridBagConstraints();
        c.weightx = c.weighty = c.gridx = c.gridy = 1;
        c.fill = GridBagConstraints.BOTH;

        add(previewPanel, c);
    }

    /**
     * Synchronizes values between pixel and percentage
     * sliders when switching units.
     */
    private void syncToNewUnit(ResizeUnit newUnit) {
        syncPair(northPixels, northPercentage, newUnit);
        syncPair(eastPixels, eastPercentage, newUnit);
        syncPair(westPixels, westPercentage, newUnit);
        syncPair(southPixels, southPercentage, newUnit);
    }

    private static void syncPair(RangeParam pixels, RangeParam percent, ResizeUnit newUnit) {
        switch (newUnit) {
            case PIXELS -> pixels.setValue(
                percent.getValueAsDouble() * pixels.getMaximum() / 100, false);
            case PERCENT -> percent.setValue(
                pixels.getValueAsDouble() * 100 / pixels.getMaximum(), false);
        }
    }

    private int getNorth(Canvas canvas) {
        if (usePixels()) {
            return northPixels.getValue();
        } else {
            return percentToPixels(northPercentage, canvas.getHeight());
        }
    }

    private int getSouth(Canvas canvas) {
        if (usePixels()) {
            return southPixels.getValue();
        } else {
            return percentToPixels(southPercentage, canvas.getHeight());
        }
    }

    private int getWest(Canvas canvas) {
        if (usePixels()) {
            return westPixels.getValue();
        } else {
            return percentToPixels(westPercentage, canvas.getWidth());
        }
    }

    private int getEast(Canvas canvas) {
        if (usePixels()) {
            return eastPixels.getValue();
        } else {
            return percentToPixels(eastPercentage, canvas.getWidth());
        }
    }

    private boolean usePixels() {
        return usePixelsRadio.isSelected();
    }

    private static int percentToPixels(RangeParam percentParam, int fullSize) {
        return (int) (fullSize * percentParam.getValueAsDouble() / 100.0);
    }

    public EnlargeCanvas getCompAction(Canvas canvas) {
        return new EnlargeCanvas(
            getNorth(canvas), getEast(canvas),
            getSouth(canvas), getWest(canvas));
    }

    @Override
    public boolean canHaveUserPresets() {
        return true;
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        boolean usePixels = usePixels();
        preset.putBoolean("Pixels", usePixels);
        if (usePixels) {
            northPixels.saveStateTo(preset);
            eastPixels.saveStateTo(preset);
            southPixels.saveStateTo(preset);
            westPixels.saveStateTo(preset);
        } else {
            northPercentage.saveStateTo(preset);
            eastPercentage.saveStateTo(preset);
            southPercentage.saveStateTo(preset);
            westPercentage.saveStateTo(preset);
        }
    }

    @Override
    public void loadUserPreset(UserPreset preset) {
        boolean usePixels = preset.getBoolean("Pixels");
        if (usePixels) {
            usePixelsRadio.doClick();

            northPixels.loadStateFrom(preset);
            eastPixels.loadStateFrom(preset);
            southPixels.loadStateFrom(preset);
            westPixels.loadStateFrom(preset);
        } else {
            usePercentsRadio.doClick();

            northPercentage.loadStateFrom(preset);
            eastPercentage.loadStateFrom(preset);
            southPercentage.loadStateFrom(preset);
            westPercentage.loadStateFrom(preset);
        }
    }

    @Override
    public String getPresetDirName() {
        return "Enlarge Canvas";
    }

    /**
     * A panel that displays a preview of how the canvas will look after
     * enlargement. Shows the relative size of new canvas to the current one.
     */
    private class CanvasPreviewPanel extends JPanel {
        // Show arrows when enlargement > 20px in preview space
        private static final int ENLARGEMENT_THRESHOLD_PIXELS = 20;

        // Scale factor for fitting preview in panel
        private static final float PREVIEW_SCALE_FACTOR = 0.75f;

        private static final Color NEW_CANVAS_COLOR = new Color(136, 139, 146);
        private static final CheckerboardPainter checkerboard = ImageUtils.createCheckerboardPainter();
        private BufferedImage previewImg;

        public CanvasPreviewPanel() {
            addComponentListener(new PreviewResizeListener());
            setBorder(createTitledBorder("Preview"));
        }

        public void updatePreviewImage(Composition comp) {
            if (comp != null) {
                BufferedImage actualImage = comp.getCompositeImage();
                previewImg = ImageUtils.createThumbnail(actualImage, getWidth() / 2, null);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Composition comp = Views.getActiveComp();
            if (previewImg == null) {
                updatePreviewImage(comp);
            }

            drawPreview(g, comp);
        }

        private void drawPreview(Graphics g, Composition comp) {
            Canvas canvas = comp.getCanvas();
            PreviewDimensions dims = calculatePreviewDimensions(canvas);

            // Draw the new canvas boundary
            drawNewCanvasBoundary(g, dims);

            // Draw the original canvas with checkerboard and image
            drawOriginalCanvas(g, dims);

            // Draw direction arrows if enlargement exceeds threshold
            Graphics2D g2d = (Graphics2D) g;
            drawEnlargementArrows(g2d, dims);
        }

        /**
         * Draws the boundary of the new enlarged canvas.
         */
        private static void drawNewCanvasBoundary(Graphics g, PreviewDimensions dims) {
            g.setColor(NEW_CANVAS_COLOR);
            g.fillRect(
                (int) (dims.centerX - dims.newWidth / 2),
                (int) (dims.centerY - dims.newHeight / 2),
                (int) dims.newWidth,
                (int) dims.newHeight
            );
        }

        /**
         * Draws the original canvas with checkerboard background and preview image.
         */
        private void drawOriginalCanvas(Graphics g, PreviewDimensions dims) {
            // Create and draw checkerboard background
            BufferedImage board = new BufferedImage(
                (int) dims.originalWidth,
                (int) dims.originalHeight,
                previewImg.getType()
            );
            Graphics2D boardGraphics = board.createGraphics();
            checkerboard.paint(boardGraphics, null, board.getWidth(), board.getHeight());
            boardGraphics.dispose();

            // Calculate position for original canvas
            int x = (int) (dims.centerX - dims.newWidth / 2 + dims.westEnlargement);
            int y = (int) (dims.centerY - dims.newHeight / 2 + dims.northEnlargement);

            // Draw checkerboard and preview image
            g.drawImage(board, x, y, (int) dims.originalWidth, (int) dims.originalHeight, null);
            g.drawImage(previewImg, x, y, (int) dims.originalWidth, (int) dims.originalHeight, null);
        }

        /**
         * Draws direction arrows when enlargement exceeds threshold in any direction.
         */
        private static void drawEnlargementArrows(Graphics2D g, PreviewDimensions dims) {
            g.setColor(Color.WHITE);
            g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

            // Get the coordinates of the original canvas in preview space
            float origLeft = dims.centerX - dims.newWidth / 2 + dims.westEnlargement;
            float origRight = origLeft + dims.originalWidth;
            float origTop = dims.centerY - dims.newHeight / 2 + dims.northEnlargement;
            float origBottom = origTop + dims.originalHeight;

            // Get the coordinates of the enlarged canvas in preview space
            float enlargedLeft = dims.centerX - dims.newWidth / 2;
            float enlargedRight = dims.centerX + dims.newWidth / 2;
            float enlargedTop = dims.centerY - dims.newHeight / 2;
            float enlargedBottom = dims.centerY + dims.newHeight / 2;

            // Draw arrows for each direction if enlargement exceeds threshold
            if (dims.northEnlargement > ENLARGEMENT_THRESHOLD_PIXELS) {
                // Start from middle of original top edge
                float startX = origLeft + dims.originalWidth / 2;
                float startY = origTop;
                // End at enlarged canvas top edge
                float endY = enlargedTop;
                g.fill(Shapes.createFixWidthArrow(
                    startX, startY, startX, endY));
            }

            if (dims.southEnlargement > ENLARGEMENT_THRESHOLD_PIXELS) {
                // Start from middle of original bottom edge
                float startX = origLeft + dims.originalWidth / 2;
                float startY = origBottom;
                // End at enlarged canvas bottom edge
                float endY = enlargedBottom;
                g.fill(Shapes.createFixWidthArrow(
                    startX, startY, startX, endY));
            }

            if (dims.eastEnlargement > ENLARGEMENT_THRESHOLD_PIXELS) {
                // Start from middle of original right edge
                float startX = origRight;
                float startY = origTop + dims.originalHeight / 2;
                // End at enlarged canvas right edge
                float endX = enlargedRight;
                g.fill(Shapes.createFixWidthArrow(
                    startX, startY, endX, startY));
            }

            if (dims.westEnlargement > ENLARGEMENT_THRESHOLD_PIXELS) {
                // Start from middle of original left edge
                float startX = origLeft;
                float startY = origTop + dims.originalHeight / 2;
                // End at enlarged canvas left edge
                float endX = enlargedLeft;
                g.fill(Shapes.createFixWidthArrow(
                    startX, startY, endX, startY));
            }
        }

        /**
         * Helper class to store and manage preview dimensions and calculations
         */
        private static class PreviewDimensions {
            float originalWidth, originalHeight;
            float newWidth, newHeight;
            float northEnlargement, southEnlargement, eastEnlargement, westEnlargement;
            float centerX, centerY;
            float scale;

            /**
             * Applies scaling factor to all dimensional values
             */
            void applyScaling() {
                originalWidth *= scale;
                originalHeight *= scale;
                newWidth *= scale;
                newHeight *= scale;
                northEnlargement *= scale;
                southEnlargement *= scale;
                eastEnlargement *= scale;
                westEnlargement *= scale;
            }
        }

        /**
         * Calculates all necessary dimensions for the preview, including scaling factors
         * and positions for both original and enlarged canvas.
         */
        private PreviewDimensions calculatePreviewDimensions(Canvas canvas) {
            PreviewDimensions dims = new PreviewDimensions();

            // Original canvas dimensions
            dims.originalWidth = canvas.getWidth();
            dims.originalHeight = canvas.getHeight();

            // Calculate enlargements
            dims.northEnlargement = getNorth(canvas);
            dims.westEnlargement = getWest(canvas);
            dims.eastEnlargement = getEast(canvas);
            dims.southEnlargement = getSouth(canvas);

            dims.newWidth = dims.westEnlargement + dims.originalWidth + dims.eastEnlargement;
            dims.newHeight = dims.northEnlargement + dims.originalHeight + dims.southEnlargement;

            // Calculate center point of preview panel
            dims.centerX = getWidth() / 2.0f;
            dims.centerY = getHeight() / 2.0f;

            // Calculate scaling factor to fit preview in panel
            float widthRatio = dims.newWidth / getWidth();
            float heightRatio = dims.newHeight / getHeight();
            dims.scale = PREVIEW_SCALE_FACTOR / Math.max(widthRatio, heightRatio);

            // Apply scaling to all dimensions
            dims.applyScaling();

            return dims;
        }

        private class PreviewResizeListener extends ComponentAdapter {
            @Override
            public void componentResized(ComponentEvent e) {
                updatePreviewImage(Views.getActiveComp());
            }

            @Override
            public void componentShown(ComponentEvent e) {
                componentResized(e);
                repaint();
            }
        }

    }
}
