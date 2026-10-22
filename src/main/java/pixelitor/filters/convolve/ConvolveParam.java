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

package pixelitor.filters.convolve;

import pixelitor.filters.gui.AbstractFilterParam;
import pixelitor.filters.gui.ParamState;
import pixelitor.filters.gui.RandomizeMode;

import javax.swing.*;
import java.io.Serial;
import java.util.Arrays;
import java.util.StringJoiner;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.String.format;

/**
 * A filter parameter holding the (square) kernel matrix of a convolution.
 * The matrix is stored as a flat array in row-major order.
 */
public class ConvolveParam extends AbstractFilterParam {
    private final int kernelSize;
    private final float[] defaultKernel;

    // The array is never modified in place, only replaced, because the filter
    // can read it from a worker thread while the user is editing it on the EDT.
    private volatile float[] kernel;

    public ConvolveParam(String name, int kernelSize) {
        super(name, RandomizeMode.ALLOW);

        if (kernelSize <= 0 || kernelSize % 2 == 0) {
            throw new IllegalArgumentException("kernel size = " + kernelSize);
        }
        this.kernelSize = kernelSize;
        defaultKernel = createIdentityKernel(kernelSize);
        kernel = defaultKernel.clone();
    }

    @Override
    public JComponent createGUI() {
        return installGUI(new ConvolveParamGUI(this));
    }

    public int getKernelSize() {
        return kernelSize;
    }

    /**
     * Returns a copy of the current kernel matrix.
     */
    public float[] getKernel() {
        return kernel.clone();
    }

    /**
     * Returns the sum of all kernel values.
     */
    public float getSum() {
        return sum(kernel);
    }

    /**
     * Sets a new kernel, updates the GUI, and optionally triggers the filter.
     * The filter is triggered even if the new kernel is the same as the old one.
     */
    public void setKernel(float[] newKernel, boolean trigger) {
        checkLength(newKernel);
        kernel = newKernel.clone();

        updateGUI();

        if (trigger) {
            triggerFilter();
        }
    }

    /**
     * Called by the GUI when the user edits the text fields.
     * Doesn't update the GUI (it is already up to date) and
     * doesn't trigger the filter.
     */
    void setKernelFromGUI(float[] newKernel) {
        checkLength(newKernel);
        kernel = newKernel.clone();
    }

    /**
     * Scales the kernel so that its sum is 1, which preserves the brightness.
     * Does nothing if the sum is zero.
     */
    public void normalize(boolean trigger) {
        float[] values = kernel.clone();
        float total = sum(values);
        if (total == 0.0f) {
            return;
        }
        for (int i = 0; i < values.length; i++) {
            values[i] /= total;
        }
        setKernel(values, trigger);
    }

    @Override
    public boolean isAtDefault() {
        return Arrays.equals(kernel, defaultKernel);
    }

    @Override
    public void reset(boolean trigger) {
        setKernel(defaultKernel, trigger);
    }

    @Override
    protected void doRandomize() {
        setKernel(createRandomKernel(kernelSize), false);
    }

    @Override
    public boolean isAnimatable() {
        return true;
    }

    @Override
    public boolean requiresPresetSupport() {
        // the filter must have a "Presets" menu even though this is its only parameter
        return true;
    }

    @Override
    public ConvolveParamState copyState() {
        return new ConvolveParamState(kernel);
    }

    @Override
    public void loadStateFrom(ParamState<?> state, boolean updateGUI) {
        float[] newKernel = ((ConvolveParamState) state).kernel();
        checkLength(newKernel);

        if (updateGUI) {
            setKernel(newKernel, false);
        } else {
            kernel = newKernel;
        }
    }

    @Override
    public void loadStateFrom(String savedValue) {
        // throws an exception for invalid values, because presets can
        // be loaded at any time (for example for smart filters)
        setKernel(parsePresetString(savedValue, kernelSize * kernelSize), false);
    }

    @Override
    public String getValueAsString() {
        return Arrays.toString(kernel);
    }

    @Override
    public String toString() {
        return format("%s[name = '%s', size = %d, kernel = %s]",
            getClass().getSimpleName(), getName(), kernelSize, Arrays.toString(kernel));
    }

    private void checkLength(float[] values) {
        if (values.length != kernelSize * kernelSize) {
            throw new IllegalArgumentException("array length = " + values.length
                + ", kernel size = " + kernelSize);
        }
    }

    static float sum(float[] values) {
        float total = 0.0f;
        for (float value : values) {
            total += value;
        }
        return total;
    }

    private static float[] createIdentityKernel(int size) {
        float[] values = new float[size * size];
        values[values.length / 2] = 1.0f;
        return values;
    }

    /**
     * Returns a randomized array that is on average close to being
     * normalized: each value is uniform in [-1/n, 3/n), which
     * averages to 1/n, so the sum of all n values averages to 1.
     */
    public static float[] createRandomKernel(int size) {
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        int numValues = size * size;
        float[] kernelValues = new float[numValues];
        for (int i = 0; i < numValues; i++) {
            kernelValues[i] = rand.nextFloat() * (4.0f / numValues) - (1.0f / numValues);
        }
        return kernelValues;
    }

    private static float[] parsePresetString(String s, int expectedLength) {
        String[] parts = s.split(",", -1);
        if (parts.length != expectedLength) {
            throw new IllegalArgumentException("Expected " + expectedLength
                + " kernel values, but found " + parts.length + " in \"" + s + "\"");
        }

        float[] values = new float[expectedLength];
        for (int i = 0; i < expectedLength; i++) {
            try {
                values[i] = Float.parseFloat(parts[i].trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                    "Invalid kernel value \"" + parts[i] + "\" in \"" + s + "\"", e);
            }
            if (!Float.isFinite(values[i])) {
                throw new IllegalArgumentException(
                    "Non-finite kernel value \"" + parts[i] + "\" in \"" + s + "\"");
            }
        }
        return values;
    }

    /**
     * Encapsulates the state of a {@link ConvolveParam} as a memento object.
     * The array is defensively copied on the way in and out, so instances are immutable.
     */
    public record ConvolveParamState(float[] kernel) implements ParamState<ConvolveParamState> {
        @Serial
        private static final long serialVersionUID = 1L;

        public ConvolveParamState {
            kernel = kernel.clone();
        }

        @Override
        public float[] kernel() {
            return kernel.clone();
        }

        @Override
        public ConvolveParamState interpolate(ConvolveParamState endState, double progress) {
            if (kernel.length != endState.kernel.length) {
                throw new IllegalArgumentException("kernel lengths: "
                    + kernel.length + " and " + endState.kernel.length);
            }

            float[] result = new float[kernel.length];
            for (int i = 0; i < result.length; i++) {
                result[i] = (float) ((1.0 - progress) * kernel[i] + progress * endState.kernel[i]);
            }
            return new ConvolveParamState(result);
        }

        @Override
        public String toPresetString() {
            // Float.toString is lossless and locale-independent
            StringJoiner joiner = new StringJoiner(",");
            for (float value : kernel) {
                joiner.add(Float.toString(value));
            }
            return joiner.toString();
        }

        @Override
        public boolean equals(Object o) {
            return this == o
                || (o instanceof ConvolveParamState that && Arrays.equals(kernel, that.kernel));
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(kernel);
        }

        @Override
        public String toString() {
            return "ConvolveParamState" + Arrays.toString(kernel);
        }
    }
}
