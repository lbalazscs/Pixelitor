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

package pixelitor.filters.gui;

import com.jhlabs.image.ImageMath;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.tools.shapes.*;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import java.awt.Stroke;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static pixelitor.filters.gui.RandomizeMode.ALLOW_RANDOMIZE;
import static pixelitor.gui.GUIText.CLOSE_DIALOG;
import static pixelitor.tools.shapes.StrokeType.BASIC;
import static pixelitor.tools.shapes.StrokeType.SHAPE;
import static pixelitor.tools.shapes.StrokeType.ZIGZAG;

/**
 * A {@link FilterParam} that configures stroke settings through a dialog-based GUI.
 */
public class StrokeParam extends AbstractFilterParam {
    public static final ShapeType DEFAULT_SHAPE_TYPE = ShapeType.KIWI;

    private final RangeParam strokeWidthParam = new RangeParam("Stroke Width", 1, 5, 100);
    private final EnumParam<StrokeCap> strokeCapParam = StrokeCap.asParam();
    private final EnumParam<StrokeJoin> strokeJoinParam = StrokeJoin.asParam();
    private final EnumParam<StrokeType> strokeTypeParam = StrokeType.asParam();
    private final EnumParam<ShapeType> shapeTypeParam = ShapeType.asParam();
    private final BooleanParam dashedParam = new BooleanParam("Dashed");

    private ResetButton resetButton;
    private JComponent previewer;

    private final FilterParam[] allParams = {
        strokeWidthParam, strokeCapParam, strokeJoinParam,
        strokeTypeParam, shapeTypeParam, dashedParam};

    public StrokeParam(String name) {
        super(name, ALLOW_RANDOMIZE);

        shapeTypeParam.withDefault(DEFAULT_SHAPE_TYPE);

        // enable shape type only when stroke type is SHAPE
        strokeTypeParam.setupEnableOtherIf(shapeTypeParam,
            strokeType -> strokeType == SHAPE);

        // disable dashed option for stroke types that don't support it
        strokeTypeParam.setupDisableOtherIf(dashedParam,
            strokeType -> strokeType != BASIC
                && strokeType != ZIGZAG
                && strokeType != SHAPE);
    }

    @Override
    public JComponent createGUI() {
        resetButton = new ResetButton(this);
        paramGUI = new DialogLauncherGUI(this::configureSettingsDialog, resetButton);
        guiCreated();
        return (JComponent) paramGUI;
    }

    @Override
    public void setAdjustmentListener(ParamAdjustmentListener listener) {
        // this listener updates the reset button and preview,
        // then delegates to the original listener
        ParamAdjustmentListener decoratedListener = () -> {
            updateResetButtonState();
            if (previewer != null) {
                previewer.repaint();
            }
            listener.paramAdjusted();
        };

        super.setAdjustmentListener(decoratedListener);

        for (FilterParam param : allParams) {
            param.setAdjustmentListener(decoratedListener);
        }
    }

    /**
     * Creates a stroke based on the current settings.
     */
    public Stroke createStroke() {
        return getStrokeType().createStroke(this, getStrokeWidth());
    }

    /**
     * Creates a stroke based on the current settings, but with randomized width.
     */
    public Stroke createStrokeWithRandomWidth(Random random, float randomness) {
        assert randomness != 0.0f;

        float baseWidth = strokeWidthParam.getValueAsFloat();
        // calculate a random width between 50% and 150% of the base width
        float randomWidth = baseWidth / 2.0f + baseWidth * random.nextFloat();
        float finalWidth = ImageMath.lerp(randomness, baseWidth, randomWidth);

        return getStrokeType().createStroke(this, finalWidth);
    }

    public float[] getDashPattern(float width) {
        if (hasDashes()) {
            return new float[]{2 * width, 2 * width};
        }
        return null;
    }

    @Override
    protected void doRandomize() {
        strokeWidthParam.doRandomize();
        strokeCapParam.doRandomize();
        strokeJoinParam.doRandomize();

        // avoid slow stroke types during randomization
        do {
            strokeTypeParam.doRandomize();
        } while (strokeTypeParam.getSelected().isSlow());

        shapeTypeParam.doRandomize();
        dashedParam.doRandomize();

        updateResetButtonState();
    }

