package pixelitor.utils.debug;

import pixelitor.layers.ContentLayer;
import pixelitor.layers.LayerMask;

public class ContentLayerNode extends DebugNode {
    public ContentLayerNode(ContentLayer layer) {
        this("Layer", layer);
    }

    public ContentLayerNode(String name, ContentLayer layer) {
        super(name, layer);

        if (layer.hasLayerMask()) {
            LayerMask mask = layer.getLayerMask();
            add(new ImageLayerNode("layer mask", mask));
        }

        addFloatChild("opacity", layer.getOpacity());
        addQuotedStringChild("blending mode", layer.getBlendingMode().toString());
        addQuotedStringChild("name", layer.getName());
        addIntChild("translation X", layer.getTranslationX());
        addIntChild("translation Y", layer.getTranslationY());
    }
}
