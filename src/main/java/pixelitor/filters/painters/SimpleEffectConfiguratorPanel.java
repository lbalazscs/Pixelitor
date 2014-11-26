package pixelitor.filters.painters;

import pixelitor.filters.gui.ParamAdjustmentListener;
import pixelitor.filters.gui.RangeParam;
import pixelitor.utils.GridBagHelper;
import pixelitor.utils.SliderSpinner;

import java.awt.Color;

/**
 * An EffectConfiguratorPanel that has a width parameter.
 * Most effect configurator panels need this as the superclass
 */
public class SimpleEffectConfiguratorPanel extends EffectConfiguratorPanel {
    private final RangeParam widthRange;
    private final SliderSpinner widthSlider;

    public SimpleEffectConfiguratorPanel(String effectName, boolean defaultSelected, Color defaultColor, int defaultWidth) {
        super(effectName, defaultSelected, defaultColor);

        widthRange = new RangeParam("Width:", 1, 100, defaultWidth);
        widthSlider = new SliderSpinner(widthRange, false, SliderSpinner.TextPosition.NONE);

        GridBagHelper.addLabel(this, "Width:", 0, 6);
        GridBagHelper.addControl(this, widthSlider);
    }

    @Override
    public int getBrushWidth() {
        return widthSlider.getCurrentValue();
    }

    @Override
    public void setAdjustmentListener(ParamAdjustmentListener adjustmentListener) {
        super.setAdjustmentListener(adjustmentListener);
        widthRange.setAdjustmentListener(adjustmentListener);
    }
}
