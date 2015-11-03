package pixelitor.filters.levels;

import pixelitor.filters.gui.ParamAdjustmentListener;
import pixelitor.filters.gui.RangeParam;

import java.awt.Color;

public class OneChannelLevelsModel implements ParamAdjustmentListener {
    private static final int BLACK_DEFAULT = 0;
    private static final int WHITE_DEFAULT = 255;

    private GrayScaleLookup adjustment = GrayScaleLookup.getDefault();

    private final RangeParam inputBlackParam;
    private final RangeParam inputWhiteParam;
    private final RangeParam outputBlackParam;
    private final RangeParam outputWhiteParam;
    private final LevelsAdjustmentType type;
    private final LevelsModel bigModel;

    public OneChannelLevelsModel(LevelsAdjustmentType type, LevelsModel bigModel) {
        this.type = type;
        this.bigModel = bigModel;

        inputBlackParam = new RangeParam("Input Dark", 0, BLACK_DEFAULT, 254);
        inputWhiteParam = new RangeParam("Input Light", 1, WHITE_DEFAULT, 255);
        outputBlackParam = new RangeParam("Output Dark", 0, BLACK_DEFAULT, 254);
        outputWhiteParam = new RangeParam("Output Light", 1, WHITE_DEFAULT, 255);

        inputBlackParam.setAdjustmentListener(this);
        inputWhiteParam.setAdjustmentListener(this);
        outputBlackParam.setAdjustmentListener(this);
        outputWhiteParam.setAdjustmentListener(this);
    }

    public Color getBackColor() {
        return type.getBackColor();
    }

    public Color getWhiteColor() {
        return type.getWhiteColor();
    }

    public RangeParam getInputBlackParam() {
        return inputBlackParam;
    }

    public RangeParam getInputWhiteParam() {
        return inputWhiteParam;
    }

    public RangeParam getOutputBlackParam() {
        return outputBlackParam;
    }

    public RangeParam getOutputWhiteParam() {
        return outputWhiteParam;
    }

    public String getName() {
        return type.getName();
    }


    public GrayScaleLookup getAdjustment() {
        return adjustment;
    }

    @Override
    public void paramAdjusted() {
        updateAdjustment();

        bigModel.adjustmentChanged();
    }

    private void updateAdjustment() {
        int inputBlackValue = inputBlackParam.getValue();
        int inputWhiteValue = inputWhiteParam.getValue();
        int outputBlackValue = outputBlackParam.getValue();
        int outputWhiteValue = outputWhiteParam.getValue();

        adjustment = new GrayScaleLookup(inputBlackValue, inputWhiteValue, outputBlackValue, outputWhiteValue);
    }

    public void resetToDefaultSettings() {
        inputBlackParam.reset(false);
        inputWhiteParam.reset(false);
        outputBlackParam.reset(false);
        outputWhiteParam.reset(false);

        updateAdjustment();
    }
}
