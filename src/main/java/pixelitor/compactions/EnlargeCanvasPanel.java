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
import pixelitor.utils.CustomShapes;
import pixelitor.utils.Icons;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.ResizeUnit;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

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

    private final List<RangeParam> pixelParams;
    private final List<RangeParam> percentParams;
    private final Map<RangeParam, RangeParam> percentToPixelMap;

    private final JRadioButton usePixelsRadio = new JRadioButton("Pixels");
    private final JRadioButton usePercentsRadio = new JRadioButton("Percentage");
    private final JButton resetButton = new JButton("Reset", Icons.getResetIcon());
    private final CanvasPreviewPanel previewPanel = new CanvasPreviewPanel();

    private final JPanel northCardPanel;
    private final JPanel eastCardPanel;
    private final JPanel southCardPanel;
    private final JPanel westCardPanel;

    private enum SymmetryMode {
        INDEPENDENT("None"),
        UNIFORM_BORDER("Uniform Border"),
        KEEP_ASPECT_RATIO("Keep Aspect Ratio"),
        HORIZONTAL("Horizontal"),
        VERTICAL("Vertical");

        private final String displayName;

        SymmetryMode(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private final JComboBox<SymmetryMode> symmetryComboBox = new JComboBox<>(SymmetryMode.values());
    private boolean isUpdatingFromLink = false;

    private record ListenerSubscription(RangeParam param, ChangeListener listener) {
        private ListenerSubscription {
            param.addChangeListener(listener);
        }

        void unsubscribe() {
            param.removeChangeListener(listener);
        }
    }

    private final List<ListenerSubscription> activeLinks = new ArrayList<>();

    EnlargeCanvasPanel() {
        setLayout(new GridBagLayout());

        Canvas c = Views.getActiveComp().getCanvas();
        northPixels = new RangeParam("North", 0, 0, c.getHeight());
        eastPixels = new RangeParam("East", 0, 0, c.getWidth());
        southPixels = new RangeParam("South", 0, 0, c.getHeight());
        westPixels = new RangeParam("West", 0, 0, c.getWidth());

        pixelParams = List.of(northPixels, eastPixels, southPixels, westPixels);
        percentParams = List.of(northPercentage, eastPercentage, southPercentage, westPercentage);

        percentToPixelMap = new IdentityHashMap<>();
        percentToPixelMap.put(northPercentage, northPixels);
        percentToPixelMap.put(eastPercentage, eastPixels);
        percentToPixelMap.put(southPercentage, southPixels);
        percentToPixelMap.put(westPercentage, westPixels);

//        percentToPixelMap = Map.of(
//            northPercentage, northPixels,
//            eastPercentage, eastPixels,
//            southPercentage, southPixels,
//            westPercentage, westPixels
//        );

        northCardPanel = createSliderCardPanel(northPercentage, northPixels, "north", SliderSpinner.HORIZONTAL);
        eastCardPanel = createSliderCardPanel(eastPercentage, eastPixels, "east", SliderSpinner.VERTICAL);
        southCardPanel = createSliderCardPanel(southPercentage, southPixels, "south", SliderSpinner.HORIZONTAL);
        westCardPanel = createSliderCardPanel(westPercentage, westPixels, "west", SliderSpinner.VERTICAL);

        setupSymmetryControls();
        setupRadioButtons();
        setupResetButton();
        addComponentsToLayout();

        usePixelsRadio.doClick(); // default to pixel mode
    }

    private void setupSymmetryControls() {
        symmetryComboBox.setSelectedItem(SymmetryMode.INDEPENDENT);
        symmetryComboBox.addActionListener(e -> updateSymmetry());
    }

    private void addComponentsToLayout() {
        GridBagConstraints c = new GridBagConstraints();

        // symmetry controls at the top
        JPanel symmetryPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        symmetryPanel.add(new JLabel("Link Edges:"));
        symmetryPanel.add(symmetryComboBox);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 3;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(5, 0, 5, 0);
        add(symmetryPanel, c);

        c.gridwidth = 1;
        c.insets = new Insets(0, 0, 0, 0);
        c.anchor = GridBagConstraints.CENTER;

        c.gridx = 1;
        c.gridy = 1;
        add(northCardPanel, c);

        c.gridx = 2;
        c.gridy = 2;
        add(eastCardPanel, c);

        c.gridx = 1;
        c.gridy = 3;
        add(southCardPanel, c);

        c.gridx = 0;
        c.gridy = 2;
        add(westCardPanel, c);

        c.gridx = 1;
        c.gridy = 2;
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
        c.gridy = 3;
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
        syncAllPairsTo(newUnit);
        String cardName = switch (newUnit) {
            case PIXELS -> PIXEL_CARD;
            case PERCENTAGE -> PERCENT_CARD;
            case CENTIMETERS, INCHES -> throw new IllegalArgumentException("newUnit = " + newUnit);
        };

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
        c.gridy = 3;
        c.anchor = GridBagConstraints.CENTER;
        add(resetButton, c);
    }

    private void reset() {
        for (RangeParam param : getActiveRangeParams()) {
            param.setValue(0);
        }
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
    private void syncAllPairsTo(ResizeUnit newUnit) {
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
            case CENTIMETERS, INCHES -> throw new IllegalArgumentException();
        }
    }

    private void updateSymmetry() {
        clearLinks();
        SymmetryMode selectedMode = (SymmetryMode) symmetryComboBox.getSelectedItem();
        if (selectedMode == null) {
            return;
        }

        switch (selectedMode) {
            case HORIZONTAL -> {
                linkPair(eastPixels, westPixels);
                linkPair(eastPercentage, westPercentage);
            }
            case VERTICAL -> {
                linkPair(northPixels, southPixels);
                linkPair(northPercentage, southPercentage);
            }
            case UNIFORM_BORDER -> linkAll(true);
            case KEEP_ASPECT_RATIO -> linkAll(false);
            case INDEPENDENT -> { /* no links needed */ }
        }
    }

    private void clearLinks() {
        activeLinks.forEach(ListenerSubscription::unsubscribe);
        activeLinks.clear();
    }

    private void linkPair(RangeParam p1, RangeParam p2) {
        activeLinks.add(new ListenerSubscription(p1, e -> updateLinkedParam(p1, p2)));
        activeLinks.add(new ListenerSubscription(p2, e -> updateLinkedParam(p2, p1)));
    }

    private void updateLinkedParam(RangeParam source, RangeParam target) {
        if (isUpdatingFromLink) {
            return;
        }
        isUpdatingFromLink = true;
        target.setValue(source.getValueAsDouble(), true);
        isUpdatingFromLink = false;
    }

    private void linkAll(boolean basedOnPixels) {
        List<RangeParam> allParams = new ArrayList<>();
        allParams.addAll(pixelParams);
        allParams.addAll(percentParams);

        for (RangeParam source : allParams) {
            ChangeListener listener = e -> {
                if (isUpdatingFromLink) {
                    return;
                }
                isUpdatingFromLink = true;

                if (basedOnPixels) {
                    // uniform border: sync all to a single pixel value
                    double newPixelValue = getPixelValueFromSource(source);
                    setAllPixelValues(newPixelValue);
                    syncAllPairsTo(ResizeUnit.PERCENTAGE);
                } else {
                    // keep aspect ratio: sync all to a single percentage value
                    double newPercentValue = getPercentValueFromSource(source);
                    setAllPercentValues(newPercentValue);
                    syncAllPairsTo(ResizeUnit.PIXELS);
                }

                isUpdatingFromLink = false;
            };
            activeLinks.add(new ListenerSubscription(source, listener));
        }
    }

    private double getPixelValueFromSource(RangeParam source) {
        if (pixelParams.contains(source)) {
            return source.getValueAsDouble();
        } else {
            RangeParam sourcePixel = getCorrespondingPixelParam(source);
            return source.getValueAsDouble() * sourcePixel.getMaximum() / 100.0;
        }
    }

    private double getPercentValueFromSource(RangeParam source) {
        if (percentParams.contains(source)) {
            return source.getValueAsDouble();
        } else {
            return source.getValueAsDouble() * 100.0 / source.getMaximum();
        }
    }

    private void setAllPixelValues(double value) {
        for (RangeParam pixelParam : pixelParams) {
            pixelParam.setValue(value, true);
        }
    }

    private void setAllPercentValues(double value) {
        for (RangeParam percentParam : percentParams) {
            percentParam.setValue(value, true);
        }
    }

    private RangeParam getCorrespondingPixelParam(RangeParam percentParam) {
        RangeParam pixelParam = percentToPixelMap.get(percentParam);
        if (pixelParam == null) {
            throw new IllegalArgumentException("Unknown percentage param: " + percentParam.getName());
        }
        return pixelParam;
    }

    private int getEnlargementInPixels(RangeParam pixels, RangeParam percent, int canvasDim) {
        return usePixels() ? pixels.getValue() : percentToPixels(percent, canvasDim);
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
        return usePixels()
            ? pixelParams.toArray(new RangeParam[0])
            : percentParams.toArray(new RangeParam[0]);
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
     * A panel that displays a preview of the enlarged canvas.
     */
    private class CanvasPreviewPanel extends JPanel {
        // show arrows when enlargement > 20px in preview space
        private static final int ENLARGEMENT_THRESHOLD_PIXELS = 20;
        // scale factor for fitting preview in panel
        private static final float PREVIEW_SCALE_FACTOR = 0.75f;

        private static final Color ENLARGED_AREA_COLOR = new Color(136, 139, 146);
        private static final CheckerboardPainter checkerboard = ImageUtils.createCheckerboardPainter();
        private BufferedImage previewImg;

        CanvasPreviewPanel() {
            addComponentListener(new PreviewResizeListener());
            setBorder(createTitledBorder("Preview"));
        }

        void updatePreviewImage(Composition comp) {
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

            drawNewCanvasBoundary(g, dims);
            drawOriginalCanvas(g, dims);
            drawEnlargementArrows((Graphics2D) g, dims);
        }

        /**
         * Draws the boundary of the new enlarged canvas.
         */
        private static void drawNewCanvasBoundary(Graphics g, PreviewDimensions dims) {
            g.setColor(ENLARGED_AREA_COLOR);
            g.fillRect(
                Math.round(dims.centerX - dims.newWidth / 2),
                Math.round(dims.centerY - dims.newHeight / 2),
                Math.round(dims.newWidth),
                Math.round(dims.newHeight)
            );
        }

        /**
         * Draws the original canvas with checkerboard background and preview image.
         */
        private void drawOriginalCanvas(Graphics g, PreviewDimensions dims) {
            int x = Math.round(dims.centerX - dims.newWidth / 2 + dims.westEnlargement);
            int y = Math.round(dims.centerY - dims.newHeight / 2 + dims.northEnlargement);
            int w = Math.round(dims.originalWidth);
            int h = Math.round(dims.originalHeight);

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
         * Draws direction arrows when enlargement exceeds a threshold.
         */
        private static void drawEnlargementArrows(Graphics2D g, PreviewDimensions dims) {
            g.setColor(Color.WHITE);
            g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

            // coordinates of the original canvas in preview space
            float origLeft = dims.centerX - dims.newWidth / 2 + dims.westEnlargement;
            float origRight = origLeft + dims.originalWidth;
            float origTop = dims.centerY - dims.newHeight / 2 + dims.northEnlargement;
            float origBottom = origTop + dims.originalHeight;

            // coordinates of the enlarged canvas in preview space
            float enlargedLeft = dims.centerX - dims.newWidth / 2;
            float enlargedRight = dims.centerX + dims.newWidth / 2;
            float enlargedTop = dims.centerY - dims.newHeight / 2;
            float enlargedBottom = dims.centerY + dims.newHeight / 2;

            float centerX = origLeft + dims.originalWidth / 2;
            float centerY = origTop + dims.originalHeight / 2;

            drawArrowIfLargeEnough(g, dims.northEnlargement, centerX, origTop, centerX, enlargedTop);
            drawArrowIfLargeEnough(g, dims.southEnlargement, centerX, origBottom, centerX, enlargedBottom);
            drawArrowIfLargeEnough(g, dims.eastEnlargement, origRight, centerY, enlargedRight, centerY);
            drawArrowIfLargeEnough(g, dims.westEnlargement, origLeft, centerY, enlargedLeft, centerY);
        }

        private static void drawArrowIfLargeEnough(Graphics2D g, float enlargement,
                                                   float fromX, float fromY, float toX, float toY) {
            if (enlargement > ENLARGEMENT_THRESHOLD_PIXELS) {
                g.fill(CustomShapes.createFixedWidthArrow(fromX, fromY, toX, toY));
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
         * Calculates all necessary dimensions for the preview.
         */
        private PreviewDimensions calculatePreviewDimensions(Canvas canvas) {
            PreviewDimensions dims = new PreviewDimensions();

            dims.originalWidth = canvas.getWidth();
            dims.originalHeight = canvas.getHeight();

            dims.northEnlargement = getNorth(canvas);
            dims.westEnlargement = getWest(canvas);
            dims.eastEnlargement = getEast(canvas);
            dims.southEnlargement = getSouth(canvas);

            dims.newWidth = dims.westEnlargement + dims.originalWidth + dims.eastEnlargement;
            dims.newHeight = dims.northEnlargement + dims.originalHeight + dims.southEnlargement;

            dims.centerX = getWidth() / 2.0f;
            dims.centerY = getHeight() / 2.0f;

            // calculate scaling factor to fit preview in panel
            float widthRatio = dims.newWidth / getWidth();
            float heightRatio = dims.newHeight / getHeight();
            dims.scale = PREVIEW_SCALE_FACTOR / Math.max(widthRatio, heightRatio);

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
