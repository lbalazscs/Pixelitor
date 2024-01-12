package pixelitor.filters.gmic;

import java.io.Serial;
import java.util.List;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.gui.RangeParam;

public class GMICRandomObjects extends GMICFilter {
    @Serial
    private static final long serialVersionUID = 1L;

    public static final String NAME = "3D Random Objects";

    private final IntChoiceParam type = new IntChoiceParam("Type", new Item[] {
        new Item("Cube", 0),
        new Item("Cone", 1),
        new Item("Cylinder", 2),
        new Item("Sphere", 3),
        new Item("Torus", 4)
    });
    private final RangeParam density = new RangeParam("Density", 1, 50, 300);
    private final RangeParam size = new RangeParam("Size", 1, 3, 20);
    private final RangeParam zRange = new RangeParam("Z-range", 0, 100, 300);
    private final RangeParam fov = new RangeParam("Fov", 1, 45, 90);
    private final RangeParam xLight = new RangeParam("X-light", -100, 0, 100);
    private final RangeParam yLight = new RangeParam("Y-light", -100, 0, 100);
    private final RangeParam zLight = new RangeParam("Z-light", -100, -100, 0);
    private final RangeParam specularLightness = new RangeParam("Specular Lightness", 0, 50, 100);
    private final RangeParam specularShininess = new RangeParam("Specular Shininess", 0, 70, 300);
    private final IntChoiceParam rendering = new IntChoiceParam("Rendering", new Item[] {
        new Item("Dots", 0),
        new Item("Wireframe", 1),
        new Item("Flat", 2),
        new Item("Flat-Shaded", 3),
        new Item("Gouraud", 4),
        new Item("Phong", 5)
    }).withDefaultChoice(3);
    private final RangeParam opacity = new RangeParam("Opacity", 0, 1, 1);

    public GMICRandomObjects() {
        setParams(
            type,
            density,
            size,
            zRange,
            fov,
            xLight,
            yLight,
            zLight,
            specularLightness,
            specularShininess,
            rendering,
            opacity
        ).withReseedGmicAction(this);
    }

    @Override
    public List<String> getArgs() {
        return List.of("srand", String.valueOf(seed), "fx_random3d",
            type.getValue() + "," +
            density.getValue() + "," +
            size.getValue() + "," +
            zRange.getValue() + "," +
            fov.getValue() + "," +
            xLight.getValue() + "," +
            yLight.getValue() + "," +
            zLight.getValue() + "," +
            specularLightness.getPercentage() + "," +
            specularShininess.getPercentage() + "," +
            rendering.getValue() + "," +
            opacity.getValue()
        );
    }
}

