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

package pixelitor.gui.utils;

import pixelitor.filters.gui.*;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import static java.awt.BorderLayout.*;
import static java.awt.Color.GRAY;
import static java.awt.FlowLayout.LEFT;
import static javax.swing.BorderFactory.createTitledBorder;

/**
 * A GUI component for selecting a value within a range.
 * It consists of a JSlider and a JSpinner synchronized with each other,
 * allowing users to select values either by sliding or direct numeric
 * input.
 * It optionally includes a {@link ResetButton} for restoring the default value.
 */
public class SliderSpinner extends JPanel implements ParamGUI {
    // special-case ranges and their major tick spacings
    private static final int RANGE_90 = 90;
    private static final int MAJOR_SPACING_90 = 15;

    // 8-bit channel range (Levels, Solarize, etc.)
    private static final int RANGE_255 = 255;
    private static final int MAJOR_SPACING_255 = 51;

    // zoom sliders: min = 1, max < 702, range % 100 == 0
    private static final int ZOOM_MAJOR_TICK_SPACING = 100;
    private static final int ZOOM_MAX_LIMIT = 702;

    // the preferred numbers of major intervals for automatic ticks, in order of preference
    private static final int[] PREFERRED_INTERVAL_COUNTS = {4, 5, 6, 3, 2};

    /**
     * Major and minor tick spacings for a slider.
     */
    public record TickSpacing(int major, int minor) {
    }

    /**
     * The possible positions of the label of a {@link SliderSpinner}.
     */
    public enum LabelPosition {
        BORDER(true),
        WEST(false),
        WEST_WITH_TICKS(true),
        NORTH(false),
        NONE(false),
        NONE_WITH_TICKS(true);

        private final boolean showTicks;

        LabelPosition(boolean showTicks) {
            this.showTicks = showTicks;
        }

        boolean shouldShowTicks() {
            return showTicks;
        }

        public boolean isWest() {
            return this == WEST || this == WEST_WITH_TICKS;
        }
    }

    /**
     * A slider label that paints its own tick mark above its text
     * (or to the left of it, for vertical sliders). Used for explicitly
     * configured ticks, because the slider UI can paint ticks only at
     * min + k * spacing. A label without text paints only a shorter tick,
     * this is used for the minor ticks.
     */
    private static class TickLabel extends JLabel {
        private static final int MAJOR_TICK_LENGTH = 6;
        private static final int MINOR_TICK_LENGTH = 3;
        private static final int TEXT_GAP = 2;

        private final boolean vertical;
        private final int tickLength;

        TickLabel(String text, boolean vertical) {
            super(text, SwingConstants.CENTER);
            this.vertical = vertical;

            boolean hasText = text != null;
            tickLength = hasText ? MAJOR_TICK_LENGTH : MINOR_TICK_LENGTH;

            // the space before the text, where the tick is painted
            int tickSpace = hasText ? tickLength + TEXT_GAP : tickLength;
            // a tick-only label must be at least one pixel wide
            // (or high), otherwise Swing doesn't paint it
            int thickness = hasText ? 0 : 1;
            setBorder(vertical
                ? BorderFactory.createEmptyBorder(0, tickSpace, thickness, 0)
                : BorderFactory.createEmptyBorder(tickSpace, 0, 0, thickness));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Color color = isEnabled()
                ? UIManager.getColor("Slider.tickColor")
                : UIManager.getColor("Label.disabledForeground");
            g.setColor(color != null ? color : getForeground());

            if (vertical) {
                int y = getHeight() / 2;
                g.drawLine(0, y, tickLength - 1, y);
            } else {
                int x = getWidth() / 2;
                g.drawLine(x, 0, x, tickLength - 1);
            }
        }
    }

    public static final int HORIZONTAL = JSlider.HORIZONTAL;
    public static final int VERTICAL = JSlider.VERTICAL;
    private final int orientation;

    private final JSlider slider;
    private final JSpinner spinner;
    private ResetButton resetButton;
    private final RangeParam model;

