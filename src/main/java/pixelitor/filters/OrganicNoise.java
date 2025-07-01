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

package pixelitor.filters;

import com.jhlabs.image.PointFilter;
import pd.fastnoise.FastNoiseLite;
import pd.fastnoise.FastNoiseLite.*;
import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.CompositeParam;
import pixelitor.filters.gui.EnumParam;
import pixelitor.filters.gui.RangeParam;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.function.Predicate;

import static pixelitor.gui.GUIText.ZOOM;

public class OrganicNoise extends ParametrizedFilter {
    public static final String NAME = "Organic Noise";

    private final EnumParam<NoiseType> type
        = new EnumParam<>("Noise Type", NoiseType.class);

    private final EnumParam<FractalType> fractalType
        = new EnumParam<>("Fractal Type", FractalType.class);
    private final RangeParam octaves
        = new RangeParam("Octaves", 1, 3, 10);
    private final RangeParam lacunarity
        = new RangeParam("Lacunarity", 1, 200, 500);
    private final RangeParam gain
        = new RangeParam("Gain", 1, 50, 200);
    private final RangeParam weightedStrength
        = new RangeParam("Weighted Strength", 0, 0, 100);
    private final RangeParam pingPongStrength
        = new RangeParam("Ping-pong Strength", 1, 200, 500);

    private final EnumParam<CellularDistanceFunction> cdf
        = new EnumParam<>("Cellular Distance Function", CellularDistanceFunction.class);
    private final RangeParam cellularJitter
        = new RangeParam("Cellular Randomness", 0, 100, 100);
    private final EnumParam<CellularReturnType> cellularReturnType
        = new EnumParam<>("Cellular Return Type", CellularReturnType.class);

    private final RangeParam zoom = new RangeParam(ZOOM, 1, 100, 1000);
    //    private final GroupedColorsParam colors = new GroupedColorsParam("Colors",
//        "First", Color.BLACK,
//        "Second", Color.WHITE,
//        NO_TRANSPARENCY, false, false);
    private final AngleParam angle = new AngleParam("Rotate", 0);

    private final EnumParam<DomainWarpType> domainWarpType
        = new EnumParam<>("Distortion Type", DomainWarpType.class);
    private final RangeParam domainWarpAmp
        = new RangeParam("Distortion Amount", 0, 100, 200);
    private final EnumParam<DomainWarpFractalType> domainWarpFractalType
        = new EnumParam<>("Distortion Fractal", DomainWarpFractalType.class);

    private Impl impl;

    public OrganicNoise() {
        super(false);

        zoom.setPresetKey("Zoom");

        Predicate<DomainWarpType> domainWarpEnabled = t -> t != DomainWarpType.None;
        domainWarpType.setupEnableOtherIf(domainWarpAmp, domainWarpEnabled);
        domainWarpType.setupEnableOtherIf(domainWarpFractalType, domainWarpEnabled);

        CompositeParam cellDetails = new CompositeParam("Cellular Details",
            cdf, cellularJitter, cellularReturnType);
        type.setupDisableOtherIf(cellDetails, t -> t != NoiseType.Cellular);

        Predicate<FractalType> fractalEnabled = t -> t != FractalType.None;
        fractalType.setupEnableOtherIf(octaves, fractalEnabled);
        fractalType.setupEnableOtherIf(lacunarity, fractalEnabled);
        fractalType.setupEnableOtherIf(gain, fractalEnabled);
        fractalType.setupEnableOtherIf(weightedStrength, fractalEnabled);
        fractalType.setupEnableOtherIf(pingPongStrength, t -> t == FractalType.PingPong);

        initParams(
            type,
            cellDetails,

            new CompositeParam("Fractal",
                fractalType,
                octaves,
                lacunarity,
                gain,
                weightedStrength,
                pingPongStrength
            ),

            zoom,
            angle,
//            colors,

            new CompositeParam("Distort",
                domainWarpType,
                domainWarpAmp,
                domainWarpFractalType
            )
        ).withAction(paramSet.createReseedAction(this::setSeed));
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        if (impl == null) {
            impl = new Impl();
        }

        impl.setCenter(src.getWidth() / 2.0f, src.getHeight() / 2.0f);
        impl.setScale((float) zoom.getPercentage());
        impl.setAngle(angle.getValueInRadians());
//        impl.setColors(colors.getColor(0), colors.getColor(1));
        impl.setNoiseType(type.getSelected());
        impl.setFractalType(fractalType.getSelected());
        impl.setOctaves(octaves.getValue());
        impl.setFractalLacunarity((float) lacunarity.getPercentage());
        impl.setFractalGain((float) gain.getPercentage());
        impl.setFractalWeightedStrength((float) weightedStrength.getPercentage());
        impl.setFractalPingPongStrength((float) pingPongStrength.getPercentage());

        impl.setCellularDistanceFunction(cdf.getSelected());
        impl.setCellularJitter((float) cellularJitter.getPercentage());
        impl.setCellularReturnType(cellularReturnType.getSelected());

        boolean doDomainWarp = domainWarpType.getSelected() != DomainWarpType.None;
        impl.setDomainWarp(doDomainWarp);
        if (doDomainWarp) {
            impl.setDomainWarpType(domainWarpType.getSelected());
            impl.setDomainWarpAmp(domainWarpAmp.getValueAsFloat());
            impl.setDomainWarpFractalType(domainWarpFractalType.getSelected());
        }

        return impl.filter(src, dest);
    }

