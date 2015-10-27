package pixelitor.filters.gui;

import pixelitor.filters.painters.AreaEffects;
import pixelitor.filters.painters.EffectsPanel;
import pixelitor.utils.DefaultButton;
import pixelitor.utils.OKDialog;

import javax.swing.*;
import java.awt.Rectangle;

public class EffectsParam extends AbstractFilterParam {
    private EffectsPanel effectsPanel;
    private final boolean separateDialog;

    public EffectsParam(String name, boolean ignoreRandomize, boolean separateDialog) {
        super(name, ignoreRandomize);
        this.separateDialog = separateDialog;
    }

    @Override
    public JComponent createGUI() {
        assert adjustmentListener != null;
        effectsPanel = new EffectsPanel(adjustmentListener, null);

        if (separateDialog) {
            DefaultButton button = new DefaultButton(effectsPanel);
            effectsPanel.setDefaultButton(button);

            ConfigureParamGUI configureParamGUI = new ConfigureParamGUI(owner -> {
                OKDialog effectsDialog = new OKDialog(owner, "Effects", "Close");
                effectsDialog.setupGUI(effectsPanel);
                return effectsDialog;
            }, button);

            paramGUI = configureParamGUI;
            paramGUI.setEnabled(shouldBeEnabled());
            return configureParamGUI;
        } else {
            effectsPanel.setBorder(BorderFactory.createTitledBorder("Effects"));
            return effectsPanel;
        }
    }

    public AreaEffects getEffects() {
//        NeonBorderEffect effect = new NeonBorderEffect(Color.RED, Color.WHITE, 10, 1.0f);
//        AreaEffects retVal = new AreaEffects();
//        retVal.setNeonBorderEffect(effect);

        effectsPanel.updateEffectsFromGUI();
        return effectsPanel.getEffects();
    }

    @Override
    public void randomize() {

    }

    @Override
    public void considerImageSize(Rectangle bounds) {

    }

    @Override
    public ParamState copyState() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setState(ParamState state) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canBeAnimated() {
        return false;
    }

    @Override
    public int getNrOfGridBagCols() {
        return separateDialog ? 2 : 1;
    }

    @Override
    public boolean isSetToDefault() {
        return effectsPanel.isSetToDefault();
    }

    @Override
    public void reset(boolean triggerAction) {
        if(effectsPanel != null) {
            effectsPanel.reset(triggerAction);
        }
    }
}