    private Color startColor;
    private Color endColor;
    private final boolean hasGradient;

    private JLabel label;
    private final LabelPosition labelPosition;
    private final JPanel controlPanel;

    private boolean syncing;

    public SliderSpinner(RangeParam model, LabelPosition position, boolean addResetButton) {
        this(model, position, addResetButton, HORIZONTAL);
    }

    public SliderSpinner(RangeParam model, LabelPosition position, boolean addResetButton, int orientation) {
        this(model, null, null, position, addResetButton, orientation);
    }

    public SliderSpinner(RangeParam model, Color startColor, Color endColor, int orientation) {
        this(model, startColor, endColor, LabelPosition.BORDER, true, orientation);
    }

    public SliderSpinner(RangeParam model, Color startColor, Color endColor) {
        this(model, startColor, endColor, LabelPosition.BORDER, true, HORIZONTAL);
    }

    private SliderSpinner(RangeParam model,
                          Color startColor, Color endColor,
                          LabelPosition labelPosition,
                          boolean addResetButton,
                          int orientation) {
        super(new BorderLayout());

        this.model = model;
        this.labelPosition = labelPosition;
        this.orientation = orientation;

        this.startColor = startColor;
        this.endColor = endColor;
        assert (startColor == null) == (endColor == null);
        hasGradient = startColor != null;

        if (labelPosition == LabelPosition.BORDER) {
            updateBorder();
        }

        if (labelPosition.isWest()) {
            label = new JLabel(labelText());
            add(label, WEST);
        } else if (labelPosition == LabelPosition.NORTH) {
            label = new JLabel(labelText());
            add(label, NORTH);
        }

        slider = createSlider();
        if (labelPosition.shouldShowTicks()) {
            setupTicks();
        }
        add(slider, CENTER);

        spinner = createSpinner();
        controlPanel = new JPanel(new FlowLayout(LEFT));
        controlPanel.add(spinner);

        FilterButtonModel action = model.getSideButtonModel();
        if (action != null) {
            controlPanel.add(action.createGUI());
        }

        if (addResetButton) {
            resetButton = new ResetButton(this.model);
            updateResetButton();
            controlPanel.add(resetButton);
        }

        add(controlPanel, orientation == HORIZONTAL ? EAST : SOUTH);
    }

    public static SliderSpinner from(RangeParam model) {
        return new SliderSpinner(model, LabelPosition.NONE, false);
    }

    private JSlider createSlider() {
        JSlider s = new JSlider(model);
        s.setOrientation(orientation);
        s.addChangeListener(_ -> onSliderChanged());
        return s;
    }

    private void onSliderChanged() {
        if (syncing) {
            // echo of a change that originated from the spinner
            // or from a programmatic update
            return;
        }
        // The slider has only int precision, so the spinner is
        // synchronized with the model (which is also the slider's
        // model), not with the slider's value.
        // (the guard against the spinner's echo is in setSpinnerValueOnly)
        syncSpinnerWithModel();

        updateResetButton();
    }

    private JSpinner createSpinner() {
        assert model.checkInvariants();

        int decimalPlaces = model.getDecimalPlaces();
        JSpinner s = new JSpinner(createSpinnerModel(model, decimalPlaces));

        if (decimalPlaces > 0) {
            configureDecimalFormat(model, s, decimalPlaces);
        }

        s.addChangeListener(_ -> onSpinnerChanged());
        return s;
    }

    private void onSpinnerChanged() {
        if (syncing) {
            // echo of a change that originated from the slider
            // or from a programmatic update
            return;
        }
        // synchronize the model (and thus the slider) with the spinner value
        runWithoutFeedback(() ->
            // (the spinner returns a Double or an Integer)
            model.setValue(((Number) spinner.getValue()).doubleValue(), true));

        updateResetButton();
    }

