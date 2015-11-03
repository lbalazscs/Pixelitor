package pixelitor.filters.gui;

import pixelitor.PixelitorWindow;
import pixelitor.tools.ShapeType;
import pixelitor.tools.StrokeType;
import pixelitor.tools.shapestool.BasicStrokeCap;
import pixelitor.tools.shapestool.BasicStrokeJoin;
import pixelitor.tools.shapestool.StrokeSettingsPanel;
import pixelitor.utils.DefaultButton;
import pixelitor.utils.GUIUtils;
import pixelitor.utils.OKDialog;

import javax.swing.*;
import java.awt.Rectangle;
import java.awt.Stroke;

import static pixelitor.filters.gui.RandomizePolicy.IGNORE_RANDOMIZE;

public class StrokeParam extends AbstractFilterParam {
    private final RangeParam strokeWidthParam = new RangeParam("Stroke Width", 1, 5, 100);
    // controls in the Stroke Settings dialog
    private final EnumParam<BasicStrokeCap> strokeCapParam = new EnumParam<>("", BasicStrokeCap.class);
    private final EnumParam<BasicStrokeJoin> strokeJoinParam = new EnumParam<>("", BasicStrokeJoin.class);
    private final EnumParam<StrokeType> strokeTypeParam = new EnumParam<>("", StrokeType.class);
    private final EnumParam<ShapeType> shapeTypeParam = new EnumParam<>("", ShapeType.class);
    private final BooleanParam dashedParam = new BooleanParam("", false);
    private DefaultButton defaultButton;

    private final FilterParam[] allParams = {strokeWidthParam,
            strokeCapParam, strokeJoinParam,
            strokeTypeParam, shapeTypeParam, dashedParam
    };

    public StrokeParam(String name) {
        super(name, IGNORE_RANDOMIZE);
    }

    @Override
    public JComponent createGUI() {
        defaultButton = new DefaultButton(this);
        paramGUI = new ConfigureParamGUI(owner -> {
            JDialog dialog = createSettingsDialogForFilter(owner);
            GUIUtils.centerOnScreen(dialog);
            return dialog;
        }, defaultButton);

        paramGUI.setEnabled(shouldBeEnabled());
        return (JComponent) paramGUI;
    }

    @Override
    public void setAdjustmentListener(ParamAdjustmentListener listener) {
        ParamAdjustmentListener decoratedListener = () -> {
            updateDefaultButtonState();
            listener.paramAdjusted();
        };

        super.setAdjustmentListener(decoratedListener);

        strokeWidthParam.setAdjustmentListener(decoratedListener);
        strokeTypeParam.setAdjustmentListener(decoratedListener);
        strokeCapParam.setAdjustmentListener(decoratedListener);
        strokeJoinParam.setAdjustmentListener(decoratedListener);

        shapeTypeParam.setAdjustmentListener(() -> {
            ShapeType selectedItem = (ShapeType) shapeTypeParam.getSelectedItem();
            StrokeType.SHAPE.setShapeType(selectedItem);
            // it is important to call this only after the previous setup!
            decoratedListener.paramAdjusted();
        });

        dashedParam.setAdjustmentListener(decoratedListener);
    }

    public int getStrokeWidth() {
        return strokeWidthParam.getValue();
    }

    public StrokeType getStrokeType() {
        return (StrokeType) strokeTypeParam.getSelectedItem();
    }

    public JDialog createSettingsDialogForShapesTool() {
        JFrame owner = PixelitorWindow.getInstance();
        JPanel p = createStrokeSettingsPanel();
        JDialog d = new OKDialog(owner, p, "Stroke Settings", "Close");
        return d;
    }

    private JDialog createSettingsDialogForFilter(JDialog owner) {
        JPanel p = createStrokeSettingsPanel();
        OKDialog d = new OKDialog(owner, "Stroke Settings", "Close");
        d.setupGUI(p);
        return d;
    }

    private JPanel createStrokeSettingsPanel() {
        JPanel p = new StrokeSettingsPanel(strokeWidthParam, strokeCapParam,
                strokeJoinParam, strokeTypeParam, dashedParam, shapeTypeParam);
        return p;
    }

    public Stroke createStroke() {
        int strokeWidth = strokeWidthParam.getValue();

        float[] dashFloats = null;
        if (dashedParam.isChecked()) {
            dashFloats = new float[]{2 * strokeWidth, 2 * strokeWidth};
        }

        StrokeType strokeType = getStrokeType();

        BasicStrokeCap strokeCap = (BasicStrokeCap) strokeCapParam.getSelectedItem();
        BasicStrokeJoin strokeJoin = (BasicStrokeJoin) strokeJoinParam.getSelectedItem();
        Stroke s = strokeType.getStroke(
                strokeWidth,
                strokeCap.getValue(),
                strokeJoin.getValue(),
                dashFloats
        );

        return s;
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
        return 2;
    }

    @Override
    public boolean isSetToDefault() {
        for (FilterParam param : allParams) {
            if (!param.isSetToDefault()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void reset(boolean triggerAction) {
        for (FilterParam param : allParams) {
            param.reset(false);
        }
        if (triggerAction) {
            adjustmentListener.paramAdjusted();
        }
    }

    private void updateDefaultButtonState() {
        if (defaultButton != null) {
            defaultButton.updateState();
        }
    }
}
