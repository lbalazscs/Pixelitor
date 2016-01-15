package pixelitor.layers;

public interface LayerUI {
    void setOpenEye(boolean newVisibility);

    Layer getLayer();

    String getLayerName();

    boolean isVisibilityChecked();

    void changeNameProgrammatically(String newName);

    void updateLayerIconImage(ImageLayer layer);

    void addMaskIconLabel();

    void deleteMaskIconLabel();

    void setSelected(boolean b);

    LayerButton getLayerButton();

    void setOpacityFromModel(float newOpacity);
}
