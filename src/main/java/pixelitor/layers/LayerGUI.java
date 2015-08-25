package pixelitor.layers;

public class LayerGUI implements LayerUI {
    private LayerButton layerButton;

    public LayerGUI(Layer layer) {
        this.layerButton = new LayerButton(layer);
    }

    @Override
    public void setOpenEye(boolean newVisibility) {
        layerButton.setOpenEye(newVisibility);
    }

    @Override
    public void setUserInteraction(boolean userInteraction) {
        layerButton.setUserInteraction(userInteraction);
    }

    @Override
    public void addMouseHandler(LayersMouseHandler mouseHandler) {
        layerButton.addMouseHandler(mouseHandler);
    }

    @Override
    public void removeMouseHandler(LayersMouseHandler mouseHandler) {
        layerButton.removeMouseHandler(mouseHandler);
    }

    @Override
    public int getStaticY() {
        return layerButton.getStaticY();
    }

    @Override
    public void setStaticY(int staticY) {
        layerButton.setStaticY(staticY);
    }

    @Override
    public void dragFinished(int newLayerIndex) {
        layerButton.dragFinished(newLayerIndex);
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
    public boolean isNameEditing() {
        return layerButton.isNameEditing();
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
