package pixelitor.layers;

public interface LayerUI {
    void setOpenEye(boolean newVisibility);

    void setUserInteraction(boolean userInteraction);

    void addMouseHandler(LayersMouseHandler mouseHandler);

    void removeMouseHandler(LayersMouseHandler mouseHandler);

    int getStaticY();

    void setStaticY(int staticY);

    void dragFinished(int newLayerIndex);

    Layer getLayer();

    String getLayerName();

    boolean isNameEditing();

    boolean isVisibilityChecked();

    void changeNameProgrammatically(String newName);

    void updateLayerIconImage(ImageLayer layer);

    void addMaskIconLabel();

    void deleteMaskIconLabel();

    void setSelected(boolean b);

    LayerButton getLayerButton();

    void setUIOpacity(float newOpacity);
}
