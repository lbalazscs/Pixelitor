package pixelitor.filters.gui;

import pixelitor.utils.DefaultButton;
import pixelitor.utils.GUIUtils;

import javax.swing.*;
import java.awt.Rectangle;

import static pixelitor.filters.gui.RandomizePolicy.ALLOW_RANDOMIZE;

/**
 * A composite FilterParam which can show its children in a dialog
 */
public class DialogParam extends AbstractFilterParam {
    private final FilterParam[] children;
    private DefaultButton defaultButton;

    public DialogParam(FilterParam... children) {
        this("More Settings", children);
    }

    public DialogParam(String name, FilterParam... children) {
        super(name, ALLOW_RANDOMIZE);
        this.children = children;
    }

    @Override
    public JComponent createGUI() {
        defaultButton = new DefaultButton(this);
        paramGUI = new ConfigureParamGUI(owner -> {
            JDialog dialog = createDialog(owner);
            GUIUtils.centerOnScreen(dialog);
            return dialog;
        }, defaultButton);

        setParamGUIEnabledState();
        return (JComponent) paramGUI;
    }

    private JDialog createDialog(JDialog owner) {
        // TODO The functionality of ParametrizedAdjustPanel
        // should be reused here

        return null;
    }

    @Override
    public void randomize() {

    }

    @Override
    public void considerImageSize(Rectangle bounds) {

    }

    @Override
    public ParamState copyState() {
        return null;
    }

    @Override
    public void setState(ParamState state) {

    }

    @Override
    public boolean canBeAnimated() {
        return false;
    }

    @Override
    public int getNrOfGridBagCols() {
        return 0;
    }

    @Override
    public boolean isSetToDefault() {
        return false;
    }

    @Override
    public void reset(boolean triggerAction) {

    }

    private void updateDefaultButtonState() {
        if (defaultButton != null) {
            defaultButton.updateState();
        }
    }
}