    /**
     * Runs the given action while the feedback guard is set, so that the
     * slider and spinner listeners ignore the echo events caused by it.
     * The previous value is restored afterward, so calls can be nested.
     * <p>
     * This guard has nothing to do with {@link JSlider#getValueIsAdjusting()}.
     */
    private void runWithoutFeedback(Runnable action) {
        boolean wasSyncing = syncing;
        syncing = true;
        try {
            action.run();
        } finally {
            syncing = wasSyncing;
        }
    }

    private static SpinnerNumberModel createSpinnerModel(RangeParam model, int decimalPlaces) {
        if (decimalPlaces > 0) {
            double stepSize = switch (decimalPlaces) {
                case 1 -> 0.1;
                case 2 -> 0.01;
                default -> throw new IllegalStateException();
            };
            return new SpinnerNumberModel( // constructor using doubles
                model.getValueAsDouble(),
                model.getMinimum(),
                model.getMaximum(),
                stepSize);
        } else {
            return new SpinnerNumberModel( // constructor using ints
                model.getValue(),
                model.getMinimum(),
                model.getMaximum(),
                1);
        }
    }

    private static void configureDecimalFormat(RangeParam model, JSpinner s, int decimalPlaces) {
        var editor = (JSpinner.NumberEditor) s.getEditor();
        DecimalFormat format = editor.getFormat();
        format.setMinimumFractionDigits(decimalPlaces);
        format.setMaximumFractionDigits(decimalPlaces);

        // make sure that the fractional form is displayed
        editor.getTextField().setValue(model.getValueAsDouble());
    }

    public void addExplicitResetButton(ResetButton resetButton) {
        assert this.resetButton == null;

        this.resetButton = resetButton;
        controlPanel.add(resetButton);
    }

    private void updateBorder() {
        String title = model.getName();
        setBorder(hasGradient
            ? createTitledBorder(new GradientBorder(startColor, endColor), title)
            : createTitledBorder(title));
    }

    private String labelText() {
        return model.getName() + ": ";
    }

    /**
     * Determines whether the given min/max bounds describe a zoom percentage slider
     * (starts at 1% and steps by 100% increments up to limit).
     */
    static boolean isZoomSlider(int min, int max) {
        int range = max - min;
        return min == 1 && max < ZOOM_MAX_LIMIT && range > 0 && range % ZOOM_MAJOR_TICK_SPACING == 0;
    }

    /**
     * Computes the automatic major and minor tick spacings for the specified
     * range bounds. Used if the ticks are not configured explicitly
     * (and always for ranges adjusted to the canvas size).
     * <p>
     * The spacing is chosen so that the ticks (counted from the minimum)
     * always reach the maximum. For symmetric ranges (min = -max) it also
     * divides the maximum, so that 0 gets a tick and the negative ticks
     * mirror the positive ones.
     */
    public static TickSpacing calcTickSpacing(int min, int max) {
        assert min < max : "min (" + min + ") >= max (" + max + ")";

        int range = max - min;

        if (isZoomSlider(min, max)) {
            return new TickSpacing(ZOOM_MAJOR_TICK_SPACING, 0);
        }
        if (range == RANGE_90) {
            return new TickSpacing(MAJOR_SPACING_90, 0);
        }
        if (range == RANGE_255) { // Levels, Solarize, etc.
            return new TickSpacing(MAJOR_SPACING_255, 0);
        }
        if (range < 11) { // small ranges
            return new TickSpacing(1, 0);
        }

        // the spacing must divide this span, otherwise the ticks
        // would not reach the max (or would not be symmetrical)
        boolean symmetric = min < 0 && max == -min;
        int span = symmetric ? max : range;

        for (int intervals : PREFERRED_INTERVAL_COUNTS) {
            if (range % intervals != 0) {
                continue;
            }
            int major = range / intervals;
            if (span % major == 0) {
                int minor = major % 2 == 0 ? major / 2 : 0;
                return new TickSpacing(major, minor);
            }
        }

        // No nice spacing exists, so only the ends get ticks.
        // (This is never reached for symmetric ranges, because
        // two intervals always fit.)
        return new TickSpacing(span, 0);
    }