    private void setSeed(long seed) {
        if (impl != null) {
            impl.setSeed(seed);
        }
    }

    @Override
    public boolean supportsUserPresets() {
        return false;
    }

    @Override
    public boolean canBeSmart() {
        return false;
    }
}

class Impl extends PointFilter {
    private final FastNoiseLite fastNoise;
    private boolean domainWarp;
    private float scale;
    private float cx;
    private float cy;

    private boolean rotate;
    private double cos;
    private double sin;

    protected Impl() {
        super(OrganicNoise.NAME);
        fastNoise = new FastNoiseLite();
    }

    @Override
    public int processPixel(int x, int y, int rgb) {
        double sampleX = (x - cx) / scale;
        double sampleY = (y - cy) / scale;

        if (rotate) {
            double newX = cos * sampleX + sin * sampleY;
            double newY = -sin * sampleX + cos * sampleY;
            sampleX = newX;
            sampleY = newY;
        }

        if (domainWarp) {
            Vector2 vec = new Vector2(sampleX, sampleY);
            fastNoise.DomainWarp(vec);
            sampleX = vec.x;
            sampleY = vec.y;
        }

        // GetNoise should return floats between -1 and 1, but has errors
        float noise = fastNoise.GetNoise(sampleX, sampleY);
        int v = (int) (127.5 * (noise + 1.0f));
        if (v < 0) {
            v = 0;
        }
        if (v > 255) {
            v = 255;
        }
        return 0xFF000000 | (v << 16) | (v << 8) | v;
    }

    public void setNoiseType(NoiseType type) {
        fastNoise.SetNoiseType(type);
    }

    public void setFractalType(FractalType type) {
        fastNoise.SetFractalType(type);
    }

    public void setCellularDistanceFunction(CellularDistanceFunction cdf) {
        fastNoise.SetCellularDistanceFunction(cdf);
    }

    public void setCellularJitter(float cj) {
        fastNoise.SetCellularJitter(cj);
    }

    public void setCellularReturnType(CellularReturnType type) {
        fastNoise.SetCellularReturnType(type);
    }

    public void setDomainWarpType(DomainWarpType type) {
        fastNoise.SetDomainWarpType(type);
    }

    public void setDomainWarpAmp(float dwa) {
        fastNoise.SetDomainWarpAmp(dwa);
    }

    public void setDomainWarp(boolean domainWarp) {
        this.domainWarp = domainWarp;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public void setColors(Color colorA, Color colorB) {
        // TODO clarify why is Clouds premultiplying
    }

    public void setDomainWarpFractalType(DomainWarpFractalType fractalType) {
        fastNoise.SetDomainFractalType(fractalType);
    }

    public void setOctaves(int octaves) {
        fastNoise.SetFractalOctaves(octaves);
    }

    public void setFractalLacunarity(float lacunarity) {
        fastNoise.SetFractalLacunarity(lacunarity);
    }

    public void setFractalGain(float gain) {
        fastNoise.SetFractalGain(gain);
    }

    public void setFractalWeightedStrength(float weightedStrength) {
        fastNoise.SetFractalWeightedStrength(weightedStrength);
    }

    public void setFractalPingPongStrength(float pingPongStrength) {
        fastNoise.SetFractalPingPongStrength(pingPongStrength);
    }

    public void setCenter(float cx, float cy) {
        this.cx = cx;
        this.cy = cy;
    }

    public void setAngle(double angle) {
        if (angle != 0) {
            rotate = true;
            cos = Math.cos(angle);
            sin = Math.sin(angle);
        }
    }

    public void setSeed(long seed) {
        fastNoise.SetSeed((int) (seed % Integer.MAX_VALUE));
    }
}