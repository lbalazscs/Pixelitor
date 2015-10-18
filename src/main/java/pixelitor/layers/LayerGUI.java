package pixelitor.layers;

public class LayerGUI implements LayerUI {
    private final LayerButton layerButton;

    public LayerGUI(Layer layer) {
        this.layerButton = new LayerButton(layer);
    }

    @Override
    public void setOpenEye(boolean newVisibility) {
        layerButton.setOpenEye(newVisibility);
    }

    @Override
    public Layer getLayer() {
        return layerButton.getLayer();
    }

    @Override
    public String getLayerName() {
        return layerButton.getLayerName();
    }

    @Override
    public boolean isVisibilityChecked() {
        return layerButton.isVisibilityChecked();
    }

    @Override
    public void changeNameProgrammatically(String newName) {
        layerButton.changeNameProgrammatically(newName);
    }

    @Override
    public void updateLayerIconImage(ImageLayer layer) {
        // TODO why the argument, the button already knows
        layerButton.updateLayerIconImage(layer);
    }

    @Override
    public void addMaskIconLabel() {
        layerButton.addMaskIconLabel();
    }

    @Override
    public void deleteMaskIconLabel() {
        layerButton.deleteMaskIconLabel();
    }

    @Override
    public void setSelected(boolean b) {
        layerButton.setSelected(b);
    }

    @Override
    public LayerButton getLayerButton() {
        return layerButton;
    }

    @Override
    public void setUIOpacity(float newOpacity) {
        LayerBlendingModePanel.INSTANCE.setOpacity(newOpacity);
    }
}