    /**
     * Calculates the positions of the major ticks for explicitly configured ticks:
     * the minimum, followed by all multiples of the major spacing
     * (counted from 0, not from the minimum) up to the maximum.
     * Throws an exception if the maximum doesn't get a tick,
     * because that means that the ticks are misconfigured.
     */
    public static List<Integer> calcMajorTickPositions(int min, int max, int major) {
        var positions = new ArrayList<Integer>();
        positions.add(min);

        // the first multiple of major that is greater than min
        int first = (Math.floorDiv(min, major) + 1) * major;
        for (int pos = first; pos <= max; pos += major) {
            positions.add(pos);
        }

        if (positions.getLast() != max) {
            throw new IllegalStateException("The max value (%d) doesn't get a tick with a major spacing of %d (ticks: %s)"
                .formatted(max, major, positions));
        }
        return positions;
    }

    public void setupTicks() {
        TickSpacing explicit = model.getExplicitTicks();
        if (explicit != null) {
            setupExplicitTicks(explicit);
            return;
        }

        int min = model.getMinimum();
        int max = model.getMaximum();
        TickSpacing spacing = calcTickSpacing(min, max);
        var labels = createLabelTable(min, max, spacing.major());
        applyTicks(spacing.major(), spacing.minor(), labels);
    }

    private void setupExplicitTicks(TickSpacing spacing) {
        int min = model.getMinimum();
        int max = model.getMaximum();
        boolean vertical = orientation == VERTICAL;

        Hashtable<Integer, JComponent> labels = new Hashtable<>();
        if (spacing.minor() > 0) {
            int minor = spacing.minor();
            for (int pos = Math.ceilDiv(min, minor) * minor; pos <= max; pos += minor) {
                labels.put(pos, new TickLabel(null, vertical));
            }
        }
        // the major ticks are added after the minor ones,
        // so they replace the minor ticks at the same positions
        for (int pos : calcMajorTickPositions(min, max, spacing.major())) {
            labels.put(pos, new TickLabel(String.valueOf(pos), vertical));
        }

        // the labels paint the ticks, because the slider UI
        // can only paint them at min + k * spacing
        slider.setPaintTicks(false);
        slider.setMajorTickSpacing(0);
        slider.setMinorTickSpacing(0);
        slider.setLabelTable(labels);
        slider.setPaintLabels(true);
    }

    public void setupTicks(int majorSpacing, int minorSpacing) {
        var labels = majorSpacing > 0 ? slider.createStandardLabels(majorSpacing) : null;
        applyTicks(majorSpacing, minorSpacing, labels);
    }

    private void applyTicks(int major, int minor, Hashtable<Integer, JComponent> labels) {
        slider.setMajorTickSpacing(major);
        slider.setMinorTickSpacing(minor);
        slider.setLabelTable(labels);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
    }

    private Hashtable<Integer, JComponent> createLabelTable(int min, int max, int major) {
        if (isZoomSlider(min, max)) {
            Hashtable<Integer, JComponent> labels = slider.createStandardLabels(max + 1); // only min
            labels.putAll(slider.createStandardLabels(ZOOM_MAJOR_TICK_SPACING, ZOOM_MAJOR_TICK_SPACING));
            return labels;
        }
        return slider.createStandardLabels(major);
    }

    public int getValue() {
        return model.getValue();
    }

    public void setValue(int value) {
        model.setValue(value);
    }

    private void updateResetButton() {
        if (resetButton != null) {
            if (hasGradient) {
                updateResetButtonColor();
            }
            resetButton.updateState();
        }
    }

    private void updateResetButtonColor() {
        Color bg;
        if (model.isAtDefault()) {
            bg = GRAY;
        } else if (model.getValueAsDouble() > model.getDefaultValue()) {
            bg = endColor;
        } else {
            bg = startColor;
        }
        resetButton.setBackground(bg);
    }

