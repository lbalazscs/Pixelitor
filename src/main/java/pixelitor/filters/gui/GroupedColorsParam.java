/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.filters.gui;

import pixelitor.colors.Colors;
import pixelitor.utils.Rnd;
import pixelitor.utils.Utils;

import javax.swing.*;
import java.awt.Color;
import java.util.Arrays;
import java.util.StringTokenizer;

/**
 * Represents two or more color selectors that are grouped together in the GUI.
 */
public class GroupedColorsParam extends AbstractFilterParam implements Linkable {
    private Color[] colors;
    private final Color[] defaultColors;
    private final String[] names;

    private final ButtonModel linkedModel;
    private final boolean linkedByDefault;

    private final TransparencyMode transparencyMode;

    public GroupedColorsParam(String name,
                              String firstName, Color firstColor,
                              String secondName, Color secondColor,
                              TransparencyMode transparencyMode,
                              boolean linkable, boolean linked) {
        this(name,
            new String[]{firstName, secondName},
            new Color[]{firstColor, secondColor},
            transparencyMode, linkable, linked);
    }

    public GroupedColorsParam(String name,
                              String[] names, Color[] colors,
                              TransparencyMode transparencyMode,
                              boolean linkable, boolean linked) {
        super(name, RandomizeMode.ALLOW_RANDOMIZE);

        this.names = names;
        this.transparencyMode = transparencyMode;

        this.colors = colors;

        // it can be the same reference because any color
        // change will change the colors array reference
        this.defaultColors = colors;

        this.linkedModel = linkable ? new JToggleButton.ToggleButtonModel() : null;
        this.linkedByDefault = linked;
        setLinked(linked);

        // if linked, the default colors must be the same
        assert !linked || Utils.allElementsEqual(defaultColors);
    }

    @Override
    public JComponent createGUI() {
        var gui = new GroupedColorsParamGUI(this);
        paramGUI = gui;
        guiCreated();

        return gui;
    }

    public int getNumColors() {
        return colors.length;
    }

    public Color getColor(int index) {
        return colors[index];
    }

    /**
     * Returns the color at the given index in the format expected by G'MIC.
     */
    public String getColorStr(int index) {
        return Colors.formatGMIC(colors[index]);
    }

    public String getName(int index) {
        return names[index];
    }

    /**
     * Sets a color, applying it to all swatches if they are linked.
     */
    public void setColor(Color newColor, int index, boolean trigger) {
        Color[] newColors = colors.clone();
        if (isLinked()) {
            Arrays.fill(newColors, newColor);
        } else {
            newColors[index] = newColor;
        }
        setColors(newColors, trigger);
    }

    /**
     * Sets all colors in the group at once.
     */
    public void setColors(Color[] newColors, boolean trigger) {
        if (Arrays.equals(colors, newColors)) {
            return;
        }
        colors = newColors;

        if (paramGUI != null) {
            paramGUI.updateGUI();
        }

        if (trigger && adjustmentListener != null) {
            adjustmentListener.paramAdjusted();
        }
    }

    @Override
    public ButtonModel getLinkedModel() {
        return linkedModel;
    }

    @Override
    public String createLinkedToolTip() {
        if (names.length == 2) {
            return "<html>Whether the <b>%s</b> and <b>%s</b> colors are the same"
                .formatted(names[0], names[1]);
        } else {
            return "Whether the colors are the same";
        }
    }

    @Override
    public ParamState<?> copyState() {
        return new GroupedColorsParamState(colors, isLinked());
    }

    @Override
    public void loadStateFrom(ParamState<?> state, boolean updateGUI) {
        var newState = (GroupedColorsParamState) state;
        setLinked(newState.linked());

        if (updateGUI) {
            setColors(newState.colors(), false);
        } else {
            // if there's no GUI, just update the model's internal state
            this.colors = newState.colors();
        }
    }

    @Override
    public void loadStateFrom(String savedValue) {
        StringTokenizer st = new StringTokenizer(savedValue, ",");
        setLinked(Boolean.parseBoolean(st.nextToken()));

        Color[] newColors = new Color[colors.length];
        for (int i = 0; i < newColors.length; i++) {
            newColors[i] = Colors.fromHTMLHex(st.nextToken());
        }
        setColors(newColors, false);
    }

    public boolean allowTransparency() {
        return transparencyMode.allowTransparency();
    }

    @Override
    public boolean isAnimatable() {
        return true;
    }

    @Override
    public String getParamValue() {
        return Colors.formatForDebugging(colors);
    }

    @Override
    public boolean hasDefault() {
        return Arrays.equals(colors, defaultColors);
    }

    @Override
    protected void doRandomize() {
        int numColors = colors.length;
        boolean randomAlpha = transparencyMode.randomizeTransparency();
        Color firstNewColor = Rnd.createRandomColor(randomAlpha);

        Color[] newColors = new Color[numColors];
        if (isLinked()) {
            Arrays.fill(newColors, firstNewColor);
        } else {
            newColors[0] = firstNewColor;
            for (int i = 1; i < numColors; i++) {
                newColors[i] = Rnd.createRandomColor(randomAlpha);
            }
        }
        setColors(newColors, false);
    }

    @Override
    public void reset(boolean trigger) {
        setLinked(linkedByDefault);
        setColors(defaultColors, trigger);
    }

    @Override
    public String getResetToolTip() {
        return "Reset the color values";
    }

    /**
     * The state of a {@link GroupedColorsParam}.
     */
    public record GroupedColorsParamState(Color[] colors,
                                          boolean linked) implements ParamState<GroupedColorsParamState> {
        @Override
        public GroupedColorsParamState interpolate(GroupedColorsParamState endState, double progress) {
            int numColors = colors.length;
            Color[] interpolatedColors = new Color[numColors];
            for (int i = 0; i < numColors; i++) {
                interpolatedColors[i] = Colors.interpolateRGB(
                    colors[i], endState.colors[i], progress);
            }
            // the 'linked' state can't be interpolated
            return new GroupedColorsParamState(interpolatedColors, linked());
        }

        @Override
        public String toSaveString() {
            StringBuilder sb = new StringBuilder();
            sb.append(linked);
            for (Color color : colors) {
                sb.append(',');
                sb.append(Colors.toHTMLHex(color, true));
            }
            return sb.toString();
        }
    }
}
