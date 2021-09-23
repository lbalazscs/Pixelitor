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

package pixelitor.filters.gui;

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
import static pixelitor.filters.gui.RandomizePolicy.ALLOW_RANDOMIZE;
import static pixelitor.gui.GUIText.CLOSE_DIALOG;
import static pixelitor.tools.shapes.StrokeType.*;

/**
 * A {@link FilterParam} for stroke settings.
 * Its GUI is a button, which shows a dialog when pressed.
 */
public class StrokeParam extends AbstractFilterParam {
    private final RangeParam strokeWidthParam = new RangeParam("Stroke Width", 1, 5, 100);
    private final EnumParam<StrokeCap> strokeCapParam = StrokeCap.asParam();
    private final EnumParam<StrokeJoin> strokeJoinParam = StrokeJoin.asParam();
    private final EnumParam<StrokeType> strokeTypeParam = StrokeType.asParam();
    private final EnumParam<ShapeType> shapeTypeParam = ShapeType.asParam();
    private final BooleanParam dashedParam = new BooleanParam("Dashed", false);
    private DefaultButton defaultButton;
    private JComponent previewer;

    private final FilterParam[] allParams = {
        strokeWidthParam, strokeCapParam, strokeJoinParam,
        strokeTypeParam, shapeTypeParam, dashedParam};

    public StrokeParam(String name) {
        super(name, ALLOW_RANDOMIZE);

        shapeTypeParam.withDefault(ShapeType.KIWI);

        strokeTypeParam.setupEnableOtherIf(shapeTypeParam,
            strokeType -> strokeType == SHAPE);

        strokeTypeParam.setupDisableOtherIf(dashedParam,
            strokeType -> strokeType != BASIC
                          && strokeType != ZIGZAG
                          && strokeType != SHAPE);
    }

    @Override
    public JComponent createGUI() {
        defaultButton = new DefaultButton(this);
        paramGUI = new ConfigureParamGUI(this::configureSettingsDialog, defaultButton);

        afterGUICreation();
        return (JComponent) paramGUI;
    }

    @Override
    public void setAdjustmentListener(ParamAdjustmentListener listener) {
        ParamAdjustmentListener decoratedListener = () -> {
            updateDefaultButtonState();
            if (previewer != null) {
                previewer.repaint();
            }
            listener.paramAdjusted();
        };

        super.setAdjustmentListener(decoratedListener);

        strokeWidthParam.setAdjustmentListener(decoratedListener);
        strokeTypeParam.setAdjustmentListener(decoratedListener);
        strokeCapParam.setAdjustmentListener(decoratedListener);
        strokeJoinParam.setAdjustmentListener(decoratedListener);
        shapeTypeParam.setAdjustmentListener(decoratedListener);
        dashedParam.setAdjustmentListener(decoratedListener);
    }

    public ShapeType getShapeType() {
        return shapeTypeParam.getSelected();
    }

    public int getStrokeWidth() {
        return strokeWidthParam.getValue();
    }

    public StrokeType getStrokeType() {
        return strokeTypeParam.getSelected();
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

    public Stroke createStroke() {
        return getStrokeType().createStroke(this);
    }

    public float[] getDashFloats(float strokeWidth) {
        float[] dashFloats = null;
        if (hasDashes()) {
            dashFloats = new float[]{2 * strokeWidth, 2 * strokeWidth};
        }
        return dashFloats;
    }

    public boolean hasDashes() {
        return dashedParam.isChecked();
    }

    public Stroke createStrokeWithRandomWidth(Random random, float randomness) {
        float strokeWidth = strokeWidthParam.getValueAsFloat();
        strokeWidth += strokeWidth * randomness * random.nextFloat();

        return getStrokeType().createStroke(
            strokeWidth,
            strokeCapParam.getSelected().getValue(),
            strokeJoinParam.getSelected().getValue(),
            getDashFloats(strokeWidth));
    }

    @Override
    protected void doRandomize() {
        strokeWidthParam.doRandomize();
        strokeCapParam.doRandomize();
        strokeJoinParam.doRandomize();

        // make sure that the slow settings can't be set by "randomize settings"
        do {
            strokeTypeParam.doRandomize();
        } while (strokeTypeParam.getSelected().isSlow());

        shapeTypeParam.doRandomize();
        dashedParam.doRandomize();

        updateDefaultButtonState();
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
        updateDefaultButtonState();
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        for (FilterParam param : allParams) {
            preset.put(param.getName(), param.copyState().toSaveString());
        }
    }

    @Override
    public boolean canBeAnimated() {
        return true;
    }

    @Override
    public void setEnabled(boolean b, EnabledReason reason) {
        // call super to set the enabled state of the launching button
        super.setEnabled(b, reason);

        for (FilterParam param : allParams) {
            param.setEnabled(b, reason);
        }
    }

    @Override
    public boolean isSetToDefault() {
        return Arrays.stream(allParams)
            .allMatch(Resettable::isSetToDefault);
    }

    @Override
    public void reset(boolean trigger) {
        for (FilterParam param : allParams) {
            // trigger only once, later
            param.reset(false);
        }
        if (trigger) {
            adjustmentListener.paramAdjusted();
            // the default button state is updated
            // in the decorated adjustment listener
        } else {
            updateDefaultButtonState();
        }
    }

    private void updateDefaultButtonState() {
        if (defaultButton != null) {
            defaultButton.updateIcon();
        }
    }

    public RangeParam getStrokeWidthParam() {
        return strokeWidthParam;
    }

    public EnumParam<StrokeCap> getStrokeCapParam() {
        return strokeCapParam;
    }

    public EnumParam<StrokeJoin> getStrokeJoinParam() {
        return strokeJoinParam;
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

    public void addDebugNodeInfo(DebugNode node) {
        DebugNode strokeNode = new DebugNode("stroke settings", this);

        strokeNode.addInt("width", strokeWidthParam.getValue());
        strokeNode.addString("cap", strokeCapParam.getSelected().toString());
        strokeNode.addString("join", strokeJoinParam.getSelected().toString());
        strokeNode.addString("type", strokeTypeParam.getSelected().toString());
        strokeNode.addString("shape type", shapeTypeParam.getSelected().toString());
        strokeNode.addBoolean("dashed", hasDashes());

        node.add(strokeNode);
    }

    @Override
    public List<Object> getParamValue() {
        return Stream.of(allParams)
            .map(FilterParam::getParamValue)
            .collect(toList());
    }
}
