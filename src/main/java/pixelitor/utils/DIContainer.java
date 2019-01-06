/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor. If not, see <http://www.gnu.org/licenses/>.
 */

package pixelitor.utils;

import pixelitor.guides.GuidesRenderer;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple dependency injection container
 */
public class DIContainer {

    public static final int GUIDES_RENDERER = 1;
    public static final int CROP_GUIDES_RENDERER = 2;

    private static final Map<Class, Object> diMap = new HashMap<>();
    private static final Map<Integer, Object> diMap2 = new HashMap<>();

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
        if (result == null) {
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
        if (result == null) {
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
