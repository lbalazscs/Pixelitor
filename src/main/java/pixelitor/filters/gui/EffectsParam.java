package pixelitor.filters.gui;

import pixelitor.filters.painters.AreaEffects;
import pixelitor.filters.painters.EffectsPanel;
import pixelitor.utils.DefaultButton;
import pixelitor.utils.OKDialog;

import javax.swing.*;
import java.awt.Rectangle;

import static pixelitor.filters.gui.RandomizePolicy.ALLOW_RANDOMIZE;

public class EffectsParam extends AbstractFilterParam {
    private EffectsPanel effectsPanel;
    private final boolean separateDialog;

    public EffectsParam(String name) {
        super(name, ALLOW_RANDOMIZE);
        this.separateDialog = true;
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
        // if a GUI filter is executing without a GUI
        // (for example in a robot test), the panel needs to be created here
        if (effectsPanel == null) {
            effectsPanel = new EffectsPanel(adjustmentListener, null);
        }

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
        if (effectsPanel != null) {
            effectsPanel.reset(triggerAction);
        }
    }
}
