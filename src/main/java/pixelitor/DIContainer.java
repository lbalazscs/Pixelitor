package pixelitor;

import pixelitor.guides.GuidesRenderer;
import pixelitor.utils.AppPreferences;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple dependency injection container
 */
public class DIContainer {

    public static final int GUIDES_RENDERER = 1;
    public static final int CROP_GUIDES_RENDERER = 2;

    private static Map<Class, Object> diMap = new HashMap<>();
    private static Map<Integer, Object> diMap2 = new HashMap<>();

    private DIContainer() {
        // should not be called
    }

    /**
     * Return object of provided class type.
     * For every call always the same instance of the object is returned.
     * Use this if you need one object of given class in application
     */
    public static <T> T get(Class<T> classType) {

        Object result = diMap.get(classType);
        if (null == result) {
            result = factorize(classType);
            diMap.put(classType, result);
        }

        return classType.cast(result);
    }

    /**
     * Return object of provided class type and specified id.
     * For every call always the same instance of the object is returned.
     * Use this if you need many objects of given class in application that differ in configuration
     */
    public static <T> T get(Class<T> classType, Integer id) {

        Object result = diMap2.get(id);
        if (null == result) {
            result = factorize(id);
            diMap2.put(id, result);
        }

        return classType.cast(result);
    }

    private static Object factorize(Integer id) {
        switch (id) {
            case GUIDES_RENDERER:
                return new GuidesRenderer(AppPreferences.getGuideStyle());
            case CROP_GUIDES_RENDERER:
                return new GuidesRenderer(AppPreferences.getCropGuideStyle());
            default:
                throw new IllegalArgumentException("Unsupported object type for provided id");
        }
    }

    private static Object factorize(Class classType) {
        // defined classes here

        // dynamic classes
        try {
            return classType.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalArgumentException("Unsupported class type");
        }
    }
}