    /**
     * Converts a value to the number type that the spinner's
     * SpinnerNumberModel works with: a Double if the model has decimal
     * places, otherwise an Integer.
     * <p>
     * The spinner model's bounds and step are Doubles or Integers (see
     * createSpinnerModel), and its getNextValue/getPreviousValue throw a
     * ClassCastException if it holds an Integer while the bounds are Doubles.
     */
    private Number toSpinnerValue(double value) {
        if (model.getDecimalPlaces() > 0) {
            return Double.valueOf(value);
        } else {
            return Integer.valueOf((int) value);
        }
    }

    /**
     * Sets the spinner's value from the model, with full precision.
     */
    private void syncSpinnerWithModel() {
        setSpinnerValueOnly(toSpinnerValue(model.getValueAsDouble()));
    }

    /**
     * Changes the spinner's value without feeding the change back to the model.
     */
    private void setSpinnerValueOnly(Number value) {
        runWithoutFeedback(() -> spinner.setValue(value));
    }

    public void forceSpinnerValueOnly(int value) {
        setSpinnerValueOnly(toSpinnerValue(value));
    }

    /**
     * Intentionally a no-op. The RangeParam is also the slider's model
     * and fires a change event on every value change, and the slider's
     * change listener ({@link #onSliderChanged}) synchronizes the spinner
     * at full precision in response. The GUI is therefore always up to date.
     * (Required by {@link ParamGUI}.)
     */
    @Override
    public void updateGUI() {
        // nothing to do
    }

    /**
     * Updates the visual appearance of the component based on its model's current state.
     * This includes the title, colors, and gradients.
     */
    public void updateAppearance(boolean revalidate) {
        if (model instanceof RangeWithColorsParam rwc) {
            startColor = rwc.getLeftColor();
            endColor = rwc.getRightColor();
        }

        if (labelPosition == LabelPosition.BORDER) {
            updateBorder();
        } else if (label != null) {
            label.setText(labelText());
        }

        updateResetButton();

        if (revalidate) {
            revalidate();
            repaint();
        }
    }

    /**
     * Sets an independently created label reference.
     */
    public void setLabel(JLabel label) {
        this.label = label;
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    public RangeParam getModel() {
        return model;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        slider.setEnabled(enabled);
        spinner.setEnabled(enabled);
        if (label != null) {
            label.setEnabled(enabled);
        }
        if (resetButton != null) {
            resetButton.setEnabled(enabled);
        }
    }

    @Override
    public void setToolTip(String tip) {
        slider.setToolTipText(tip);
        spinner.setToolTipText(tip);
    }

    public void addChangeListener(ChangeListener listener) {
        slider.addChangeListener(listener);
    }

    /**
     * Updates the range (min/max) of the components based on the model.
     * Called when the model's range changes.
     */
    public void updateRange() {
        // suppress event feedback during programmatic range updates
        runWithoutFeedback(() -> {
            // update spinner limits
            SpinnerNumberModel spinnerModel = (SpinnerNumberModel) spinner.getModel();
            if (model.getDecimalPlaces() > 0) {
                spinnerModel.setMinimum((double) model.getMinimum());
                spinnerModel.setMaximum((double) model.getMaximum());
            } else {
                spinnerModel.setMinimum(model.getMinimum());
                spinnerModel.setMaximum(model.getMaximum());
            }
            // update ticks (recalculates spacing based on new range)
            if (labelPosition.shouldShowTicks()) {
                setupTicks();
            }

            // update the value in the spinner to ensure it is within
            // the new range and reflects any changes in the model
            syncSpinnerWithModel();
        });

        if (resetButton != null) {
            resetButton.setToolTipText(model.getResetToolTip());
        }
        updateResetButton();
        revalidate();
        repaint();
    }

    @Override
    public void dispose() {
        slider.setModel(new DefaultBoundedRangeModel());
    }

    // overridden so that AssertJSwing tests find the components easily
    @Override
    public void setName(String name) {
        super.setName(name);
        slider.setName(name);
        spinner.setName(name);
    }

    @Override
    public int getNumLayoutColumns() {
        return labelPosition == LabelPosition.NONE ? 2 : 1;
    }
}
