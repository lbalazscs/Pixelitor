package pixelitor.filters.truchets;

import pixelitor.filters.gui.*;

import javax.swing.*;

public class TruchetParam extends AbstractFilterParam {

    private TruchetSwatch swatch;

    public TruchetParam(String name, RandomizePolicy randomizePolicy) {
        super(name, randomizePolicy);
        swatch = new TruchetSwatch();
    }

    @Override
    public JComponent createGUI() {
        var truchetParamGUI = new TruchetParamGUI(this, swatch);
        paramGUI = truchetParamGUI;
        guiCreated();

        if (action != null) {
            return new ParamGUIWithAction(truchetParamGUI, action);
        }

        return truchetParamGUI;
    }

    @Override
    public void setAdjustmentListener(ParamAdjustmentListener listener) {
        super.setAdjustmentListener(listener);
        if (paramGUI!=null)((TruchetParamGUI)paramGUI).setAdjustmentListener(listener);
    }

    @Override
    public boolean hasDefault() {
        return false;
    }


    @Override
    public void reset(boolean trigger) {

    }

    @Override
    protected void doRandomize() {

    }

    @Override
    public boolean isAnimatable() {
        return false;
    }

    @Override
    public ParamState<?> copyState() {
        return null;
    }

    @Override
    public void loadStateFrom(ParamState<?> state, boolean updateGUI) {

    }

    @Override
    public void loadStateFrom(String savedValue) {

    }

    @Override
    public Object getParamValue() {
        return swatch;
    }

    public TruchetSwatch getSwatch() {
        return swatch;
    }


    public void paramAdjusted() {
        if (adjustmentListener != null) {
            adjustmentListener.paramAdjusted();
        }
    }
}
