package pixelitor.filters.levels;

import pixelitor.filters.gui.FilterParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.PreviewExecutor;

import java.util.ArrayList;
import java.util.List;

import static pixelitor.filters.levels.LevelsAdjustmentType.B;
import static pixelitor.filters.levels.LevelsAdjustmentType.G;
import static pixelitor.filters.levels.LevelsAdjustmentType.GB;
import static pixelitor.filters.levels.LevelsAdjustmentType.R;
import static pixelitor.filters.levels.LevelsAdjustmentType.RB;
import static pixelitor.filters.levels.LevelsAdjustmentType.RG;
import static pixelitor.filters.levels.LevelsAdjustmentType.RGB;

public class LevelsModel {
    private final OneChannelLevelsModel rgbModel;
    private final OneChannelLevelsModel rModel;
    private final OneChannelLevelsModel gModel;
    private final OneChannelLevelsModel bModel;
    private final OneChannelLevelsModel rgModel;
    private final OneChannelLevelsModel gbModel;
    private final OneChannelLevelsModel rbModel;
    private final LookupFilter filter;
    private PreviewExecutor executor;

    /**
     * Contains the sub-models in the order they should appear in
     * the GUI
     */
    private final OneChannelLevelsModel[] subModels;

    public LevelsModel(LookupFilter filter) {
        this.filter = filter;
        this.rgbModel = new OneChannelLevelsModel(RGB, this);
        this.rModel = new OneChannelLevelsModel(R, this);
        this.gModel = new OneChannelLevelsModel(G, this);
        this.bModel = new OneChannelLevelsModel(B, this);
        this.rgModel = new OneChannelLevelsModel(RG, this);
        this.rbModel = new OneChannelLevelsModel(RB, this);
        this.gbModel = new OneChannelLevelsModel(GB, this);

        subModels = new OneChannelLevelsModel[]{
                rgbModel, rModel, gModel, bModel, rgModel, rbModel, gbModel
        };
    }

    public void setExecutor(PreviewExecutor previewExecutor) {
        this.executor = previewExecutor;
    }

    public void adjustmentChanged() {
        GrayScaleLookup rgb = rgbModel.getAdjustment();

        GrayScaleLookup r = rModel.getAdjustment();
        GrayScaleLookup g = gModel.getAdjustment();
        GrayScaleLookup b = bModel.getAdjustment();

        GrayScaleLookup rg = rgModel.getAdjustment();
        GrayScaleLookup gb = gbModel.getAdjustment();
        GrayScaleLookup rb = rbModel.getAdjustment();

        RGBLookup unifiedAdjustments = new RGBLookup(rgb, r, g, b, rg, rb, gb);
        filter.setRGBLookup(unifiedAdjustments);
        executor.executeFilterPreview();
    }

    public void resetToDefaultSettings() {
        for (OneChannelLevelsModel model : subModels) {
            model.resetToDefaultSettings();
        }

        adjustmentChanged();
    }

    public OneChannelLevelsModel[] getSubModels() {
        return subModels;
    }

    public ParamSet getParamSet() {
        List<FilterParam> params = new ArrayList<>();
        for (OneChannelLevelsModel subModel : subModels) {
            params.add(subModel.getInputBlackParam());
            params.add(subModel.getInputWhiteParam());
            params.add(subModel.getOutputBlackParam());
            params.add(subModel.getOutputWhiteParam());
        }

        ParamSet ps = new ParamSet(params);
        return ps;
    }
}
