/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.tools;

import pixelitor.tools.brushes.*;

import java.util.IdentityHashMap;
import java.util.function.DoubleFunction;
import java.util.function.Function;

import static pixelitor.tools.brushes.AngleSettings.NOT_DIRECTIONAL;

/**
 * The brush types in the brush and eraser tools.
 * <p>
 * Brush types that have settings declare the factory of their settings and
 * the factory of their brushes together. The settings type is a type
 * parameter of the constructor, so the compiler guarantees that a brush
 * receives the kind of settings it expects (no casts are needed).
 */
public enum BrushType {
    HARD("Hard", HardBrush::new),
    SOFT("Soft", radius -> new ImageDabsBrush(radius,
        ImageBrushType.SOFT, 0.25, NOT_DIRECTIONAL)),
    WOBBLE("Wobble", WobbleBrush::new),
    CALLIGRAPHY("Calligraphy",
        _ -> new CalligraphyBrushSettings(),
        (_, settings, radius) -> new CalligraphyBrush(settings, radius)),
    REALISTIC("Realistic", radius -> new ImageDabsBrush(radius,
        ImageBrushType.REAL, 0.05, NOT_DIRECTIONAL)),
    HAIR("Hair", radius -> new ImageDabsBrush(radius,
        ImageBrushType.HAIR, 0.02, NOT_DIRECTIONAL)),
    SHAPE("Shapes",
        _ -> new ShapeDabsBrushSettings(),
        (_, settings, radius) -> new ShapeDabsBrush(settings, radius)),
    SPRAY("Spray Shapes",
        SprayBrushSettings::new,
        (_, settings, radius) -> new SprayBrush(settings, radius)),
    CONNECT("Connect",
        _ -> new ConnectBrushSettings(),
        (_, settings, radius) -> new ConnectBrush(settings, radius)),
    OUTLINE_CIRCLE("Circles",
        _ -> new OutlineBrushSettings(),
        OutlineBrush::new),
    OUTLINE_SQUARE("Squares",
        _ -> new OutlineBrushSettings(),
        OutlineBrush::new),
    ONE_PIXEL("One Pixel",
        _ -> new OnePixelBrushSettings(),
        (_, settings, _) -> new OnePixelBrush(settings)) {
        @Override
        public boolean hasRadius() {
            return false;
        }
    };

    public static final String PRESET_KEY = "Brush Type";

    private final String displayName;

    // creates the brushes of this type, supplying them with their settings
    private final BrushCreator brushCreator;

    // null if this brush type has no settings
    private final Function<AbstractBrushTool, ? extends BrushSettings> settingsProvider;

    /**
     * Creates a brush type whose brushes have no settings.
     */
    BrushType(String displayName, DoubleFunction<Brush> brushFactory) {
        this.displayName = displayName;
        this.brushCreator = (_, radius) -> brushFactory.apply(radius);
        this.settingsProvider = null;
    }

    /**
     * Creates a brush type whose brushes have shared, tool-specific settings.
     *
     * @param settingsFactory creates the settings for a given tool
     * @param brushFactory    creates a brush from the settings created by
     *                        the settingsFactory (hence the shared type parameter)
     */
    <S extends BrushSettings> BrushType(String displayName,
                                        Function<AbstractBrushTool, S> settingsFactory,
                                        BrushFromSettingsFactory<S> brushFactory) {
        this.displayName = displayName;

        // the settings are shared between the symmetry-brushes of a
        // tool, but they are different between the different tools
        var settingsByTool = new IdentityHashMap<AbstractBrushTool, S>();
        Function<AbstractBrushTool, S> provider =
            tool -> settingsByTool.computeIfAbsent(tool, settingsFactory);

        this.settingsProvider = provider;
        this.brushCreator = (tool, radius) ->
            brushFactory.create(this, provider.apply(tool), radius);
    }

    public Brush createBrush(AbstractBrushTool tool, double radius) {
        return brushCreator.create(tool, radius);
    }

    @Override
    public String toString() {
        return displayName;
    }

    public boolean hasRadius() {
        return true; // overridden if necessary
    }

    public boolean hasSettings() {
        return settingsProvider != null;
    }

    /**
     * Returns the settings tied to the {@link AbstractBrushTool} and {@link BrushType} combination.
     */
    public BrushSettings getSettings(AbstractBrushTool tool) {
        assert hasSettings();

        return settingsProvider.apply(tool);
    }

    @FunctionalInterface
    private interface BrushCreator {
        Brush create(AbstractBrushTool tool, double radius);
    }

    /**
     * Creates a brush from its (already available) settings.
     * The first parameter is the brush type being created.
     */
    @FunctionalInterface
    private interface BrushFromSettingsFactory<S extends BrushSettings> {
        Brush create(BrushType type, S settings, double radius);
    }
}