    @Override
    public StrokeSettings copyState() {
        return new StrokeSettings(
            strokeWidthParam.getValueAsDouble(),
            strokeCapParam.getSelected(),
            strokeJoinParam.getSelected(),
            strokeTypeParam.getSelected(),
            shapeTypeParam.getSelected(),
            dashedParam.isChecked());
    }

    @Override
    public void loadStateFrom(ParamState<?> state, boolean updateGUI) {
        StrokeSettings setting = (StrokeSettings) state;

        strokeWidthParam.setValueNoTrigger(setting.width());
        strokeCapParam.setSelectedItem(setting.cap(), false);
        strokeJoinParam.setSelectedItem(setting.join(), false);
        strokeTypeParam.setSelectedItem(setting.type(), false);
        shapeTypeParam.setSelectedItem(setting.shapeType(), false);
        dashedParam.setValue(setting.dashed(), true, false);
    }

    @Override
    public void loadStateFrom(String savedValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void loadStateFrom(UserPreset preset) {
        for (FilterParam param : allParams) {
            String savedString = preset.get(param.getPresetKey());
            if (savedString != null) { // presets don't have to include everything
                param.loadStateFrom(savedString);
            }
        }
        updateResetButtonState();
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        for (FilterParam param : allParams) {
            preset.put(param.getName(), param.copyState().toSaveString());
        }
    }

    @Override
    public void setEnabled(boolean enabled, EnabledReason reason) {
        // call super to set the enabled state of the launching button
        super.setEnabled(enabled, reason);

        for (FilterParam param : allParams) {
            param.setEnabled(enabled, reason);
        }
    }

    @Override
    public boolean hasDefault() {
        return Arrays.stream(allParams)
            .allMatch(Resettable::hasDefault);
    }

    @Override
    public void reset(boolean trigger) {
        for (FilterParam param : allParams) {
            // trigger the listener only once at the end
            param.reset(false);
        }

        if (trigger) {
            adjustmentListener.paramAdjusted();
            // the reset button state is updated in the decorated adjustment listener
        } else {
            // if not triggered, the reset button state must be updated manually
            updateResetButtonState();
        }
    }

    private void updateResetButtonState() {
        if (resetButton != null) {
            resetButton.updateState();
        }
    }

    public void configureSettingsDialog(DialogBuilder builder) {
        builder
            .title("Stroke Settings")
            .notModal()
            .content(new StrokeSettingsPanel(this))
            .withScrollbars()
            .noCancelButton()
            .okText(CLOSE_DIALOG);
    }

    public FilterParam withStrokeWidth(int newWidth) {
        strokeWidthParam.setDefaultValue(newWidth);
        return this;
    }

    public ShapeType getShapeType() {
        return shapeTypeParam.getSelected();
    }

    public float getStrokeWidth() {
        return strokeWidthParam.getValueAsFloat();
    }

    public StrokeType getStrokeType() {
        return strokeTypeParam.getSelected();
    }

    private boolean hasDashes() {
        return dashedParam.isChecked();
    }

    @Override
    public boolean isAnimatable() {
        return true;
    }

    public RangeParam getStrokeWidthParam() {
        return strokeWidthParam;
    }

    public EnumParam<StrokeCap> getStrokeCapParam() {
        return strokeCapParam;
    }

    public int getCap() {
        return strokeCapParam.getSelected().getValue();
    }

    public EnumParam<StrokeJoin> getStrokeJoinParam() {
        return strokeJoinParam;
    }

    public int getJoin() {
        return strokeJoinParam.getSelected().getValue();
    }

    public EnumParam<StrokeType> getStrokeTypeParam() {
        return strokeTypeParam;
    }

    public EnumParam<ShapeType> getShapeTypeParam() {
        return shapeTypeParam;
    }

    public BooleanParam getDashedParam() {
        return dashedParam;
    }

    public void setPreviewer(JComponent previewer) {
        this.previewer = previewer;
    }

    @Override
    public List<Object> getParamValue() {
        return Stream.of(allParams)
            .map(FilterParam::getParamValue)
            .collect(toList());
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = super.createDebugNode(key);

        node.addInt("width", strokeWidthParam.getValue());
        node.addAsString("cap", strokeCapParam.getSelected());
        node.addAsString("join", strokeJoinParam.getSelected());
        node.addAsString("type", strokeTypeParam.getSelected());
        node.addAsString("shape type", shapeTypeParam.getSelected());
        node.addBoolean("dashed", hasDashes());

        return node;
    }
}
