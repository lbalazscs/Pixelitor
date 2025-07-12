/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

    // percentage-based sliders (0-100%)
    private final RangeParam northPercentage = new RangeParam("North", 0, 0, 100);
    private final RangeParam eastPercentage = new RangeParam("East", 0, 0, 100);
    private final RangeParam southPercentage = new RangeParam("South", 0, 0, 100);
    private final RangeParam westPercentage = new RangeParam("West", 0, 0, 100);

    // pixel-based sliders (0 to canvas dimension)
    private final RangeParam northPixels;
    private final RangeParam eastPixels;
    private final RangeParam southPixels;
    private final RangeParam westPixels;

    private final JRadioButton usePixelsRadio = new JRadioButton("Pixels");
    private final JRadioButton usePercentsRadio = new JRadioButton("Percentage");
    private final JButton resetButton = new JButton("Reset", Icons.getResetIcon());
    private final CanvasPreviewPanel previewPanel = new CanvasPreviewPanel();

    private final JPanel northCardPanel;
    private final JPanel eastCardPanel;
    private final JPanel southCardPanel;
    private final JPanel westCardPanel;

    EnlargeCanvasPanel() {
        setLayout(new GridBagLayout());

        Canvas c = Views.getActiveComp().getCanvas();
        northPixels = new RangeParam("North", 0, 0, c.getHeight());
        eastPixels = new RangeParam("East", 0, 0, c.getWidth());
        southPixels = new RangeParam("South", 0, 0, c.getHeight());
        westPixels = new RangeParam("West", 0, 0, c.getWidth());

        northCardPanel = createSliderCardPanel(northPercentage, northPixels, "north", SliderSpinner.HORIZONTAL);
        eastCardPanel = createSliderCardPanel(eastPercentage, eastPixels, "east", SliderSpinner.VERTICAL);
        southCardPanel = createSliderCardPanel(southPercentage, southPixels, "south", SliderSpinner.HORIZONTAL);
        westCardPanel = createSliderCardPanel(westPercentage, westPixels, "west", SliderSpinner.VERTICAL);

        setupRadioButtons();
        setupResetButton();
        addComponentsToLayout();

        usePixelsRadio.doClick(); // default to pixel mode
    }

    private void addComponentsToLayout() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 0;
        add(northCardPanel, c);

        c.gridx = 2;
        c.gridy = 1;
        add(eastCardPanel, c);

        c.gridx = 1;
        c.gridy = 2;
        add(southCardPanel, c);

        c.gridx = 0;
        c.gridy = 1;
        add(westCardPanel, c);

        c.gridx = 1;
        c.gridy = 1;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;

        add(previewPanel, c);
    }

    private JPanel createSliderCardPanel(RangeParam percent, RangeParam pixels, String sliderName, int orientation) {
        var percentGUI = new SliderSpinner(percent, BORDER, false, orientation);
        percentGUI.setName(sliderName);
        percentGUI.addChangeListener(e -> sliderChanged());

        var pixelGUI = new SliderSpinner(pixels, BORDER, false, orientation);
        pixelGUI.setName(sliderName);
        pixelGUI.addChangeListener(e -> sliderChanged());

        JPanel cardPanel = new JPanel(new CardLayout());
        cardPanel.add(pixelGUI, PIXEL_CARD);
        cardPanel.add(percentGUI, PERCENT_CARD);
        return cardPanel;
    }

    private void setupRadioButtons() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 2;
        c.anchor = GridBagConstraints.CENTER;

        usePixelsRadio.addActionListener(e -> setUnit(ResizeUnit.PIXELS));
        usePercentsRadio.addActionListener(e -> setUnit(ResizeUnit.PERCENTAGE));

        ButtonGroup unitToggleGroup = new ButtonGroup();
        unitToggleGroup.add(usePixelsRadio);
        unitToggleGroup.add(usePercentsRadio);

        Box radioContainer = new Box(BoxLayout.Y_AXIS);
        radioContainer.add(usePixelsRadio);
        radioContainer.add(usePercentsRadio);
        add(radioContainer, c);
    }

    private void setUnit(ResizeUnit newUnit) {
        syncToNewUnit(newUnit);
        String cardName = (newUnit == ResizeUnit.PIXELS) ? PIXEL_CARD : PERCENT_CARD;
        showCard(northCardPanel, cardName);
        showCard(eastCardPanel, cardName);
        showCard(southCardPanel, cardName);
        showCard(westCardPanel, cardName);
    }

    private static void showCard(JPanel panel, String cardName) {
        ((CardLayout) panel.getLayout()).show(panel, cardName);
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
        for (RangeParam param : getActiveRangeParams()) {
            param.setValue(0);
        }
        resetButton.setEnabled(false);
    }

    private void updateResetButtonState() {
        resetButton.setEnabled(hasNonZeroEnlargement());
    }

    private boolean hasNonZeroEnlargement() {
        for (RangeParam param : getActiveRangeParams()) {
            if (!param.isZero()) {
                return true;
            }
        }
        return false;
    }

    private void sliderChanged() {
        previewPanel.repaint();
        updateResetButtonState();
    }

    /**
     * Synchronizes values between pixel and percentage sliders when switching units.
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
            case PERCENTAGE -> percent.setValue(
                pixels.getValueAsDouble() * 100 / pixels.getMaximum(), false);
        }
    }

    private int getEnlargementInPixels(RangeParam pixels, RangeParam percent, int canvasDim) {
        if (usePixels()) {
            return pixels.getValue();
        } else {
            return percentToPixels(percent, canvasDim);
        }
    }

    private int getNorth(Canvas canvas) {
        return getEnlargementInPixels(northPixels, northPercentage, canvas.getHeight());
    }

    private int getSouth(Canvas canvas) {
        return getEnlargementInPixels(southPixels, southPercentage, canvas.getHeight());
    }

    private int getWest(Canvas canvas) {
        return getEnlargementInPixels(westPixels, westPercentage, canvas.getWidth());
    }

    private int getEast(Canvas canvas) {
        return getEnlargementInPixels(eastPixels, eastPercentage, canvas.getWidth());
    }

    private boolean usePixels() {
        return usePixelsRadio.isSelected();
    }

    private static int percentToPixels(RangeParam percentParam, int fullSize) {
        return (int) (fullSize * percentParam.getValueAsDouble() / 100.0);
    }

    /**
     * Creates an {@link EnlargeCanvas} action with the current settings.
     */
    public EnlargeCanvas createCompAction(Canvas canvas) {
        return new EnlargeCanvas(
            getNorth(canvas), getEast(canvas),
            getSouth(canvas), getWest(canvas));
    }

    private RangeParam[] getActiveRangeParams() {
        if (usePixels()) {
            return new RangeParam[]{northPixels, eastPixels, southPixels, westPixels};
        } else {
            return new RangeParam[]{northPercentage, eastPercentage, southPercentage, westPercentage};
        }
    }

    @Override
    public boolean supportsUserPresets() {
        return true;
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        preset.putBoolean("Pixels", usePixels());
        for (RangeParam param : getActiveRangeParams()) {
            param.saveStateTo(preset);
        }
    }

    @Override
    public void loadUserPreset(UserPreset preset) {
        boolean usePixels = preset.getBoolean("Pixels");
        if (usePixels) {
            usePixelsRadio.doClick();
        } else {
            usePercentsRadio.doClick();
        }

        for (RangeParam param : getActiveRangeParams()) {
            param.loadStateFrom(preset);
        }
    }

    @Override
    public String getPresetDirName() {
        return "Enlarge Canvas";
    }

    /**
     * A panel that displays a preview of the enlarged canvas,
     * showing the new boundaries relative to the original image.
     */
    private class CanvasPreviewPanel extends JPanel {
        // show arrows when enlargement > 20px in preview space
        private static final int ENLARGEMENT_THRESHOLD_PIXELS = 20;
        // scale factor for fitting preview in panel
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

            // draw the new canvas boundary
            drawNewCanvasBoundary(g, dims);

            // draw the original canvas with checkerboard and image
            drawOriginalCanvas(g, dims);

            // draw direction arrows if enlargement exceeds threshold
            drawEnlargementArrows((Graphics2D) g, dims);
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
            // calculate position for original canvas
            int x = (int) (dims.centerX - dims.newWidth / 2 + dims.westEnlargement);
            int y = (int) (dims.centerY - dims.newHeight / 2 + dims.northEnlargement);
            int w = (int) dims.originalWidth;
            int h = (int) dims.originalHeight;

            // draw checkerboard background
            Graphics2D g2d = (Graphics2D) g.create();
            try {
                g2d.translate(x, y);
                checkerboard.paint(g2d, null, w, h);
            } finally {
                g2d.dispose();
            }

            // draw preview image on top
            g.drawImage(previewImg, x, y, w, h, null);
        }

        /**
         * Draws direction arrows when enlargement exceeds threshold in any direction.
         */
        private static void drawEnlargementArrows(Graphics2D g, PreviewDimensions dims) {
            g.setColor(Color.WHITE);
            g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

            // get the coordinates of the original canvas in preview space
            float origLeft = dims.centerX - dims.newWidth / 2 + dims.westEnlargement;
            float origRight = origLeft + dims.originalWidth;
            float origTop = dims.centerY - dims.newHeight / 2 + dims.northEnlargement;
            float origBottom = origTop + dims.originalHeight;

            // get the coordinates of the enlarged canvas in preview space
            float enlargedLeft = dims.centerX - dims.newWidth / 2;
            float enlargedRight = dims.centerX + dims.newWidth / 2;
            float enlargedTop = dims.centerY - dims.newHeight / 2;
            float enlargedBottom = dims.centerY + dims.newHeight / 2;

            // draw arrows for each direction if enlargement exceeds threshold
            if (dims.northEnlargement > ENLARGEMENT_THRESHOLD_PIXELS) {
                float startX = origLeft + dims.originalWidth / 2;
                g.fill(Shapes.createFixedWidthArrow(startX, origTop, startX, enlargedTop));
            }
            if (dims.southEnlargement > ENLARGEMENT_THRESHOLD_PIXELS) {
                float startX = origLeft + dims.originalWidth / 2;
                g.fill(Shapes.createFixedWidthArrow(startX, origBottom, startX, enlargedBottom));
            }
            if (dims.eastEnlargement > ENLARGEMENT_THRESHOLD_PIXELS) {
                float startY = origTop + dims.originalHeight / 2;
                g.fill(Shapes.createFixedWidthArrow(origRight, startY, enlargedRight, startY));
            }
            if (dims.westEnlargement > ENLARGEMENT_THRESHOLD_PIXELS) {
                float startY = origTop + dims.originalHeight / 2;
                g.fill(Shapes.createFixedWidthArrow(origLeft, startY, enlargedLeft, startY));
            }
        }

        /**
         * Helper class to store and manage preview dimensions and calculations.
         */
        private static class PreviewDimensions {
            float originalWidth, originalHeight;
            float newWidth, newHeight;
            float northEnlargement, southEnlargement, eastEnlargement, westEnlargement;
            float centerX, centerY;
            float scale;

            /**
             * Applies scaling factor to all dimensional values.
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
         * Calculates all necessary dimensions for the preview, including
         * scaling factors and positions for both original and enlarged canvas.
         */
        private PreviewDimensions calculatePreviewDimensions(Canvas canvas) {
            PreviewDimensions dims = new PreviewDimensions();

            // original canvas dimensions
            dims.originalWidth = canvas.getWidth();
            dims.originalHeight = canvas.getHeight();

            // calculate enlargements
            dims.northEnlargement = getNorth(canvas);
            dims.westEnlargement = getWest(canvas);
            dims.eastEnlargement = getEast(canvas);
            dims.southEnlargement = getSouth(canvas);

            dims.newWidth = dims.westEnlargement + dims.originalWidth + dims.eastEnlargement;
            dims.newHeight = dims.northEnlargement + dims.originalHeight + dims.southEnlargement;

            // calculate center point of preview panel
            dims.centerX = getWidth() / 2.0f;
            dims.centerY = getHeight() / 2.0f;

            // calculate scaling factor to fit preview in panel
            float widthRatio = dims.newWidth / getWidth();
            float heightRatio = dims.newHeight / getHeight();
            dims.scale = PREVIEW_SCALE_FACTOR / Math.max(widthRatio, heightRatio);

            // apply scaling to all dimensions
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
