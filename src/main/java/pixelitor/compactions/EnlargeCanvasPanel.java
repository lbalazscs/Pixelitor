/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.gui.GUIText;
import pixelitor.gui.utils.SliderSpinner;
import pixelitor.utils.*;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

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

    private static final String PRESET_KEY_USE_PIXELS = "Pixels";

    // percentage-based sliders (0-100%)
    private final RangeParam topPercentage = new RangeParam(GUIText.TOP, 0, 0, 100);
    private final RangeParam rightPercentage = new RangeParam(GUIText.RIGHT, 0, 0, 100);
    private final RangeParam bottomPercentage = new RangeParam(GUIText.BOTTOM, 0, 0, 100);
    private final RangeParam leftPercentage = new RangeParam(GUIText.LEFT, 0, 0, 100);

    // pixel-based sliders (0 to the corresponding canvas width or height)
    private final RangeParam topPixels;
    private final RangeParam rightPixels;
    private final RangeParam bottomPixels;
    private final RangeParam leftPixels;

    private final List<RangeParam> pixelParams;
    private final List<RangeParam> percentParams;

    private final JRadioButton usePixelsRadio = new JRadioButton("Pixels");
    private final JRadioButton usePercentsRadio = new JRadioButton("Percentage");
    private final JButton resetButton = new JButton("Reset", Icons.getResetIcon());
    private final PreviewPanel previewPanel = new PreviewPanel();

    private final JPanel topCardPanel;
    private final JPanel rightCardPanel;
    private final JPanel bottomCardPanel;
    private final JPanel leftCardPanel;

    private enum LinkMode {
        NONE("None"),
        UNIFORM_BORDER("Uniform Border"),
        KEEP_ASPECT_RATIO("Keep Aspect Ratio"),
        CENTER_HOR("Center Horizontally"),
        CENTER_VER("Center Vertically");

        private final String displayName;

        LinkMode(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private final JComboBox<LinkMode> linkModeComboBox = new JComboBox<>(LinkMode.values());
    private boolean propagatingLinkUpdate = false;

    /**
     * Tracks a {@link ChangeListener} registered on a {@link RangeParam} so the listener can be removed later.
     */
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
        topPixels = new RangeParam(GUIText.TOP, 0, 0, c.getHeight());
        rightPixels = new RangeParam(GUIText.RIGHT, 0, 0, c.getWidth());
        bottomPixels = new RangeParam(GUIText.BOTTOM, 0, 0, c.getHeight());
        leftPixels = new RangeParam(GUIText.LEFT, 0, 0, c.getWidth());

        pixelParams = List.of(topPixels, rightPixels, bottomPixels, leftPixels);
        percentParams = List.of(topPercentage, rightPercentage, bottomPercentage, leftPercentage);

        topCardPanel = createSliderCardPanel(topPercentage, topPixels, "top", SliderSpinner.HORIZONTAL);
        rightCardPanel = createSliderCardPanel(rightPercentage, rightPixels, "right", SliderSpinner.VERTICAL);
        bottomCardPanel = createSliderCardPanel(bottomPercentage, bottomPixels, "bottom", SliderSpinner.HORIZONTAL);
        leftCardPanel = createSliderCardPanel(leftPercentage, leftPixels, "left", SliderSpinner.VERTICAL);

        setupConstraintsControls();
        setupRadioButtons();
        setupResetButton();
        addComponentsToLayout();

        usePixelsRadio.doClick(); // default to pixel mode
    }

    private void setupConstraintsControls() {
        linkModeComboBox.setSelectedItem(LinkMode.NONE);
        linkModeComboBox.addActionListener(_ -> updateConstraints());
    }

    private void addComponentsToLayout() {
        GridBagConstraints c = new GridBagConstraints();

        // constraints controls at the top
        JPanel constraintsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        constraintsPanel.add(new JLabel("Constraints:"));
        constraintsPanel.add(linkModeComboBox);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 3;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(5, 0, 5, 0);
        add(constraintsPanel, c);

        c.gridwidth = 1;
        c.insets = new Insets(0, 0, 0, 0);
        c.anchor = GridBagConstraints.CENTER;

        c.gridx = 1;
        c.gridy = 1;
        add(topCardPanel, c);

        c.gridx = 2;
        c.gridy = 2;
        add(rightCardPanel, c);

        c.gridx = 1;
        c.gridy = 3;
        add(bottomCardPanel, c);

        c.gridx = 0;
        c.gridy = 2;
        add(leftCardPanel, c);

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
        percentGUI.addChangeListener(_ -> sliderChanged());

        var pixelGUI = new SliderSpinner(pixels, BORDER, false, orientation);
        pixelGUI.setName(sliderName);
        pixelGUI.addChangeListener(_ -> sliderChanged());

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

        usePixelsRadio.addActionListener(_ -> setUnit(ResizeUnit.PIXELS));
        usePercentsRadio.addActionListener(_ -> setUnit(ResizeUnit.PERCENTAGE));

        ButtonGroup unitButtonGroup = new ButtonGroup();
        unitButtonGroup.add(usePixelsRadio);
        unitButtonGroup.add(usePercentsRadio);

        Box unitContainer = new Box(BoxLayout.Y_AXIS);
        unitContainer.add(usePixelsRadio);
        unitContainer.add(usePercentsRadio);
        add(unitContainer, c);
    }

    private void setUnit(ResizeUnit newUnit) {
        syncAllPairsTo(newUnit);
        String cardName = switch (newUnit) {
            case PIXELS -> PIXEL_CARD;
            case PERCENTAGE -> PERCENT_CARD;
            case CENTIMETERS, INCHES -> throw new IllegalArgumentException("newUnit = " + newUnit);
        };

        showCard(topCardPanel, cardName);
        showCard(rightCardPanel, cardName);
        showCard(bottomCardPanel, cardName);
        showCard(leftCardPanel, cardName);
    }

    private static void showCard(JPanel panel, String cardName) {
        ((CardLayout) panel.getLayout()).show(panel, cardName);
    }

    private void setupResetButton() {
        resetButton.setEnabled(false);
        resetButton.addActionListener(_ -> reset());

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
        syncPair(topPixels, topPercentage, newUnit);
        syncPair(rightPixels, rightPercentage, newUnit);
        syncPair(leftPixels, leftPercentage, newUnit);
        syncPair(bottomPixels, bottomPercentage, newUnit);
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

    private void updateConstraints() {
        clearLinks();
        LinkMode selectedMode = (LinkMode) linkModeComboBox.getSelectedItem();
        if (selectedMode == null) {
            return;
        }

        switch (selectedMode) {
            case CENTER_HOR -> {
                linkPair(rightPixels, leftPixels);
                linkPair(rightPercentage, leftPercentage);
            }
            case CENTER_VER -> {
                linkPair(topPixels, bottomPixels);
                linkPair(topPercentage, bottomPercentage);
            }
            case UNIFORM_BORDER -> linkAll(true);
            case KEEP_ASPECT_RATIO -> linkAll(false);
            case NONE -> { /* no links needed */ }
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
        if (propagatingLinkUpdate) {
            return;
        }
        propagatingLinkUpdate = true;
        target.setValue(source.getValueAsDouble(), true);
        propagatingLinkUpdate = false;
    }

    private void linkAll(boolean basedOnPixels) {
        for (RangeParam source : pixelParams) {
            activeLinks.add(new ListenerSubscription(source, e ->
                onLinkAllChanged(source, basedOnPixels)));
        }
        for (RangeParam source : percentParams) {
            activeLinks.add(new ListenerSubscription(source, e ->
                onLinkAllChanged(source, basedOnPixels)));
        }
    }

    private void onLinkAllChanged(RangeParam source, boolean basedOnPixels) {
        if (propagatingLinkUpdate) {
            return;
        }
        propagatingLinkUpdate = true;

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

        propagatingLinkUpdate = false;
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
        int index = percentParams.indexOf(percentParam);
        if (index == -1) {
            throw new IllegalArgumentException("Unknown percentage param: " + percentParam.getName());
        }
        // the two lists use the same index order
        return pixelParams.get(index);
    }

    private int getEnlargementInPixels(RangeParam pixels, RangeParam percent, int canvasDim) {
        return usePixels() ? pixels.getValue() : percentToPixels(percent, canvasDim);
    }

    private int getTop(Canvas canvas) {
        return getEnlargementInPixels(topPixels, topPercentage, canvas.getHeight());
    }

    private int getBottom(Canvas canvas) {
        return getEnlargementInPixels(bottomPixels, bottomPercentage, canvas.getHeight());
    }

    private int getLeft(Canvas canvas) {
        return getEnlargementInPixels(leftPixels, leftPercentage, canvas.getWidth());
    }

    private int getRight(Canvas canvas) {
        return getEnlargementInPixels(rightPixels, rightPercentage, canvas.getWidth());
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
            getTop(canvas), getRight(canvas),
            getBottom(canvas), getLeft(canvas));
    }

    private List<RangeParam> getActiveRangeParams() {
        return usePixels() ? pixelParams : percentParams;
    }

    @Override
    public boolean supportsUserPresets() {
        return true;
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        preset.putBoolean(PRESET_KEY_USE_PIXELS, usePixels());
        for (RangeParam param : getActiveRangeParams()) {
            param.saveStateTo(preset);
        }
    }

    @Override
    public void loadUserPreset(UserPreset preset) {
        boolean usePixels = preset.getBoolean(PRESET_KEY_USE_PIXELS);
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
    private class PreviewPanel extends JPanel {
        // show arrows when enlargement > 20px in preview space
        private static final int ENLARGEMENT_THRESHOLD_PIXELS = 20;
        // scale factor for fitting preview in panel
        private static final float PREVIEW_SCALE_FACTOR = 0.75f;

        private static final Color ENLARGED_CANVAS_BACKGROUND = new Color(136, 139, 146);
        private static final CheckerboardPainter checkerboard = ImageUtils.createCheckerboardPainter();
        private BufferedImage previewImg;

        PreviewPanel() {
            addComponentListener(new PreviewResizeListener());
            setBorder(createTitledBorder("Preview"));
        }

        void updatePreviewImage(Composition comp) {
            if (comp != null) {
                BufferedImage actualImage = comp.getCompositeImage();
                previewImg = Thumbnails.createThumbnail(actualImage, getWidth() / 2, null);
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
            PreviewLayout layout = calcPreviewLayout(canvas);

            drawNewCanvasBoundary(g, layout);
            drawOriginalCanvas(g, layout);
            drawEnlargementArrows((Graphics2D) g, layout);
        }

        /**
         * Draws the boundary of the new enlarged canvas.
         */
        private static void drawNewCanvasBoundary(Graphics g, PreviewLayout layout) {
            g.setColor(ENLARGED_CANVAS_BACKGROUND);
            g.fillRect(
                Math.round(layout.centerX - layout.newWidth / 2),
                Math.round(layout.centerY - layout.newHeight / 2),
                Math.round(layout.newWidth),
                Math.round(layout.newHeight)
            );
        }

        /**
         * Draws the original canvas with a checkerboard background and a preview image.
         */
        private void drawOriginalCanvas(Graphics g, PreviewLayout layout) {
            int x = Math.round(layout.centerX - layout.newWidth / 2 + layout.leftEnlargement);
            int y = Math.round(layout.centerY - layout.newHeight / 2 + layout.topEnlargement);
            int w = Math.round(layout.originalWidth);
            int h = Math.round(layout.originalHeight);

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
        private static void drawEnlargementArrows(Graphics2D g, PreviewLayout layout) {
            g.setColor(Color.WHITE);
            g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

            // coordinates of the original canvas in preview space
            float origLeft = layout.centerX - layout.newWidth / 2 + layout.leftEnlargement;
            float origRight = origLeft + layout.originalWidth;
            float origTop = layout.centerY - layout.newHeight / 2 + layout.topEnlargement;
            float origBottom = origTop + layout.originalHeight;

            // coordinates of the enlarged canvas in preview space
            float enlargedLeft = layout.centerX - layout.newWidth / 2;
            float enlargedRight = layout.centerX + layout.newWidth / 2;
            float enlargedTop = layout.centerY - layout.newHeight / 2;
            float enlargedBottom = layout.centerY + layout.newHeight / 2;

            float centerX = origLeft + layout.originalWidth / 2;
            float centerY = origTop + layout.originalHeight / 2;

            drawArrowIfLargeEnough(g, layout.topEnlargement, centerX, origTop, centerX, enlargedTop);
            drawArrowIfLargeEnough(g, layout.bottomEnlargement, centerX, origBottom, centerX, enlargedBottom);
            drawArrowIfLargeEnough(g, layout.rightEnlargement, origRight, centerY, enlargedRight, centerY);
            drawArrowIfLargeEnough(g, layout.leftEnlargement, origLeft, centerY, enlargedLeft, centerY);
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
        private static class PreviewLayout {
            float originalWidth, originalHeight;
            float newWidth, newHeight;
            float topEnlargement, bottomEnlargement, rightEnlargement, leftEnlargement;
            float centerX, centerY;
            float scale;

            /**
             * Applies a scaling factor to all dimensional values.
             */
            void applyScaling() {
                originalWidth *= scale;
                originalHeight *= scale;
                newWidth *= scale;
                newHeight *= scale;
                topEnlargement *= scale;
                bottomEnlargement *= scale;
                rightEnlargement *= scale;
                leftEnlargement *= scale;
            }
        }

        /**
         * Calculates all necessary metrics for the preview.
         */
        private PreviewLayout calcPreviewLayout(Canvas canvas) {
            PreviewLayout layout = new PreviewLayout();

            layout.originalWidth = canvas.getWidth();
            layout.originalHeight = canvas.getHeight();

            layout.topEnlargement = getTop(canvas);
            layout.leftEnlargement = getLeft(canvas);
            layout.rightEnlargement = getRight(canvas);
            layout.bottomEnlargement = getBottom(canvas);

            layout.newWidth = layout.leftEnlargement + layout.originalWidth + layout.rightEnlargement;
            layout.newHeight = layout.topEnlargement + layout.originalHeight + layout.bottomEnlargement;

            layout.centerX = getWidth() / 2.0f;
            layout.centerY = getHeight() / 2.0f;

            // calculate scaling factor to fit preview in panel
            float widthRatio = layout.newWidth / getWidth();
            float heightRatio = layout.newHeight / getHeight();
            layout.scale = PREVIEW_SCALE_FACTOR / Math.max(widthRatio, heightRatio);

            layout.applyScaling();

            return layout;
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
