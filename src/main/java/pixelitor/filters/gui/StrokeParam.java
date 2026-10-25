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

package pixelitor.filters.gui;

import com.jhlabs.image.ImageMath;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.tools.shapes.*;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import java.awt.Stroke;
import java.util.Random;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static pixelitor.gui.GUIText.CLOSE_DIALOG;
import static pixelitor.tools.shapes.StrokeType.SHAPE;

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

    private JComponent previewer;
    private JPanel dialogContentPanel;

    private final FilterParam[] childParams = {
        strokeWidthParam, strokeCapParam, strokeJoinParam,
        strokeTypeParam, shapeTypeParam, dashedParam};

    public StrokeParam(String name) {
        super(name, RandomizeMode.ALLOW);

        shapeTypeParam.withDefault(DEFAULT_SHAPE_TYPE);

        // enable shape type only when stroke type is SHAPE
        strokeTypeParam.enableOtherWhen(shapeTypeParam,
            strokeType -> strokeType == SHAPE);

        // disable dashed option for stroke types that don't support it
        strokeTypeParam.disableOtherWhen(dashedParam,
            strokeType -> !strokeType.supportsDashes());
    }

    @Override
    public JComponent createGUI() {
        return installGUI(new DialogLauncherGUI(
            this::configureDialog, new ResetButton(this)));
    }

    @Override
    public void setAdjustmentListener(ParamAdjustmentListener listener) {
        // use a wrapper to ensure that the reset button and the
        // stroke preview are updated whenever any sub-param changes
        ParamAdjustmentListener decoratedListener = () -> {
            updateResetButtonState();
            if (previewer != null) {
                previewer.repaint();
            }
            listener.paramAdjusted(); // notify the app
        };

        super.setAdjustmentListener(decoratedListener);

        for (FilterParam param : childParams) {
            param.setAdjustmentListener(decoratedListener);
        }
    }

    @Override
    public void guiDisposed() {
        super.guiDisposed();

        if (dialogContentPanel == null) {
            // the dialog was never opened, the children GUIs
            // were never created, so no cleanup is necessary
            return;
        }

        for (FilterParam param : childParams) {
            param.guiDisposed();
        }
        dialogContentPanel = null;
        previewer = null;
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
        float effectiveWidth = ImageMath.lerp(randomness, baseWidth, randomWidth);

        return getStrokeType().createStroke(this, effectiveWidth);
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

        // biases the randomization toward fast-rendering strokes
        // while still allowing slow ones to be selected manually
        int attempts = 0; // avoid infinite loops
        do {
            strokeTypeParam.doRandomize();
            attempts++;
        } while (strokeTypeParam.getSelected().isSlow() && attempts < 20);

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
        StrokeSettings settings = (StrokeSettings) state;

        strokeWidthParam.setValueNoTrigger(settings.width());
        strokeCapParam.setSelectedItem(settings.cap(), false);
        strokeJoinParam.setSelectedItem(settings.join(), false);
        strokeTypeParam.setSelectedItem(settings.type(), false);
        shapeTypeParam.setSelectedItem(settings.shapeType(), false);
        dashedParam.setValue(settings.dashed(), updateGUI, false);
    }

    @Override
    public void loadStateFrom(String savedValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        for (FilterParam param : childParams) {
            param.saveStateTo(preset);
        }
    }

    @Override
    public void loadStateFrom(UserPreset preset) {
        for (FilterParam param : childParams) {
            param.loadStateFrom(preset);
        }
        updateResetButtonState();
    }

    @Override
    public void setEnabled(boolean enabled, EnabledReason reason) {
        // call super to set the enabled state of the launching button
        super.setEnabled(enabled, reason);

        for (FilterParam param : childParams) {
            param.setEnabled(enabled, EnabledReason.PARENT_PARAM);
        }
    }

    @Override
    public boolean isAtDefault() {
        for (FilterParam param : childParams) {
            if (!param.isAtDefault()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void reset(boolean trigger) {
        for (FilterParam param : childParams) {
            // trigger the listener only once at the end
            param.reset(false);
        }

        if (trigger) {
            triggerFilter();
            // the reset button state is updated in the decorated adjustment listener
        } else {
            // if not triggered, the reset button state must be updated manually
            updateResetButtonState();
        }
    }

    private void updateResetButtonState() {
        if (paramGUI != null) {
            paramGUI.updateGUI();
        }
    }

    public void configureDialog(DialogBuilder builder) {
        if (dialogContentPanel == null) {
            dialogContentPanel = new StrokeSettingsPanel(this);
        }
        builder
            .title("Stroke Settings")
            .modeless()
            .content(dialogContentPanel)
            .withScrollbars()
            .noCancelButton()
            .okText(CLOSE_DIALOG);
    }

    public StrokeParam withDefaultStrokeWidth(int newWidth) {
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
    public String getValueAsString() {
        return Stream.of(childParams)
            .map(FilterParam::getValueAsString)
            .collect(joining(", ", "[", "]"));
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
